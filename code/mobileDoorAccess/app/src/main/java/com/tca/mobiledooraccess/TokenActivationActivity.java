package com.tca.mobiledooraccess;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * Created by basti on 22.06.15.
 */
public class TokenActivationActivity extends Activity {
    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS";
    public static final String TAG = "TokenActivationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.token_activation_activity);
        //Get the token out of the shared preferences and
        //put it into the TextView
        SharedPreferences settings = getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
        String token = settings.getString("tumOnlineToken", null);
        TextView tokenTextView = (TextView)findViewById(R.id.textViewToken);
        tokenTextView.setText(token);
    }


    public void visitTUMOnlineClick(View view){
        //Create View intent for the TUMOnline internet address.
        Uri uri = Uri.parse("https://campus.tum.de/tumonline/webnav.ini");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }
    public void checkTokenStatusClick(View view){

    }

}
