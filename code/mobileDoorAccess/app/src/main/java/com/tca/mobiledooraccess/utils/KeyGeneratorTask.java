package com.tca.mobiledooraccess.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.tca.mobiledooraccess.Backend;
import com.tca.mobiledooraccess.MainActivity;
import com.tca.mobiledooraccess.R;

import java.security.KeyPair;

/**
 * Created by Stefan on 05.07.2015.
 */
public final class KeyGeneratorTask extends AsyncTask<Void, Void, Integer> {
    public final static String TAG = "KeyGeneratorTask";

    private Context activityContext;
    ProgressDialog dialog;

    public KeyGeneratorTask(Context c) {
        this(c, null);
    }

    public KeyGeneratorTask(Context c, ProgressDialog progress) {
        super();
        this.activityContext = c;
        if (progress != null) {
            this.dialog = progress;
        } else {
            this.dialog = new ProgressDialog(activityContext);
        }
    }

    /**
     * Generates a key pair, stores it locally in the private preferences and uploads
     * the public key to the backend infrastructure.
     * @param c Context of the current activity to access the shared preferences.
     * @param dialog Optional ProgressDialog. Allows to update the displayed text during
     *               the key generation and remote storage of the public key.
     * @return 0 on success. Otherwise, a negative value indicates an error occurred.
     */
    public static int generateAndStoreKeys(Context c, ProgressDialog dialog) {
        int success = 0;

        if (dialog != null) dialog.setMessage(c.getString(R.string.keygen_dialog_text));
        dialog.setProgress(50);
        KeyPair keys = RSACrypto.generateKeyPair();
        String publicKeyB64 = RSACrypto.serializeKey(keys.getPublic());
        String privateKeyB64 = RSACrypto.serializeKey(keys.getPrivate());

        Log.d(TAG, "Generated public key: " + publicKeyB64);
        // Get preferences for key deposit
        SharedPreferences prefs = c.getSharedPreferences(
                MainActivity.TUM_GETIN_PREFERENCES,
                Context.MODE_PRIVATE
        );
        SharedPreferences.Editor settings = prefs.edit();
        String tumId = prefs.getString("tum_id", "");
        String token = prefs.getString("tumOnlineToken", "");
        // Store private key in protected area
        settings.putString("priv_key", privateKeyB64);
        // Immediately write changes so that other threads are informed about the success
        // of this operation
        settings.commit();

        // Send public key to backend
        // if (dialog != null) dialog.setMessage(c.getString(R.string.uploadKey));
        Backend backend = new Backend("www.grandcat.org", "3000");
        int res = backend.sendPublicKey(tumId, token, publicKeyB64);
        Log.d(TAG, "Result for key transmission to backend: " + res);
        if (0 != res) {
            success = -1;
        }

        return success;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle(R.string.keygen_dialog_title);
        dialog.setMessage(activityContext.getString(R.string.keygen_dialog_text));
        dialog.show();
    }

    @Override
    protected Integer doInBackground(Void... params) {
        return generateAndStoreKeys(activityContext, dialog);
    }

    @Override
    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        dialog.dismiss();
        SharedPreferences prefs = MainActivity.context.getSharedPreferences(
                MainActivity.TUM_GETIN_PREFERENCES,
                Context.MODE_PRIVATE
        );
        SharedPreferences.Editor settings = prefs.edit();
        settings.putBoolean("keys_generated", true);
        settings.commit();
        MainActivity.viewPager.setCurrentItem(2);
        // Todo: on error, reset the state and rollback any changes
    }
}