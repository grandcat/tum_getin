import json
import http.client
import ssl


user = {"tum_id" : "ga00aaa", "token" : "491652672440A20D6BD49B63E60DADB9"}

userString = json.dumps(user)

headers = {"Content-Type" : "application/json",
           "Content-Length" : len(userString)}

host = '192.168.178.37'
port = 3000

#Cert Stuff
ctx = ssl.SSLContext(ssl.PROTOCOL_SSLv23)
#ctx = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)
ctx.set_default_verify_paths()

#The HTTPS Connection
conn = http.client.HTTPSConnection(host, port, context=ctx)

#Pseudo ID creation?
pseudoID = user['tum_id'] + "1234"

#POST example
#conn.request('POST', '/register', userString, headers)
#response = conn.getresponse()
#print(response.status, response.reason)

#GET example
conn.request('GET', '/check?pseudo_id=' + pseudoID)
response = conn.getresponse()
print(response.status, response.reason, response.msg)


conn.close()




