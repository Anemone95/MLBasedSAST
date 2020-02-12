#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2020 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""
import time

import django
from celery import shared_task
from .models import *


@shared_task()
def blstm_train(config_name: str):
    from _pytorch import ctrl as blstm
    config = ModelConfig.objects.get(name=config_name)
    current_time = time.strftime("%Y-%m-%d-%H-%M", time.localtime())
    model_file = 'model/pytorch-lstm-{}'.format(current_time)
    token, model, acc, pre, rec, f1 = blstm.train(config.slice_dir, config.label_dir,
                                                  embedding_dim=config.embedding_dim,
                                                  hidden_dim=config.hidden_dim,
                                                  base_learning_rate=config.base_learning_rate,
                                                  early_stop_patience=config.early_stop_patience,
                                                  batch_size=config.batch_size,
                                                  total_epoch=config.epoch,
                                                  word_freq=config.word_freq_gt,
                                                  train_precent=config.train_percent, saveto=model_file)

    token_file = model_file + ".token"
    model_file = model_file + ".pkl"
    model = MLModel.objects.create(token_dict_path=token_file, model_path=model_file, config=config,
                                   accurate=acc, precision=pre, recall=rec, f1=f1)
    return str(model)


if __name__ == '__main__':
    pass
