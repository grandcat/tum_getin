var vows = require('vows'),
    assert = require('assert');
var https = require('https');
var request = require('request');
var fs = require('fs');
// getting test data from central test data folder
var testUser1 = require('../../testResources/user_01.json');
var config = require('./test_config');

var user = {
  tum_id: testUser1.tum_id,
  token: testUser1.token
};

var message = {
  tum_id: testUser1.tum_id,
  token: testUser1.token
};

var headers = {
  'Content-Type': 'application/json',
  'Content-Length': message.length
};

var options = {
  uri: 'https://localhost:3000/register',
  ca: [ fs.readFileSync('./cert.pem') ],
  'content-type': 'application/json',
  headers: headers,
  form: message
};

var fkt = function() {
	request.post(options, function (err, res, body) {
	  if (err) {
	    console.error('Error: ', err);
	  }
	  console.log('Server response: ', body);
	});
};
//fkt();

vows.describe('Backend Interface Unit Tests:').addBatch({
	'/register get token correct': {
		topic: function() {
			console.log('testing something');
			fkt();
			return 0;
		},
		'we get 0': function (topic) {
            		assert.equal (topic, 0);
        	}
	}
	//it('/register post key correct', function(done) {
}).run();

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
