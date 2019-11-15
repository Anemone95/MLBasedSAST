from __future__ import print_function

import pickle

import numpy
import sys
import theano
import time
import logging
from collections import OrderedDict
from itertools import tee
import theano.tensor as tensor
from theano import config
from theano.sandbox.rng_mrg import MRG_RandomStreams as RandomStreams
import _theano.dataloader as dataloader
import settings
from _theano.tokenizer import *

# Set the random number generators' seeds for consistency
SEED = 5827
numpy.random.seed(SEED)


def prepare_data(seqs, labels, maxlen=None):
    lengths = [len(s) for s in seqs]

    if maxlen is not None:
        new_seqs = []
        new_labels = []
        new_lengths = []
        for l, s, y in zip(lengths, seqs, labels):
            if l < maxlen:
                new_seqs.append(s)
                new_labels.append(y)
                new_lengths.append(l)
        lengths = new_lengths
        labels = new_labels
        seqs = new_seqs

        if len(lengths) < 1:
            return None, None, None

    n_samples = len(seqs)
    maxlen = numpy.max(lengths)

    x = numpy.zeros((maxlen, n_samples)).astype('int64')
    x_mask = numpy.zeros((maxlen, n_samples)).astype(theano.config.floatX)
    for idx, s in enumerate(seqs):
        x[:lengths[idx], idx] = s
        x_mask[:lengths[idx], idx] = 1.

    return x, x_mask, labels


def numpy_floatX(data):
    return numpy.asarray(data, dtype=config.floatX)


