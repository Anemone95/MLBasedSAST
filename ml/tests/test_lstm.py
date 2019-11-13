#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2019 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""

import unittest
import tf_lstm


class TestLSTMMethods(unittest.TestCase):

    def test_load_json(self):
        label_dict = tf_lstm.load_label('../data/label')
        print(tf_lstm.load_json(tf_lstm.simple_text_processing, '../data/slice/benchmark/slice-5888683.json', label_dict))


if __name__ == '__main__':
    unittest.main()
