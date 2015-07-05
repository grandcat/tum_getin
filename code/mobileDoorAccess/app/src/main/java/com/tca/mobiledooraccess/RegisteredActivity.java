package com.tca.mobiledooraccess;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.tca.mobiledooraccess.service.StatefulProtocolHandler;
import com.tca.mobiledooraccess.utils.RSACrypto;

import java.security.KeyPair;
import java.util.concurrent.ExecutionException;

/**
 * Created by basti on 22.06.15.
 */
public class RegisteredActivity extends Activity{

    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS";
    public static final String TAG = "RegisteredActivity";

    private ProgressBar mProgressBar;

    final class DeleteAccount extends AsyncTask<Void, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            // Get settings
            SharedPreferences prefs = getSharedPreferences(
                    MainActivity.TUM_GETIN_PREFERENCES,
                    Context.MODE_PRIVATE
            );
            String tumId = prefs.getString("tum_id", "");
            String token = prefs.getString("tumOnlineToken", "");
            // Try to delete account
            Backend backend = new Backend("www.grandcat.org", "3000");
            int status = backend.deleteAccount(tumId, token);
            if (0 == status) {
                // Delete local data
                SharedPreferences.Editor settings = prefs.edit();
                settings.remove("tum_id").remove("pseudo_ID").remove("tumOnlineToken").remove("priv_key");
                settings.remove("token_activated").remove("token_received").remove("registered");
                settings.apply();
                // Notify UI
                return true;

            } else {
                // Something went wrong
                return false;
            }
        }
    }

    private BroadcastReceiver mNfcProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            int progress = intent.getIntExtra(
                    "progress",
                    StatefulProtocolHandler.PROTO_MSG0_TERMINAL_CERTIFICATE_HASH
            );
            Log.d(TAG, "Got progress: " + progress);
            // Update progressBar in UI
            mProgressBar.setProgress(progress);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registered_activity);

        // Fields
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar_nfc_protocol);

        // Register for NFC protocol progress
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mNfcProgressReceiver,
                new IntentFilter(StatefulProtocolHandler.INTENT_PROTOCOL_PROGRESS)
        );

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister NFC protocol progress
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mNfcProgressReceiver);
    }

    public void deleteKeys(View v) {
        Log.d(TAG, "DeleteKeys called.");

        try {
            boolean success = new DeleteAccount().execute().get();
            if (success) {
                // Account deleted successfully online
                Toast.makeText(
                        this,
                        "Account deleted. Don't forget to delete the token in TUMonline, too.",
                        Toast.LENGTH_LONG
                ).show();
            } else {
                Toast.makeText(
                        this,
                        "Could not delete account online. Please contact the TUM administration.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
