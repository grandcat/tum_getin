function 
connectDB(callback)
{
	mongoClient.connect(dbConfig.testDBURL, function(err, db) {
		assert.equal(null, err);
		reader_test_db = db;
		console.log("Connected correctly to server");
		callback(0);
	});
}

function 
dropUserCollection(callback)
{
	console.log("dropUserCollection");
	user = reader_test_db.collection('user');
	if (undefined != user) {
		user.drop(function(err, reply) {
			console.log('user collection dropped');
			callback(0);
		});
	} else {
		callback(0);
	}
}

function 
getApplication(callback)
{
	console.log("getApplication");
	client.getApplications({
name:		SP_APP_NAME
	}, function(err, applications) {
		console.log(applications);
		if (err) {
			log("Error in getApplications");
			throw		err;
		}
		app = applications.items[0];
		callback(0);
	});
}

function 
closeDB(callback)
{
	reader_test_db.close();
}

async.series([connectDB, dropUserCollection, getApplication, closeDB]);
