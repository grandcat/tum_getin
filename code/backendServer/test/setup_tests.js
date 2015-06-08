var async = require('async');
var dbConfig = require('./config/db.js');
var mongodb = require('mongodb');
assert = require('assert');

var mongoClient = mongodb.MongoClient
var test_db = null;

function connectDB(callback)
{
	mongoClient.connect(dbConfig.url, function(err, db) {
		assert.equal(null, err);
		test_db = db;
		console.log("Connected correctly to server");
		callback(0);
	});
}

function dropUserCollection(callback)
{
	console.log("dropUserCollection");
	user = test_db.collection('user');
	if (undefined != user) {
		user.drop(function(err, reply) {
			console.log('user collection dropped');

			callback(0);
		});
	} else {
		callback(0);
	}
}

function closeDB(callback)
{
	test_db.close();
}

async.series([connectDB, dropUserCollection, closeDB]);
