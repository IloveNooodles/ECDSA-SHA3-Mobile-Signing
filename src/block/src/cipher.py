from roundFunction import RoundFunction
from keygen import KeyScheduler
from feistel import FeistelNetwork

class Cipher:
    __key_size = 128//8
    __block_size = 128//8
    __no_rounds = 16

    def __init__(self, key: bytes):
        assert(len(key)==self.__key_size)
        self.__round_keys = KeyScheduler(key).take(self.__no_rounds)
        self.__feistel = FeistelNetwork(RoundFunction().get_f(), self.__no_rounds)

    def encrypt(self, data: bytes):
        assert(len(data) == self.__block_size)
        self.__feistel.push_key(*self.__round_keys)
        return self.__feistel.transform(data)

    def decrypt(self, data: bytes):
        assert(len(data) == self.__block_size)
        self.__round_keys.reverse()
        self.__feistel.push_key(*self.__round_keys)
        self.__round_keys.reverse()
        return self.__feistel.invert(data)