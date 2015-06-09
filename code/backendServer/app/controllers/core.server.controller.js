'use strict';

/**
 * Module dependencies.
 */
exports.index = function(req, res) {
	/*
	res.render('index', {
		user: req.user || null,
		request: req
	});
	*/
	res.json({ message: 'Backend running! Welcome!' });
};
