'use strict';
var vows = require('vows'),
    assert = require('assert'),
    https = require('https'),
    fs = require('fs');
// getting test data from central test data folder
var testUser1 = require('../../testResources/user_01.json');
var config = require('./test_config');
//require('./test_helpers');

vows.describe('Backend Interface /check').addBatch({
	'/register get token correct': {
		topic: function() {
			callback = this.callback;
			sendGet('/register?tum_id=' + testUser1.tum_id);
		},
		'Check response status': function (topic) {
            		assert.equal (topic.status, 200);
        	},
		'Check response user data': function (topic) {
            		assert.equal (topic.tum_id, testUser1.tum_id);
			assert.equal (topic.pseudo_id, testUser1.pseudo_id);
			assert.equal (topic.token, testUser1.token);
        	}

	}
}).export(module);

vows.describe('Backend Interface /register').addBatch({
	'/register get token correct': {
		topic: function() {
			callback = this.callback;
			sendGet('/register?tum_id=' + testUser1.tum_id);
		},
		'Check response status': function (topic) {
            		assert.equal (topic.status, 200);
        	},
		'Check response user data': function (topic) {
            		assert.equal (topic.tum_id, testUser1.tum_id);
			assert.equal (topic.pseudo_id, testUser1.pseudo_id);
			assert.equal (topic.token, testUser1.token);
        	}

	}
}).addBatch({
	'/register post key correct': {
		topic: function() {
			callback = this.callback;
			var user = {
			  tum_id: testUser1.tum_id,
			  token: testUser1.token,
			  key: testUser1.key
			};
			sendPost('/register', user);
		},
		'Check response status': function (topic) {
            		assert.equal (topic.status, 200);
        	}
	}
}).addBatch({
	'/register post key - missing tum_id': {
		topic: function() {
			callback = this.callback;
			var user = {
			  token: testUser1.token,
			  key: testUser1.key
			};
			sendPost('/register', user);
		},
		'Check response status': function (topic) {
            		assert.equal (topic.status, 400);
        	}
	}
}).addBatch({
	'/register post key - missing token': {
		topic: function() {
			callback = this.callback;
			var user = {
			  tum_id: testUser1.tum_id,
			  key: testUser1.key
			};
			sendPost('/register', user);
		},
		'Check response status': function (topic) {
            		assert.equal (topic.status, 400);
        	}
	}
}).addBatch({
	'/register post key - missing key': {
		topic: function() {
			callback = this.callback;
			var user = {
			  tum_id: testUser1.tum_id,
			  token: testUser1.token
			};
			sendPost('/register', user);
		},
		'Check response status': function (topic) {
            		assert.equal (topic.status, 400);
        	}
	}
}).addBatch({
	'/register post key - sending non-existing tum_id': {
		topic: function() {
			callback = this.callback;
			var user = {
			  tum_id: 'gg44ggg',
			  token: testUser1.token,
			  key: testUser1.key
			};
			sendPost('/register', user);
		},
		'Check response status': function (topic) {
            		assert.equal (topic.status, 404);
        	}
	}
}).export(module);

var req; // request object
var callback;   // callback function called on server response
		// use via callback(null, params..)
		// first parameter is useless 
/**
 * Callback function called in sendData() on server responses
 */
var handleResponse = function(res) {
	//console.log("statusCode: ", res.statusCode);
	res.setEncoding('utf-8');
	var responseString = '';

	res.on('data', function(data) {
	  responseString += data;
	});
	
	res.on('end', function() {
	  var resultObject = JSON.parse(responseString);
	  //console.log(resultObject);
	  callback(null, resultObject);
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
			ca: [ fs.readFileSync('./cert.pem') ]
		};
	}
	// Setup the request. 
	req = https.request(options, handleResponse);
	
	req.on('error', function(e) {
	  console.log(e);
	});

	if(method === 'POST')
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

/* Same functionality with request.js instead of https.js */
//var message = {
//  tum_id: testUser1.tum_id,
//  token: testUser1.token
//};
//
//var headers = {
//  'Content-Type': 'application/json',
//  'Content-Length': message.length
//};
//
//var options = {
//  uri: 'https://localhost:3000/register',
//  ca: [ fs.readFileSync('./cert.pem') ],
//  'content-type': 'application/json',
//  headers: headers,
//  form: message
//};
//
//var fkt = function() {
//	request.post(options, function (err, res, body) {
//	  if (err) {
//	    console.error('Error: ', err);
//	  }
//	  console.log('Server response: ', body);
//	});
//};
////fkt();
