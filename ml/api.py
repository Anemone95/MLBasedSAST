#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2019 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""
import fire
from flask import Flask, jsonify, request
import os
import settings
import json
import _theano.theanoLSTM as lstm
import logging

app = Flask(__name__)

MODEL = None


@app.route('/alive')
def alive():
    return jsonify({"msg": "alive"}), 200


@app.route('/predict', methods=['GET', 'POST'])
def predict():
    """
    预测一个slice, 同时将slice保存
    :return:
    """
    if not MODEL:
        logging.warning("Model not specify, can't predict")
        return jsonify({"msg": "Model not specify, can't predict"}), 500
    slice_json = request.get_json()
    data_dir = settings.relative_path_from_root('data/slice/' + slice_json['project'])
    if not os.path.exists(data_dir):
        os.makedirs(data_dir)
    with open(data_dir + '/slice-' + slice_json["flowHash"] + ".json", 'w') as f:
        json.dump(slice_json, f)
    isTP = lstm.predict(MODEL, slice_json["slice"])
    logging.info("Predict {0} as {1}".format(slice_json["flowHash"], isTP))
    return jsonify({"msg": str(isTP)}), 200


@app.route('/label', methods=['GET', 'POST'])
def label():
    """
    接受一条标记数据
    :return:
    """
    label_json = request.get_json()
    data_dir = settings.relative_path_from_root('data/label/' + label_json['project'])
    if not os.path.exists(data_dir):
        os.makedirs(data_dir)

    with open(data_dir + "/label-" + label_json["flowHash"] + ".json", 'w') as f:
        json.dump(label_json, f)

    return jsonify({"msg": "true"}), 200


def server(model_npz=None, host='127.0.0.1', port=8888, debug=False):
    global MODEL
    if model_npz:
        MODEL = lstm.load_model(settings.relative_path_from_root(model_npz))
    else:
        logging.warning("Model not specify, can't predict")
    app.run(host=host, port=port, debug=debug)


if __name__ == '__main__':
    fire.Fire(server)
