var express = require('express');
var bodyParser = require('body-parser');
var mongoose = require('mongoose');
var routes = require('./app/routes');
var morgan = require('morgan'); // HTTP request logger middleware
var db	 = require('./config/db');
var security = require('./config/security');
 
var app = express();
app.use(morgan);
var port = 8000;
mongoose.connect(db.url);
 
app.use(bodyParser.urlencoded({ extended: true }));
 
routes.addAPIRouter(app, mongoose);

app.use(function(req, res, next){
   res.status(404);
   res.json({ error: 'Invalid URL' });
});

app.listen(port);

console.log('Seeing activity on port ' + port);
 
exports = module.exports = app;

