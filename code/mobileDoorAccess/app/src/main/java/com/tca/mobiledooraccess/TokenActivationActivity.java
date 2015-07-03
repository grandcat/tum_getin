package com.tca.mobiledooraccess;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.tca.mobiledooraccess.utils.CryptoUtils;
import com.tca.mobiledooraccess.utils.RSACrypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Created by basti on 22.06.15.
 */
public class TokenActivationActivity extends Activity {
    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS";
    public static final String TAG = "TokenActivationActivity";
    private Backend backend;
    private SharedPreferences appSettings;


    boolean tokenActivated;

    final class KeyGeneration extends AsyncTask<Void, Integer, String> {
        ProgressDialog dialog = new ProgressDialog(TokenActivationActivity.this);

        @Override
        protected void onPreExecute() {
            dialog.setMessage(getString(R.string.keygen_dialog));
            dialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance(
                        RSACrypto.ALGORITHM,
                        RSACrypto.SEC_PROVIDER);
                keyGen.initialize(RSACrypto.KEYSIZE);
                KeyPair keys = keyGen.generateKeyPair();
                keys.getPrivate().getEncoded();

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            dialog.dismiss();

            super.onPostExecute(result);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.token_activation_activity);

        backend = new Backend("www.grandcat.org", "3000");
        appSettings = getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
        //Get the token out of the shared preferences and
        //put it into the TextView
        String token = appSettings.getString("tumOnlineToken", null);
        TextView tokenTextView = (TextView)findViewById(R.id.textViewToken);
        tokenTextView.setText(token);
    }


    public void visitTUMOnlineClick(View view){
        //Create View intent for the TUMOnline internet address.
        Log.d(TAG, "Visit TUM Online Click");
        Uri uri = Uri.parse("https://campus.tum.de/tumonline/webnav.ini");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }
    public void checkTokenStatusClick(View view){
        Log.d(TAG, "Check Token status Click");
        final ProgressDialog progress = new ProgressDialog(view.getContext());

        progress.setMessage("Connecting to server...");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.show();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                String token = appSettings.getString("tumOnlineToken", null);
                setTokenActivated(backend.tokenActivated(token));
                progress.dismiss();
            }
        });

        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (tokenActivated){
            Log.d(TAG, "Token activated");
            SharedPreferences.Editor editor = appSettings.edit();
            editor.putBoolean("token_activated", true);
            // Generate keys
            new KeyGeneration().execute();
            // User should be registered successfully now
            editor.putBoolean("registered", true);
            editor.commit();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else {
            Log.d(TAG, "Token not activated");
            Toast.makeText(this, "Your token is not yet activated!", Toast.LENGTH_SHORT).show();
        }
    }
    //We need this method to set the variable inside of a thread
    private void setTokenActivated (boolean tokenActivated){
        this.tokenActivated = tokenActivated;
    }

}
