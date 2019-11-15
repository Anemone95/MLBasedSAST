#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2019 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""
import logging
import pickle

import fire
import time
from _theano.theanoLSTM import train_lstm, predict,test_lstm
from _theano.tokenizer import Tokenizer
import settings


class TheanoCommand:
    def train(self, slice_dir: str, label_dir: str, dim: int = 128, epochs: int = 20, timeout: float = 5):
        tokenizer = Tokenizer()
        current_time = time.strftime("%Y-%m-%d-%H-%M", time.localtime())
        model_file = settings.relative_path_from_root('model/theano-lstm-{}.npz'.format(current_time))
        train_lstm(data_dir=slice_dir,
                   label_dir=label_dir,
                   tokenizer=tokenizer,
                   dim_proj=dim, max_epochs=epochs, batch_size=8,
                   saveto=model_file, time_out=timeout * 60.0)
        with open(settings.relative_path_from_root('model/theano-tokenizer-{}.pkl'.format(current_time)), 'wb') as f:
            pickle.dump(tokenizer, f)

    def test(self, model, data_dir, tokenizer_pkl: str):
        with open(tokenizer_pkl, 'rb') as f:
            tokenizer = pickle.load(f)
        test_lstm(model, data_dir, tokenizer)

    def predict(self, model, data_dir, tokenizer_pkl: str):
        with open(tokenizer_pkl, 'rb') as f:
            tokenizer = pickle.load(f)
        predict(model, data_dir, tokenizer)


if __name__ == '__main__':
    logging.basicConfig(format='%(asctime)s : %(levelname)s : %(filename)s : %(funcName)s : %(message)s',
                        level=logging.INFO)
    fire.Fire(TheanoCommand)
