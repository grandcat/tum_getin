'use strict';
var XRegExp = require('xregexp').XRegExp;

/**
 * Example: ga99aaa
 */
exports.check_tum_id = function(arg) {
	var regEx = new XRegExp('[a-z]{2}[0-9]{2}[a-z]{3}');
	if(arg.length === 7 && regEx.test(arg)) {
		return true;
	} else {
		console.log('tum_id format error: ' + arg);
		return false;
	}
};

/**
 * Example: 5f494dab23950a6b81c0621b9dc9a876 
 */
exports.check_pseudo_id = function(arg) {
	var regEx = new XRegExp('[a-f0-9]{32}');
	if(arg.length === 32 && regEx.test(arg)) {
		return true;
	} else {
		console.log('pseudo_id format error: ' + arg);
		return false;
	}
};

/**
 * Example: 491652672440A20D6BD49B63E60DADB9
 */
exports.check_token = function(arg) {
	var regEx = new XRegExp('[A-Z0-9]{32}');
	if(arg.length === 32 && regEx.test(arg)) {
		return true;
	} else {
		console.log('Token format error: ' + arg);
		return false;
	}
};


/**
 * Example: 2048 bit = 256 bytes ~393 base64 encoded characters
 */
exports.check_key = function(arg) {
	// var regEx = new XRegExp('[a-f0-9]{514}');
	if(350 < arg.length && arg.length < 400) {
		return true;
	} else {
		console.log('Key format error: ' + arg);
		return false;
	}
};

