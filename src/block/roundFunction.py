from pwn import xor
from math import gcd
from typing import Callable
from .expand import expand
from .sbox import sub_bytes
from .pbox import permute


class RoundFunction:
    def get_f(self) -> Callable[[bytes, bytes], bytes]:
        return self._f

    def _f(self, input: bytes, round_key: bytes) -> bytes:
        assert(len(round_key) == 8)
        keys = [round_key[i:i+2] for i in range(0, len(round_key), 2)]
        assert(len(keys) == 4)
        left, right = self._slice_box(input, keys[0])
        left = self._permute(left)
        right = self._substitute(right)
        concat = b"".join([left, right])
        left, right = self._slice_box(concat, keys[1])
        left = self._permute(left)
        right = self._substitute(right)
        left = self._expand(left, keys[2])
        right = self._expand(right, keys[3])
        return xor(left, right)

    def _slice_box(self, input: bytes, key: bytes) -> (bytes, bytes):
        assert(len(key) == 2)
        assert(len(input) == 8)

        key_binary = format(int.from_bytes(key, "big"), '#018b')[2:]
        n = 64
        a = int(key_binary[:6], 2)
        init = int(key_binary[6:12], 2)
        cnt = int(key_binary[12:], 2)

        a = a*3//gcd(a, n)
        init = (init + cnt * a) % 64

        left = ""
        right = ""
        input_binary = format(int.from_bytes(input, "big"), '#066b')[2:]
        for _ in range(32):
            left += input_binary[init]
            init = (init + a) % 64
            right += input_binary[init]
            init = (init + a) % 64
        left = int(left, 2).to_bytes(4, "big")
        right = int(right, 2).to_bytes(4, "big")
        return (left, right)

    def _expand(self, input: bytes, key: bytes) -> bytes:
        return expand(input, key)

    def _substitute(self, input: bytes) -> bytes:
        return sub_bytes(input)

    def _permute(self, input: bytes) -> bytes:
        return permute(input)
