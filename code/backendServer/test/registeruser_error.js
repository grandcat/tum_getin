TU1_ID = "gg00xxx";
TU1_Token = "sfjlkirueijc8743sof";
SP_APP_NAME = 'Backend Test';
 
var frisby = require('frisby');
var tc = require('./config/test_config');

frisby.create('POST missing TUM ID')
    .post(tc.url + '/user/register',
          { 'token' : TU1_Token })
    .expectStatus(400)
    .expectHeader('Content-Type', 'application/json; charset=utf-8')
    .expectJSON({'error' : 'Undefined TUM ID'})
    .toss()
