'use strict';
var mongoose = require('mongoose'),
    User = mongoose.model('User'),
    out = require('./reply.js'),
    db = require('./db-utils.js'),
    val = require('./validity.js'),
    cd = require('../../config/message_codes.js');

//TODO: remove code duplicates!!!!!!!!!!!!!!

/**
 *
 */
function returnStoredKey(req, res, pid, user) {
	if (user === null || user === undefined) { // no user found: bad! 
		out.reply(res, 404, cd.NO_USR, 'This pseudo_id does not exist!');
	} else {
		var key = user.key;
		if (key === undefined) {
			out.reply(res, 404, cd.NO_KEY, 'No key for this pseudo_id!');
		} else {
			//TODO: check student status in AD!!!

			if (user.status === 'student') {
				out.reply(res, 200, cd.OK, 'Valid user. Sending public key.',
					{ pseudo_id: pid,
					  student_status: user.status,
					  key: user.key	});
			} else { // not a student!
				out.reply(res, 403, cd.FORBIDDEN, 
					'This person is no longer a student!');
			}
		}
	}
}

exports.check = function(req, res) {
	var pid = req.query.pseudo_id;
	// check if pid is present in the request and has the right format
	if(pid === undefined || !val.check_pseudo_id(pid)) { // then something is wrong
		out.reply(res, 400, cd.ARG_FORM, 
			'pseudo_id does not have the correct form!');
	} else {
		db.getUserByPseudoId(req, res, pid, returnStoredKey);
	}
};
