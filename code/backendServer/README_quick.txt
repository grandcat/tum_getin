Quick guide to start the tum_getin backend server:


>>> Install necessary stuff (needed only once): <<<
- Install Node.js
- Install NPM
- Download MongoDB
$ npm install -g bower
$ npm install -g grunt-cli
$ npm install -g vows (program for running tests)
$ npm install	(-> if dependencies change, this may
		be necessary again...)


>>>------------	Run the server:	----------------<<<
$ <path-to-mongodb>/bin/mongod
$ grunt
	(for testing better: $ NODE_ENV=test grunt)

For restarting the server just type rs into the running
grunt application. When running in test mode, this will
reconstruct the defined initial state, also in the DB.

>>>------------ Testing: -----------------------<<<
/test contains a test suite for all backend interfaces.
Acts as a client (the smartphone or the NFC reader).
Does black-box testing basically.
Run tests with:
$ vows test.js --spec

White-box tests of the interal functioning of the backend
can be found in /app/tests


>>>------------ Important infos ----------------<<<
- Certificate and Key file in /config/sslcerts/ are of course
selfsigned for localhost and need to be replaced eventually!

- /test/cert.pem is a whole CA root store with the selfsigned
certificate mentioned above included. Do not use otherwise!

- MongoDB web interface: addr:port/db-name/collection-name/
  (db name in development mode: tum-getin-dev, 
  in production mode: tum-getin)
  http://localhost:28017/tum-getin-test/users/
