#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2020 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""
import json
import logging
import pathlib
import random
import numpy as np
from sklearn.metrics import *


def divide():
    """
    将数据集划分，所有数据打标记，但只有90%标记作为label 保存，其他标记保存为test_name, is_vuln 列表，作为真实情况
    :return:
    """
    expected_csv = 'data/expectedresults-1.1.csv'
    cared_types = ['ldapi', 'sqli', 'xpathi', 'xss', 'cmdi']
    cates = set()
    is_real_vuln = {}
    with open(expected_csv, 'r') as f:
        for e in f.readlines():
            if e.startswith('#'):
                continue
            test_name, category, real, cwe = e.replace('\n', '').split(',')[:4]
            if category in cared_types:
                is_real_vuln[test_name] = True if real.lower() == 'true' else False
    data_set = list(is_real_vuln.keys())
    random.shuffle(data_set)

    print(len(is_real_vuln))
    print(len(list(filter(lambda e:e[1], is_real_vuln.items()))))
    # print(data_set)
    train_set = data_set[:int(len(data_set) * 0.9)]
    test_set = data_set[int(len(data_set) * 0.9):]

    save_label(train_set, is_real_vuln, "data/train.txt")
    save_label(test_set, is_real_vuln, "data/test.txt")
    train_label2json("G:/slice/benchmark1.1", train_set, is_real_vuln, "G:/label/benchmark1.1/label.json")


def train_label2json(slice_dir, train_set, label_table, output_dir):
    clazz_set = set()
    is_real_num, not_real_num = 0, 0
    res = []
    for each_file in pathlib.Path(slice_dir).glob('**/*.json'):
        label = {"flowHash": None, "isSafe": None, "flow": None}
        try:
            with open(each_file, 'r') as f:
                slice = json.load(f)
        except json.decoder.JSONDecodeError:
            logging.warning("Parse {} error".format(each_file))
            continue
        if slice["flow"]["entry"]["method"] != "doPost":
            continue
        if not slice["flow"]["entry"]["clazz"] in clazz_set:
            clazz_set.add(slice["flow"]["entry"]["clazz"])
        else:
            print("Already have " + slice["flow"]["entry"]["clazz"])

        label["flowHash"] = each_file.name.split("-")[-1].split(".")[0]
        label["flow"] = slice["flow"]
        test_name = slice["flow"]["entry"]["clazz"].split(".")[-1]
        if test_name in label_table and test_name in train_set:
            label["isSafe"] = (not label_table[test_name])
            res.append(label)
            if label_table[test_name]:
                is_real_num += 1
            else:
                not_real_num += 1
    print("Get records:", len(res), ",true records:", is_real_num, ",false records:", not_real_num)
    with open(output_dir, 'w') as f:
        json.dump(res, f, indent=4)


def save_label(label_set, label_table, filename):
    with open(filename, 'w') as f:
        for e in label_set:
            f.write("{0},{1}\n".format(e, label_table[e]))


def compare(real_file, predict_file):
    real = []
    spotbugs = []
    predict = []
    testcases = []
    with open('./data/test.txt', 'r') as f:
        for line in f.readlines():
            testcase, res = line.split(",")
            testcases.append(testcase)
            if res.startswith("True"):
                real.append(1)
            else:
                real.append(0)
    predict_dict = {}
    with open('./data/predict.json', 'r') as f:
        bug_instances = json.load(f)
        for bug, p in bug_instances:
            classname = bug["annotationList"][0]["className"]
            classname = classname.split('.')[-1]
            predict_dict[classname] = p
    for testcase in testcases:
        if testcase in predict_dict.keys():
            spotbugs.append(1)
            if predict_dict[testcase]:
                predict.append(0)
            else:
                predict.append(1)

        else:
            spotbugs.append(0)
            predict.append(0)

    # print(predict_dict)

    compute(np.array(real), np.array(spotbugs))
    compute(np.array(real), np.array(predict))


def compute(real, scan):
    """

    :param real: 测试集label，1为漏洞，0为误报
    :param scan: 扫描结果label，1为漏洞，0为误报
    :return:
    """
    accuracy = accuracy_score(real, scan)
    vuln_recall = recall_score(real, scan, pos_label=1)
    vuln_precision = precision_score(real, scan, pos_label=1)
    f1 = (2 * vuln_precision * vuln_recall) / (vuln_precision + vuln_recall)

    print("Acc: {acc:.5}, Recall: {rec:.5}, Precision: {pre:.5}, F1: {f1: .5}".format(acc=accuracy, rec=vuln_recall,
                                                                                      pre=vuln_precision, f1=f1))
    mat = confusion_matrix(real, scan, labels=[1, 0])

    print(mat)


if __name__ == '__main__':
    divide()
    # compare('./data/test.txt', './data/predict.json')
