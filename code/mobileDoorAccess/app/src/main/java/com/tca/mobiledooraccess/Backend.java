package com.tca.mobiledooraccess;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Sebastian on 19.06.2015.
 * The Class com.tca.mobiledooraccess.Backend is used to create
 * an URL Connection via SSL to our Server
 * Since the certificate is self signed,
 * additional functions are needed to create a secured https connection
 *
 */
public class Backend {
    private static final String TAG = "BackendClass";

    private String host;
    private String port;
    private static SSLContext context;

    //Backend Contructor
    public Backend(String host, String port){
        Log.d(TAG, "Initalizing Backend-Class");
        this.host = host;
        this.port = port;
        Log.i(TAG, "Specified Host: " + host + ", Port: " + port);

        //A try block for catching all kinds of Exceptions during the initialization
        //of the ssl context
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
            this.context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

        }catch (CertificateException e){
            Log.e(TAG, "Certificate Exception: " + e.getMessage());
        }catch (IOException e){
            Log.e(TAG, "IOException: " + e.getMessage());
        }catch (KeyStoreException e){
            Log.e(TAG, "KeyStoreException: " + e.getMessage());
        }catch (NoSuchAlgorithmException e){
            Log.e(TAG, "NoSuchAlgorithmException: " + e.getMessage());
        }catch (KeyManagementException e){
            Log.e(TAG, "KeyManagementException: " + e.getMessage());
        }
    }

    //This Method will make a GET request to the backend server
    //It will extract the token and the pseudoID from the json-body
    //and will return it as a String-Array
    // Index 0: token
    // Index 1: pseudoID
    public String[] getTokenPseudoID(String tumID){
        Log.d(TAG, "getting Token and Pseudo ID");
        String [] results = new String[2];
        try {
            URL url = new URL("https://" + host + ":" + port + "/register?tum_id=" + tumID );
            HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
            urlConnection.setSSLSocketFactory(context.getSocketFactory());
            InputStream in = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line;
            while((line = br.readLine()) != null){
                sb.append(line + "\n");
            }
            JSONObject jsonObj = new JSONObject(sb.toString());
            int status = jsonObj.getInt("status");

            Log.d(TAG, "STATUS of Server: " + status);

            if (status == 200){
                results[0] = jsonObj.getString("token");
                results[1] = jsonObj.getString("pseudo_id");
            }else{
                Log.e(TAG, "STATUS NOT OKAY:" + status);
            }

        } catch (MalformedURLException e){
            Log.e(TAG, "MalformedURLException: " + e.getMessage());
        } catch (IOException e){
            Log.e(TAG, "IOException: " + e.getMessage());
        } catch (JSONException e){

        } catch (Exception e){
            String msg = e.getMessage();
            String asdf = msg;
        }
        return results;
    }

    //This method will send the created public key of the student to the server
    //For this, the specific tumID and the token must be added
    //(actually one of the 2 parameters, either tumID or token would be enough
    public int sendPublicKey(String tumID, String token, String key){
        try{
            URL url = new URL("https://" + host + ":" + port + "/register");
            HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
            urlConnection.setSSLSocketFactory(context.getSocketFactory());
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setDoOutput(true);
            urlConnection.connect();

            JSONObject jsonParams = new JSONObject();
            jsonParams.put("tum_id", tumID);
            jsonParams.put("token", token);
            jsonParams.put("key", key);

            OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
            out.write(jsonParams.toString());
            out.close();

            InputStream in = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line;
            while((line = br.readLine()) != null){
                sb.append(line + "\n");
            }
            JSONObject jsonObj = new JSONObject(sb.toString());
            int status = jsonObj.getInt("status");

            Log.d(TAG, "STATUS of Server: " + status);

            if (status == 200){
                Log.d(TAG, "STATUS OF SERVER OKAY");
                return 0;
            }else{
                Log.e(TAG, "STATUS NOT OKAY:" + status + ", Message: " + jsonObj.getString("message"));
            }

        }catch (MalformedURLException e){
            Log.e(TAG, "MalformedURLException: " + e.getMessage());
        }catch (ProtocolException e){
            Log.e(TAG, "ProtocolException (POST): " + e.getMessage());
        }catch (IOException e){
            Log.e(TAG, "IOException: " + e.getMessage());
        }catch (JSONException e){
            Log.e(TAG, "JSONException: " + e.getMessage());
        }
        return -1;
    }

    public boolean tokenActivated(String token){
        Log.d(TAG, "Token Active GET-Request-Method");
        boolean result = false;
        try{
            URL url = new URL("https://" + host + ":" + port + "/tokenactive?token=" + token);
            HttpsURLConnection urlConnection = (HttpsURLConnection)url.openConnection();
            urlConnection.setSSLSocketFactory(context.getSocketFactory());
            InputStream in = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line;
            while((line = br.readLine()) != null){
                sb.append(line + "\n");
            }
            JSONObject jsonObj = new JSONObject(sb.toString());
            result = jsonObj.getBoolean("active");

        } catch (MalformedURLException e){
            Log.e(TAG, "MalformedURLException: " + e.getMessage());
        } catch (IOException e){
            Log.e(TAG, "IOException: " + e.getMessage());
        } catch (JSONException e){

        } catch (Exception e){
            String msg = e.getMessage();
            String asdf = msg;
        }
        return result;
    }


}
