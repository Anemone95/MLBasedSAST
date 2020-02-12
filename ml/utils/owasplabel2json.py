#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2019 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""
import json
import logging
import pathlib


def owasplabel2json(expected_csv, slice_dir, output_json):
    res = []
    table = {}
    is_real_num = 0
    not_real_num = 0
    with open(expected_csv, 'r') as f:
        for e in f.readlines():
            if e.startswith('#'):
                continue
            test_name, category, real, cwe = e.replace('\n', '').split(',')[:4]
            table[test_name] = True if real.lower() == 'true' else False
    for each_file in pathlib.Path(slice_dir).glob('*.json'):
        label = {"flowHash": None, "isSafe": None, "flow": None}
        try:
            with open(each_file, 'r') as f:
                slice = json.load(f)
        except json.decoder.JSONDecodeError:
            logging.warning("Parse {} error".format(each_file))
            continue
        label["flowHash"] = each_file.name.split("-")[-1].split(".")[0]
        label["flow"] = slice["flow"]
        test_name = slice["flow"]["entry"]["clazz"].split(".")[-1]
        if test_name not in table:
            continue
        label["isSafe"] = (not table[test_name])
        res.append(label)
        if table[test_name]:
            is_real_num += 1
        else:
            not_real_num += 1
    print("Get records:", len(res), ",true records:", is_real_num, ",false records:", not_real_num)
    with open(output_json, 'w') as f:
        json.dump(res, f, indent=4)


if __name__ == '__main__':
    owasplabel2json('../data/expectedresults-1.1.csv',
                    '../data/slice/benchmark1.1',
                    '../data/label/benchmark1.1/label.json')
