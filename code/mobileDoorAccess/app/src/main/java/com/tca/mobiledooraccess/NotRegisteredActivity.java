package com.tca.mobiledooraccess;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


/**
 * Created by basti on 22.06.15.
 */
public class NotRegisteredActivity extends Activity{
    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS";
    public static final String TAG = "NotRegisteredActivity";

    private Backend backend;
    private SharedPreferences appSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.not_registered_activity);
        backend = new Backend("www.grandcat.org", "3000");
        appSettings = getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
    }

    public void sendTumIDClick(View view){
        Log.d(TAG, "Clicked Send TUM ID");
        EditText tumIDEditText = (EditText) findViewById(R.id.editTextTUMID);
        final String tumID = tumIDEditText.getText().toString();

        if (tumID.matches("")) {
            Log.d(TAG, "TUM ID field empty");
            Toast.makeText(this, "You did not enter a TUM-ID", Toast.LENGTH_SHORT).show();
            return;
        }else{
            Log.d(TAG, "TUM ID:" + tumID);
            final ProgressDialog progress = new ProgressDialog(view.getContext());
            progress.setMessage("Connecting to server...");
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.show();
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    String result[] = backend.getUserCredentials(tumID);
                    Log.d(TAG, "Result from server: " + result[0] + "and " + result[1]);
                    SharedPreferences.Editor editor = appSettings.edit();
                    editor.putString("tum_id", tumID);
                    editor.putString("tumOnlineToken", result[0]);
                    editor.putString("pseudo_ID", result[1]);
                    editor.putString("salt", result[2]);
                    editor.putBoolean("token_received", true);
                    editor.commit();
                    progress.dismiss();
                }
            });
            t.start();
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "Starting main Activity");
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }
}
