var https = require('https');
var fs = require('fs');
// getting test data from central test data folder
var testUser1 = require('../../testResources/user_01.json');
var config = require('./test_config');

//var user = {
//  tum_id: 'ga00aaa',
//  token: '491652672440A20D6BD49B63E60DADB9'
//};
var user = {
  tum_id: testUser1.tum_id,
  token: testUser1.token
};

var userString = JSON.stringify(user);

var headers = {
  'Content-Type': 'application/json',
  'Content-Length': userString.length
};

var options = {
  host: config.host,
  port: config.port,
  path: '/register',
  method: 'POST',
  headers: headers,
  ca: [ fs.readFileSync('./cert.pem') ]
};

// Setup the request. 
var req = https.request(options, function(res) {
  console.log("statusCode: ", res.statusCode);

  res.setEncoding('utf-8');

  var responseString = '';

  res.on('data', function(data) {
    responseString += data;
  });

  res.on('end', function() {
    var resultObject = JSON.parse(responseString);
    console.log(resultObject);
  });

});

req.on('error', function(e) {
  console.log(e);
});

req.write(userString);
req.end();

