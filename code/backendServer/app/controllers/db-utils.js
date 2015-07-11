'use strict';
var mongoose = require('mongoose'),
    User = mongoose.model('User'),
    log = require('../../config/logger.js'),
    out = require('./reply.js');

/**
 * helper function. Looks up user by tum_id and calls callback
 */
exports.getUserByTumId = function(req, res, tum_id, callback) {
	this.getUser(req, res, { tum_id: tum_id }, tum_id, callback);
};

/**
 * helper function. Looks up user by tum_id and calls callback
 */
exports.getUserByPseudoId = function(req, res, pid, callback) {
	this.getUser(req, res, { pseudo_id: pid }, pid, callback);
}; 

/**
 * General version of the two functions above
 */
exports.getUser = function(req, res, searchJSON, value, callback) {
        User.findOne(searchJSON, function(err, user) {
                if (err) {
                        this.handleDBsave(err, res);
                }       
                callback(req, res, value, user);
        });     
}; 

/**
 * Is called on DB save operations.
 * Sends 500 error message if on error
 */
exports.handleDBsave = function(err, res) {
	if (err) { 
		log.error('user save error: ', err); 
		out.send500error(res);
	}
};
