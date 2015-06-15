'use strict';

module.exports = function(app) {
	var register = require('../../app/controllers/register.server.controller');
//	app.route('/register').get(register.register_get_token);
	app.route('/register').post(register.register_store_key);
};
