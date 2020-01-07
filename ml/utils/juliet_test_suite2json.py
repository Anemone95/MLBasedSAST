#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2020 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""
import json
import os


def generate_json(slice_dir: str, label_dir: str):
    label = {}
    hash = {}
    filenames = {}
    for root, dirs, files in os.walk(slice_dir):
        for f in files:
            if f.endswith(".json"):
                with open(os.path.join(root, f), 'r') as file:
                    slice = json.load(file)
                    hash[f] = slice["flowHash"]
                    label[f] = True if 'bad' in slice["flow"]["source"]["method"] else False
                    filenames[f] = slice["flow"]["source"]["fileName"]
    FNs = list(filter(lambda e: e[1] is False, label.items()))
    if not os.path.exists(label_dir):
        os.makedirs(label_dir)
    print("FNs num:", len(FNs))
    data_num = 0
    for slice_file, _ in FNs:
        filename = filenames[slice_file]
        for slice_f, _ in filter(lambda e: e[1] == filename, filenames.items()):
            label_dict = {"flowHash": hash[slice_f], "isReal": label[slice_f]}
            label_file = os.path.join(label_dir, slice_f.replace("slice", "label"))
            if os.path.exists(label_file):
                print("hash corruption: ", label_file)
            with open(label_file, "w") as f:
                json.dump(label_dict, f)
            data_num += 1
    print("Get records:", data_num,",true records:", data_num-len(FNs), ",false records:", len(FNs))


if __name__ == '__main__':
    generate_json("data/slice/juliet_test_suite_v1.3", "data/label/juliet_test_suite_v1.3")
