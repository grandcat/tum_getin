var express = require('express');
var bodyParser = require('body-parser');
var mongoose = require('mongoose');
var routes = require('./app/routes');
var morgan = require('morgan'); // HTTP request logger middleware
var db	 = require('./config/db');
var security = require('./config/security');

// --- start the App! 
var app = express();
app.use(morgan);

var port = 8000;

// --- connect to DB
mongoose.connect(db.url);
 
app.use(bodyParser.urlencoded({ extended: true }));
 
routes.addAPIRouter(app, mongoose);

// standard error message
app.use(function(req, res, next){
   res.status(404);
   res.json({ error: 'Invalid URL' });
});

// --- Go live...
app.listen(port);

console.log('TUM_getin backend started on port ' + port);
 
exports = module.exports = app;

