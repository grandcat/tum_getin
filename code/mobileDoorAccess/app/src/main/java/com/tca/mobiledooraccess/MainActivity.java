package com.tca.mobiledooraccess;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.util.Log;

/**
 * Created by basti on 15.06.15.
 */
public class MainActivity extends Activity {

    public static final String TUM_GETIN_PREFERENCES = "TUM_GETIN_PREFS";
    public static Context context;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        MainActivity.context = getApplicationContext();

        SharedPreferences settings;
        settings = getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
        boolean registered = settings.getBoolean("registered",false);

        if (registered){
            Log.d("Registration", "The user is registered");
            //start registered Activity
        }else {
            //start register Activity
            Log.d("Registration", "The user is NOT registered");
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        }
    }
}
