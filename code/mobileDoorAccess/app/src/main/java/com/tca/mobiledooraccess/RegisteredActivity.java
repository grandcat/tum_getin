package com.tca.mobiledooraccess;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by basti on 22.06.15.
 */
public class RegisteredActivity extends Activity{

    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS";
    public static final String TAG = "RegisteredActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registered_activity);
    }
}
