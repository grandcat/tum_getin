var should = require('should');
var mocha = require('mocha');
var https = require('https');
var fs = require('fs');
// getting test data from central test data folder
var testUser1 = require('../../testResources/user_01.json');
var config = require('./test_config');

describe('Backend Interface Unit Tests:', function() {
	// @before : do setup tasks
	before(function(done) {
		done();
	});

	it('Test test case', function(done) {
		testUser1.should.equal(undefined);
		done();
	});
	it('test222', function(done) {
		true.should.equal(true);
		done();
	});
});

createHTTPheader = function() {

}

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


