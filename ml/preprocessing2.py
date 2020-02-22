#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2020 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""
import json
import re

STRING_PATTERN = re.compile(r'(#\([^);]+\))')
WORD_PATTERN = re.compile(r'[A-Za-z]+')

tokenazables = ['\(', '\)', '\[', '\]', '{', '}', '\$', '\'', ':', 'XML', ',']
BRANCH_PATTERN = '|'.join(tokenazables)

arbitraryThings = ['\.\.\.', '_', '#', ';', ',', '->', '&#\d+', '\d+:']
SPACE_THINGS = '|'.join(arbitraryThings)

REMOVE_THINGS = ['BenchmarkTest\d+', 'testcode', 'moresafe', 'safe', 'Safe', 'good', 'bad']

VAR_PATTERN = re.compile(r'v\d+')


class Preprocessor:

    def __init__(self, prog_slice: str):
        self.prog_slice = prog_slice
        self.str_list = []
        self.var_list = []

    def preprocess(self) -> str:
        abstract_slice = ""
        for line in self.prog_slice.split('\n'):
            if line:
                # nid, kind, operation, ntype, value = [s.strip() for s in line.strip().split('::')]
                nid, kind, operation, ntype, value,_ = [s.strip() for s in line.strip().split('::')]
                # 处理返回类型
                abstract_slice += operation + " "

                # 分解类名包名方法名
                ntype = self.process_type(ntype)
                ntype = re.sub(r'([A-Za-z0-9\.]+)\.([A-Z][a-zA-Z0-9\_\$]+)', 'P\g<1> C\g<2>', ntype)
                abstract_slice += ntype + " "

                # 处理数字常量
                value = self.process_integer(value)

                # 处理字符串常量
                value = value
                value = re.sub(r'#\(null\)', ' null ', value)  # 空值
                value = self.process_str(value)

                # 处理变量
                value = self.process_var(value)

                # 处理函数调用
                value = re.sub(r'\$[A-Za-z0-9_$]* ', ' ', value)  # 形参名无用
                # 将 v17.getNextException 变为 v17 getNextException()
                value = re.sub(r'(v[0-9]+).([a-zA-Z]+)', '\g<1> \g<2>', value)
                value = re.sub(r' \.([a-zA-Z]+)', ' \g<1> ', value)  # 将.getNextException 变为 getNextException()

                # 处理括号
                value = re.sub(r'(' + BRANCH_PATTERN + ')', ' \g<1> ', value)  # 括号前加上空格

                # 分解类名包名方法名
                value = self.process_type_in_value(value)
                value = re.sub(r'([A-Za-z0-9\.]+)\.([A-Z][a-zA-Z0-9\_\$]+)\.([a-z][a-zA-Z]+)', '\g<1>.\g<2> \g<3> ',
                               value)
                value = re.sub(r'([A-Za-z0-9\.]+)\.([A-Z][a-zA-Z0-9\_\$]+)', 'P\g<1> C\g<2>', value)

                # 删除黑名单上的前缀

                # 替换影响实验效果的干扰词
                abstractionPattern = '|'.join(REMOVE_THINGS)
                value = re.sub(r'(' + abstractionPattern + ')', '', value)  # 替换影响实验效果的变量

                value = re.sub(r'(' + SPACE_THINGS + ')', ' ', value)  # 去除某些符号转为空格

                # unknown
                value = re.sub(r'compile\(#\(.*\)\)', ' compile ( PATTERN ) ', value)
                value = re.sub(r'<([a-zA-Z0-9]+)>?', ' \g<1> ', value)
                value = re.sub(r'([\w\']+)>', ' \g<1> ', value)  # 对于xxx>换成 "xxx "
                value = re.sub(r'(:@?|\w+)<', ' \g<1> ', value)  # 对于xxx>换成 "xxx "
                value = re.sub(r'(\w+):', ' \g<1> ', value)  # 对于xxx:换成 "xxx "
                value = re.sub(r'(\w+)-\s', ' \g<1> ', value)  # 对于"xxx- "换成 "xxx "
                value = re.sub(r'([a-zA-Z]+)-', ' \g<1> ', value)  # 对于xxx- 换成 xxx
                abstract_slice += value
        return abstract_slice

    def process_integer(self, value: str) -> str:
        value = re.sub(r'#\(-?([0-9]+)[eE]-([0-9])\)', ' NSMALL ', value)  # 科学计数法
        value = re.sub(r'#\(\d\d\d+\)', ' N3P ', value)  # 三位以上正整数
        value = re.sub(r'#\(-\d\d\d+\)', ' NN3P ', value)  # 三位以上负数
        text = re.sub(r'#\((\d*)\)', ' N\g<1> ', value)  # 其余数字常量脱括号

        return text

    def process_str(self, value) -> str:
        all_matches = STRING_PATTERN.findall(value)  # replace each unique string with STRING #
        text = value
        for s in set(all_matches):

            if len(s) > 4 + 1 + 1 + 1:
                if s.startswith("http"):
                    text = text.replace(s, "SHTTP_URL")
                else:
                    if not s in self.str_list:
                        self.str_list.append(s)
                    id = self.str_list.index(s)
                    text = text.replace(s, 'STRING ' + "S" + str(id) + ' ')
            else:
                text = text.replace(s, " " + s[2:-1].lower() + " ")

        return text

    def process_var(self, value: str) -> str:
        text = value
        for var in VAR_PATTERN.findall(value):
            if not var in self.var_list:
                self.var_list.append(var)
            id = self.var_list.index(var)
            text = text.replace(var, 'VAR ' + "V" + str(id) + ' ')
        return text

    def process_type(self, value: str) -> str:
        TYPE_PATTERN = re.compile(r'L([a-zA-Z0-9]+(/[a-zA-Z0-9])+)')
        text = TYPE_PATTERN.sub('\g<1>', value)  # Lorg/h2/message/DbException to org/h2/message/DbException
        text = text.replace('/', '.')  # org/h2/message/DbException to org.h2.message.DbException
        if text.startswith("L"):
            text = text[1:]
        return text.replace("[", "")

    def process_type_in_value(self, value: str) -> str:
        for token in re.findall(r'L[a-zA-Z0-9]+(/[a-zA-Z0-9])+', value):
            value = value.replace(token, self.process_type(token))
        return value


if __name__ == '__main__':
    with open(r"G:\slice-00af0452a5ece47edb79e95498ac502d66385c91.json", "r") as f:
        j = json.load(f)
        print(Preprocessor(j["slice"]).preprocess())
