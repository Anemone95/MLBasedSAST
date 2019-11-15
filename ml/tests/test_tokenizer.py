#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2019 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""
from unittest import TestCase
from _theano.tokenizer import *


class TestTokenizer(TestCase):
    def test_encode(self):
        data = ["good very well", "not so good", "hello world"]
        tokenizer = Tokenizer()
        tokenizer.update_dict(data, gt=1)
        res = tokenizer.encode("hello good er")
        self.assertEqual(len(res), 3)
        self.assertIn(2, res)


if __name__ == '__main__':
    pass
