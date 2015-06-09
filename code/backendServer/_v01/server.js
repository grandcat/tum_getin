var express = require('express');
var bodyParser = require('body-parser');
var mongoose = require('mongoose');
var routes = require('./app/routes');
var morgan = require('morgan'); // HTTP request logger middleware
var db	 = require('./config/db');
var security = require('./config/security');
var fs = require('fs');
var https = require('https');
var privateKey  = fs.readFileSync('sslcert/example-key.pem', 'utf8');
var certificate = fs.readFileSync('sslcert/example-cert.pem', 'utf8');

// --- start the App! 
var app = express();
app.use(morgan);

var port = 8443;

// --- connect to DB
mongoose.connect(db.url);
 
app.use(bodyParser.urlencoded({ extended: true }));
 
routes.addAPIRouter(app, mongoose);

// standard error message
app.use(function(req, res, next){
   res.status(404);
   res.json({ error: 'Invalid URL' });
});

var credentials = {key: privateKey, cert: certificate};
var httpsServer = https.createServer(credentials, app);

// --- Go live...
httpsServer.listen(port);

console.log('TUM_getin backend started on port ' + port);
 
exports = module.exports = app;

