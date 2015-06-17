'use strict'
var vows = require('vows'),
    assert = require('assert'),
    https = require('https'),
    fs = require('fs');
// getting test data from central test data folder
var testUser1 = require('../../testResources/user_01.json');
var config = require('./test_config');

var req; // request object
global.callback;   // callback function called on server response
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
global.sendData = function(path, method, jsonString) {
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
global.sendGet = function(path) {
	sendData(path, 'GET', null);
}
// Wrapper for sendData with POST
global.sendPost = function(path, jsonString) {
	global.sendData(path, 'POST', jsonString);
}

