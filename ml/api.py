#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2019 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""

from flask import Flask, jsonify, request
import os
import settings
import json
from _theano.theanoLSTM import load_model

app = Flask(__name__)

model = load_model(settings.relative_path_from_root("model/theano-lstm-2019-11-19-12-22.npz"))


@app.route('/alive')
def alive():
    return jsonify({"msg": "alive"}), 200


@app.route('/predict')
def predict():
    """
    预测一个slice, 同时将slice保存
    :return:
    """
    slice = request.get_json()
    data_dir = settings.relative_path_from_root('data/slice/' + slice['project'])
    if not os.path.exists(data_dir):
        os.mkdir(data_dir)

    return jsonify({"msg": "true"}), 200


@app.route('/label', methods=['GET', 'POST'])
def label():
    """
    接受一条标记数据
    :return:
    """
    label_json = request.get_json()
    data_dir = settings.relative_path_from_root('data/label/' + label_json['project'])
    if not os.path.exists(data_dir):
        os.mkdir(data_dir)

    with open(data_dir+"/label-"+label_json["traceHash"], 'w') as f:
        json.dump(label_json, f)

    return jsonify({"msg": "true"}), 200


# @app.route('/nodes/get', methods=['GET'])
# def get_nodes():
#     request.args.get('aaa')
#     return jsonify(response), 200

if __name__ == '__main__':
    app.run(host='127.0.0.1', port=8888)
