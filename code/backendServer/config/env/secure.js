'use strict';

module.exports = {
	port: 8443,
	db: process.env.MONGOHQ_URL || process.env.MONGOLAB_URI || 'mongodb://localhost/tum-getin',
	assets: {
	}
};
