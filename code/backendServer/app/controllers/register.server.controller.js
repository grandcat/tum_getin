'use strict';
var mongoose = require('mongoose'),
    User = mongoose.model('User'),
    XRegExp = require('xregexp').XRegExp,
    crypto = require('crypto'),
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
 * Example: 2048 bit = 256 bytes = 512 hex numbers
 * Plus first Byte 00 (signed), so 514 in total.
 */
function check_key(arg) {
	var regEx = new XRegExp('[a-f0-9]{514}');
	if(arg.length === 514 && regEx.test(arg)) {
		return true;
	} else {
		console.log('Key format error: ' + arg);
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
 * Returns 16 random Bytes as hex number.
 */
function random () {
	try {
		return crypto.randomBytes(16).toString('hex');
	} catch (ex) {
		console.error('Error creating random string in crypto lib! ' + ex);
		return null;
	}
}

/**
 * The callback fired if everything with the token request to 
 * TumOnline went fine.
 */ 
function onTumTokenResponse(res, tum_id, tumAnswerJson) {
	console.log('onTumTokenRes; id: ' + tum_id + '; jsonAns: ' + tumAnswerJson);

	var token = tumAnswerJson.token;
	console.log('Extracting token from TUM response: ' + token);

	var status = 'student'; //TODO: check that!

	// generating pseudo ID
	var pid = random();
	console.log(pid);	
	// saving the user
	var user = new User({
		tum_id: tum_id,
		pseudo_id: pid,
		token: token,
		status: status
	});
	user.save(handleDBsave);
	// responding...
	reply(res, 200, 'Token generation successful. Please send public key.',
		{ tum_id: tum_id, pseudo_id: pid, token: token });
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
			onTumTokenResponse(res, tum_id, result);
		});		
	});
}

/**
 * Requests a token from TUMonline
 * and saves it in the DB if successful.
 * Finally sends return message.
 */
function contactTUMonline(res, tum_id) {
	console.log('------> Contacting TUMonline...');
	//TODO: add token name parameter
	var path = path_tumOnl + url_reqToken + 'pUsername=' + tum_id;
	var options = {
		host: host_tumOnl,
		port: 443,
		path: path,
		method: 'GET'
	};
	// Do HTTPS request to TUMonline
	var req = https.request(options, function(httpRes) {
		handleTumHttpsReq(httpRes, res, tum_id);
		});
	req.on('error', function(err) {
		handleTumHttpsError(err, res);
	});
	req.end();
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
 * Proceeds with the register_get_token method 
 * after a user has been searched in DB
 */
function registerGetTokenForUser(req, res, tum_id, user) {
	if (user === null || user === undefined) { // new user
		contactTUMonline(res, tum_id);
	} else {
		var token = user.token;
		if (token !== undefined) {
			console.log('Register returning saved token; id: ' + 
					tum_id + ', tok: ' + token);
			//TODO: validity check!
			// Check if token from DB is still valid
			if(true) { // if yes, then send the one from the DB
				reply(res, 200, 
				'Token generation successful. Please send public key.',
				{ tum_id: user.tum_id, 
				  pseudo_id: user.pseudo_id,
				  token: user.token });
			} else { // otherwise request a new one from TUMonline
				contactTUMonline(res, tum_id);
			}
		} else {
			// Request new token from TUMonline and save it in DB
			contactTUMonline(res, tum_id);
		}
	}
}

/**
 * Handle step 1 of the smartphone - backend communication.
 * Looks for tum_id in the request and returns a token.
 * This token is either fetched from the DB or from TUMonline, if it is the first time.
 */
exports.register_get_token = function(req, res) {

	var tum_id = req.query.tum_id;
	//console.log('----> /register get token : tum_id: %s, token: %s ', tum_id, token);

	if(tum_id === undefined) { // then something is wrong
		// Send error message back
		reply(res, 400, 'Please set tum_id in the HTTPS request!');
	} else if (check_tum_id(tum_id)) { // tum_id has the correct format
		// Check if we already have a token in the DB
		// ...and proceed with function registerGetTokenForUser()...
		getUserByTumId(req, res, tum_id, registerGetTokenForUser);
	} else { // Then the tum_id does not adhere to the correct format.
		reply(res, 400, 'tum_id does not have the correct form!');
	}
};

/**
 * Proceeds with the register_store_key method 
 * after a user has been searched in DB
 */
function registerStoreKeyForUser(req, res, tum_id, user) {
	var token = req.body.token;
	var key = req.body.key;
	if(user === undefined || user === null) {
		// if user is not found in db, send error message
		reply(res, 404,
		'tum_id not found in DB. Please start with step 1!');	
	} else { 
		if(user.token !== token) {
			// if token in the request does not equal the DB
			reply(res, 400, 'token seems to be wrong!');	
		} else {
		// actually store key
		user.key = key;
		user.save(handleDBsave);
		reply(res, 200, 
			'Key stored successfully. User registration complete.');
		}
	}
}

/**
 * Handle step 2 of the smartphone - backend communication.
 * Looks for tum_id, token and key in the request and returns a status message.
 * Stores the key if everything is correct.
 */
exports.register_store_key = function(req, res) {

	var tum_id = req.body.tum_id;
	var token = req.body.token;
	var key = req.body.key;
	//console.log('----> /register store key : tum_id: %s, token: %s ', tum_id, token);

	// check if all parameters are set in the request
	if(tum_id === undefined || token === undefined || key === undefined) { 
		// something is missing...
		reply(res, 400, 'Please set tum_id, token and key in the request!');
	} else {
		// check if all parameters have the correct form
		if(check_tum_id(tum_id) && check_token(token) && check_key(key)) {
			// Find user and save the new key
			getUserByTumId(req, res, tum_id, registerStoreKeyForUser);
		} else { // something has a wrong format. Send error message...
			reply(res, 400, 'tum_id, token or key do not have the correct form!');
		}
	}
};

