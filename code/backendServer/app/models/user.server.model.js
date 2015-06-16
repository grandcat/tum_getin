'use strict';

/**
 * Module dependencies.
 */
var mongoose = require('mongoose'),
	Schema = mongoose.Schema;

/**
 * User Schema
 */
var UserSchema = new Schema({
	tum_id: {
		type: String,
		trim: true,
	},
	pseudo_id: {
		type: String,
		trim: true,
	},
	token: {
		type: String,
		trim: true,
	},
	status: {
		type: String,
		trim: true,
	},
	revoked: {
		type: Boolean,
		default: false
	},
	created: {
		type: Date,
		default: Date.now
	},
	last_access: {
		type: Date,
		default: Date.now
	}
});

/**
 * Build indices in DB
 */
UserSchema.index({tum_id : 1}, {unique : true});
UserSchema.index({pseudo_id : 1}, {unique : true});
UserSchema.index({token : 1}, {unique : true});


mongoose.model('User', UserSchema);
