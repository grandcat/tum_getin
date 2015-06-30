'use strict';
var mongoose = require('mongoose'),
    User = mongoose.model('User'),
    out = require('./reply.js');

/**
 * helper function. Looks up user by tum_id and calls callback
 */
exports.getUserByTumId = function(req, res, tum_id, callback) {
	User.findOne({ tum_id: tum_id }, function(err, user) {
		if (err) {
			this.handleDBsave(err, res);
		}
		callback(req, res, tum_id, user);
	});
};

/**
 * helper function. Looks up user by tum_id and calls callback
 */
exports.getUserByPseudoId = function(req, res, pid, callback) {
        User.findOne({ pseudo_id: pid }, function(err, user) {
                if (err) {
                        this.handleDBsave(err, res);
                }       
                callback(req, res, pid, user);
        });     
}; 

/**
 * Is called on DB save operations.
 * Sends 500 error message if on error
 */
exports.handleDBsave = function(err, res) {
	if (err) { 
		console.err('user save error: ', err); 
		out.send500error(res);
	}
};
