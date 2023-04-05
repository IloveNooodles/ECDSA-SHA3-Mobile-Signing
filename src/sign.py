import os
from hashlib import sha256
from random import randint
from Crypto.Util.number import bytes_to_long, long_to_bytes
from ecdsa import ellipticcurve

class ecdsa:
    def __init__(self):
        p = 0xffffffff00000001000000000000000000000000ffffffffffffffffffffffff
        a = 0xffffffff00000001000000000000000000000000fffffffffffffffffffffffc
        b = 0x5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b

        Gx = 0x6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296
        Gy = 0x4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5
        self.n = 0xffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551

        self.E = ellipticcurve.CurveFp(p, a, b, 1)

        self.G = ellipticcurve.PointJacobi(self.E, Gx, Gy, 1, self.n)
        self.x = None
        self.Q = None

    # Initialize private key for signing, returns public key pair
    def priv(self, x: bytes):
        x = bytes_to_long(x) % self.n
        self.x = x
        self.Q = self.G * x
        return long_to_bytes(self.Q.x()), long_to_bytes(self.Q.y())

    # Initialize public key for verification
    def pub(self, qx: bytes, qy: bytes):
        qx = bytes_to_long(qx)
        qy = bytes_to_long(qy)
        if not self.E.contains_point(qx, qy):
            return False
        self.Q = ellipticcurve.PointJacobi(self.E, qx, qy, 1, self.n)
        return True
    
    # Input:
    # m: message, x: private key
    # Output:
    # (r, s): signature pair
    def sign(self, m: bytes) -> (bytes, bytes):
        if self.x == None:
            raise Exception("Private key not initialized")
        h = bytes_to_long(sha256(m).digest())
        k = 0
        while k == 0:
            k = bytes_to_long(os.urandom(32)) % self.n
        R = k * self.G
        s = pow(k, -1, self.n) * (h + R.x() * self.x) % self.n

        return long_to_bytes(R.x()), long_to_bytes(s)

    # Input:
    # m: message
    # r: first of signature pair
    # s: second of signature pair
    # Output:
    # boolean, message verified or not
    def verify(self, m: bytes, r: bytes, s: bytes) -> bool:
        if self.Q == None:
            raise Exception("Public key not initialized")
        r = bytes_to_long(r)
        s = bytes_to_long(s)
        if (r % self.n != r) or (s % self.n != s):
            return False
        h = bytes_to_long(sha256(m).digest())
        u = h * pow(s, -1, self.n) % self.n
        v = r * pow(s, -1, self.n) % self.n
        X = u * self.G + v * self.Q
        return r == X.x() % self.n