'use strict';
//TODO: kill useless imports
var mongoose = require('mongoose'),
    User = mongoose.model('User'),
    XRegExp = require('xregexp').XRegExp,
    https = require('https'),
    parseString = require('xml2js').parseString;
// 'https://campus.tum.de/tumonline/wbservicesbasic.';
var host_tumOnl = 'campus.tum.de';
var path_tumOnl = '/tumonline/wbservicesbasic.';
var url_reqToken = 'requestToken?';

//TODO: do proper logging! Into a file and not the console...

/**
 * Sends a JSON answer message.
 * Last parameter is optional for additional fields
 */
function reply(res, res_status, res_message, json) {
	var msg = { status: res_status, message: res_message };
	if(json !== null && json !== undefined) { // if we have json, concat it to msg
		for (var attrname in json) { msg[attrname] = json[attrname]; }
	}
	console.log('Sending reply: ' + JSON.stringify(msg));
	res.json(msg);
}

/**
 * Example: ga99aaa
 */
function check_tum_id(arg) {
	var regEx = new XRegExp('[a-z]{2}[0-9]{2}[a-z]{3}');
	if(arg.length === 7 && regEx.test(arg)) {
		return true;
	} else {
		console.log('tum_id format error: ' + arg);
		return false;
	}
}

/**
 * Example: 5f494dab23950a6b81c0621b9dc9a876 
 */
function check_pseudo_id(arg) {
	var regEx = new XRegExp('[a-f0-9]{32}');
	if(arg.length === 32 && regEx.test(arg)) {
		return true;
	} else {
		console.log('pseudo_id format error: ' + arg);
		return false;
	}
}

/**
 * Example: 491652672440A20D6BD49B63E60DADB9
 */
function check_token(arg) {
	var regEx = new XRegExp('[A-Z0-9]{32}');
	if(arg.length === 32 && regEx.test(arg)) {
		return true;
	} else {
		console.log('Token format error: ' + arg);
		return false;
	}
}

/**
 * Wrapper for reply(). Is sent on server errors.
 */
function send500error(res) {
	reply(res, 500, 'The server encountered an error. Please try again!');
}

/**
 * Is called on DB save operations.
 * Sends 500 error message if on error
 */
function handleDBsave(err, res) {
	if (err) { 
		console.err('user save error: ', err); 
		send500error(res);
	}
}

/**
 * helper function. Looks up user by tum_id and calls callback
 */
function getUserByTumId(req, res, tum_id, callback) {
	User.findOne({ tum_id: tum_id }, function(err, user) {
		if (err) {
			handleDBsave(err, res);
		}
		callback(req, res, tum_id, user);
	});
}


/**
 * The callback fired if everything with the request to 
 * TumOnline went fine.
 */ 
function onTumActiveResponse(res, tum_id, tumAnswerJson) {
	console.log('tumActResp; id: ' + tum_id + '; jsonAns: ' + tumAnswerJson);

	//TODO: change from here on
	var token = tumAnswerJson.token;
	console.log('Extracting token from TUM response: ' + token);

	var status = 'student'; //TODO: check that!

}

/**
 * The callback fired if the HTTPS to TumOnline makes problems
 */
function handleTumHttpsError(err, res) {
	console.error('Error at contacting TumOnline' + err);
	send500error(res);
}

/**
 * The callback fired by the HTTPS request to TumOnline
 */
function handleTumHttpsReq(httpResp, res, tum_id) {
	httpResp.setEncoding('utf-8');
	var responseString = '';
	httpResp.on('data', function(data) {
		responseString += data;
	});
	httpResp.on('end', function() {
		console.log('-----> Response from TUMonline: ' + responseString);
		parseString(responseString, function (err, result) {
			if(err) {
				handleTumHttpsError(err, res);
			}
			onTumActiveResponse(res, tum_id, result);
		});		
	});
}

/**
 *
 */
exports.tokenactive = function(req, res) {
	//TODO: ....

	var tum_id = req.query.tum_id;

	if(tum_id === undefined) { // then something is wrong
		// Send error message back
		reply(res, 400, 'Please set tum_id in the HTTPS request!');
	} else if (check_tum_id(tum_id)) { // tum_id has the correct format
		//TODO: contact TumOnline here...
		console.log('------> Contacting TUMonline...');
		var path = path_tumOnl + url_reqToken + 'pUsername=' + tum_id; //TODO: path
		var options = {
			host: host_tumOnl,
			port: 443,
			path: path,
			method: 'GET'
		};
		// Do HTTPS request to TUMonline
		var httpsReq = https.request(options, function(httpRes) {
			handleTumHttpsReq(httpRes, res, tum_id);
			});
		httpsReq.on('error', function(err) {
			handleTumHttpsError(err, httpsReq );
		});
		httpsReq.end();

	} else { // Then the tum_id does not adhere to the correct format.
		reply(res, 400, 'tum_id does not have the correct form!');
	}
};
