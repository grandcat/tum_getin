'use strict';
var mongoose = require('mongoose'),
    User = mongoose.model('User');

/**
 * Sends a JSON answer message.
 * Last parameter is optional for additional fields
 */
function reply(res, res_status, res_message, json) {
	var msg = { status: res_status, message: res_message };
	if(json !== null && json !== undefined) { // if we have json, concat it to msg
		for (var attrname in json) { msg[attrname] = json[attrname]; }
	}
	res.json(msg);
}

/**
 * Requests a token from TUMonline
 * and saves it in the DB if successful.
 * Finally sends return message.
 */
function contactTUMonline(res, tum_id) {
	//TODO: contact tumonline

	//TODO: send proper response
	reply(res, 200, 'Token generation successful. Please send public key.',
	{ tum_id: 'basfd', pseudo_id: 'afdgtrsyjd', token: 'ewtry' });
}

// helper function
function getUserByTumId(tum_id) {
	User.findOne({
		tum_id: tum_id
	}).exec(function(err, user) {
		if (err) return err;
		if (!user) return null;
		return user;
	});
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
	//TODO: regex for tum_id form!
	} else if (true) { // tum_id has the correct format
		// Check if we already have a token in the DB
		var user = getUserByTumId(tum_id);
		console.log(user);
		if (user === null || user === undefined) { // new user
			contactTUMonline(res, tum_id);
		} else {
			var token = user.token;
			if (token !== undefined) {
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
	} else { // Then the tum_id does not adhere to the correct format.
		reply(400, 'tum_id does not have the correct form!');
	}
};

/**
 * Handle step 2 of the smartphone - backend communication.
 * Looks for tum_id, token and key in the request and returns a status message.
 * Stores the key if everything is correct.
 */
exports.register_store_key = function(req, res) {

	var tum_id = req.query.tum_id;
	var token = req.query.token;
	
	console.log('----> /register store key : tum_id: %s, token: %s ', tum_id, token);
	console.log(req.method);

	console.log(req.body);

	if(token === undefined) { // no token in the request
		// ...then it is the initial request
		// and we ask TUMOnline for a new token.
		console.log('\tNo token sent -> request in TUMOnline');
		// ...
		res.json({ status: 200, message: '/register is sending a new token' });
	} else {
		// The user already has a token.
		// Check if he sent a key and store the key.
		var key = req.query.key;
		

		// ...
		res.json({ status: 200, message: '/register successfully stored key' });
	}
};

