package com.tca.mobiledooraccess;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import com.tca.mobiledooraccess.utils.RSACrypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Created by basti on 22.06.15.
 */
public class RegisteredActivity extends Activity{

    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS";
    public static final String TAG = "RegisteredActivity";

    final class UploadPubKey extends AsyncTask<Void, Integer, String> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSACrypto.ALGORITHM, RSACrypto.SEC_PROVIDER);
                keyGen.initialize(RSACrypto.KEYSIZE);
                KeyPair keys = keyGen.generateKeyPair();
                byte[] privateKey = keys.getPrivate().getEncoded();
                byte[] publicKey = keys.getPublic().getEncoded();
                String publicKeyB64 = Base64.encodeToString(publicKey, Base64.NO_WRAP);
                String privateKeyB64 = Base64.encodeToString(privateKey, Base64.NO_WRAP);

                Log.d(TAG, "Generated public key: " + publicKeyB64);
                // Get settings
                SharedPreferences prefs = getSharedPreferences(
                        MainActivity.TUM_GETIN_PREFERENCES,
                        Context.MODE_PRIVATE
                );
                SharedPreferences.Editor settings = prefs.edit();
                String tumId = prefs.getString("tum_id", "");
                String token = prefs.getString("tumOnlineToken", "");
                // Store private key in protected area
                settings.putString("priv_key", privateKeyB64);
                settings.commit();
                // Send public key to backend
                Backend backend = new Backend("www.grandcat.org", "3000");
                int res = backend.sendPublicKey(tumId, token, publicKeyB64);
                Log.d(TAG, "Result for transmission of key to backend: " + res);

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {

            super.onPostExecute(result);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registered_activity);

        new UploadPubKey().execute();

    }
}
