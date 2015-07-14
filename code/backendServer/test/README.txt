Most tests require the file
../../testResources/real_user.json
with your TUM ID in order to request a valid token.
See the README in that folder for information about the file.


Run tests with:

$ vows 1-interface-test.js --spec

Then activate the new tokens that have been generated in TUMOnline.
Afterwards you can proceed with:

$ vows 2-interface-test.js --spec



single-get-token-test.js and single-post-key-test.js work like the scripts above but only contain the one basic test case.
