'use strict';
var mongoose = require('mongoose'),
    User = mongoose.model('User'),
    val = require('./validity.js'),
    out = require('./reply.js'),
    utils = require('./utils.js'),
    db = require('./db-utils.js'),
    crypto = require('crypto'),
    config = require('../../config/config.js'),
    cd = require('../../config/message_codes.js');

//TODO: do proper logging! Into a file and not the console...

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
	var pid = utils.random();
	console.log(pid);	
	// saving the user
	var user = new User({
		tum_id: tum_id,
		pseudo_id: pid,
		token: token,
		status: status
	});
	user.save(db.handleDBsave);
	// responding...
	out.reply(res, 200, cd.OK, 
		'Token generation successful. Please send public key.',
		{ tum_id: tum_id, pseudo_id: pid, token: token });
}

/**
 * Requests a token from TUMonline
 * and saves it in the DB if successful.
 * Finally sends return message.
 */
function contactTUMonline(res, tum_id) {
	var path = config.tumOnl_url_path + config.tumOnl_reqToken + 'pUsername=' + 
		tum_id + config.tumOnl_tokenName;
	out.tumOnlineReq(res, path, tum_id, onTumTokenResponse);
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
				out.reply(res, 200, cd.OK, 
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
		out.reply(res, 400, cd.ARG_MISS, 
			'Please set tum_id in the HTTPS request!');
	} else if (val.check_tum_id(tum_id)) { // tum_id has the correct format
		// Check if we already have a token in the DB
		// ...and proceed with function registerGetTokenForUser()...
		db.getUserByTumId(req, res, tum_id, registerGetTokenForUser);
	} else { // Then the tum_id does not adhere to the correct format.
		out.reply(res, 400, cd.ARG_FORM, 
			'tum_id does not have the correct form!');
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
		out.reply(res, 404, cd.NO_USR,
		'tum_id not found in DB. Please start with step 1!');	
	} else { 
		if(user.token !== token) {
			// if token in the request does not equal the DB
			out.reply(res, 400, cd.WRONG_TOK, 
				'token seems to be wrong!');	
		} else {
		// actually store key
		user.key = key;
		user.save(db.handleDBsave);
		out.reply(res, 200, cd.OK,
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
		out.reply(res, 400, cd.ARG_MISS, 
			'Please set tum_id, token and key in the request!');
	} else {
		// check if all parameters have the correct form
		if(val.check_tum_id(tum_id) && val.check_token(token) && val.check_key(key)) {
			// Find user and save the new key
			db.getUserByTumId(req, res, tum_id, registerStoreKeyForUser);
		} else { // something has a wrong format. Send error message...
			out.reply(res, 400, cd.ARG_FORM, 
				'tum_id, token or key do not have the correct form!');
		}
	}
};

