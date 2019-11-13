#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2019 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""

import tensorflow as tf
import tensorflow_datasets as tfds
import pathlib
import json
import preprocessing


def load_label(base_dir):
    hash2label = {}
    for each_file in pathlib.Path(base_dir).glob('**/*.json'):
        with open(each_file, 'r') as f:
            labeled_slices = json.load(f)
        for each_slice in labeled_slices:
            hash2label[each_slice["sliceHash"]] = 1 if each_slice["isReal"] else 0
    return hash2label


def simple_text_processing(text: str) -> str:
    line = text.split('\n')[1:]  # FIXME
    line = map(lambda e: e.split('::')[4] if len(e.split()) >= 5 else e, line)
    return " ".join(line)


def json_loader(text_processing_func, data_dir: str, label_dict: dict):
    def gen():
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
            if slice_hash not in label_dict:
                continue
            label = label_dict[slice_hash]
            yield slice_text, label

    return gen


def encode(text_tensor, label, encoder):
    """
    编码器
    :param text_tensor:
    :param label:
    :return:
    """
    encoded_text = encoder.encode(text_tensor.numpy())
    return encoded_text, label


def encode_map_fn(text, label, encoder):
    return tf.py_function(lambda t, l: encode(t, l, encoder), inp=[text, label], Tout=(tf.int64, tf.int64))


def lr_schedule(epoch):
    """
    Returns a custom learning rate that decreases as epochs progress.
    """
    learning_rate = 0.001
    # if epoch > 10:
    #     learning_rate = 0.02
    # if epoch > 20:
    #     learning_rate = 0.01
    # if epoch > 50:
    #     learning_rate = 0.005

    tf.summary.scalar('learning rate', data=learning_rate, step=epoch)
    return learning_rate


def train(slice_dir: str, label_dir: str):
    label_dict = load_label(label_dir)
    all_labeled_data = tf.data.Dataset.from_generator(
        json_loader(preprocessing.preprocessing, slice_dir, label_dict),
        (tf.string, tf.int64), (tf.TensorShape([]), tf.TensorShape([])))

    # 打乱数据
    BUFFER_SIZE = 5000  # 要大于数据数
    all_labeled_data = all_labeled_data.shuffle(BUFFER_SIZE, reshuffle_each_iteration=False)

    # 建立单词集合, 删除出现频率过少的词
    tokenizer = tfds.features.text.Tokenizer()
    vocabulary_freq = {}
    for text_tensor, _ in all_labeled_data:
        some_tokens = tokenizer.tokenize(text_tensor.numpy())
        for token in some_tokens:
            if token in vocabulary_freq:
                vocabulary_freq[token] += 1
            else:
                vocabulary_freq[token] = 1
    filtered_vocab = filter(lambda e: e[1] > 5, vocabulary_freq.items())
    filtered_vocab = set(map(lambda e: e[0], filtered_vocab))

    # 对字符串编码
    encoder = tfds.features.text.TokenTextEncoder(filtered_vocab)

    # 打包编码器为py_function然后调用map
    all_encoded_data = all_labeled_data.map(lambda text, label: encode_map_fn(text, label, encoder))

    # 对数据分组（之后按组计算损失函数），并且填充文本至固定长度，这时vocabsize=len（vocabulary_set）+1
    BATCH_SIZE = 8  # BATCH_SIZE>epoch*epoches
    TAKE_SIZE = 200
    train_data = all_encoded_data.skip(TAKE_SIZE).shuffle(BUFFER_SIZE)

    train_data = train_data.padded_batch(BATCH_SIZE, padded_shapes=([-1], []))
    # train_data = train_data.repeat(2)
    # 获取补全后的长度
    FIXED_LENGTH = next(iter(train_data))[0].shape[1]

    test_data = all_encoded_data.take(TAKE_SIZE)
    test_data = test_data.padded_batch(BATCH_SIZE, padded_shapes=([-1], []))

    # 初始化一个BLSTM
    model = tf.keras.Sequential([
        tf.keras.layers.Embedding(encoder.vocab_size + 100, 128),
        tf.keras.layers.Bidirectional(tf.keras.layers.LSTM(128)),  # BLSTM
        # tf.keras.layers.Dense(128, activation='sigmoid'),
        tf.keras.layers.Dense(1, activation='sigmoid')  # 标签个数
    ])

    # 配置BLSTM
    model.compile(loss='mse',  # 损失函数
                  optimizer=tf.keras.optimizers.Adam(1e-4),  # 优化器
                  metrics=['accuracy'])

    # 训练
    lr_callback = tf.keras.callbacks.LearningRateScheduler(lr_schedule)
    # tensorboard_callback = tf.keras.callbacks.TensorBoard(log_dir=logdir)
    history = model.fit(train_data, epochs=1000, callbacks=[lr_callback])

    # 测试
    test_loss, test_acc = model.evaluate(test_data)

    print('Test Loss: {}'.format(test_loss))
    print('Test Accuracy: {}'.format(test_acc))

    # print(sample_predict("You are wrong!", encoder, model, FIXED_LENGTH))


def main():
    train('data/slice', 'data/label')


if __name__ == '__main__':
    main()
