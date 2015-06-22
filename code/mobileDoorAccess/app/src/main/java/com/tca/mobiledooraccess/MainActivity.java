package com.tca.mobiledooraccess;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.util.Log;

/**
 * Available Preferences:
 * Bool - "registered"
 * Bool - "token_received"
 * Bool - "token_activated"
 *
 * String - "tum_ID"
 * String - "pseudo_ID"
 * String - "tumOnlineToken"
 * String - "publicKey"
 * String - "privateKey"
 *
 */
public class MainActivity extends Activity {

    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS";
    public static Context context;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        MainActivity.context = getApplicationContext();

        SharedPreferences settings;
        settings = getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
        //get Settings - init with false in case of missing
        boolean registered = settings.getBoolean("registered",false);
        boolean token_received = settings.getBoolean("token_received", false);
        boolean token_activated = settings.getBoolean("token_activated", false);

        //Check status of the App...
        if (registered){
            Log.d(TAG, "User registered, starting RegisteredActivity...");
            //start Regeistered Activity
            Intent intent = new Intent(this, RegisteredActivity.class);
            startActivity(intent);
        }else{
            Log.d(TAG, "User not registered, checking for token status...");
            if (token_received){
                if (token_activated){
                    Log.e(TAG, "This should not happen! Registered flag missing!");
                }else{
                    Log.d(TAG, "Token already received, starting TokenActivation Activity...");
                    //start TokenActivation Activity
                    Intent intent = new Intent(this, TokenActivationActivity.class);
                    startActivity(intent);
                }
            }else{
                Log.d(TAG, "No token received yet, starting NotRegistered Activity...");
                //start NotRegisteredActivity
                Intent intent = new Intent(this, NotRegisteredActivity.class);
                startActivity(intent);
            }
        }
    }
}
