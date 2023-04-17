from pwn import xor

from .cipher import Cipher


class IES:
    __key_size = 128//8
    __block_size = 128//8

    # modes
    MODE_ECB = 0
    MODE_CBC = 1
    MODE_OFB = 2
    MODE_CFB = 3
    MODE_CTR = 4

    def __init__(self, key: bytes, mode: int, iv: bytes = b''):
        assert(len(key) == self.__key_size)

        if (mode == self.MODE_ECB):
            assert(len(iv) == 0)
        elif (mode == self.MODE_CTR):
            assert(len(iv) == self.__block_size//2)
        else:
            assert(len(iv) == self.__block_size)

        self._cipher = Cipher(key)
        self._key = key
        self._mode = mode
        self._iv = iv

    def encrypt(self, data: bytes):
        if (self._mode == self.MODE_ECB):
            return self._encrypt_ecb(data)
        elif (self._mode == self.MODE_CBC):
            return self._encrypt_cbc(data)
        elif (self._mode == self.MODE_OFB):
            return self._encrypt_ofb(data)
        elif (self._mode == self.MODE_CFB):
            return self._encrypt_cfb(data)
        elif (self._mode == self.MODE_CTR):
            return self._encrypt_ctr(data)
        else:
            raise NotImplementedError()

    def decrypt(self, data: bytes):
        if (self._mode == self.MODE_ECB):
            return self._decrypt_ecb(data)
        elif (self._mode == self.MODE_CBC):
            return self._decrypt_cbc(data)
        elif (self._mode == self.MODE_OFB):
            return self._decrypt_ofb(data)
        elif (self._mode == self.MODE_CFB):
            return self._decrypt_cfb(data)
        elif (self._mode == self.MODE_CTR):
            return self._decrypt_ctr(data)
        else:
            raise NotImplementedError()

    def _encrypt_ecb(self, data: bytes):
        assert(len(data) % self.__block_size == 0)
        result = bytearray()
        for i in range(0, len(data), self.__block_size):
            result += self._cipher.encrypt(data[i:i+self.__block_size])
        return bytes(result)

    def _decrypt_ecb(self, data: bytes):
        assert(len(data) % self.__block_size == 0)
        result = bytearray()
        for i in range(0, len(data), self.__block_size):
            result += self._cipher.decrypt(data[i:i+self.__block_size])
        return bytes(result)

    def _encrypt_cbc(self, data: bytes):
        assert(len(data) % self.__block_size == 0)
        result = bytearray()
        cur_xor = self._iv
        for i in range(0, len(data), self.__block_size):
            block = data[i:i+self.__block_size]
            block = xor(block, cur_xor)
            cur_xor = self._cipher.encrypt(block)
            result += cur_xor
        return bytes(result)

    def _decrypt_cbc(self, data: bytes):
        assert(len(data) % self.__block_size == 0)
        result = bytearray()
        cur_xor = self._iv
        for i in range(0, len(data), self.__block_size):
            block = data[i:i+self.__block_size]
            dec_block = self._cipher.decrypt(block)
            result += xor(dec_block, cur_xor)
            cur_xor = block
        return bytes(result)

    def _encrypt_ofb(self, data: bytes):
        assert(len(data) % self.__block_size == 0)
        result = bytearray()
        cur_xor = self._iv
        for i in range(0, len(data), self.__block_size):
            block = data[i:i+self.__block_size]
            cur_xor = self._cipher.encrypt(cur_xor)
            result += xor(block, cur_xor)
        return bytes(result)

    def _decrypt_ofb(self, data: bytes):
        assert(len(data) % self.__block_size == 0)
        result = bytearray()
        cur_xor = self._iv
        for i in range(0, len(data), self.__block_size):
            block = data[i:i+self.__block_size]
            cur_xor = self._cipher.encrypt(cur_xor)
            result += xor(block, cur_xor)
        return bytes(result)

    def _encrypt_cfb(self, data: bytes):
        assert(len(data) % self.__block_size == 0)
        result = bytearray()
        cur_xor = self._iv
        for i in range(0, len(data), self.__block_size):
            block = data[i:i+self.__block_size]
            cur_xor = self._cipher.encrypt(cur_xor)
            cur_xor = xor(block, cur_xor)
            result += cur_xor
        return bytes(result)

    def _decrypt_cfb(self, data: bytes):
        assert(len(data) % self.__block_size == 0)
        result = bytearray()
        cur_xor = self._iv
        for i in range(0, len(data), self.__block_size):
            block = data[i:i+self.__block_size]
            cur_xor = self._cipher.encrypt(cur_xor)
            result += xor(block, cur_xor)
            cur_xor = block
        return bytes(result)

    def _encrypt_ctr(self, data: bytes):
        assert(len(data) % self.__block_size == 0)
        result = bytearray()
        for i in range(0, len(data), self.__block_size):
            block = data[i:i+self.__block_size]
            ctr = i//self.__block_size
            ctr = ctr.to_bytes(self.__block_size//2, "big")
            cur_xor = b"".join([self._iv, ctr])
            cur_xor = self._cipher.encrypt(cur_xor)
            result += xor(block, cur_xor)
        return bytes(result)

    def _decrypt_ctr(self, data: bytes):
        assert(len(data) % self.__block_size == 0)
        result = bytearray()
        for i in range(0, len(data), self.__block_size):
            block = data[i:i+self.__block_size]
            ctr = i//self.__block_size
            ctr = ctr.to_bytes(self.__block_size//2, "big")
            cur_xor = b"".join([self._iv, ctr])
            cur_xor = self._cipher.encrypt(cur_xor)
            result += xor(block, cur_xor)
        return bytes(result)
