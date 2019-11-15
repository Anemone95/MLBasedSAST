#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2019 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""
import json
import pathlib
import re
import logging

import settings

STRING_PATTERN = re.compile(r'(#\([^);]+\))')
ABSTRACT_LIST = ['BenchmarkTest\d+', 'testcode', 'moresafe', 'safe', 'Safe']

arbitraryThings = ['javax.servlet.http.', 'java.util.', 'java.lang.', '\.\.\.', '_', '\.', '#', ';', ',', '->', '&#\d+',
                   '\d+:']
arbitraryThingsPattern = '|'.join(arbitraryThings)

tokenazables = ['\(', '\)', '\[', '\]', '{', '}', '\$', '\'', ':', 'XML']
tokenazablesPattern = '|'.join(tokenazables)

methodPrefixes = ['null', 'STRING', 'this', 'is', 'to', 'get', 'set', 'add', 'Listener', 'equal', 'basic', 'binary',
                  'bit', 'block', 'android', 'check', 'class', 'close', 'command', 'column', 'convert', 'create', 'new',
                  'file', 'current', 'contain', 'database', 'receive', 'click', 'date', 'decode', 'encode', 'default',
                  'define', 'delete', 'directory', 'domain', 'download', 'dynamic', 'enable', 'local', 'public',
                  'disable', 'execute', 'FIELD', 'field', 'find', 'first', 'focus', 'generate', 'generic', 'has',
                  'include', 'init', 'secure', 'initialize', 'input', 'insert', 'write', 'read', 'window', 'warning',
                  'index', 'verify', 'user', 'url', 'update', 'type', 'send', 'text', 'test', 'target', 'source',
                  'table', 'stream', 'status', 'sql', 'space', 'socket', 'show', 'server', 'select', 'search', 'simple',
                  'scroll', 'schema', 'scan', 'save', 'safe', 'run', 'row', 'resolve', 'request', 'response', 'remove',
                  'register', 'read', 'random', 'commit', 'quote', 'query', 'put', 'provider', 'property', 'process',
                  'print', 'prepare', 'perform', 'pattern', 'path', 'parse', 'parameter', 'xml', 'other', 'ordinal',
                  'open', 'old', 'object', 'next', 'new', 'method', 'menu', 'max', 'min', 'log', 'load', 'list', 'link',
                  'line', 'last', 'do']
prefixesPattern = '|'.join(methodPrefixes);

replaceMap = {'@': ' @ ', ' -0 ': ' 0 ', '= =': ' == ', '! =': ' != ', ' &': ' & ', '& &': '&&', 'a-z': ' a-z ',
              '\d': ' \d ', '?': ' ? ', '\s': ' \s ', '`': ' ` '}


def preprocessing(text: str) -> str:
    text = re.sub(r'#\((\d*)\)', ' \g<1> ', text)
    text = re.sub(r'#\((.)\)', ' \g<1> ', text)
    text = re.sub(r'#\(null\)', ' null ', text)
    text = re.sub(r'compile\(#\(.*\)\)', ' compile ( PATTERN ) ', text)

    # string abstraction
    all_matches = re.findall(STRING_PATTERN, text)  # replace each unique string with STRING #
    counter = 0
    for s in set(all_matches):
        text = text.replace(s, 'STRING ' + str(counter) + ' ')
        counter = counter + 1

    lines = text.split('\n')
    sample = ''
    for line in lines:
        if line.strip() == '' or re.match(r'\[([0-9]+, )*[0-9]+\]', line):
            continue
        # try:
        nid, kind, operation, ntype, value, edges = [s.strip() for s in line.strip().split('::')]
        sample += parseSDGNodeValue(ntype + ' ' + value, ABSTRACT_LIST) + ' :: '  # .lower()
        # except Exception as e:
        #     logging.warning('Exception: {} in {}'.format(e, line))
    return sample.replace('::', ' ')


