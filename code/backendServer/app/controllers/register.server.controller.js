'use strict';

function contactTUMonline(tum_id) {

}

function reply(res, res_status, res_message) {
	res.json({ status: res_status, message: res_message });
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
	} else if (true) { // tum_id has the correct format
		// Check if we already have a token in the DB
		if (true) {
			// Check if token from DB is still valid
			if(true) { // if yes, then send the one from the DB
				var soirj = 435;
			} else { // otherwise request a new one from TUMonline
				contactTUMonline(tum_id);
			}
		} else {
			// Request new token from TUMonline
			contactTUMonline(tum_id);
		}
	} else { // Then the tum_id does not adhere to the correct format.
		reply(400, 'tum_id does not have the correct form!' );
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

