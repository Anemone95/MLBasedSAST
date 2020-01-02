#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2019 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""
import logging
import time

import torch
from torch.utils.data import DataLoader
import torch.nn as nn
import torch.nn.utils.rnn as rnn_utils
from torch.autograd import Variable
import torch.optim as optim

import preprocessing
from _pytorch.metriccalculator import MetricCalculator
from _pytorch import blstm
from _pytorch import text

## hyper parameter
EPOCH = 20
BATCH_SIZE = 16
HAS_GPU = torch.cuda.is_available()
BASE_LEARNING_RATE = 0.01
EMBEDDING_DIM = 16  # embedding
HIDDEN_DIM = 16  # hidden dim
LABEL_NUM = 2  # number of labels


def adjust_learning_rate(optimizer, epoch):
    lr = BASE_LEARNING_RATE * (0.1 ** (epoch // 10))
    for param_group in optimizer.param_groups:
        param_group['lr'] = lr
    return optimizer


def train():
    dataset = text.TextDataset("data/slice/benchmark1.1", "data/label/benchmark1.1", preprocessing.preprocessing)
    train_data, test_data = dataset.divide(0.9)
    tokenizer = text.Tokenizer(freq_gt=3)
    tokenizer.build_dict(train_data)

    train_loader = DataLoader(train_data,
                              batch_size=BATCH_SIZE,
                              shuffle=True,
                              num_workers=4,
                              collate_fn=tokenizer.tokenize_labeled_batch)

    test_loader = DataLoader(test_data,
                             batch_size=BATCH_SIZE,
                             shuffle=True,
                             num_workers=4,
                             collate_fn=tokenizer.tokenize_labeled_batch)
    ### create model
    model = blstm.BLSTM(embedding_dim=EMBEDDING_DIM, hidden_dim=HIDDEN_DIM,
                        vocab_size=len(tokenizer.dict), label_size=LABEL_NUM, use_gpu=HAS_GPU)
    if HAS_GPU:
        model = model.cuda()

    optimizer = optim.Adam(model.parameters(), lr=BASE_LEARNING_RATE)
    loss_function = nn.CrossEntropyLoss()
    train_loss_ = []

    t = []

    metric = MetricCalculator()
    for epoch in range(EPOCH):
        t.append(time.time())
        optimizer = adjust_learning_rate(optimizer, epoch)

        ## training epoch
        total_acc = 0.0
        total_loss = 0.0
        total = 0.0
        for iter, traindata in enumerate(train_loader):
            train_inputs, lengths, train_labels = traindata
            if len(train_labels.shape) > 1:
                train_labels = torch.squeeze(train_labels)

            if HAS_GPU:
                train_inputs, lengths, train_labels = train_inputs.cuda(), lengths, train_labels.cuda()

            # 清空梯度
            model.zero_grad()
            #
            # 转置，否则需要batchfirst=True
            output = model(train_inputs, lengths)

            loss = loss_function(output, train_labels)
            loss.backward()
            optimizer.step()

            # calc training acc
            _, predicted = torch.max(output.data, 1)
            if HAS_GPU:
                metric.update(predicted.cpu(), train_labels.cpu())
            else:
                metric.update(predicted, train_labels)
            total += len(train_labels)
            total_loss += loss.item()
        train_loss_.append(total_loss / total)
        accuracy, recall, precision = metric.compute(["accuracy", "recall", "precision"])
        print("[Epoch: {cur_epoch}/{total_epoch}] Training Loss: {loss:.3}, "
              "Acc: {acc:.3}, Precision: {precision:.3}, Recall: {recall:.3}, F1: {f1:.3}"
              .format(cur_epoch=epoch, total_epoch=EPOCH, loss=train_loss_[epoch],
                      acc=accuracy, precision=precision, recall=recall,
                      f1=(2 * precision * recall) / (precision + recall)))

        ## Test
        for iter, testdata in enumerate(test_loader):
            test_inputs, lengths, test_labels = testdata
            if len(test_labels.shape) > 1:
                test_labels = torch.squeeze(test_labels)

            if HAS_GPU:
                test_inputs, lengths, test_labels = test_inputs.cuda(), lengths, test_labels.cuda()

            # 清空梯度
            model.zero_grad()
            #
            # 转置，否则需要batchfirst=True
            output = model(test_inputs, lengths)

            # calc testing acc
            _, predicted = torch.max(output.data, 1)
            if HAS_GPU:
                metric.update(predicted.cpu(), test_labels.cpu())
            else:
                metric.update(predicted, test_labels)

        accuracy, recall, precision = metric.compute(["accuracy", "recall", "precision"])

        print("[Epoch: {cur_epoch}/{total_epoch}] Test Acc: {acc:.3},"
              " Precision: {precision:.3}, Recall: {recall:.3}, F1: {f1:.3}"
              .format(cur_epoch=epoch, total_epoch=EPOCH, acc=accuracy, precision=precision, recall=recall,
                      f1=(2 * precision * recall) / (precision + recall)))
        t.append(time.time())
        epoch_time = t[-1] - t[-2]
        eta = (t[-1] - t[0]) / (epoch + 1) * (EPOCH - epoch - 1)
        print("Epoch Time: {0}, ETA: {1}".format(epoch_time, eta))


if __name__ == '__main__':
    logging.basicConfig(format='%(asctime)s : %(levelname)s : %(filename)s : %(funcName)s : %(message)s',
                        level=logging.INFO)
    train()
    pass
