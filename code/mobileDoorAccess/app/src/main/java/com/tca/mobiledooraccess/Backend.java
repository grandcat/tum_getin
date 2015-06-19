package com.tca.mobiledooraccess;

import com.tca.mobiledooraccess.MainActivity;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.InputMismatchException;
import android.util.Log;

/**
 * Created by Sebastian on 19.06.2015.
 * The Class com.tca.mobiledooraccess.Backend is used to create
 * an URL Connection via SSL to our Server
 * Since the certificate is self signed,
 * additional are needed to create a secured https connection
 *
 */
public class Backend {
    public static HttpsURLConnection setUpHttpsConnection(String urlString){
        try{
            //Create a Certificate Factory and open our local cert...
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = new BufferedInputStream(MainActivity.context.getAssets().open("cert.crt"));
            Certificate ca = cf.generateCertificate(caInput);

            //Create a new keystore with our server ca inside...
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            //Trust Manager
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            //Create SSL Context
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            URL url = new URL(urlString);
            HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
            urlConnection.setSSLSocketFactory(context.getSocketFactory());

            return urlConnection;
        }catch (Exception e){
            Log.e("Backend", "Failed to create https-connection with backend");
            return null;
        }
    }
}
