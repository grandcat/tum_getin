'use strict';
var mongoose = require('mongoose'),
    User = mongoose.model('User'),
    log = require('../../config/logger.js'),
    val = require('./validity.js'),
    out = require('./reply.js'),
    cd = require('../../config/message_codes.js'),
    db = require('./db-utils.js'),
    config = require('../../config/config.js');

/**
 * The callback fired if everything with the request to 
 * TumOnline went fine.
 */ 
function onTumActiveDeleteResponse(res, tum_id, tumAnswerJson) {
	log.info('tumActDelResp; id: ' + tum_id + '; jsonAns: ' + JSON.stringify(tumAnswerJson));

	var active = tumAnswerJson.confirmed;
	
	if(active === null || active === undefined || active === 'false') {
		// fine. delete user now
		User.remove({ tum_id: tum_id }, function(err) {
			if (err) { log.info('user remove error: ', err); }
			out.reply(res, 200, cd.OK, 'Account has been deleted.');
		});
	} else { // token is active
		// problem. token is active but user has not given token in request
		out.reply(res, 403, cd.ARG_MISS,
			'Your token is still active. Plese send a request with the token.');
	}
}

/**
 * Callback fired when a user has been found in the DB.
 * Proceeds with functionality needed by remove().
 */
function onGetUserForDelete(req, res, tum_id, user) {
	var token = req.query.token;
	if(user === null || user === undefined) { // user is already deleted
		out.reply(res, 200, cd.OK, 'Account does not exist.');
	} else if (token === undefined && user.token !== undefined) {
		// no token in request, but we have a user in the DB.
		// delete can only work if the token in the DB does not exist 
		// in TUMonline anymore or is inactive there.
		var path = config.tumOnl_url_path + config.tumOnl_isTokenConf + 
			'pToken=' + user.token;
		// on a positive answer, proceed with callback function
		out.tumOnlineReq(res, path, user.tum_id, onTumActiveDeleteResponse);		
	} else {
		if (val.check_token(token)) {
			if (token === user.token) {
				User.remove({ tum_id: tum_id }, function(err) {
					if (err) { log.info('user remove error: ', err); }
					out.reply(res, 200, cd.OK, 'Account has been deleted.');
				});
			} else { // consistency problem!!! 
				out.reply(res, 400, cd.ARG_FORM, 
					'Request argument does not have the correct form.');
			}
		} else { //invalid form
			out.reply(res, 400, cd.ARG_FORM, 
				'Request argument does not have the correct form.');
		}
	}
}

/**
 * Removes the whole user account from the DB.
 * If the Token is inactive or has been deleted in TUMonline, only tum_id as
 * request parameter is necessary. Otherwise, the token has to be sent, too!
 */
exports.remove = function(req, res) {
	var tum_id = req.query.tum_id;
	if(tum_id === undefined) { // then something is wrong
		// Send error message back
		out.reply(res, 400, cd.ARG_MISS, 'Request argument missing.'); 
	} else if (val.check_tum_id(tum_id)) { // tum_id has the correct format
		// see if we have this user in the DB.
		// then proceed fith function onGetUserForDelete()
		db.getUserByTumId(req, res, tum_id, onGetUserForDelete);
	} else { // Then the tum_id does not adhere to the correct format.
		out.reply(res, 400, cd.ARG_FORM, 
			'Request argument does not have the correct form.');
	}
};
