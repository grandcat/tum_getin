'use strict';
var mongoose = require('mongoose'),
    User = mongoose.model('User'),
    log = require('../../config/logger.js'),
    val = require('./validity.js'),
    out = require('./reply.js'),
    utils = require('./utils.js'),
    cd = require('../../config/message_codes.js'),
    db = require('./db-utils.js');

/** 
 * Callback fired when a user has been found in the DB.
 * Proceeds with functionality for .renew_pseudo_id()
 */
function onGetUserForRenew(req, res, tum_id, user) {
	var token = req.query.token;
	if(user === null || user === undefined) { // no user found
		out.reply(res, 200, cd.NO_USR, 'User does not exist.');
	} else {
		if (token === user.token) {
			// generating new pseudo ID
			var pid = utils.random();
			// generating new salt
			var salt = utils.salt();

			if (pid === null || salt === null) {
				out.send500error(res);
			} else {
				// saving the user
				user.pseudo_id = pid;
				user.salt = salt;
				user.save(db.handleDBsave);
				// responding...
				out.reply(res, 200, cd.OK, 'Here is your new pseudo_id and salt.',
					{ tum_id: tum_id, pseudo_id: pid, salt: salt });
			}
		} else { // consistency problem!!! 
			out.reply(res, 400, cd.ARG_FORM, 
				'Request argument does not have the correct form.');
		}
	}
}

/**
 * Renew the pseudo_id of a user.
 * Makes some privacy possible. If a user often changes his pid, NFC readers
 * cannot track him.
 */
exports.renew_pseudo_id = function(req, res) {
	var tum_id = req.query.tum_id;
	var token = req.query.token;
	if(tum_id === undefined || token === undefined) { // then something is wrong
		// Send error message back
		out.reply(res, 400, cd.ARG_MISS, 'Request argument missing.'); 
	} else if (val.check_tum_id(tum_id) && val.check_token(token)) { // correct format
		// see if we have this user in the DB.
		// then proceed fith function onGetUserForRenew()
		db.getUserByTumId(req, res, tum_id, onGetUserForRenew);
	} else { // Then the tum_id does not adhere to the correct format.
		out.reply(res, 400, cd.ARG_FORM, 
			'Request argument does not have the correct form.');
	}
};
