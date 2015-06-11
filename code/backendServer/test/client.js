var https = require('https');
var fs = require('fs');

var user = {
  tumid: 'ga00aaa',
  token: '491652672440A20D6BD49B63E60DADB9'
};

var userString = JSON.stringify(user);

var headers = {
  'Content-Type': 'application/json',
  'Content-Length': userString.length
};

var options = {
  host: 'localhost',
  port: 3000,
  path: '/register',
  method: 'POST',
  headers: headers,
  ca: [ fs.readFileSync('./cert.pem') ]
};

// Setup the request.  The options parameter is
// the object we defined above.
var req = https.request(options, function(res) {
  res.setEncoding('utf-8');

  var responseString = '';

  res.on('data', function(data) {
    responseString += data;
  });

  res.on('end', function() {
    var resultObject = JSON.parse(responseString);
  });
});

req.on('error', function(e) {
  console.log(e);
});

req.write(userString);
req.end();

