from Crypto.Cipher import AES, PKCS1_OAEP
from Crypto.Hash import SHA256
from Crypto.PublicKey import RSA
from Crypto import Random

from cryptography.hazmat.primitives.serialization import load_pem_public_key
from cryptography.hazmat.backends import default_backend

def hex_dump(buffer):
    """Dumps the buffer as an hex string"""
    return ' '.join(["%0.2X" % x for x in buffer])

aeskey = Random.new().read(32)
iv = Random.new().read(AES.block_size)
cipher = AES.new(aeskey, AES.MODE_CFB, iv)
msg = iv + cipher.encrypt(b'Attack at dawn')
print('AES Encrypted message: %s' % msg)
print('Size: %d' % len(msg))

# Import public key for encryption
key = RSA.importKey(open('keys/public_key.pem').read())
print('Imported pub key: %s' % str(key))
msg = bytes('Very secret messages with a lot of fun'.encode('utf-8'))
h = SHA256.new(msg)
print('Msg hash: %s' % hex_dump(h.digest()))
print('Msg hash size: %d' % len(h.digest()))
#cipher = PKCS1_v1_5.new(key)
cipher = PKCS1_OAEP.new(key, hashAlgo=SHA256)
ciphertext =  cipher.encrypt(msg + h.digest())
print('Encrypted msg: %s' % hex_dump(ciphertext))
print('Len: %d' % len(ciphertext))

# Test more recent cryptography library
public_key_pem_export = open('keys/public_key.pem').read()
print('Read pubkey: %s' % public_key_pem_export)    # Down from here: does NOT work with RPi Wheezy!!
public_key_pem_export = (bytes(public_key_pem_export, encoding='utf8') if not isinstance(public_key_pem_export, bytes) else public_key_pem_export)
public_key_pem_loaded = load_pem_public_key(data=public_key_pem_export, backend=default_backend())