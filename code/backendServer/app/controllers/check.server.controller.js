'use strict';
var mongoose = require('mongoose'),
    User = mongoose.model('User'),
    ldap = require('ldapjs'),
    crypto = require('crypto'),
    out = require('./reply.js'),
    db = require('./db-utils.js'),
    val = require('./validity.js'),
    config = require('../../config/config.js'),
    acc = require('../../config/ad_credentials.js'),
    cd = require('../../config/message_codes.js');

var opts = {
	scope: 'base',
	//filter: '(CN=gu95ray)'
	filter: '(&(&(&(OU=Users)(OU=TU))(OU=IAM))(CN=gu95ray))'
	// (&(&(&(&(OU=Users)(OU=TU))(OU=IAM))
	//attributes: 'cn',
	//sizeLimit: 1
};

/**
 * Callback fired if the Active Directory does not behave properly
 */
function handleADerror(res, err, info) {
	console.log('\n!!! Error connecting to AD. ' + info + ' --- ' + err + ' !!! \n');
	out.reply(res, 500, cd.TUM_ERR,
	'Active Directory is not behaving properly.');
}

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
			var tum_id = 'gu95ray'; //TODO: change
			console.log('----> Trying to connect to TUM AD...');
			
			var client = ldap.createClient({
				url: config.ldap_url
			});
			try {
			  client.bind(acc.user, acc.pw, function(err) {
				if(err) {
					handleADerror(res, err, 'in .bind()');
				}

				console.log('----> AD bind OK.');

				//TODO: do something
				// search string: OU=Users,OU=TU,OU=IAM,DC=ads,DC=mwn,DC=de;
  				// filter: '(&(&(&(&(&(OU=Users)(OU=TU))(OU=IAM))(DC=ads))(DC=mwn))(DC=de))',
				var searchString = config.ldap_search_string + ',CN=' + tum_id;
				client.search('DC=ads,DC=mwn,DC=de', opts, 
					function (err, res) {
	
					console.log('----> AD search returned. ' + res);

					if(err) {
						handleADerror(res, err, 'in .search()');
					}

					res.on('searchEntry', function(entry) {
					  console.log('\n\nentry: ' + JSON.stringify(entry.object));
						//var user = entry.object;
					});
					res.on('searchReference', function(referral) {
					  console.log('\n\nreferral: ' + referral.uris.join());
					});
					res.on('error', function(err) {
					  console.error('\n\nerror: ' + err.message);
					});
					res.on('end', function(result) {
					  console.log('\n\nstatus: ' + result.status);
					});
				});
			  });
			} catch(err) {
				handleADerror(res, err, 'catching');
			}
			
			if (user.status && user.status === 'student') {
				// hashing user token so that the NFC terminal does not get it in clear text

				var shasum = crypto.createHash('sha256');
				shasum.update(user.salt + user.token);	// hashing token + salt
				var hash = shasum.digest('base64');	// in base64 encoding

				// sending the reply
				out.reply(res, 200, cd.OK, 'Valid user. Sending public key.',
					{ pseudo_id: pid,
					  token_hash: hash, 
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
