#!/usr/bin/env python3
"""

:Author: Anemone Xu
:Email: anemone95@qq.com
:copyright: (c) 2019 by Anemone Xu.
:license: Apache 2.0, see LICENSE for more details.
"""
import torch
import torch.nn as nn
import torch.nn.utils.rnn as rnn_utils
from torch import Tensor


class BLSTM(nn.Module):

    def __init__(self, embedding_dim, hidden_dim, vocab_size, label_size, use_gpu, batch_first=True):
        super(BLSTM, self).__init__()
        self.hidden_dim = hidden_dim
        self.use_gpu = use_gpu
        self.num_directions = 2
        self.batch_first = batch_first
        self.word_embeddings = nn.Embedding(vocab_size, embedding_dim, padding_idx=0)
        self.lstm = nn.LSTM(input_size=embedding_dim, hidden_size=hidden_dim, num_layers=1, dropout=0.5,
                            bidirectional=True, batch_first=self.batch_first)
        self.hidden2label = nn.Linear(hidden_dim * self.num_directions, label_size)

    def get_bi_last_output_deprecated(self, lstm_out: Tensor, lengths: Tensor, batch_first=False) -> Tensor:
        if batch_first:
            raise NotImplementedError
        indices = lengths - 1
        # @Anemone 因为输入向后传播，考虑batch中的padding，根据lengths从lstm_out中获取最后一个输入的输出
        indices = indices.unsqueeze(1).expand(lstm_out.shape[1:]).unsqueeze(0)
        forward_last_output = lstm_out.gather(0, indices)[0]

        backward_last_output = lstm_out[0]

        forward_last_output = forward_last_output.index_select(1, torch.arange(0, self.hidden_dim, dtype=torch.long))
        backward_last_output = backward_last_output.index_select(1,
                                                                 torch.arange(self.hidden_dim, self.hidden_dim * 2,
                                                                              dtype=torch.long))
        last_output = torch.cat([forward_last_output, backward_last_output], dim=1)
        return last_output

    def get_bi_last_output(self, lstm_out: Tensor, lengths: Tensor) -> Tensor:
        batch_size = len(lengths)
        forward_indices = lengths - 1
        if self.batch_first:
            # lstm_out=batch*sentence_len*(hiddendim*2)
            forward_indices = forward_indices.unsqueeze(1).unsqueeze(1).expand(batch_size, 1, self.hidden_dim)
            if self.use_gpu:
                backward_indices = torch.cuda.LongTensor(batch_size, 1, self.hidden_dim).fill_(0)
            else:
                backward_indices = torch.zeros(batch_size, 1, self.hidden_dim, dtype=torch.long)
            indices = torch.cat([forward_indices, backward_indices], dim=2)
            last_output = lstm_out.gather(1, indices).squeeze(1)
        else:
            # lstm_out=batch*sentence_len*(hiddendim*2)
            # @Anemone 因为输入向后传播，考虑batch中的padding，根据lengths从lstm_out中获取最后一个输入的输出
            forward_indices = forward_indices.unsqueeze(1).expand(batch_size, self.hidden_dim).unsqueeze(0)
            if self.use_gpu:
                backward_indices = torch.cuda.LongTensor(1, batch_size, self.hidden_dim).fill_(0)
            else:
                backward_indices = torch.zeros(1, batch_size, self.hidden_dim, dtype=torch.long)
            indices = torch.cat([forward_indices, backward_indices], dim=2)

            last_output = lstm_out.gather(0, indices).squeeze(0)
        return last_output

    def forward(self, sentences: Tensor, lengths: Tensor):
        embeds = self.word_embeddings(sentences)
        x_packed = rnn_utils.pack_padded_sequence(embeds, lengths, batch_first=self.batch_first)
        lstm_out, (h_n, h_c) = self.lstm(x_packed, None)
        lstm_out_unpacked, lstm_out_lengths = rnn_utils.pad_packed_sequence(lstm_out, batch_first=self.batch_first)
        if self.use_gpu:
            lstm_out_lengths = lstm_out_lengths.cuda()
        # @Anemone 这里不应该用unpacked[-1], 而因该用lengths获取最后真实的序列最后一位
        last_lstm_out = self.get_bi_last_output(lstm_out_unpacked, lstm_out_lengths)
        y = self.hidden2label(last_lstm_out)
        return y


if __name__ == '__main__':
    pass
