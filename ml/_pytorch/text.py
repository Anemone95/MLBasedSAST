#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2019 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""
import copy
import json
import logging
import pathlib
import os
import random

import torch

import torch.nn.utils.rnn as rnn_utils
from torch.utils.data.dataset import Dataset

PADDING_VALUE = 0


class TextDataset(Dataset):
    def __init__(self, slice_dir, label_dir, text_preprocessing_func):
        self.data = list()
        if os.path.exists(slice_dir):
            self.slice_dir = slice_dir
        else:
            raise FileNotFoundError(slice_dir)
        if os.path.exists(label_dir):
            self.label_dir = label_dir
        else:
            raise FileNotFoundError(label_dir)
        self.text_preprocessing_func = text_preprocessing_func
        for label_file in pathlib.Path(label_dir).glob('**/*.json'):
            with open(str(label_file), 'r') as f:
                labels_json = json.load(f)
                for label in labels_json:
                    is_real = 1 if label["isReal"] else 0
                    self.data.append((label["flowHash"], is_real))

    def divide(self, train_per: float):
        train_num = int(len(self.data) * train_per)
        random.shuffle(self.data)
        train, test = copy.copy(self), copy.copy(self)
        train.data = copy.copy(self.data[:train_num])
        test.data = copy.copy(self.data[train_num:])
        return train, test

    def __getitem__(self, item) -> (str, int):
        ID, LABEL = 0, 1
        with open(os.path.join(self.slice_dir, "slice-{}.json".format(self.data[item][ID])), 'r') as f:
            slice = json.load(f)
        return self.text_preprocessing_func(slice["slice"]), self.data[item][LABEL]

    def __len__(self):
        return len(self.data)


class WordTokenDict:
    def __init__(self, unk_token: str = "<unk>"):
        self._idx2word = [""]  # 0常用做padding
        self._word2idx = {}
        self.unk_token = unk_token
        self.add_word(unk_token)

    def add_word(self, word: str):
        if word not in self._word2idx:
            self._idx2word.append(word)
            self._word2idx[word] = len(self._idx2word) - 1

    def wtoi(self, word: str) -> int:
        return self._word2idx.get(word, self._word2idx[self.unk_token])

    def itow(self, idx: int) -> str:
        return self._idx2word[idx] if idx < len(self._idx2word) else self.unk_token

    def __str__(self):
        return self._word2idx.__str__()

    def __len__(self) -> int:
        return len(self._idx2word)


class Tokenizer:

    def __init__(self, freq_gt: int = 0, token_dict: WordTokenDict = None):
        self.dict = token_dict
        self.freq_gt = freq_gt

    def __str__(self):
        if self.dict:
            return "Tokenizer(" + self.dict.__str__() + ")"
        else:
            return "Tokenizer(None)"

    def build_dict(self, dataset: TextDataset) -> WordTokenDict:
        logging.info("building dict...")
        self.dict = WordTokenDict()
        # generate_dict
        word_freq = {}
        for sentence, label in dataset:
            for word in sentence.split():
                word = word.strip()
                if word in word_freq:
                    word_freq[word] += 1
                else:
                    word_freq[word] = 1
        for word, freq in word_freq.items():
            if freq > self.freq_gt:
                self.dict.add_word(word)
        return self.dict

    def encode(self, string: str) -> [int]:
        if not self.dict:
            raise AttributeError("Must specify sentence_iterator or token_dict")
        words = string.split()
        encoded = map(lambda e: self.dict.wtoi(e.strip()), words)
        return list(encoded)

    def decode(self, int_list: [int]) -> str:
        if not self.dict:
            raise AttributeError("Must specify sentence_iterator or token_dict")
        return " ".join(map(lambda e: self.dict.itow(e), int_list))

    def tokenize_labeled_batch(self, data: [(str, int)]) -> (torch.Tensor, torch.Tensor, torch.Tensor):
        X, Y = 0, 1
        data = list(map(lambda e: (torch.LongTensor(self.encode(e[X])), e[Y]), data))
        data.sort(key=lambda x: len(x[X]), reverse=True)
        data_x = list(map(lambda e: e[X], data))
        data_lengths = torch.LongTensor([len(sq) for sq in data_x])
        data_x = rnn_utils.pad_sequence(data_x, batch_first=True, padding_value=PADDING_VALUE)
        data_y = torch.LongTensor(list(map(lambda e: e[Y], data)))

        return data_x, data_lengths, data_y


if __name__ == '__main__':
    pass
    # for i in DataIterator('./data/train.txt'):
    #     print(i)
    # pass
