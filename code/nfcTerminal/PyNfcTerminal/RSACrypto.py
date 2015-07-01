import logging
from Crypto.Cipher import PKCS1_OAEP
from Crypto.Hash import SHA256
from Crypto.PublicKey import RSA


class RSACrypto:

    def __init__(self, log=None):
        self.name = 'rsacrypto'
        self.log = log or logging.getLogger(__name__)
        # Keys
        self.__target_public_key = None
        self.__own_private_key = None
        # Encryption and decryption cipher
        self.__encryption_cipher = None
        self.__decryption_cipher = None

    def import_private_key_from_pem_file(self, path='keys/private_key.pem'):
        self.__own_private_key = RSA.importKey(open(path).read())
        self.log.debug('Loaded private key from %s.', path)
        self.init_decryption()

    def import_public_key_from_pem_file(self, path='keys/public_key.pem'):
        """Just for testing; will not be used in production"""
        self.__target_public_key = RSA.importKey(open(path).read())
        self.log.debug('Loaded public key from %s.', path)
        self.init_encryption()

    def init_encryption(self, public_key=None):
        if public_key is not None:
            self.__target_public_key = public_key

        self.__encryption_cipher = PKCS1_OAEP.new(self.__target_public_key, hashAlgo=SHA256)
        self.log.debug('Initialized successfully RSA encryption.')

    def init_decryption(self, private_key=None):
        if private_key is not None:
            self.__own_private_key = private_key

        self.__decryption_cipher = PKCS1_OAEP.new(self.__own_private_key, hashAlgo=SHA256)
        self.log.debug('Initialized successfully RSA decryption.')

    def encrypt(self, plaintext_bytes):
        ciphertext = self.__encryption_cipher.encrypt(plaintext_bytes)
        return ciphertext

    def decrypt(self, ciphertext_bytes):
        plaintext = bytes()
        try:
            plaintext = self.__decryption_cipher.decrypt(bytes(ciphertext_bytes))
        except ValueError as e:
            self.log.error('Cannot decrypt message: ' + str(e))

        return plaintext
