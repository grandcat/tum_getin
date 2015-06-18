import json
import http.client
import ssl


class Backend(object):
    def __init__(self, host, port):
        self.host = host
        self.port = port
        self.context = ssl.SSLContext(ssl.PROTOCOL_SSLv3)
        self.context.set_default_verify_paths()


    def isConnected(self):
        conn = http.client.HTTPSConnection(self.host, self.port, context = self.context)
        conn.request('GET', '/')
        response = json.loads(conn.getresponse().read().decode())
        conn.close()
        if 'message' in response:
            if response['message'] == 'Backend running! Welcome!':
                return True
            else:
                return False
        else:
            return False

    def isStudent(self, pseudoID):
        conn = http.client.HTTPSConnection(self.host, self.port, context = self.context)
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


    def getPublicKey(self, pseudoID):
        conn = http.client.HTTPSConnection(self.host, self.port, context = self.context)
        conn.request('GET', '/check?pseudo_id=' + pseudoID)
        response = json.loads(conn.getresponse().read().decode())
        conn.close()
        if 'key' in response:
            return response['key']
        else:
            return -1








