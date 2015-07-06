'use strict';
var crypto = require('crypto');

/**
 * Returns 16 random Bytes as hex number.
 */
exports.random = function () {
	try {
		return crypto.randomBytes(16).toString('hex');
	} catch (ex) {
		console.error('\n !!! Error creating random string in crypto lib! ' + ex);
		return null;
	}
};

/**
 * Returns a 8 random Bytes in base64 encoding.
 */
exports.salt = function() {
	try {
		// creating 64 bit salt
		return crypto.randomBytes(8).toString('base64');
	} catch (ex) {
		console.log('\n!!! Error while generating random bits for salt! ' + ex);
		return null;
	}
};
