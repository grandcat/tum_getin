'use strict';

exports.init = function(req, res) {
	//console.log('----> /register');

	var tum_id = req.query.tumid;
	var token = req.query.token;

	//console.log('\ttum_id: ' + tum_id);
	//console.log('\ttoken: ' + token);

	if(true) { // no token in the request
		// ...then it is the initial request
		// and we ask TUMOnline for a new token.
	} else {
		// The user already has a token.
		// Check if he sent a key and store the key.
	}
	res.json({ message: 'Answer from /register' });
};
