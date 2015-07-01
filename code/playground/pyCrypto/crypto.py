import base64
import json
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

##########################################
# Public / Private key crypto
##########################################
pub_key = RSA.importKey(open('keys/public_key.pem').read())
priv_key = RSA.importKey(open('keys/private_key.pem').read())
print('Imported pub key: %s' % str(pub_key))
print('Imported priv key: %s' % str(priv_key))
msg = bytes('Very secret messages with a lot of fun'.encode('utf-8'))
# JSON object
jmsg = json.dumps({
			"type": "hello",
            "nonce": 721837291837,
		}).encode('utf-8')
print('Json Dump (%d bytes): %s \n' % (len(jmsg), jmsg))
msg = jmsg

h = SHA256.new(msg)
print('Msg hash: %s' % hex_dump(h.digest()))
print('Msg hash size: %d' % len(h.digest()))
# cipher = PKCS1_v1_5.new(key)   # should not be used anymore
cipher = PKCS1_OAEP.new(pub_key, hashAlgo=SHA256)
ciphertext =  bytearray(cipher.encrypt(msg)) # Add h.digest here
print('Encrypted msg: %s' % hex_dump(ciphertext))
print('Len: %d' % len(ciphertext))
print('As base64: %s' % base64.b64encode(ciphertext))

# ciphertext[1] ^= 0x11  # Add error for try/catch

# Decryption
cipher_decrypt = PKCS1_OAEP.new(priv_key, hashAlgo=SHA256)
plaintext = bytes()
try:
    plaintext = cipher_decrypt.decrypt(bytes(ciphertext))
except ValueError as e:
    print("Got value error: " + str(e))
except Exception as e:
    print("Fallback to exception: " + str(e))

print('Decrypted msg: %s' % plaintext)

# Test base64 compatibility to Android => works =)
# bytetext = 'Hello world.123'.encode('utf-8')
# print('Base64 Test: %s' % base64.b64encode(bytetext))
# assert b'SGVsbG8gd29ybGQuMTIz' == base64.b64encode(bytetext)

# Test more recent cryptography.io library
# This might be a great alternative if Raspberry Pi is upgraded to Jessie

# public_key_pem_export = open('keys/public_key.pem').read()
# print('Read pubkey: %s' % public_key_pem_export)    # Down from here: does NOT work with RPi Wheezy!!
# public_key_pem_export = (bytes(public_key_pem_export, encoding='utf8') if not isinstance(public_key_pem_export, bytes) else public_key_pem_export)
# public_key_pem_loaded = load_pem_public_key(data=public_key_pem_export, backend=default_backend())