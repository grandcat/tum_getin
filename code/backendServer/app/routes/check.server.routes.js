'use strict';

module.exports = function(app) {
	var check = require('../../app/controllers/check.server.controller');
	app.route('/check').get(check.check);
};
