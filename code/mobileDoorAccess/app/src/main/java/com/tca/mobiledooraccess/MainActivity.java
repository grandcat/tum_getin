package com.tca.mobiledooraccess;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.tca.mobiledooraccess.utils.KeyGeneratorTask;

/**
 * Main Activity with ActionBar
 *
 * implements a viewpager with 3 fragments for every registration step
 * Checks user status with backend on every onCreate() and onResume()
 * and shows the corresponding fragment
 *
 * Available Preferences:
 * Bool - "registered"
 * Bool - "token_received"
 * Bool - "token_activated"
 * Bool - "keys_generated"
 *
 * String - "tum_ID"
 * String - "pseudo_ID"
 * String - "tumOnlineToken"
 * String - "publicKey"
 * String - "privateKey"
 *
 */
public class MainActivity extends ActionBarActivity {

    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS"; //Reference for Preferences
    public static Context context;
    private static final String TAG = "MainActivity";
    SharedPreferences appSettings;
    private static RegisterStep1 fragmentStep1;
    private static RegisterStep2 fragmentStep2;
    private static RegisterCompleted fragmentRegistered;
    private Backend backend;
    ProgressDialog mProgressDialog;


    public static FragmentPagerAdapter adapterViewPager;
    public static ViewPager viewPager;

    MyServiceConnection mConnection;
    Messenger mService;


    //crosschecks the user status with the backend
    final class CheckUserStatus extends AsyncTask<String, Void, Boolean> {
        protected void onPreExecute(){
            //Show a progress dialog with a spinning weel
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMessage("Checking user status...");
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.show();
        }
        protected Boolean doInBackground(String... params) {
            boolean tokenActivated = backend.tokenActivated(params[0]);
            Log.d(TAG, "Token activated: " + tokenActivated);
            SharedPreferences.Editor editor = appSettings.edit();

            editor.putBoolean("token_activated", tokenActivated);
            if (tokenActivated){
                editor.putBoolean("registered", true);
            }else{
                editor.putBoolean("registered", false);
            }
            editor.commit();
            mProgressDialog.dismiss();
            return tokenActivated;
        }
        @Override
        protected void onPostExecute(Boolean tokenActivated) {
            super.onPostExecute(tokenActivated);
            setPage(); //set the page depending on the user registration status
        }
    }

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

    // Checks the user status internally and shows the fragment responsible for the next
    // registration steps
    private void setPage(){
        boolean tokenReceived = appSettings.getBoolean("token_received", false);
        boolean tokenActivated = appSettings.getBoolean("token_activated", false);
        boolean registered = appSettings.getBoolean("registered", false);

        if (registered){
            viewPager.setCurrentItem(2);
        }else if(tokenReceived & !tokenActivated){
            viewPager.setCurrentItem(1);
        }else{
            viewPager.setCurrentItem(0);
        }

        //to detect a page change and update the content of the destination page
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }
            @Override
            public void onPageSelected(int position) {
                // Call the onRefresh() interface of the selected fragment
                ((OnRefreshListener) adapterViewPager.getItem(position)).onRefresh();
            }
            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Lifecycle-" + TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setOffscreenPageLimit(3); // forces the pageviewer to create all fragments at once
        fragmentStep1 = new RegisterStep1();
        fragmentStep2 = new RegisterStep2();
        fragmentRegistered = new RegisterCompleted();
        FragmentManager fm = getSupportFragmentManager();
        adapterViewPager = new MyPagerAdapter(fm);
        viewPager.setAdapter(adapterViewPager);

        appSettings = getSharedPreferences(TUM_GETIN_PREFERENCES, 0);

        MainActivity.context = getApplicationContext();
        backend = new Backend("www.grandcat.org", "3000");
    }
    @Override
    protected void onResume() {
        Log.d("Lifecycle-"+TAG, "onResume");
        super.onResume();
        String token = appSettings.getString("tumOnlineToken", "");
        String userID = appSettings.getString("tum_id", "");
        new CheckUserStatus().execute(token, userID); //which calls setPage() on PostExecut to update content
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
    }
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
        private static int NUM_ITEMS = 3;

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
                case 0: // Fragment # 0 - This will show RegisterStep1
                    return fragmentStep1;
                case 1: // Fragment # 1 - This will show RegisterStep2
                    return fragmentStep2;
                case 2: // Fragment # 2 - This will show RegisterCompleted
                    return fragmentRegistered;
                default:
                    return null;
            }
        }
        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
           if (position <= 1 ) {
               return "Step " + (position + 1);
           }else{
               return "Registration completed";
           }
        }



    }



}
