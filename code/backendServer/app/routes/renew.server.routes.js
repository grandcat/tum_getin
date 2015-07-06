'use strict';

module.exports = function(app) {
	var r = require('../../app/controllers/renew.server.controller');
	app.route('/renew').get(r.renew_pseudo_id);
};
