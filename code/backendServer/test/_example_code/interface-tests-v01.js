var should = require('should');
var mocha = require('mocha');
var https = require('https');
var http = require('http');
var fs = require('fs');
// getting test data from central test data folder
var testUser1 = require('../../testResources/user_01.json');
var config = require('./test_config');

var user = {
  tum_id: testUser1.tum_id,
  token: testUser1.token
};



describe('Backend Interface Unit Tests:', function() {
	// @before : do setup tasks
	before(function(done) {
		done();
	});

	it('/register get token correct', function(done) {
//		sendGet('/register?tum_id=ga00aaa');
		done();
	});
	it('/register post key correct', function(done) {
		sendPost('/register', user);
		done();
	});
	it('test222', function(done) {
		true.should.equal(true);
		done();
	});
});

var req; // request object

var handleResponse = function(res) {
	console.log("statusCode: ", res.statusCode);

		res.setEncoding('utf-8');

		console.log(res);

		var responseString = '';
		
		res.on('data', function(data) {
		  console.log('llllllllllll');
		  responseString += data;
		});
		
		res.on('end', function() {
		  var resultObject = JSON.parse(responseString);
		  console.log(';;;;;;;;;;');
		  console.log(resultObject);
		});
}
/**
 * Creates a Https request
 * Example: sendData('/register', 'POST', msgBodyAsJson);
 */
var sendData = function(path, method, jsonString) {
	var options = {};
	var body = JSON.stringify(jsonString);
	if(method === 'POST') {
		var headers = {
			'Content-Type': 'application/json',
			'Content-Length': body.length
		};
		options = {
			host: config.host,
			port: config.port,
			path: path,
			method: method,
			headers: headers,
			ca: [ fs.readFileSync('./cert.pem') ]
		};
	} else {
		options = {
			host: config.host,
			port: config.port,
			path: path,
			method: method,
			headers: {},
			ca: [ fs.readFileSync('./cert.pem') ]
		};
		jsonString = {};
		body = JSON.stringify(jsonString);
	}

console.log(options.method);
console.log(body);
console.log(body.length);

console.log('1');
	// Setup the request. 
	req = https.request(options, function(res) {
		console.log('///////////////d/d/d/d/d/d/');
	});
	//req = https.request(options, handleResponse);
console.log('2');
	
	req.on('error', function(e) {
	  console.log(e);
	});

	req.write(body);
	req.end();
}
// Wrapper for sendData GET
var sendGet = function(path) {
	sendData(path, 'GET', null);
}
// Wrapper for sendData with POST
var sendPost = function(path, jsonString) {
	sendData(path, 'POST', jsonString);
}
