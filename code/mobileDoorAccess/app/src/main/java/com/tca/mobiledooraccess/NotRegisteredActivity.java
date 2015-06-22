package com.tca.mobiledooraccess;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by basti on 22.06.15.
 */
public class NotRegisteredActivity extends Activity{
    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS";
    public static final String TAG = "NotRegisteredActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.not_registered_activity);
    }
}
