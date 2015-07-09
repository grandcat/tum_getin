'use strict';

var winston = require('winston');
var server = require('../server.js');

var logger = new (winston.Logger)({
	transports: [
		new (winston.transports.Console)({ json: false, timestamp: true }),
		new winston.transports.File({ name: 'all-logs-file',
			 filename: server.__logdir + 'info.log', json: false, timestamp: true }),
		new winston.transports.File({ name: 'error-file', level: 'error',
			filename: server.__logdir + 'errors.log', json: false, timestamp: true })
	],
	exceptionHandlers: [
		new (winston.transports.Console)({ json: false, timestamp: true }),
		new winston.transports.File({ filename: server.__logdir + '/exceptions.log', json: false, timestamp: true })
	],
	exitOnError: false
});

module.exports = logger;
