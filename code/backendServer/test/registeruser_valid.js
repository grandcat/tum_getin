TEST_USERS = [{'id' : 'ga99aaa', 'token' : 'dssriueijwwr'},
              {'id' : 'gb88bbb', 'token' : 'wklroipoegfe'},
              {'id' : 'gc77ccc', 'token' : 'rykutyrdtrww'}]
 
SP_APP_NAME = 'Backend Test';
 
var frisby = require('frisby');
var tc = require('./config/test_config');

TEST_USERS.forEach(function createUser(user, index, array) {
    frisby.create('POST register user ' + user.email)
        .post(tc.url + '/register',
              { 'id' : user.id,
                'token' : user.token })
        .expectStatus(201)
        .expectHeader('Content-Type', 'application/json; charset=utf-8')
        .expectJSON({ 'id' : user.id,
                      'token' : user.token })
        .toss()
});

frisby.create('POST register duplicate user ')
    .post(tc.url + '/register',
          { 'id' : TEST_USERS[0].id,
            'token' : TEST_USERS[0].token })
    .expectStatus(400)
    .expectHeader('Content-Type', 'application/json; charset=utf-8')
    .expectJSON({'error' : 'Account with that TUM ID already exists.'})
    .toss()


