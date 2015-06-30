'use strict';

/**
 * Sends a JSON answer message.
 * Last parameter is optional for additional fields
 */
exports.reply = function(res, res_status, res_message, json) {
	var msg = { status: res_status, message: res_message };
	if(json !== null && json !== undefined) { // if we have json, concat it to msg
		for (var attrname in json) { msg[attrname] = json[attrname]; }
	}
	console.log('Sending reply: ' + JSON.stringify(msg));
	res.json(msg);
};

/**
 * Wrapper for reply(). Is sent on server errors.
 */
exports.send500error = function (res) {
	this.reply(res, 500, 'The server encountered an error. Please try again!');
};

