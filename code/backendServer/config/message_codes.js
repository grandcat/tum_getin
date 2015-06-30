'use strict';

/**
 * Defines all status codes for answer messages
 */
module.exports = {
	// 0 - 9: general status codes
	OK: 0,		// OK, everything fine
	FORBIDDEN: 5,	// user is not allowed (e.g. no student anymore)
	ERR: 9,		// general server error

	// 10 - 19: DB related problems
	NO_USR: 10,	// no such user found in the DB
	WRONG_TOK: 11,	// token in the request does not match the one in the DB
	NO_KEY: 12,	// no key for this user is known in DB

	// 20 - 29: request argument issues
	ARG_MISS: 20, 	// argument missing
	ARG_FORM: 21,	// argument has a wrong format

	// 30 - 39: TUMonline related issues
	TUM_NO_TOK: 30,	// TUMonline does not answer with a token
	TUM_DUP_TOK: 31 // The user already has a token
};
