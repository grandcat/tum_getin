var express = require('express');
var logger = require('../logger');     
var security = require('../config/security');
var validator = require('validator');
var async = require('async');

exports.addAPIRouter = function(app, mongoose) {

    var router = express.Router();

    // --------- define DB schema ----------- //
    var userSchema = new mongoose.Schema({
             tum_id: { type: String, trim: true },
             token: { type: String, trim: true },
             created: { type: Date, default: Date.now },
             lastAccess: { type: Date, default: Date.now },
         },
         { collection: 'user' }
    );
    
    userSchema.index({tum_id : 1}, {unique:true});
    userSchema.index({token : 1}, {unique:true});
    
    var UserModel = mongoose.model( 'User', userSchema );
    
    // --------- define content type ------- //
    // GET
    app.get('/*', function(req, res, next) {
        res.contentType('application/json');
        next();
    });
    // POST
    app.post('/*', function(req, res, next) {
        res.contentType('application/json');
        next();
    });
    // PUT
    app.put('/*', function(req, res, next) {
        res.contentType('application/json');
        next();
    });
    // DELETE
    app.delete('/*', function(req, res, next) {
        res.contentType('application/json');
        next();
    });
    
    // --------- define handlers for requests --- //

    // route at root is for testing if the server responds.
    router.get('/', function(req, res) {
        res.json({ message: 'Backend running! Welcome!' });
    });
    
    // main functionality: register new user
    router.post('/register', function(req, res) {
        logger.debug('Router for /register');
        res.json({ message: 'Test answer...' });
    });

    // ---- hand all routes defined above to app -- //
    app.use('/tum_getin', router);

}
