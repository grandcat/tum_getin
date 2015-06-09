'use strict';

module.exports = {
	app: {
		title: 'tum_getin',
		description: 'TUM_getin backend server. For registration and legitimacy checks.',
		keywords: 'tum_getin'
	},
	port: process.env.PORT || 3000,
	templateEngine: 'swig',
	sessionSecret: 'MEAN',
	sessionCollection: 'sessions',
	assets: {
	}
};
