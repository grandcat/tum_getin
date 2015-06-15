var request = require('request');
var https = require('https');
var fs = require('fs');
// getting test data from central test data folder
var testUser1 = require('../../../testResources/user_01.json');
var config = require('../test_config');

var body = {
  tum_id:testUser1.tum_id,
  token:testUser1.token
};

console.log(body);

var headers = {
  'Content-Type': 'application/json',
  'Content-Length': body.length
};

var options = {
  uri: 'https://localhost:3000/register',
  ca: [ fs.readFileSync('./cert.pem') ],
  'content-type': 'application/json',
  headers: headers,
  form: body
};

request.post(options, function optionalCallback(err, httpResponse, body) {
  if (err) {
    return console.error('upload failed:', err);
  }
  console.log('Upload successful!  Server responded with:', body);
});

