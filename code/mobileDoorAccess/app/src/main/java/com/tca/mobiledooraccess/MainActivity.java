package com.tca.mobiledooraccess;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.tca.mobiledooraccess.service.MessageExchangeService;
import com.tca.mobiledooraccess.utils.KeyGeneratorTask;
import com.tca.mobiledooraccess.utils.RSACrypto;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;

/**
 * Available Preferences:
 * Bool - "registered"
 * Bool - "token_received"
 * Bool - "token_activated"
 *
 * String - "tum_ID"
 * String - "pseudo_ID"
 * String - "tumOnlineToken"
 * String - "publicKey"
 * String - "privateKey"
 *
 */
public class MainActivity extends ActionBarActivity {

    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS";
    public static Context context;
    private static final String TAG = "MainActivity";
    SharedPreferences appSettings;
    private static Fragment fragmentStep1;
    private static Fragment fragmentStep2;
    private Backend backend;
    ProgressDialog mProgressDialog;


    public static FragmentPagerAdapter adapterViewPager;
    public static ViewPager viewPager;

    MyServiceConnection mConnection;
    Messenger mService;

    /**
     * Class for interacting with the main interface of the service.
     */
    final class MyServiceConnection implements ServiceConnection {
        private boolean connectionEstablished = false;
        private Object lockBind = new Object();

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = new Messenger(service);
            Log.i(TAG, "onServiceConnected in Mainactivity with mService: " + mService.toString());
        }

        /**
         * Wait for connection to be established. Only works if executed in separate thread.
         * @throws InterruptedException
         */
        public void waitUntilConnected() throws InterruptedException {
            if (!connectionEstablished) {
                synchronized (lockBind) {
                    lockBind.wait();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            Log.i(TAG, "onServiceDisconnected in Mainactivity");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        adapterViewPager = new MyPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapterViewPager);
        fragmentStep1 = new RegisterStep1();
        fragmentStep2 = new RegisterStep2();

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                ((OnRefreshListener) adapterViewPager.getItem(position)).onRefresh();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        MainActivity.context = getApplicationContext();
        backend = new Backend("www.grandcat.org", "3000");
        appSettings = getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
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
            case R.id.action_delete_account:
                new DeleteAccount().execute();
                return true;
            case R.id.action_update_key_pair:
                new UpdateKeyPair().execute();
                return true;
            case R.id.action_update_pseudo_id:
                new UpdatePseudoIDandSalt().execute();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    final class DeleteAccount extends AsyncTask<Void, Integer, Boolean> {
        protected void onPreExecute(){
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMessage("Connecting to server...");
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.show();
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            String tumId = appSettings.getString("tum_id", "");
            String token = appSettings.getString("tumOnlineToken", "");
            // Try to delete account
            int status = backend.deleteAccount(tumId, token);
            if (0 == status) {
                // Delete local data
                SharedPreferences.Editor settings = appSettings.edit();
                settings.remove("tum_id").remove("pseudo_ID").remove("tumOnlineToken").remove("priv_key");
                settings.remove("token_activated").remove("token_received").remove("registered");
                settings.apply();
                // Notify UI
                mProgressDialog.dismiss();
                return true;
            } else {
                mProgressDialog.dismiss();
                return false;
            }
        }
        @Override
        protected void onPostExecute(Boolean aBoolean) {
            Toast.makeText(MainActivity.this, "Deletion complete, dont forget to delete your token in TUMOnline", Toast.LENGTH_SHORT).show();
            super.onPostExecute(aBoolean);
        }
    }
    final class UpdateKeyPair extends AsyncTask<Void, Void, Void> {
        protected void onPreExecute(){
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMessage("Connecting to server...");
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.show();
        }
        @Override
        protected Void doInBackground(Void... params) {
            String publicKey = appSettings.getString("publicKey", null);
            String privateKey = appSettings.getString("privateKey", null);
            String tumID = appSettings.getString("tum_id", null);
            String token = appSettings.getString("tumOnlineToken", null);

            if (publicKey != null && privateKey != null && tumID != null && token != null){
                new KeyGeneratorTask(context).execute();
            }else{

            }
            mProgressDialog.dismiss();
            return null;
        }
    }
    final class UpdatePseudoIDandSalt extends AsyncTask<Void, Void, Void> {
        protected void onPreExecute(){
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMessage("Connecting to server...");
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.show();
        }
        @Override
        protected Void doInBackground(Void... params) {
            String tumID = appSettings.getString("tum_id", null);
            String token = appSettings.getString("tumOnlineToken", null);
            SharedPreferences.Editor editor = appSettings.edit();

            String result[] = backend.getNewPseudoID(tumID, token);

            if (!result[0].equals("0")){
                editor.putString("pseudo_id", result[0]);
                editor.putString("salt", result[1]);
            }
            mProgressDialog.dismiss();
            return null;
        }
    }



//    private void checkAppStatus(){
//        SharedPreferences settings;
//        settings = getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
//        //get Settings - init with false in case of missing
//        boolean registered = settings.getBoolean("registered",false);
//        boolean token_received = settings.getBoolean("token_received", false);
//        boolean token_activated = settings.getBoolean("token_activated", false);
//
//        final class IncomingHandler extends Handler {
//            @Override
//            public void handleMessage(Message msg) {
//                Log.d(TAG, "Mainactivity handler: message " + msg.arg1);
//            }
//        }
//        mConnection = new MyServiceConnection();
//
//        final Messenger mMessenger = new Messenger(new IncomingHandler());
//        // Try binding and send a message to worker thread
//        Intent bindIntent = new Intent(this, MessageExchangeService.class);
////        // Service should now be resistance to unbinding
////        bindService(bindIntent, mConnection, Context.BIND_AUTO_CREATE);
//        startService(bindIntent);
//        // TODO: action has to be done within bindConnection
//        // unbindService(mConnection);
//
//        //Check status of the App...
//        if (registered){
//            Log.d(TAG, "User registered, starting RegisteredActivity...");
//            //start Regeistered Activity
//            Intent intent = new Intent(this, RegisteredActivity.class);
//            startActivity(intent);
//            finish();
//        }else{
//            Log.d(TAG, "User not registered, checking for token status...");
//            if (token_received){
//                if (token_activated){
//                    Log.e(TAG, "This should not happen! Registered flag missing!");
//                }else{
//                    Log.d(TAG, "Token already received, starting TokenActivation Activity...");
//                    //start TokenActivation Activity
//                    Intent intent = new Intent(this, TokenActivationActivity.class);
//                    startActivity(intent);
//                    finish();
//                }
//            }else{
//                Log.d(TAG, "No token received yet, starting NotRegistered Activity...");
//                //start NotRegisteredActivity
//                Intent intent = new Intent(this, NotRegisteredActivity.class);
//                startActivity(intent);
//                finish();
//            }
//        }
//    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mConnection != null) {
            // Destroy NFC service to reduce energy consumption
            // stopService(new Intent(this, MessageExchangeService.class));
            //unbindService(mConnection);
            // Log.d(TAG, "Shutting down NFC MessageExchange service.");
        }
    }
    public static class MyPagerAdapter extends FragmentPagerAdapter{
        private static int NUM_ITEMS = 2;

        public MyPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return NUM_ITEMS;
        }
        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: // Fragment # 0 - This will show FirstFragment
                    return fragmentStep1;
                case 1: // Fragment # 0 - This will show FirstFragment different title
                    return fragmentStep2;
                default:
                    return null;
            }
        }
        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return "Step " + (position + 1);
        }

    }
}
