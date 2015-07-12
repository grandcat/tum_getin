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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.tca.mobiledooraccess.service.StatefulProtocolHandler;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by basti on 22.06.15.
 */
public class UnlockProgressActivity extends Activity{

    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS";
    public static final String TAG = "RegisteredActivity";

    private ListView mProgressList;
    private ProgressBar mProgressBar;

    String[] itemname ={
            "Safari",
            "Camera",
            "Global"
    };

    int[] imgid={
            R.drawable.abc_ic_go,
            R.drawable.abc_ic_go,
            0
    };

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
        setContentView(R.layout.unlock_progress_activity);

        // UI fields
        mProgressList = (ListView)findViewById(R.id.progressList);
        mProgressBar = (ProgressBar)findViewById(R.id.progress_bar_nfc_protocol);
        // Define custom list layout for progress states
        ProgressListAdapter adapter = new ProgressListAdapter(this, itemname, imgid);
        mProgressList.setAdapter(adapter);

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
}
