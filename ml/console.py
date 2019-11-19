#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2019 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""
import json
import logging
import pickle

import fire
import time
from _theano.theanoLSTM import train_lstm, predict, test_lstm, load_model
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

    def test(self, model_npz, slice_dir, label_dir):
        model = load_model(model_npz)
        test_lstm(model, slice_dir, label_dir)

    def predict(self, model_npz, slice_json):
        model = load_model(model_npz)
        with open(slice_json, 'r') as f:
            slice = json.load(f)
        print(predict(model, slice["slice"]))


if __name__ == '__main__':
    logging.basicConfig(format='%(asctime)s : %(levelname)s : %(filename)s : %(funcName)s : %(message)s',
                        level=logging.INFO)
    fire.Fire(TheanoCommand)
