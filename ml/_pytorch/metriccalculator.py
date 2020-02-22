#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2020 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""
import torch
from sklearn.metrics import *


class MetricCalculator:
    def __init__(self):
        self.predict_labels = None
        self.target_labels = None

    def update(self, predict_labels: torch.Tensor, target_labels: torch.Tensor):
        if self.predict_labels is None:
            self.predict_labels = predict_labels
        else:
            self.predict_labels = torch.cat((self.predict_labels, predict_labels), dim=0)
        if self.target_labels is None:
            self.target_labels = target_labels
        else:
            self.target_labels = torch.cat((self.target_labels, target_labels), dim=0)

    def compute(self, choosed_metrics: [str]):
        predict_labels, target_labels = self.predict_labels.numpy(), self.target_labels.numpy()
        metrics = {"accuracy": accuracy_score,
                   "matrix": confusion_matrix,
                   "fp_recall": lambda t, p: recall_score(t, p, pos_label=0),
                   "fp_precision": lambda t, p: precision_score(t, p, pos_label=0),
                   "safe_recall": lambda t, p: recall_score(t, p, pos_label=1),
                   "safe_precision": lambda t, p: precision_score(t, p, pos_label=1)
                   }
        ret = []
        for metric in choosed_metrics:
            ret.append(metrics[metric](target_labels, predict_labels))
        self.predict_labels = None
        self.target_labels = None
        return ret


if __name__ == '__main__':
    pass
