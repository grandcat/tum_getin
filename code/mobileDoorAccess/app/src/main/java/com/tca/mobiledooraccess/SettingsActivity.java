package com.tca.mobiledooraccess;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.style.UpdateAppearance;
import android.widget.Toast;

import com.tca.mobiledooraccess.utils.KeyGeneratorTask;

/**
 * Created by basti on 12.07.15.
 */
public class SettingsActivity extends PreferenceActivity {

    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS";
    private static final String TAG = "SettingsActivity";
    SharedPreferences appSettings;
    private Backend backend;
    ProgressDialog mProgressDialog;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        appSettings = getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
        context = getApplicationContext();
        backend = new Backend("www.grandcat.org", "3000");

        Preference p1 = (Preference)findPreference("pref_delete_account");
        p1.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new DeleteAccount().execute();
                return false;
            }
        });
        Preference p2 = (Preference)findPreference("pref_new_key_pair");
        p2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new UpdateKeyPair().execute();
                return false;
            }
        });
        Preference p3 = (Preference)findPreference("pref_new_pseudo_id");
        p3.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new UpdatePseudoIDandSalt().execute();
                return false;
            }
        });



    }


    final class DeleteAccount extends AsyncTask<Void, Integer, Boolean> {
        protected void onPreExecute(){
            mProgressDialog = new ProgressDialog(SettingsActivity.this);
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
                settings.remove("token_activated").remove("token_received").remove("registered").remove("keys_generated").remove("registered_done");
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
        protected void onPostExecute(Boolean deleteSuccess) {
            super.onPostExecute(deleteSuccess);

            String msg;
            if (deleteSuccess) {
                // Account deleted successfully online
                msg = getString(R.string.toast_account_deleted_success);
            } else {
                msg = getString(R.string.toast_account_deleted_err);
            }
            Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_LONG).show();

        }
    }
    final class UpdateKeyPair extends AsyncTask<Void, Void, Void> {
        protected void onPreExecute(){
            mProgressDialog = new ProgressDialog(SettingsActivity.this);
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
            mProgressDialog = new ProgressDialog(SettingsActivity.this);
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
}

