package com.tca.mobiledooraccess;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

/**
 * Created by basti on 15.06.15.
 */
public class RegisterActivity extends Activity {

    public static final String TUM_GETIN_PREFERENCES = "TUM_GETIN_PREFS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);
    }

    public void registerWithBackend(View view){
        Log.d("Button", "Register Button clicked");
        SharedPreferences settings = getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("registered", true);
        editor.commit();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}