def get_minibatches_idx(n, minibatch_size, shuffle=False):
    # Used to shuffle the dataset at each iteration.

    idx_list = numpy.arange(n, dtype="int32")

    if shuffle:
        numpy.random.shuffle(idx_list)

    minibatches = []
    minibatch_start = 0
    for i in range(n // minibatch_size):
        minibatches.append(idx_list[minibatch_start:minibatch_start + minibatch_size])
        minibatch_start += minibatch_size

    if (minibatch_start != n):  # Make a minibatch out of what is left
        minibatches.append(idx_list[minibatch_start:])

    return zip(range(len(minibatches)), minibatches)


def zipp(params, tparams):  # When we reload the model. Needed for the GPU stuff.
    for kk, vv in params.items():
        tparams[kk].set_value(vv)


def unzip(zipped):  # When we pickle the model. Needed for the GPU stuff.
    new_params = OrderedDict()
    for kk, vv in zipped.items():
        new_params[kk] = vv.get_value()
    return new_params


def dropout_layer(state_before, use_noise, trng):
    proj = tensor.switch(use_noise,
                         (state_before * trng.binomial(state_before.shape, p=0.5, n=1, dtype=state_before.dtype)),
                         state_before * 0.5)
    return proj


def _p(pp, name):
    return '%s_%s' % (pp, name)


def init_params(options):
    # Global (not LSTM) parameter. For the embeding and the classifier.
    params = OrderedDict()
    # embedding
    randn = numpy.random.rand(options['n_words'], options['dim_proj'])
    params['Wemb'] = (0.01 * randn).astype(config.floatX)
    encoder = 'lstm'
    params = get_layer(encoder)[0](options, params, prefix=encoder)
    # classifier
    params['U'] = 0.01 * numpy.random.randn(options['dim_proj'], options['ydim']).astype(config.floatX)
    params['b'] = numpy.zeros((options['ydim'],)).astype(config.floatX)

    return params


def load_params(path, params):
    pp = numpy.load(path)
    for kk, vv in params.items():
        if kk not in pp:
            raise Warning('%s is not in the archive' % kk)
        params[kk] = pp[kk]
    return params


def init_tparams(params):
    tparams = OrderedDict()
    for kk, pp in params.items():
        tparams[kk] = theano.shared(params[kk], name=kk)
    return tparams


def get_layer(name):
    fns = layers[name]
    return fns


def ortho_weight(ndim):
    W = numpy.random.randn(ndim, ndim)
    u, s, v = numpy.linalg.svd(W)
    return u.astype(config.floatX)


def param_init_lstm(options, params, prefix='lstm'):
    # Init the LSTM parameter:

    W = numpy.concatenate([ortho_weight(options['dim_proj']),
                           ortho_weight(options['dim_proj']),
                           ortho_weight(options['dim_proj']),
                           ortho_weight(options['dim_proj'])], axis=1)
    params[_p(prefix, 'W')] = W
    U = numpy.concatenate([ortho_weight(options['dim_proj']),
                           ortho_weight(options['dim_proj']),
                           ortho_weight(options['dim_proj']),
                           ortho_weight(options['dim_proj'])], axis=1)
    params[_p(prefix, 'U')] = U
    b = numpy.zeros((4 * options['dim_proj'],))
    params[_p(prefix, 'b')] = b.astype(config.floatX)

    return params


def lstm_layer(tparams, state_below, options, prefix='lstm', mask=None, verbose=False):
    nsteps = state_below.shape[0]
    if state_below.ndim == 3:
        n_samples = state_below.shape[1]
    else:
        n_samples = 1

    assert mask is not None

    def _slice(_x, n, dim):
        if _x.ndim == 3:
            return _x[:, :, n * dim:(n + 1) * dim]
        return _x[:, n * dim:(n + 1) * dim]

    def _step(m_, x_, h_, c_):
        preact = tensor.dot(h_, tparams[_p(prefix, 'U')])
        preact += x_

        i = tensor.nnet.sigmoid(_slice(preact, 0, options['dim_proj']))
        f_temp = tensor.nnet.sigmoid(_slice(preact, 1, options['dim_proj']))
        f = f_temp if not verbose else theano.printing.Print('f')(f_temp)
        o = tensor.nnet.sigmoid(_slice(preact, 2, options['dim_proj']))
        c = tensor.tanh(_slice(preact, 3, options['dim_proj']))
        c = f * c_ + i * c

        c = m_[:, None] * c + (1. - m_)[:, None] * c_

        h = o * tensor.tanh(c)
        h = m_[:, None] * h + (1. - m_)[:, None] * h_
        if verbose:
            c_printed = theano.printing.Print('c')(c)
            h_printed = theano.printing.Print('h')(h)
            return h_printed, c_printed
        return h, c

    state_tmp = state_below if not verbose else theano.printing.Print('state_below')(state_below)
    state_tmp = (tensor.dot(state_tmp, tparams[_p(prefix, 'W')]) + tparams[_p(prefix, 'b')])

    dim_proj = options['dim_proj']
    rval, temp_updates = theano.scan(_step,
                                     sequences=[mask, state_tmp],
                                     outputs_info=[tensor.alloc(numpy_floatX(0.), n_samples, dim_proj),
                                                   tensor.alloc(numpy_floatX(0.), n_samples, dim_proj)],
                                     name=_p(prefix, '_layers'), n_steps=nsteps)
    # print(temp_updates)
    return rval[0]  # , updates


# ff: Feed Forward (normal neural net), only useful to put after lstm
#     before the classifier.
layers = {'lstm': (param_init_lstm, lstm_layer)}


def sgd(lr, tparams, grads, x, mask, y, cost):
    gshared = [theano.shared(p.get_value() * 0., name='%s_grad' % k) for k, p in tparams.items()]
    gsup = [(gs, g) for gs, g in zip(gshared, grads)]
    # Function that computes gradients for a mini-batch, but do not updates the weights.
    f_grad_shared = theano.function([x, mask, y], cost, updates=gsup, name='sgd_f_grad_shared')

    pup = [(p, p - lr * g) for p, g in zip(tparams.values(), gshared)]

    # Function that updates the weights from the previously computed gradient.
    f_update = theano.function([lr], [], updates=pup, name='sgd_f_update')

    return f_grad_shared, f_update


def rmsprop(lr, tparams, grads, x, mask, y, cost):
    zipped_grads = [theano.shared(p.get_value() * numpy_floatX(0.), name='%s_grad' % k) for k, p in tparams.items()]
    running_grads = [theano.shared(p.get_value() * numpy_floatX(0.), name='%s_rgrad' % k) for k, p in tparams.items()]
    running_grads2 = [theano.shared(p.get_value() * numpy_floatX(0.), name='%s_rgrad2' % k) for k, p in tparams.items()]

    zgup = [(zg, g) for zg, g in zip(zipped_grads, grads)]
    rgup = [(rg, 0.95 * rg + 0.05 * g) for rg, g in zip(running_grads, grads)]
    rg2up = [(rg2, 0.95 * rg2 + 0.05 * (g ** 2)) for rg2, g in zip(running_grads2, grads)]

    f_grad_shared = theano.function([x, mask, y], cost, updates=zgup + rgup + rg2up, name='rmsprop_f_grad_shared')

    updir = [theano.shared(p.get_value() * numpy_floatX(0.), name='%s_updir' % k) for k, p in tparams.items()]
    updir_new = [(ud, 0.9 * ud - 1e-4 * zg / tensor.sqrt(rg2 - rg ** 2 + 1e-4))
                 for ud, zg, rg, rg2 in zip(updir, zipped_grads, running_grads, running_grads2)]
    param_up = [(p, p + udn[1]) for p, udn in zip(tparams.values(), updir_new)]
    f_update = theano.function([lr], [], updates=updir_new + param_up, on_unused_input='ignore',
                               name='rmsprop_f_update')

    return f_grad_shared, f_update


def adam(lr, tparams, grads, x, mask, y, cost):
    zipped_grads = [theano.shared(p.get_value() * numpy_floatX(0.), name='%s_grad' % k) for k, p in tparams.items()]
    running_grads = [theano.shared(p.get_value() * numpy_floatX(0.), name='%s_rgrad' % k) for k, p in tparams.items()]
    running_grads2 = [theano.shared(p.get_value() * numpy_floatX(0.), name='%s_rgrad2' % k) for k, p in tparams.items()]

    zgup = [(zg, g) for zg, g in zip(zipped_grads, grads)]
    rgup = [(rg, 0.9 * rg + 0.1 * g) for rg, g in zip(running_grads, grads)]
    rg2up = [(rg2, 0.999 * rg2 + 0.001 * (g ** 2)) for rg2, g in zip(running_grads2, grads)]
    t = theano.shared(numpy.float32(1))

    f_grad_shared = theano.function([x, mask, y], cost, updates=zgup + rgup + rg2up, name='adam_f_grad_shared')

    updir = [theano.shared(p.get_value() * numpy_floatX(0.), name='%s_updir' % k) for k, p in tparams.items()]
    updir_new = [(ud, -lr * rg / (1 - 0.9 ** t) / (tensor.sqrt(rg2 / (1 - 0.999 ** t)) + 1e-8))
                 for ud, rg, rg2 in zip(updir, running_grads, running_grads2)]
    updir_new.append((t, t + 1))
    param_up = [(p, p + udn[1]) for p, udn in zip(tparams.values(), updir_new)]

    f_update = theano.function([lr], [], updates=updir_new + param_up, on_unused_input='ignore', name='adam_f_update')

    return f_grad_shared, f_update


def adadelta(lr, tparams, grads, x, mask, y, cost):
    zipped_grads = [theano.shared(p.get_value() * numpy_floatX(0.), name='%s_grad' % k) for k, p in tparams.items()]
    running_up2 = [theano.shared(p.get_value() * numpy_floatX(0.), name='%s_rup2' % k) for k, p in tparams.items()]
    running_grads2 = [theano.shared(p.get_value() * numpy_floatX(0.), name='%s_rgrad2' % k) for k, p in tparams.items()]

    zgup = [(zg, g) for zg, g in zip(zipped_grads, grads)]
    rg2up = [(rg2, 0.95 * rg2 + 0.05 * (g ** 2)) for rg2, g in zip(running_grads2, grads)]  # g_tp1
    f_grad_shared = theano.function([x, mask, y], cost, updates=zgup + rg2up, name='adadelta_f_grad_shared')

    updir = [-zg * tensor.sqrt((ru2 + 1e-6) / (rg2 + 1e-6)) for zg, ru2, rg2, g in
             zip(zipped_grads, running_up2, running_grads2, grads)]  # v_tp1

    ru2up = [(ru2, 0.95 * ru2 + 0.05 * (ud ** 2)) for ru2, ud in zip(running_up2, updir)]  # x_tp1
    param_up = [(p, p + ud) for p, ud in zip(tparams.values(), updir)]

    f_update = theano.function([lr], [], updates=ru2up + param_up, on_unused_input='ignore', name='adadelta_f_update')

    return f_grad_shared, f_update


def build_model(tparams, options, verbose):
    trng = RandomStreams(SEED)

    # Used for dropout.
    use_noise = theano.shared(numpy_floatX(0.))

    x_temp = tensor.matrix('x', dtype='int64')
    x = x_temp if not verbose else theano.printing.Print('x')(x_temp)
    mask = tensor.matrix('mask', dtype=config.floatX)
    y = tensor.vector('y', dtype='int64')

    n_timesteps = x.shape[0]
    n_samples = x.shape[1]

    emb = tparams['Wemb'][x.flatten()].reshape([n_timesteps, n_samples, options['dim_proj']])
    encoder = 'lstm'
    proj = get_layer(encoder)[1](tparams, emb, options, prefix=encoder, mask=mask,
                                 verbose=verbose)
    proj = (proj * mask[:, :, None]).sum(axis=0)
    proj = proj / mask.sum(axis=0)[:, None]
    if options['use_dropout']:
        proj = dropout_layer(proj, use_noise, trng)

    pred = tensor.nnet.softmax(tensor.dot(proj, tparams['U']) + tparams['b'])
    f_pred_prob = theano.function([x, mask], pred, name='f_pred_prob')
    f_pred = theano.function([x, mask], pred.argmax(axis=1), name='f_pred')

    off = 1e-8
    if pred.dtype == 'float16':
        off = 1e-6

    cost = -tensor.log(pred[tensor.arange(n_samples), y] + off).mean()

    return use_noise, x, mask, y, f_pred_prob, f_pred, cost


def pred_probs(f_pred_prob, prepare_data, data, iterator, verbose=False):
    """ If you want to use a trained model, this is useful to compute
    the probabilities of new examples.
    """
    n_samples = len(data[0])
    probs = numpy.zeros((n_samples, 2)).astype(config.floatX)

    n_done = 0

    for _, valid_index in iterator:
        x, mask, y = prepare_data([data[0][t] for t in valid_index], numpy.array(tuple(data[1]))[valid_index],
                                  maxlen=None)
        pred_probs = f_pred_prob(x, mask)
        probs[valid_index, :] = pred_probs

        n_done += len(valid_index)
        if verbose:
            print('%d/%d samples classified' % (n_done, n_samples))

    return probs


def pred_error(f_pred, prepare_data, data, iterator, verbose=False):
    """
    Just compute the error
    f_pred: Theano fct computing the prediction
    prepare_data: usual prepare_data for that dataset.
    """
    valid_err = 0
    tp = tn = fp = fn = 0
    for _, valid_index in iterator:
        tmp = [data[0][t] for t in valid_index]
        x, mask, y = prepare_data(tmp, numpy.array(tuple(data[1]))[valid_index], maxlen=None)
        # print("x:")
        preds = f_pred(x, mask)
        targets = numpy.array(tuple(data[1]))[valid_index]
        valid_err += (preds == targets).sum()
        if verbose:
            xLen = len(tmp)
            for i in range(0, xLen):
                print(', '.join((str(v) for v in x[:, i])))
                print(preds[i], targets[i])
        tn += sum(preds * targets)
        tp += sum((1 - preds) * (1 - targets))
        fn += sum(preds * (1 - targets))
        fp += sum((1 - preds) * targets)
    valid_err = 1. - numpy_floatX(valid_err) / len(data[0])
    recall = 0 if (tp + fn) == 0 else 100.0 * tp / (tp + fn)
    precision = 0 if (tp + fp) == 0 else 100.0 * tp / (tp + fp)
    details = '%d\t%d\t%d\t%d\t%.2f\t%.2f' % (tp, tn, fp, fn, recall, precision)
    return valid_err, details


def train_lstm(
        data_dir: str,
        label_dir: str,
        tokenizer: Tokenizer,
        dim_proj=128,  # word embeding dimension and LSTM number of hidden units.
        patience=10,  # Number of epoch to wait before early stop if no progress
        max_epochs=5000,  # The maximum number of epoch to run
        disp_freq=10,  # Display to stdout the training progress every N updates
        decay_c=0.,  # Weight decay for the classifier applied to the U weights.
        lrate=0.001,  # Learning rate for sgd (not used for adadelta and rmsprop)
        optimizer=adam,
        # sgd, adadelta and rmsprop available, sgd very hard to use, not recommanded (probably need momentum and decaying learning rate).
        saveto='lstm_model.npz',  # The best model will be saved there
        validFreq=70,  # Compute the validation error after this number of update.
        maxlen=None,  # Sequence longer then this get ignored
        batch_size=16,  # The batch size during training.
        valid_batch_size=16,  # The batch size used for validation/test set.
        # Parameter for extra option
        noise_std=0.,
        use_dropout=True,  # if False slightly faster, but worst test error
        # This frequently need a bigger model.
        reload_model=False,  # Path to a saved model we want to start from.
        test_size=-1,  # If >0, we keep only this number of test example.
        time_out=1000,  # timeout
):
    # Model options
    model_options = locals().copy()
    logging.info("model options: {}".format(model_options))

    logging.info('Loading data')
    train, valid, test = dataloader.load_data(data_dir, label_dir, tokenizer=tokenizer, valid_portion=0)
    model_options['n_words'] = len(tokenizer.get_token_dict()) + 10
    if test_size > 0:
        # The test set is sorted by size, but we want to keep random
        # size example.  So we must select a random selection of the
        # examples.
        idx = numpy.arange(len(test[0]))
        numpy.random.shuffle(idx)
        idx = idx[:test_size]
        test = ([test[0][n] for n in idx], [test[1][n] for n in idx])
    if not train:
        raise Exception("Dataset could not be zero records")

    ydim = numpy.max(train[1]) + 1

    model_options['ydim'] = ydim

    logging.info('Building model')
    # This create the initial parameters as numpy ndarrays.
    # Dict name (string) -> numpy ndarray
    params = init_params(model_options)

    if reload_model:
        load_params('lstm_model.npz', params)

    # This create Theano Shared Variable from the parameters.
    # Dict name (string) -> Theano Tensor Shared Variable
    # params and tparams have different copy of the weights.
    tparams = init_tparams(params)

    # use_noise is for dropout
    (use_noise, x, mask, y, f_pred_prob, f_pred, cost) = build_model(tparams, model_options, False)

    if decay_c > 0.:
        decay_c = theano.shared(numpy_floatX(decay_c), name='decay_c')
        weight_decay = 0.
        weight_decay += (tparams['U'] ** 2).sum()
        weight_decay *= decay_c
        cost += weight_decay

    f_cost = theano.function([x, mask, y], cost, name='f_cost')

    grads = tensor.grad(cost, wrt=list(tparams.values()))
    f_grad = theano.function([x, mask, y], grads, name='f_grad')

    lr = tensor.scalar(name='lr')
    f_grad_shared, f_update = optimizer(lr, tparams, grads, x, mask, y, cost)

    logging.info("%d training and %d test datapoints" % (len(train[0]), len(test[0])))
    history_errs = []
    best_p = None
    bad_count = 0

    if validFreq == -1:
        validFreq = len(train[0]) // batch_size

    uidx = 0  # the number of update done
    estop = False  # early stop
    start_time = time.time()
    last_best_time = 0.
    total_batches = None
    try:
        eidx = 0
        # 一次训练
        while eidx < max_epochs and time_out > (time.time() - start_time) / 60.0:
            eidx = eidx + 1
            n_samples = 0

            logging.info('Epoch {}'.format(eidx))
            # Get new shuffled index for the training set.
            # 与py2（mangrove）不同，py3迭代器用完不会会从0开始，因此需要复制一个作为后面评估结果试用
            kf, train_batch = tee(get_minibatches_idx(len(train[0]), batch_size, shuffle=True), 2)
            kf_valid = get_minibatches_idx(len(valid[0]), valid_batch_size)
            test_batch = get_minibatches_idx(len(test[0]), valid_batch_size)

            id_of_batches = 0
            for _, train_index in kf:
                id_of_batches += 1
                if id_of_batches % disp_freq == 0:
                    logging.info("Calculating {}/{} batch".format(id_of_batches, total_batches))
                uidx += 1
                use_noise.set_value(1.)

                # Select the random examples for this minibatch
                y = [train[1][t] for t in train_index]
                x = [train[0][t] for t in train_index]

                x, mask, y = prepare_data(x, y)
                n_samples += x.shape[1]

                cost = f_grad_shared(x, mask, y)
                f_update(lrate)

                if numpy.isnan(cost) or numpy.isinf(cost):
                    logging.error('bad cost detected: {}'.format(cost))
                    return 1., 1., 1.
            if total_batches is None:
                total_batches = id_of_batches
            use_noise.set_value(0.)
            train_err, _ = pred_error(f_pred, prepare_data, train, train_batch)
            test_err, details = pred_error(f_pred, prepare_data, test, test_batch)
            valid_err = test_err
            history_errs.append([valid_err, test_err])

            if (best_p is None or valid_err <= numpy.array(history_errs)[:, 0].min()):
                best_p = unzip(tparams)
                bad_counter = 0
                last_best_time = (time.time() - start_time) / 60.0

            now_time = time.time()
            total_time = (now_time - start_time) / 60.0
            # print("{train_file}\t{dim}\t{batch_size}\t{depth}\t{train_acc}\t{test_acc}\t{tp}\t{tn}\t{fp}\t{fn}")
            logging.info("Train Acc: {train_acc}%, Test Acc: {test_acc}%, Recall & Precision: {details}"
                         .format(train_acc=(1 - train_err) * 100, test_acc=(1 - test_err) * 100, details=details))

            # print('%s\t%d\t%d\t%d\t%.2f\t%.2f\t%s\t%d\t%.2f\t%.2f\t%.2f' % (
            #     data_dir, dim_proj, batch_size, 0, , ,
            #     details, eidx, total_time / eidx, last_best_time, total_time))

            if (len(history_errs) > patience and valid_err >= numpy.array(history_errs)[:-patience, 0].min()):
                bad_counter += 1
                if bad_counter > patience:
                    logging.warning('Early Stop!')
                    estop = True
                    break
            if estop:
                break
    except KeyboardInterrupt:
        logging.error("Training interupted")

    end_time = time.time()
    total_time = (end_time - start_time) / 60.0

    if best_p is not None:
        zipp(best_p, tparams)
    else:
        best_p = unzip(tparams)

    use_noise.set_value(0.)
    kf_train_sorted = get_minibatches_idx(len(train[0]), batch_size)
    train_err, _ = pred_error(f_pred, prepare_data, train, kf_train_sorted)

    test_size = len(test[0])
    kf_test = get_minibatches_idx(test_size, test_size)
    test_err, details = pred_error(f_pred, prepare_data, test, kf_test)
    valid_err = test_err

    if saveto:
        with open(saveto + '.args', 'wb') as f:
            pickle.dump(model_options, f)
        numpy.savez(saveto, train_err=train_err, valid_err=valid_err, test_err=test_err, history_errs=history_errs,
                    **best_p)
    print('%s\t%d\t%d\t%d\t%.2f\t%.2f\t%s\t%d\t%.2f\t%.2f\t%.2f' % (
        data_dir, dim_proj, batch_size, 0, (1 - train_err) * 100, (1 - test_err) * 100,
        details, eidx, total_time / eidx, last_best_time, total_time))


# TODO
def test_lstm(reload_model, data_dir, tokenizer, use_dropout=True, n_words=7000, dim_proj=128):
    # Model options
    model_options = locals().copy()
    _, _, test = dataloader.load_data(data_dir, label_dir=None, tokenizer=tokenizer, valid_portion=0, test_portion=1,
                                      update_dict=False)
    model_options['ydim'] = numpy.max(test[1]) + 1
    params = init_params(model_options)
    if reload_model:
        load_params(reload_model, params)
    tparams = init_tparams(params)
    (_, _, _, _, _, f_pred, _) = build_model(tparams, model_options, False)
    test_size = len(test[0])
    kf_test = get_minibatches_idx(test_size, test_size)
    test_err, _ = pred_error(f_pred, prepare_data, test, kf_test, False)  # verbose
    print('TestAcc\n%.2f' % ((1 - test_err) * 100.0))


def load_model(model_npz_path: str, tokenizer_pkl_path: str):
    # with open(model_npz_path + '.args', 'rb') as f:
    #     model_options = pickle.load(f)
    #
    model_options = {'use_dropout': True, 'n_words': 1000, 'dim_proj': 128, "ydim": 2}
    params = init_params(model_options)
    load_params(model_npz_path, params)
    tparams = init_tparams(params)
    (_, _, _, _, _, f_pred, _) = build_model(tparams, model_options, False)
    with open(tokenizer_pkl_path, 'rb') as f:
        tokenizer = pickle.load(f)
    return f_pred, tokenizer


def predict(model, data_dir, tokenizer):
    model_options = {'use_dropout': True, 'n_words': tokenizer, 'dim_proj': 128, "ydim": 2}
    _, _, test = dataloader.load_data(data_dir, label_dir=None, tokenizer=tokenizer, valid_portion=0, test_portion=1,
                                      update_dict=False)
    params = init_params(model_options)
    load_params(model, params)
    tparams = init_tparams(params)
    (_, _, _, _, _, f_pred, _) = build_model(tparams, model_options, False)
    test_size = len(test[0])

    tmp = test[0]
    x, mask, y = prepare_data(tmp, numpy.array(test[1]), maxlen=None)
    # print("x:")
    preds = f_pred(x, mask)
    print(preds)


if __name__ == '__main__':
    logging.basicConfig(format='%(asctime)s : %(levelname)s : %(filename)s : %(funcName)s : %(message)s',
                        level=logging.INFO)
    numpy.set_printoptions(threshold=10000000, precision=2, suppress=True)
    data_dir = sys.argv[1]
    label_dir = sys.argv[2]
    dim = int(sys.argv[3])
    max_epochs = int(sys.argv[4])
    time_out_h = int(sys.argv[5])
    vocab = {}

    model_file = settings.relative_path_from_root('model/theano-lstm.npz')
    if len(sys.argv) > 6 and sys.argv[6] == 'test':
        modelFile = sys.argv[7]
        # TODO
        # test_lstm(dim_proj=dim, n_words=130, dataFile=train_file, reload_model=modelFile, vocab=vocab)
    else:
        tokenizer = Tokenizer()
        train_lstm(data_dir=data_dir,
                   label_dir=label_dir,
                   tokenizer=tokenizer,
                   dim_proj=dim, max_epochs=max_epochs, batch_size=8,
                   saveto=model_file, time_out=time_out_h * 60.0)
