'use strict';
var parseString = require('xml2js').parseString,
    https = require('https'),
    config = require('../../config/config.js');
//var util = require('util'); // for deep inspection of objects

/**
 * Sends a JSON answer message.
 * Last parameter is optional for additional fields
 */
exports.reply = function(res, http_status, msg_code, res_message, json) {
	var msg = { status: http_status, code: msg_code, message: res_message };
	if(json !== null && json !== undefined) { // if we have json, concat it to msg
		for (var attrname in json) { msg[attrname] = json[attrname]; }
	}
	// very verbose form:
	//console.log('-> Sending reply: \n\tRequest was: %s \n\tResponding: %s', 
	//		util.inspect(res, false, 1), JSON.stringify(msg));
	console.log('-> Sending reply: \n\tRequest was: %s %s \n\tResponding: %s', 
			res.req.method, res.req.url, JSON.stringify(msg));
	res.json(msg);
};

/**
 * Wrapper for reply(). Is sent on server errors.
 */
exports.send500error = function (res) {
	this.reply(res, 500, 'The server encountered an error. Please try again!');
};

/**
 * The callback fired if the HTTPS to TumOnline makes problems
 */
exports.handleTumHttpsError = function(err, res) {
	console.error('Error at contacting TumOnline' + err);
	this.send500error(res);
};

/**
 * The callback fired by the HTTPS request to TumOnline
 */
exports.handleTumHttpsReq = function(httpResp, res, tum_id, callback) {
	httpResp.setEncoding('utf-8');
	var responseString = '';
	httpResp.on('data', function(data) {
		responseString += data;
	});
	httpResp.on('end', function() {
		console.log('-----> Response from TUMonline: ' + responseString);
		parseString(responseString, function (err, result) {
			if(err) {
				this.handleTumHttpsError(err, res);
			}
			callback(res, tum_id, result);
		});		
	});
};

/**
 * Sends arbitrary requests to TUMonline
 */
exports.tumOnlineReq = function(res, path, parameter, callback) {
	console.log('------> Contacting TUMonline...');
	var options = {
		host: config.tumOnl_url_host,
		port: 443,
		path: path,
		method: 'GET'
	};
	var this_pointer = this;
	// Do HTTPS request to TUMonline
	var req = https.request(options, function(httpRes) {
		this_pointer.handleTumHttpsReq(httpRes, res, parameter, callback);
		});
	req.on('error', function(err) {
		this_pointer.handleTumHttpsError(err, res);
	});
	req.end();
};

