#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2019 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""


class Tokenizer:

    def __init__(self, token_dict: dict = None):
        self.token_dict = {'UNK': 2} if token_dict is None else token_dict
        self.int2token = {}

    def get_token_dict(self):
        return self.token_dict

    def update_dict(self, samples: [str], gt: int = 5) -> {str: int}:
        # generate_dict
        token_freq = {}
        for sample in samples:
            for token in sample.split():
                token = token.strip()
                if token in token_freq:
                    token_freq[token] += 1
                else:
                    token_freq[token] = 1
        token_id = list(self.token_dict.items())[-1][-1] + 1
        for token, freq in token_freq.items():
            if freq >= gt:
                self.token_dict[token] = token_id
                token_id += 1
        return self.token_dict

    def encode(self, string: str) -> [int]:
        tokens = string.split()
        encoded = map(lambda e: self.token_dict.get(e.strip(), 2), tokens)
        return list(encoded)

    def decode(self, int_list: [int]) -> str:
        if len(self.int2token) < len(self.token_dict):
            self.int2token = {v: k for k, v in self.token_dict.items()}
        return " ".join(map(lambda e: self.int2token[e], int_list))


if __name__ == '__main__':
    pass
