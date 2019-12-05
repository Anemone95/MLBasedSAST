#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2019 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""
import _theano.dataloader as dataloader
from _theano.tokenizer import *
import settings


def transform(data_dir, label_dir):
    tokenizer = Tokenizer()
    train, valid, test = dataloader.load_data(data_dir, label_dir, tokenizer=tokenizer, valid_portion=0)
    with open(settings.relative_path_from_root('data/mangrove/t-train.txt'), 'w') as f:
        for idx in range(len(train[0])):
            label = 'truepositive' if train[1][idx] == 0 else "falsepositive"
            _slice = tokenizer.decode(train[0][idx])
            f.write('{} :: {}\n'.format(_slice, label))

    with open(settings.relative_path_from_root('data/mangrove/t-test.txt'), 'w') as f:
        for idx in range(len(test[0])):
            label = 'truepositive' if train[1][idx] == 0 else "falsepositive"
            _slice = tokenizer.decode(train[0][idx])
            f.write('{} :: {}\n'.format(_slice, label))

    with open(settings.relative_path_from_root('data/mangrove/dict.txt'), 'w') as f:
        for token, _int in tokenizer.get_token_dict().items():
            f.write('{} {}\n'.format(token, _int))

if __name__ == '__main__':
    transform(settings.relative_path_from_root('data/slice/benchmark1.2'),
              settings.relative_path_from_root('data/label/benchmark1.2'))