def parseSDGNodeValue(node_value, to_abstract_list):
    abstractionPattern = '|'.join(to_abstract_list)

    value = re.sub(r'(v[0-9]+).([a-zA-Z]+)', '\g<1> \g<2>', node_value)  # v17.getNextException
    value = re.sub(r' \.([a-zA-Z]+)', ' \g<1> ', value)  # .getNextException
    value = re.sub(r'L([a-zA-Z0-9]+(/[a-zA-Z0-9])+)', '\g<1>',
                   value)  # Lorg/h2/message/DbException to org/h2/message/DbException
    value = value.replace('/', '.')  # org/h2/message/DbException to org.h2.message.DbException
    value = re.sub(r'<([a-zA-Z0-9]+)>?', ' \g<1> ', value)
    value = re.sub(r'(' + arbitraryThingsPattern + ')', ' ', value)
    value = re.sub(r'(' + tokenazablesPattern + ')', ' \g<1> ', value)

    # abraction
    value = re.sub(r'(' + abstractionPattern + ')', ' ARBITRARY ', value)  # abracted things

    # extraction
    value = re.sub(r'v(\d+)', 'variable \g<1> ', value)  # v1 -> variable 1
    value = re.sub(r'( |^)(' + prefixesPattern + ')', ' \g<2> ', value)
    value = re.sub(r'([A-Z]+[a-z0-9]*)', ' \g<1> ', value)  # StringBuilder -> String Builder
    value = re.sub(r'([A-Z]+)([A-Z]+[a-z0-9]+)', '\g<1> \g<2>', value)  # PDFConfigReader -> PDF Config Reader
    value = re.sub(r'( [A-Za-z]+)(\d+)\s', ' \g<1> \g<2> ', value)  # file1 -> file 1 Binary180 -> Binary 180

    # number abstraction
    value = re.sub(r'( |^)-?([0-9]+)[eE]-([0-9])($| )', ' NSMALL ',
                   value)  # numbers in scientific notation e.g. 0.12e-43
    value = re.sub(r'( |^)\d\d\d+($| )', ' N3P ', value)  # 3+ digit numbers
    value = re.sub(r'( |^)-\d\d\d+($| )', ' NN3P ', value)  # 3+ digit negative numbers

    value = re.sub(r'key[a-zA-Z0-9\-]+', ' key ', value)  # remove key56988 Owasp specific pattern

    value = re.sub(r'([\w\']+)>', ' \g<1> ', value)
    value = re.sub(r'(:@?|\w+)<', ' \g<1> ', value)
    value = re.sub(r'(\w+):', ' \g<1> ', value)
    value = re.sub(r'(\w+)-\s', ' \g<1> ', value)
    value = re.sub(r'([a-zA-Z]+)-', ' \g<1> ', value)

    for key, val in replaceMap.items():
        value = value.replace(key, val)
        value = re.sub(r'\s\s+', ' ', value)

    return value.rstrip()


def load_label(base_dir):
    hash2label = {}
    for each_file in pathlib.Path(base_dir).glob('**/*.json'):
        with open(each_file, 'r') as f:
            labeled_slices = json.load(f)
        for each_slice in labeled_slices:
            hash2label[each_slice["sliceHash"]] = 1 if each_slice["isReal"] else 0
    return hash2label


def get_data_generator(text_processing_func, data_dir: str, label_dict: dict):
    def gen() -> (str, int):
        for json_file in pathlib.Path(data_dir).glob('**/*.json'):
            with open(json_file, 'r') as f:
                slice_json = json.load(f)
            slice_hash = json_file.name.split('-')[-1].split('.')[0]
            if len(slice_json["slice"]) == 0:
                continue
            try:
                slice_text = text_processing_func(slice_json["slice"])
            except IndexError:
                print(slice_json["slice"])
                print()
                continue
            if label_dict is None:
                # 无label时只能预测
                label = -1
            elif slice_hash in label_dict:
                label = label_dict[slice_hash]
            else:
                logging.warning("{} not in label".format(json_file))
                continue
            yield slice_text, label

    return gen


def parseDataFile(dataFile):
    print('reading ' + dataFile + ' dataFile...')
    file_content = open(dataFile, 'r')
    samples = []
    labels = []
    for line in file_content:
        prog = None
        if 'truepositive' in line:
            labels.append(0)
            prog = re.sub(' truepositive', '', line)
        else:
            labels.append(1)
            prog = re.sub(' falsepositive', '', line)
        samples.append(prog)
    print('done...')
    return samples, labels


def get_magrove_generator(text_processing_func, data_dir: str, label_dict: dict):
    owasp_train_file = r'D:\Store\document\all_my_work\CZY\bishe\mangrove_old\lstm\data\extraction\owasp-slice-train-2.txt'
    owasp_test_file = r'D:\Store\document\all_my_work\CZY\bishe\mangrove_old\lstm\data\extraction\owasp-slice-test-2.txt'

    owasp_train_file = settings.relative_path_from_root('data/mangrove/t-train.txt')
    owasp_test_file = settings.relative_path_from_root('data/mangrove/t-test.txt')
    train_x, train_y = parseDataFile(owasp_train_file)
    test_x, test_y = parseDataFile(owasp_test_file)

    def gen() -> (str, int):
        for idx in range(len(train_x)):
            yield train_x[idx], train_y[idx]
        for idx in range(len(test_x)):
            yield test_x[idx], test_y[idx]

    return gen


if __name__ == '__main__':
    for x, y in get_magrove_generator(preprocessing, "", "")():
        if y == 1:
            print(x, y)
