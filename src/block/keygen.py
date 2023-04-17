from .sbox import sub_byte
from pwn import xor


class Rc:
    memo = []

    def __init__(self):
        self.current_index = 0
        if len(Rc.memo) == 0:
            Rc.memo.append(0x01)

    def __next__(self):
        if len(Rc.memo) <= self.current_index:
            current = Rc.memo[self.current_index - 1] << 1
            if current >= 0x100:
                current ^= 0x11B
            Rc.memo.append(current)

        result = Rc.memo[self.current_index]
        self.current_index += 1
        return result


class Rcon:
    memo = []

    def __init__(self):
        self.rc_gen = Rc()

    def __next__(self):
        rc = next(self.rc_gen)
        rcon = rc << 24
        Rcon.memo.append(rcon)
        return rcon

    def __getitem__(self, index: int) -> int:
        while len(Rcon.memo) < index + 1:
            next(self)
        return Rcon.memo[index]


"""
Usage:

- As generator
```
keys = KeyScheduler(external_key)
round1_key = next(keys)
round2_key = next(keys)
round3_key = next(keys)
# ...
```

- Using index
```
keys = KeyScheduler(external_key)
round16_key = keys[15]
```

- Take many keys
```
keys = KeyScheduler(external_key)
round_keys = keys.take(16)
```

- Skip n first keys
```
keys = KeyScheduler(external_key)
keys.skip(n)
round1_key = next(keys)
```

- Reset index counter
keys = KeyScheduler(external_key)
round_keys = keys.take(16)
keys.reset()
round_keys2 = keys.take(16) // same keys
"""


class KeyScheduler:
    KEY_WORD_SIZE = 4

    # External key has a length of 128 bits (16 bytes)
    def __init__(self, external_key: bytes):
        word1 = external_key[0:4]
        word2 = external_key[4:8]
        word3 = external_key[8:12]
        word4 = external_key[12:16]
        self.memo = [word1, word2, word3, word4]

        self.rcon = Rcon()
        self.current_index = 0

    def __iter__(self):
        self.rcon = Rcon()
        self.current_index = 0
        return self

    def __getitem__(self, index: int) -> bytes:
        current_index = self.current_index
        while len(self.memo) < index + 1:
            next(self)
        self.current_index = current_index
        return self.memo[index]

    def __next__(self):
        if len(self.memo) > self.current_index:
            result = self.memo[self.current_index]
            self.current_index += 1
            return result

        prev_word = self.memo[self.current_index - 1]
        prev_round_word = self.memo[self.current_index -
                                    KeyScheduler.KEY_WORD_SIZE]

        if self.current_index % KeyScheduler.KEY_WORD_SIZE == 0:
            round = self.current_index // KeyScheduler.KEY_WORD_SIZE
            current_word = xor(prev_round_word, sub(
                rot(prev_word)))

            current_word = xor(
                current_word, self.rcon[round].to_bytes(4, "big"))
        else:
            current_word = xor(prev_word, prev_round_word)

        self.memo.append(current_word)
        self.current_index += 1
        return current_word

    def reset(self):
        self.current_index = 0

    def skip(self, count: int):
        for _ in range(count):
            next(self)

    def take(self, count: int):
        return [b''.join([next(self), next(self)]) for _ in range(count)]


def rot(word: bytes):
    word_int = int.from_bytes(word, "big")
    most_significant_byte = word_int // (1 << 24)
    word_int <<= 4
    word_int %= 1 << 32
    word_int += most_significant_byte
    return word_int.to_bytes(4, "big")


def sub(word: bytes):
    new_bytes = [sub_byte(byte) for byte in word]
    return bytes(new_bytes)
