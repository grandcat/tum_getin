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
	},
	// Path to TUMonline:
	// 'https://campus.tum.de/tumonline/wbservicesbasic.';
	tumOnl_url_host: 'campus.tum.de',
	tumOnl_url_path: '/tumonline/wbservicesbasic.',
	tumOnl_reqToken: 'requestToken?',
	tumOnl_tokenName: '&pTokenName=TUM+GetIn+Door+Access+App',
	tumOnl_isTokenConf: 'isTokenConfirmed?'
};
