#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2019 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""
import preprocessing
import settings
import numpy

from _theano.tokenizer import *


def load_data(data_dir: str, label_dir: str,
              tokenizer: Tokenizer = None,
              valid_portion: int = 0.0,
              test_portion: int = 0.1,
              update_dict=True) -> (list, list, list):
    if label_dir is None:
        label_dict = None
    else:
        label_dict = preprocessing.load_label(label_dir)
    samples = []
    labels = []
    for _slice, label in preprocessing.get_data_generator(preprocessing.preprocessing, data_dir, label_dict)():
        samples.append(_slice)
        labels.append(label)
        # FIXME for debug
        # if len(labels) > 100:
        #     break

    if update_dict:
        tokenizer.update_dict(samples)
    samples = list(map(lambda e: tokenizer.encode(e), samples))

    # split all set into test set
    all_set_x, all_set_y = samples, labels
    n_samples = len(all_set_x)
    sidx = numpy.random.permutation(n_samples)
    n_train = int(numpy.round(n_samples * (1. - test_portion)))
    test_set_x = [all_set_x[s] for s in sidx[n_train:]]
    test_set_y = [all_set_y[s] for s in sidx[n_train:]]
    train_set_x = [all_set_x[s] for s in sidx[:n_train]]
    train_set_y = [all_set_y[s] for s in sidx[:n_train]]

    # split train set into valid set
    # TODO 每次训练都应重新shuffle
    n_samples = len(train_set_x)
    sidx = numpy.random.permutation(n_samples)
    n_train = int(numpy.round(n_samples * (1. - valid_portion)))

    valid_set_x = [train_set_x[s] for s in sidx[n_train:]]
    valid_set_y = [train_set_y[s] for s in sidx[n_train:]]

    real_train_set_x = [train_set_x[s] for s in sidx[:n_train]]
    real_train_set_y = [train_set_y[s] for s in sidx[:n_train]]

    # TODO sort_by_len 感觉没啥用啊 会影响实验结果吗？

    train = (real_train_set_x, real_train_set_y)
    valid = (valid_set_x, valid_set_y)
    test = (test_set_x, test_set_y)

    return train, valid, test


if __name__ == '__main__':
    load_data(settings.relative_path_from_root('data/slice/benchmark'),
              settings.relative_path_from_root('data/label/benchmark'))
