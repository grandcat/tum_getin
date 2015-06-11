'use strict';

exports.register_get_token = function(req, res) {

	var tum_id = req.query.tumid;
	var token = req.query.token;
	
	console.log('----> /register get token : tum_id: %s, token: %s ', tum_id, token);

	if(token === undefined) { // no token in the request
		// ...then it is the initial request
		// and we ask TUMOnline for a new token.
		console.log('\tNo token sent -> request in TUMOnline');
		// ...
		res.json({ message: '/register is sending a new token' });
	} else {
		// The user already has a token.
		// Check if he sent a key and store the key.
		var key = req.query.key;
		// ...
		res.json({ message: '/register successfully stored key' });
	}
};


exports.register_store_key = function(req, res) {

	var tum_id = req.query.tumid;
	var token = req.query.token;
	
	console.log('----> /register store key : tum_id: %s, token: %s ', tum_id, token);

	console.log(req.body);

	if(token === undefined) { // no token in the request
		// ...then it is the initial request
		// and we ask TUMOnline for a new token.
		console.log('\tNo token sent -> request in TUMOnline');
		// ...
		res.json({ message: '/register is sending a new token' });
	} else {
		// The user already has a token.
		// Check if he sent a key and store the key.
		var key = req.query.key;
		

		// ...
		res.json({ message: '/register successfully stored key' });
	}
};
