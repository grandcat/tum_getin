'use strict';

module.exports = function(app) {
	// Root routing
	var core = require('../../app/controllers/register.server.controller');
	app.route('/register').get(core.init);
};
