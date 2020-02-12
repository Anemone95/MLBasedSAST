#!/usr/bin/env python3
# coding=utf-8

"""
diff.py

author: Anemone95,x565178035@126.com
date: 2020-02-11 20:03
"""
import json
import pathlib

def diff():
    dir1='ml/data/slice/benchmark1.1'
    s1=set()
    jsons = pathlib.Path(dir1).glob('**/*.json')
    for e in jsons:
        with open(e, 'r') as f:
            s1.add(json.load(f)["flow"]["entry"]["clazz"])
    s2=set()
    dir2='ml/data/slice/benchmark1.1_old'
    jsons = pathlib.Path(dir2).glob('**/*.json')
    for e in jsons:
        with open(e, 'r') as f:
            s2.add(json.load(f)["flow"]["source"]["clazz"])
    print(s2-s1)


if __name__ == '__main__':
    diff()

