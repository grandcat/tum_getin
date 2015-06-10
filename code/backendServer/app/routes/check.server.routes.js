'use strict';

module.exports = function(app) {
	var core = require('../../app/controllers/check.server.controller');
	app.route('/check').get(core.init);
};
