package com.tca.mobiledooraccess;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by basti on 22.06.15.
 */
public class TokenActivationActivity extends Activity {
    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS";
    public static final String TAG = "TokenActivationActivity";

    boolean tokenActivated;

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
        final Backend backend = new Backend("www.grandcat.org", "3000");
        final ProgressDialog progress = new ProgressDialog(view.getContext());
        progress.setMessage("Connecting to server...");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.show();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences settings = getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
                String token = settings.getString("tumOnlineToken", null);
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

        if (tokenActivated ){
            SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("token_activated", true);
            editor.commit();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Your token is not yet activated!", Toast.LENGTH_SHORT).show();
        }
    }
    //We need this method to set the variable inside of a thread
    private void setTokenActivated (boolean tokenActivated){
        this.tokenActivated = tokenActivated;
    }

}
