'use strict';
//var util = require('util'); // for deep inspection of objects

/**
 * Sends a JSON answer message.
 * Last parameter is optional for additional fields
 */
exports.reply = function(res, http_status, msg_code, res_message, json) {
	var msg = { status: http_status, code: msg_code, message: res_message };
	if(json !== null && json !== undefined) { // if we have json, concat it to msg
		for (var attrname in json) { msg[attrname] = json[attrname]; }
	}
	// very verbose form:
	//console.log('-> Sending reply: \n\tRequest was: %s \n\tResponding: %s', 
	//		util.inspect(res, false, 1), JSON.stringify(msg));
	console.log('-> Sending reply: \n\tRequest was: %s %s \n\tResponding: %s', 
			res.req.method, res.req.url, JSON.stringify(msg));
	res.json(msg);
};

/**
 * Wrapper for reply(). Is sent on server errors.
 */
exports.send500error = function (res) {
	this.reply(res, 500, 'The server encountered an error. Please try again!');
};

