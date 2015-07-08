package com.tca.mobiledooraccess;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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

import com.tca.mobiledooraccess.service.MessageExchangeService;
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

    FragmentPagerAdapter adapterViewPager;
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
        MainActivity.context = getApplicationContext();

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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete_account) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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
    public static class MyPagerAdapter extends FragmentPagerAdapter {
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
                    return new RegisterStep1();
                case 1: // Fragment # 0 - This will show FirstFragment different title
                    return new RegisterStep2();
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
