var PORT = 8443;
var HOST = 'localhost';
var tls = require('tls');
var fs = require('fs');

// root CA store:
var options = { ca: [ fs.readFileSync('./cert.pem') ] };
var client = tls.connect(PORT, HOST, options, function() {
    if (client.authorized) {
        
        console.log('CONNECTED AND AUTHORIZED\n');
        
        client.on('close', function() {
            console.log('SOCKET CLOSED\n');
            process.exit();
        });
        
        process.stdin.pipe(client);
        process.stdin.resume();
        
        // Time to make some request to the server
        // We will write straight to the socket, but recommended way is to use a client library like 'request' or 'superagent'
        client.write('GET /register HTTP/1.1\r\n');
        client.write('\r\n');
        
        //client.write('POST /ho HTTP/1.1\r\n');
        //client.write('\r\n');
    }
    else {
        console.log('AUTH FAILED\n');
        process.exit();
    }
});
client.setEncoding('utf8');
client.on('data', function(data) {
    console.log('-------------');
    console.log(data);
});
