backendConnection.py

+can connect via https to node js
-the cert.pem file has to be integrated into the unix system before
	*because of the load_default_certs() call in python
	*the specific call to a cert file doesnt work so far (load_cert_chain)
	*the integration of a pem file into trusted CA's is quite simple:
		cp cert.pem /usr/share/ca-certificates
		dpkg-reconfigure ca-certificates

