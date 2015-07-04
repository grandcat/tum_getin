'use strict';
var mongoose = require('mongoose'),
    User = mongoose.model('User'),
    ldap = require('ldapjs'),
    out = require('./reply.js'),
    db = require('./db-utils.js'),
    val = require('./validity.js'),
    config = require('../../config/config.js'),
//    acc = require('../../config/ad_credentials.js'),
    cd = require('../../config/message_codes.js');

var opts = {
  filter: '(objectclass=user)',
  scope: 'sub'
};

/**
 * Callback fired when ...
 */

//client.search('ou=users,o=acme.com', opts, function(err, res) {
//  assert.ifError(err);
//
//  res.on('searchEntry', function(entry) {
//    console.log('entry: ' + JSON.stringify(entry.object));
//  });
//  res.on('searchReference', function(referral) {
//    console.log('referral: ' + referral.uris.join());
//  });
//  res.on('error', function(err) {
//    console.error('error: ' + err.message);
//  });
//  res.on('end', function(result) {
//    console.log('status: ' + result.status);
//  });
//});

/**
 * Callback fired when the DB has found an user for this pseudo_id
 */
function returnStoredKey(req, res, pid, user) {
	if (user === null || user === undefined) { // no user found: bad! 
		out.reply(res, 404, cd.NO_USR, 'This pseudo_id does not exist!');
	} else {
		var key = user.key;
		if (key === undefined) {
			out.reply(res, 404, cd.NO_KEY, 'No key for this pseudo_id!');
		} else {
			//TODO: check student status in AD!!!
			//console.log('----> Trying to connect to TUM AD...');
			//
			//var client = ldap.createClient({
			//	url: config.ldap_url
			//});
			//client.bind(acc.user, acc.pw, function(err) {
			//	if(err) {
			//		console.log('Error connecting to AD: ' + err);
			//		out.reply(res, 500, cd.TUM_ERR,
			//		'Active Directory is not behaving properly.');
			//	}
			//	console.log('AD bind OK.');
			//	//TODO: do something
			//	client.search('CN=test,OU=Development,DC=Home', opts, 
			//		function (err, search) {
			//		search.on('searchEntry', function (entry) {
			//			var user = entry.object;
			//			console.log(user.objectGUID);
			//		});
			//	});
			//});

			if (user.status && user.status === 'student') {
				out.reply(res, 200, cd.OK, 'Valid user. Sending public key.',
					{ pseudo_id: pid,
					  student_status: user.status,
					  key: user.key	});
			} else { // not a student!
				out.reply(res, 403, cd.FORBIDDEN, 
					'This person is no longer a student!');
			}
		}
	}
}

/**
 * Request contains a pseudo ID.
 * If we find a corresponding user in the DB, and we have a key,
 * and a request to the TUM AD says that this user is a student...
 * --> then we will send the user's key to the NFC reader.
 */
exports.check = function(req, res) {
	var pid = req.query.pseudo_id;
	// check if pid is present in the request and has the right format
	if(pid === undefined || !val.check_pseudo_id(pid)) { // then something is wrong
		out.reply(res, 400, cd.ARG_FORM, 
			'pseudo_id does not have the correct form!');
	} else {
		db.getUserByPseudoId(req, res, pid, returnStoredKey);
	}
};
