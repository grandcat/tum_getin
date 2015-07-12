package com.tca.mobiledooraccess;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.tca.mobiledooraccess.service.StatefulProtocolHandler;

import java.util.ArrayList;

/**
 * Created by basti on 22.06.15.
 */
public class UnlockProgressActivity extends ActionBarActivity{

    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS";
    public static final String TAG = "RegisteredActivity";

    public static final int NUM_PROGRESS_STATUS_ENTRIES = 3;

    private ListView mProgressList;
    private ProgressBar mProgressBar;
    private ProgressListAdapter mProgressAdapter;

    private int progress = 0;
    private ArrayList<ProgressStatusModel> statusItems;

    private BroadcastReceiver mNfcProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            int statusCode = intent.getIntExtra("status", 0);
            int step = intent.getIntExtra("progress", 0);

            Log.d(TAG, "Got progress: " + step + " with status " + statusCode);
            if (0 == statusCode) {
                // Successfully reached new step in protocol
                updateProgressSuccess(step);
            } else {
                // Error occurred during protocol exchange
                showProgressError(step);
            }
        }
    };

    private void updateProgressSuccess(int newProgress) {
        // Update progressBar in UI
        mProgressBar.setProgress(newProgress);

        // Update progressStatus list
        if (newProgress > 0 && newProgress <= NUM_PROGRESS_STATUS_ENTRIES) {
            // Update previous status items if we missed a broadcast
            if (newProgress - 1 > progress) {
                mProgressAdapter.setIconUntilPosition(newProgress - 1, R.drawable.btn_check_buttonless_on);
            }
            // Update item related to current progress number
            ProgressStatusModel progressItem = statusItems.get(newProgress - 1);
            progressItem.setCheckIcon();
        }
        mProgressAdapter.notifyDataSetChanged();

        // Update tracking progress counter
        progress = newProgress;
    }

    private void showProgressError(int position) {
        // Update item related to current progress position
        Log.d(TAG, "Show progress error in step " + (position - 1));
        ProgressStatusModel progressItem = statusItems.get(position - 1);
        progressItem.setErrIcon();
        mProgressAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.unlock_progress_activity);

        // UI fields
        mProgressList = (ListView)findViewById(R.id.progressList);
        mProgressBar = (ProgressBar)findViewById(R.id.progress_bar_nfc_protocol);
        // Define custom list layout for progress states
        statusItems = generateStatusList();
        mProgressAdapter = new ProgressListAdapter(this, statusItems);
        mProgressList.setAdapter(mProgressAdapter);

        // Set already reached progress from intent if supplied
        int progress = getIntent().getIntExtra("progress", 0);
        Log.d(TAG, "Progress: " + progress);
        updateProgressSuccess(progress);

        // Register for NFC protocol progress
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mNfcProgressReceiver,
                new IntentFilter(StatefulProtocolHandler.INTENT_PROTOCOL_PROGRESS)
        );
        //Check if user is still registered
        if (!stillRegistered()){
            Intent intent = new Intent(UnlockProgressActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }
    protected void onResume() {
        super.onResume();
        if (!stillRegistered()){
            Intent intent = new Intent(UnlockProgressActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }

    private boolean stillRegistered(){
        SharedPreferences prefs = getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
        return prefs.getBoolean("registered",false);
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remove progress value here that was added while starting this activity
        Log.d(TAG, "App paused; Delete progress.");
        getIntent().removeExtra("progress");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister NFC protocol progress
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mNfcProgressReceiver);
    }
    private static ArrayList<ProgressStatusModel> generateStatusList() {
        ArrayList<ProgressStatusModel> progressList = new ArrayList<>(NUM_PROGRESS_STATUS_ENTRIES);
        // Progress status message 1
        progressList.add(new ProgressStatusModel(
                "Device connected",
                "Touch a TUM terminal with your phone."));
        // Progress status message 2
        progressList.add(new ProgressStatusModel("Received handshake", "Desc2"));
        // Progress status message 3
        progressList.add(new ProgressStatusModel("Cryptographic exchange", "Desc3"));

        return progressList;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case R.id.action_settings:
                //call Settings Activity
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }}
