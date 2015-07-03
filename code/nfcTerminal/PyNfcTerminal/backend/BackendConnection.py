import json
import http.client
import logging
import ssl


class Backend(object):
    def __init__(self, host='www.grandcat.org', port=3000, cert_path='res/cert.pem', log=None):
        self.log = log or logging.getLogger(__name__)
        self.host = host
        self.port = port
        self.context = ssl.SSLContext(ssl.PROTOCOL_TLSv1)
        self.context.verify_mode = ssl.CERT_REQUIRED
        self.context.load_verify_locations(cert_path)
        # self.context.set_default_verify_paths()
        self.log.debug('Backend config initialized.')

    def is_connected(self):
        conn = http.client.HTTPSConnection(self.host, self.port, context=self.context)
        conn.request('GET', '/')
        resp = conn.getresponse()
        if resp.status == 200:
            return True
        else:
            return False

    def is_student(self, pseudoID):
        conn = http.client.HTTPSConnection(self.host, self.port, context=self.context)
        conn.request('GET', '/check?pseudo_id=' + pseudoID)
        response = json.loads(conn.getresponse().read().decode())
        conn.close()
        if 'student_status' in response:
            if response['student_status'] == 'student':
                return True
            else:
                return False
        else:
            return False

    def get_public_key_raw(self, pseudoID):
        if pseudoID is '':
            return None

        conn = http.client.HTTPSConnection(self.host, self.port, context=self.context)
        conn.request('GET', '/check?pseudo_id=' + pseudoID)
        response = json.loads(conn.getresponse().read().decode())
        conn.close()
        if 'key' in response:
            return response['key']
        else:
            return None

# Test backend connection with certificate verification
# backend = Backend(cert_path='../res/cert.pem')
# print(backend.get_public_key_raw("lsdsdfsdjkflsdf"))