'use strict';

module.exports = function(app) {
	var del = require('../../app/controllers/delete.server.controller');
	app.route('/remove').get(del.remove);
};
