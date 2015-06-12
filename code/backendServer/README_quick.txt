Quick guide to start the tum_getin backend server:


>>> Install necessary stuff (needed only once): <<<
- Install Node.js
- Download MongoDB
$ npm install -g bower
$ npm install -g grunt-cli
$ npm install -g mocha (program for running tests)
$ npm install	(-> if dependencies change, this may
		be necessary again...)


>>>------------	Run the server:	----------------<<<
$ <path-to-mongodb>/bin/mongod
$ grunt
	(also possible: $ NODE_ENV=test grunt)

>>>------------ Testing: -----------------------<<<
/test contains a test suite for all backend interfaces.
Acts as a client (the smartphone or the NFC reader).
Does black-box testing basically.
Run tests with:
$ mocha test.js

White-box tests of the interal functioning of the backend
can be found in /app/tests


>>>------------ Important infos ----------------<<<
- Certificate and Key file in /config/sslcerts/ are of course
selfsigned for localhost and need to be replaced eventually!

- /test/cert.pem is a whole CA root store with the selfsigned
certificate mentioned above included. Do not use otherwise!
