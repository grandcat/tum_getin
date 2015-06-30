'use strict';
var crypto = require('crypto');

/**
 * Returns 16 random Bytes as hex number.
 */
exports.random = function () {
	try {
		return crypto.randomBytes(16).toString('hex');
	} catch (ex) {
		console.error('Error creating random string in crypto lib! ' + ex);
		return null;
	}
};
