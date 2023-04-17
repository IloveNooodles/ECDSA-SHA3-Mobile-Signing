from flask import *
from flask_cors import CORS

from sign import custom_ecdsa

app = Flask(__name__)
CORS(app)

@app.route("/")
def index():
    response = {"data": "hello"}
    return jsonify(response)

# form body:
# hash -> hex encoded string
# private_key -> hex encoded string
# returns:
# public_key -> hex encoded string
# signature -> hex encoded string
@app.route("/sign", methods=["POST"])
def sign():
    form = request.json
    print(form)
    assert("hash" in form)
    assert("private_key" in form)
    data = bytes.fromhex(form["hash"])
    private_key = bytes.fromhex(form["private_key"])
    ecdsa = custom_ecdsa()
    public_key_x, public_key_y = ecdsa.priv(private_key)
    public_key = public_key_x.hex() + "." + public_key_y.hex()
    signature_r, signature_s = ecdsa.sign(data)
    signature = signature_r.hex() + "." + signature_s.hex()
    response = {"public_key": public_key, "signature": signature}
    return jsonify(response)



# form body:
# hash -> hex encoded string
# public_key -> hex encoded string
# signature -> hex encoded string
# returns:
# is_verified -> bool
# or
# error -> string
@app.route("/verify", methods=["POST"])
def verify():
    form = request.json
    assert("hash" in form)
    assert("signature" in form)
    assert("public_key" in form)
    data = bytes.fromhex(form["hash"])
    signature = form["signature"].strip().split(".")
    signature_r = bytes.fromhex(signature[0])
    signature_s = bytes.fromhex(signature[1])
    public_key = form["public_key"].strip().split(".")
    public_key_x = bytes.fromhex(public_key[0])
    public_key_y = bytes.fromhex(public_key[1])
    ecdsa = custom_ecdsa()
    f = ecdsa.pub(public_key_x, public_key_y)
    if not f:
        return jsonify({"error": "public key invalid"})
    verified = ecdsa.verify(data, signature_r, signature_s)
    response = {"is_verified": verified}
    return jsonify(response)

if __name__ == "__main__":
    app.run("0.0.0.0", port=9099, debug=True)