import base64
import json
import os
import time

from backend import Backend
from nfcio import ReaderIO
from RSACrypto import RSACrypto


class ProtocolLogic(ReaderIO):
    nfccrypto = RSACrypto()
    # This terminal's private certificate
    nfccrypto.import_private_key_from_pem_file('../testResources/terminal_certs/private_key.pem')
    # Just for testing, remove
    # crypto.import_public_key_from_pem_file('../testResources/terminal_certs/public_key.pem')

    def run(self):
        """
        Defines the logic for the communication with the target.

        IOErrors have to be handled carefully. Otherwise, the terminal application might crash
        and stops responding.
        :return:
        """
        backend = Backend()

        hw_activated = True
        while hw_activated:
            try:
                self.device.start_nfc_reader()
                # Protocol states
                nonce_r_S = None
                nonce_r_T = None
                backend_token_hash = None

                """
                Protocol step 1: receive {r_S, pseudo_student_id} encrypted with our T_pub key
                """
                self.log.debug('Protocol step 1: receive {r_S, pseudo_student_id}')
                raw_msg_in = self.receive_and_decrypt()
                # Todo: check for errors

                # Extract nonce and pseudo student ID
                msg_in = json.loads(raw_msg_in.decode('utf-8'))
                nonce_r_S = msg_in['rs']
                pseudo_student_id = msg_in['pid']
                # Fetch public key of student
                self.log.info('Check Pseudo StudentID: %s', pseudo_student_id)
                key, backend_token_hash = backend.get_public_key_raw(pseudo_student_id)
                if key is not None:
                    self.log.debug('Got pub key from backend: %s', key)
                    # Todo: convert key to be usable in Python
                    # Set public key for future secure communication with target
                    self.nfccrypto.import_public_key_from_b64str(key)
                else:
                    # No key yet in the database
                    # Todo: inform user
                    self.log.info('Currently no key associated with this pseudo ID.')
                    raise ValueError('No key for empty or given pseudo ID.')

                """
                Protocol step 2: send {r_s, r_t} encrypted with S_pub key

                r_s: the nonce the target sent us
                r_t: a new nonce of our own
                T: hash of our public key (does this help anything? better sign the message in addition to that)
                """
                self.log.debug('Protocol step 2: send {r_s, r_t, T}')
                rawRandom = os.urandom(32)
                nonce_r_T = base64.b64encode(rawRandom).decode('utf-8')
                self.log.debug('My random r_t: %s', nonce_r_T)

                msg_out = json.dumps({
                    "type": 2,
                    "rs": nonce_r_S,
                    "rt": nonce_r_T
                }).encode('utf-8')

                self.encrypt_and_send(msg_out)

                """
                Protocol step 3: receive {r_t, token_hash} encrypted with our T_pub key

                At this step, the properties freshness and confidentiality should be fulfilled.
                It should be ok to trust the target.
                """
                self.log.debug('Protocol step 3: receive {r_t, commands}')
                raw_msg_in = self.receive_and_decrypt()
                # Todo: check for errors
                msg_in = json.loads(raw_msg_in.decode('utf-8'))
                msg_r_T = msg_in['rt']
                msg_token_hash = msg_in['htoken']
                self.log.debug('Androids token hash: %s', msg_token_hash)
                # Validate response
                if nonce_r_T == msg_r_T and backend_token_hash == msg_token_hash:
                    # Identical nonces and hashed tokens: message should be fresh and user should be authentic
                    self.log.info('Both nonce values are identical and hashes are the same. r_T="%s", hash="%s"',
                                  nonce_r_T, backend_token_hash)
                    # Grant access here
                    self.log.info('ACCESS GRANTED.')
                else:
                    self.log.error('Nonce r_T and received value or token hashes do not match. Replay attack?')

                self.device.shutdown_nfc_reader()

                # Wait some time before permitting a client to connect again
                # Otherwise Android tries to reconnect, but actually, it should be not necessary
                time.sleep(3)

            except (KeyboardInterrupt, SystemExit):
                hw_activated = False
            except AttributeError as e:
                self.log.error("AttributeError due to mismatch in protocol: " + str(e))
                hw_activated = True
            except ValueError as e:
                # Probably the decryption failed
                self.log.error("ValueError occurred. Could be an attack! Details: " + str(e))
                time.sleep(5)
                hw_activated = True
            except IOError as e:
                self.log.error("IOError Exception: " + str(e))
                hw_activated = True
            except BufferError as e:
                self.log.error("Assuming misbehaving target. BufferError: " + str(e))
                hw_activated = True
            finally:
                self.device.shutdown_nfc_reader()

    def receive_and_decrypt(self):
        # Receive raw NFC message
        cipher_msg_in = self.receive_data()
        # Try to decrypt message using the terminal's private key
        raw_msg_in = self.nfccrypto.decrypt(cipher_msg_in)
        self.log.debug('Decrypted msg: %s', raw_msg_in)

        return raw_msg_in

    def encrypt_and_send(self, raw_msg_out):
        self.log.debug('Preparing for sending message: %s', raw_msg_out)
        # Encrypt with public key of mobile phone
        encrypted_msg_out = self.nfccrypto.encrypt(raw_msg_out)
        self.send_data(encrypted_msg_out)
