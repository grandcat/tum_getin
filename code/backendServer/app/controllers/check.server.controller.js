'use strict';
var mongoose = require('mongoose'),
    User = mongoose.model('User');

//TODO: remove code duplicates!!!!!!!!!!!!!!

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
function getUserByPseudoId(req, res, pid, callback) {
	User.findOne({ pseudo_id: pid }, function(err, user) {
		if (err) {
			handleDBsave(err, res);
		}
		callback(req, res, pid, user);
	});
}

/**
 *
 */
function returnStoredKey(req, res, pid, user) {
	if (user === null || user === undefined) { // no user found: bad! 
		reply(res, 404, 'This pseudo_id does not exist!');
	} else {
		var key = user.key;
		if (key === undefined) {
			reply(res, 404, 'No key for this pseudo_id!');
		} else {
			//TODO: check student status in AD!!!

			if (user.status === 'student') {
				reply(res, 200, 'Valid user. Sending public key.',
					{ pseudo_id: pid,
					  student_status: user.status,
					  key: user.key	});
			} else { // not a student!
				reply(res, 403, 
					'This person is no longer a student!');
			}
		}
	}
}

exports.check = function(req, res) {
	var pid = req.query.pseudo_id;
	//TODO: check pid format regex
	// check if pid is present in the request and has the right format
	if(pid === undefined || false) { // then something is wrong
		reply(res, 400, 'pseudo_id does not have the correct form!');
	} else {
		getUserByPseudoId(req, res, pid, returnStoredKey);
	}
};
