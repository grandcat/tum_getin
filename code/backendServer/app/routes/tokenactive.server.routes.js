'use strict';

module.exports = function(app) {
	var check = require('../../app/controllers/tokenactive.server.controller');
	app.route('/tokenactive').get(check.tokenactive);
};
