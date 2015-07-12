'use strict';
var vows = require('vows'),
    assert = require('assert'),
    https = require('https'),
    fs = require('fs'),
    cd = require('../config/message_codes.js');
// getting test data from central test data folder
var testUser1 = require('../../testResources/user_01.json');
var testUser3 = require('../../testResources/user_03.json');
var realUser = require('../../testResources/real_user.json');
var realUserNewToken = '';
var realUserPseudoId = '';
var config = require('./test_config');
//require('./test_helpers');

//////////////////////////////////////////////////////////////////////////////////////
vows.describe('Backend - Smartphone Interface /register').addBatch({
	'/register post key correct': {
		topic: function() {
			callback = this.callback;
			var user = {
			  tum_id: realUser.tum_id,
			  token: realUser.temp_token,
			  key: realUser.key 
			};
			sendPost('/register', user);
		},
		'Check response status': function (topic) {
            		assert.equal (topic.status, 200);
            		assert.equal (topic.code, 0);
        	}
	}
}).export(module);

//////////////////////////////////////////////////////////////////////////////////////
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
