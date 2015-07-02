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
var config = require('./test_config');
//require('./test_helpers');

//////////////////////////////////////////////////////////////////////////////////////
vows.describe('Backend - NFC Reader Interface /check').addBatch({
	'/check get user public key correct': {
		topic: function() {
			callback = this.callback;
			sendGet('/check?pseudo_id=' + testUser1.pseudo_id);
		},
		'Check response status': function (topic) {
            		assert.equal (topic.status, 200);
            		assert.equal (topic.code, 0);
        	},
		'Check response user data': function (topic) {
			assert.equal (topic.pseudo_id, testUser1.pseudo_id);
			assert.equal (topic.student_status, testUser1.status);
			assert.equal (topic.key, testUser1.key);
        	}

	}
}).export(module);

//////////////////////////////////////////////////////////////////////////////////////
vows.describe('Backend - Smartphone Interface /register').addBatch({
	'/register get token, correct, new user': {
		topic: function() {
			callback = this.callback;
			sendGet('/register?tum_id=' + realUser.tum_id);
		},
		'Check response status': function (topic) {
            		assert.equal (topic.status, 200);
            		assert.equal (topic.code, 0);
        	},
		'Check response token': function (topic) {
			assert.isNotNull (topic.token);
			assert.isFalse (topic.token === undefined);
			// save token in temp object
			// later we can check if the server has saved it, too
			realUserNewToken = topic.token;
        	}

	}
}).addBatch({
	'/register get token, known user, see if we get an error': {
		topic: function() {
			callback = this.callback;
			sendGet('/register?tum_id=' + realUser.tum_id);
		},
		'Check response status': function (topic) {
            		assert.equal (topic.status, 400);
            		assert.equal (topic.code, cd.TUM_DUP_TOK);
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
            		assert.equal (topic.code, 0);
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
            		assert.equal (topic.code, cd.ARG_MISS);
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
            		assert.equal (topic.code, cd.ARG_MISS);
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
            		assert.equal (topic.code, cd.ARG_MISS);
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
            		assert.equal (topic.code, cd.NO_USR);
        	}
	}
}).export(module);

//////////////////////////////////////////////////////////////////////////////////////
vows.describe('Backend - Smartphone Interface /register for real user!').addBatch({
	'/register get token correct': {
		topic: function() {
			callback = this.callback;
			sendGet('/register?tum_id=' + realUser.tum_id);
		},
		'Check response status': function (topic) {
            		assert.equal (topic.status, 200);
            		assert.equal (topic.code, 0);
        	},
		'Check response tum_id is correct': function (topic) {
            		assert.equal (topic.tum_id, realUser.tum_id);
        	},
		'Check response has pseudo_id': function (topic) {
			assert.isNotNull (topic.pseudo_id); 
			assert.isFalse (topic.pseudo_id === undefined); 
        	},
		'Check response has token': function (topic) {
			assert.isNotNull (topic.token);
			assert.isFalse (topic.token === undefined);
        	}

	}
}); //////// !!!!!!!!!!!! stupid test -- not active -- should lead to error!!!!!!!

//////////////////////////////////////////////////////////////////////////////////////
vows.describe('Backend - Smartphone Interface /tokenActive for real user!').addBatch({
	'/tokenActive for active token correct': {
		topic: function() {
			callback = this.callback;
			sendGet('/tokenActive?token=' + realUser.token);
		},
		'Check response status': function (topic) {
            		assert.equal (topic.status, 200);
            		assert.equal (topic.code, 0);
        	},
		'Check response active field': function (topic) {
			assert.equal (topic.active, 'true');
        	}
	}
}).addBatch({
	'/tokenActive for inactive token correct': {
		topic: function() {
			callback = this.callback;
			sendGet('/tokenActive?token=491652672440A20D6BD49B5566778899');
		},
		'Check response status': function (topic) {
            		assert.equal (topic.status, 200);
            		assert.equal (topic.code, 0);
        	},
		'Check response active field': function (topic) {
			assert.equal (topic.active, 'false');
        	}
	}
}).export(module);

//////////////////////////////////////////////////////////////////////////////////////
vows.describe('Backend - Smartphone Interface /remove for real user!').addBatch({
	'/remove for inactive token correct, easy request': {
		topic: function() {
			callback = this.callback;
			sendGet('/remove?tum_id=' + realUser.tum_id);
		},
		'Check response status': function (topic) {
            		assert.equal (topic.status, 200);
            		assert.equal (topic.code, 0);
        	}
	}
}).addBatch({
	'Now a new register should work again because the user has been deleted.': {
		topic: function() {
			callback = this.callback;
			sendGet('/register?tum_id=' + realUser.tum_id);
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
