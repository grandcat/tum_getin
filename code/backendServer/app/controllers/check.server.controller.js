'use strict';
var mongoose = require('mongoose'),
    User = mongoose.model('User'),
    ldap = require('ldapjs'),
    crypto = require('crypto'),
    log = require('../../config/logger.js'),
    out = require('./reply.js'),
    db = require('./db-utils.js'),
    val = require('./validity.js'),
    config = require('../../config/config.js'),
    acc = require('../../config/ad_credentials.js'),
    cd = require('../../config/message_codes.js');

var opts = {
	scope: 'sub',
	filter: '',//'(CN=ga00aaa)',
	attributes: 'department',
	sizeLimit: 1
};

/**
 * Callback fired if the Active Directory does not behave properly
 */
function handleADerror(res, err, info) {
	log.error('\n!!! Error connecting to AD. ' + info + ' --- ' + err + ' !!! \n');
	out.reply(res, 500, cd.TUM_ERR,
		'Active Directory is not behaving properly.');
}

/**
 * Callback fired when the AD search has returned something
 */
function handleADresult(res, user, dep) {
	if (dep !== '' && dep === 'Studium') {
		// hashing user token so that the NFC terminal does not get it in clear text

		var shasum = crypto.createHash('sha256');
		shasum.update(user.salt + user.token);	// hashing token + salt
		var hash = shasum.digest('base64');	// in base64 encoding

		// sending the reply
		out.reply(res, 200, cd.OK, 'Valid user. Sending public key.',
			{ pseudo_id: user.pseudo_id,
			  token_hash: hash,
			  student_status: 'student',
			  key: user.key	});
	} else { // not a student!
		out.reply(res, 403, cd.FORBIDDEN, 
			'This person is no longer a student!');
	}
}

/**
 * Callback fired when the DB has found an user for this pseudo_id
 */
function returnStoredKey(httpsReq, httpsRes, pid, user) {
	if (user === null || user === undefined) { // no user found: bad! 
		out.reply(httpsRes, 404, cd.NO_USR, 'This pseudo_id does not exist!');
	} else {
		var key = user.key;
		if (key === undefined) {
			out.reply(httpsRes, 404, cd.NO_KEY, 'No key for this pseudo_id!');
		} else if (process.env.NO_AD && process.env.NO_AD === 'true') {
			// for testing without AD checks when we have no LRZ-VPN available
			handleADresult(httpsRes, user, 'Studium');
		} else {
			log.info('----> Trying to connect to TUM AD...');
			try {
				var client = ldap.createClient({
					url: config.ldap_url
				});
				try {
				  client.bind(acc.user, acc.pw, function(err) {
					if(err) {
						handleADerror(httpsRes, err, 'in .bind()');
					}
					log.info('----> AD bind OK.');
					
					// setting search for tum_id
					opts.filter = '(CN=' + user.tum_id + ')';

					// search string: OU=Users,OU=TU,OU=IAM,DC=ads,DC=mwn,DC=de;
					client.search(config.ldap_search_string, opts, 
						function (err, res) {

						log.info('----> AD search returned. ' + res);
						if(err) {
							handleADerror(httpsRes, err, 'in .search()');
						}
						var dep = ''; // department attribute stores student status
						res.on('searchEntry', function(entry) {
							log.info('\nAD entry: ' + JSON.stringify(entry.object));
							dep = entry.object.department;
							log.info('\t-> ' + dep);
						});
						//res.on('searchReference', function(referral) {
						//  console.log('\n\nreferral: ' + referral.uris.join());
						//});
						res.on('error', function(err) {
							log.error('\n\nerror: ' + err.message);
							handleADerror(httpsRes, err, 'search res.on(error)');
						});
						res.on('end', function(result) {
							log.error('----> AD return status: ' + result.status + '\n');
							handleADresult(httpsRes, user, dep);
						});
					});
				  });
				} catch(err) {
					handleADerror(httpsRes, err, 'catching LDAP bind or search problem');
				}
			} catch(err) {
				handleADerror(httpsRes, err, 'catching LDAP client creation problem');
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
