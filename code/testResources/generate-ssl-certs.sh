#!/bin/bash
echo "Generating self-signed certificates..."
openssl genrsa -out ./key.pem -aes256 2048
openssl req -new -key ./key.pem -out ./csr.pem
openssl x509 -req -days 9999 -in ./csr.pem -signkey ./key.pem -out ./cert.pem
rm ./csr.pem
chmod 600 ./key.pem ./cert.pem
