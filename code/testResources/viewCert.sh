#!/bin/sh
openssl x509 -in $1 -inform pem -noout -text
