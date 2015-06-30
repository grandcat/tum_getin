'use strict';
//TODO: kill useless imports
var mongoose = require('mongoose'),
    User = mongoose.model('User'),
    XRegExp = require('xregexp').XRegExp,
    https = require('https'),
    val = require('./validity.js'),
    out = require('./reply.js'),
    db = require('./db-utils.js'),
    cd = require('../../config/message_codes.js'),
    config = require('../../config/config.js'),
    parseString = require('xml2js').parseString;

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
 * The callback fired if the HTTPS to TumOnline makes problems
 */
function handleTumHttpsError(err, res) {
	console.error('Error at contacting TumOnline' + err);
	out.send500error(res);
}

/**
 * The callback fired by the HTTPS request to TumOnline
 */
function handleTumHttpsReq(httpResp, res, tum_id) {
	httpResp.setEncoding('utf-8');
	var responseString = '';
	httpResp.on('data', function(data) {
		responseString += data;
	});
	httpResp.on('end', function() {
		console.log('-----> Response from TUMonline: ' + responseString);
		parseString(responseString, function (err, result) {
			if(err) {
				handleTumHttpsError(err, res);
			}
			onTumActiveResponse(res, tum_id, result);
		});		
	});
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
		console.log('------> Contacting TUMonline...');
		var path = config.tumOnl_url_path + config.tumOnl_isTokenConf + 
			'pToken=' + token;
		var options = {
			host: config.tumOnl_url_host,
			port: 443,
			path: path,
			method: 'GET'
		};
		// Do HTTPS request to TUMonline
		var httpsReq = https.request(options, function(httpRes) {
			handleTumHttpsReq(httpRes, res, token);
			});
		httpsReq.on('error', function(err) {
			handleTumHttpsError(err, httpsReq );
		});
		httpsReq.end();

	} else { // Then the tum_id does not adhere to the correct format.
		out.reply(res, 400, cd.ARG_FORM, 
			'token does not have the correct form!');
	}
};
