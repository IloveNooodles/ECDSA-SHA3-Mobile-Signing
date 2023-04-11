import numpy as np
from typing import Callable
from queue import Queue
from pwn import xor


class FeistelNetwork:
    def __init__(self, round_function: Callable[[bytes, bytes], bytes], rounds: int):
        self.map = round_function
        self.rounds = rounds
        self.round_keys = Queue()

        self.left = None
        self.right = None

    def transform(self, input: bytes) -> bytes:
        self.left, self.right = input[:len(input)//2], input[len(input)//2:]

        for i in range(self.rounds):
            self._transform_once()

        return b''.join([self.left, self.right])

    def invert(self, input: bytes):
        self.left, self.right = input[:len(input)//2], input[len(input)//2:]

        for i in range(self.rounds):
            self._revert_once()

        return b''.join([self.left, self.right])

    def push_key(self, *round_keys: bytes):
        for key in round_keys:
            self.round_keys.put(key)

    def _transform_once(self):
        round_key = self.round_keys.get()
        inbetween = self.map(self.right, round_key)
        next_right = xor(self.left, inbetween)
        next_left = self.right
        self.right = next_right
        self.left = next_left

    def _revert_once(self):
        round_key = self.round_keys.get()

        inbetween = self.map(self.left, round_key)
        prev_left = xor(self.right, inbetween)
        prev_right = self.left
        self.right = prev_right
        self.left = prev_left
