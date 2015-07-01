'use strict';
//TODO: kill useless imports
var mongoose = require('mongoose'),
    User = mongoose.model('User'),
    val = require('./validity.js'),
    out = require('./reply.js'),
    cd = require('../../config/message_codes.js'),
    config = require('../../config/config.js');

//TODO: do proper logging! Into a file and not the console...

/**
 * The callback fired if everything with the request to 
 * TumOnline went fine.
 */ 
function onTumActiveResponse(res, token, tumAnswerJson) {
	console.log('tumActResp; id: ' + token + '; jsonAns: ' + tumAnswerJson);

	var active = tumAnswerJson.confirmed;
	console.log('Extracting token from TUM response: ' + token);
	
	if(active === null || active === undefined || active === false) {
		out.reply(res, 200, cd.OK, 'Token is not activated.', {active: active});
	} else { // token is active
		out.reply(res, 200, cd.OK, 'Token is activated.', {active: active});
	}
}

/**
 * Sends a request to TUMonline to ask if the token is activated.
 * Possible TUMonline responses: true or false (in XML syntax)
 */
exports.tokenactive = function(req, res) {
	var token = req.query.token;
	if(token === undefined) { // then something is wrong
		// Send error message back
		out.reply(res, 400, cd.ARG_MISS, 
			'Please set token in the HTTPS request!');
	} else if (val.check_token(token)) { // tum_id has the correct format
		var path = config.tumOnl_url_path + config.tumOnl_isTokenConf + 
			'pToken=' + token;
		out.tumOnlineReq(res, path, token, onTumActiveResponse);		
	} else { // Then the tum_id does not adhere to the correct format.
		out.reply(res, 400, cd.ARG_FORM, 
			'token does not have the correct form!');
	}
};
