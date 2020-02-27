#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2020 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""
import json
import os
import random


def generate_json(slice_dir: str, label_dir: str):
    isSafe = {}
    hash = {}
    filenames = {}
    for root, dirs, files in os.walk(slice_dir):
        for f in files:
            if f.endswith(".json"):
                with open(os.path.join(root, f), 'r') as file:
                    slice = json.load(file)
                    hash[f] = slice["flowHash"]
                    if slice["flow"]["entry"]["method"] == "bad":
                        isSafe[f] = False
                    elif slice["flow"]["entry"]["method"].startswith("goodG2B") \
                            or slice["flow"]["entry"]["method"] == "good":
                        isSafe[f] = True
                    else:
                        continue
                    filenames[f] = slice["flow"]["point"]["sourceFile"]
    safes = list(filter(lambda e: e[1] is True, isSafe.items()))
    if not os.path.exists(label_dir):
        os.makedirs(label_dir)
    print("FNs num:", len(safes))
    print("TPs num:", len(isSafe) - len(safes))
    data_num = 0
    # 先抓所有FN
    for slice_file, _ in safes:
        label_dict = {"flowHash": hash[slice_file], "isSafe": isSafe[slice_file]}
        label_file_dir = label_dir + "/" + hash[slice_file][:2]
        label_file = os.path.join(label_file_dir, slice_file.replace("slice", "label"))
        if not os.path.exists(label_file_dir):
            os.makedirs(label_file_dir)
        if os.path.exists(label_file):
            print("hash corruption: ", label_file)
        with open(label_file, "w") as f:
            json.dump(label_dict, f)
        data_num += 1

    unsafes = list(filter(lambda e: e[1] is False, isSafe.items()))
    for slice_file, _ in random.sample(unsafes, len(safes)):
        label_dict = {"flowHash": hash[slice_file], "isSafe": isSafe[slice_file]}
        label_file_dir = label_dir + "/" + hash[slice_file][:2]
        label_file = os.path.join(label_file_dir, slice_file.replace("slice", "label"))
        if not os.path.exists(label_file_dir):
            os.makedirs(label_file_dir)
        if os.path.exists(label_file):
            print("hash corruption: ", label_file)
        with open(label_file, "w") as f:
            json.dump(label_dict, f)
        data_num += 1

    # 再抓平衡的TP
    print("Get records:", data_num, ",true records:", data_num - len(safes), ",false records:", len(safes))


if __name__ == '__main__':
    generate_json("G:/slice/juliet1.3", "G:/label/juliet1.3")
