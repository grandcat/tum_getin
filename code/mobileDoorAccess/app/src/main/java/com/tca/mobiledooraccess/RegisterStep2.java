package com.tca.mobiledooraccess;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.tca.mobiledooraccess.utils.KeyGeneratorTask;

/**
 * Created by basti on 07.07.15.
 */
public class RegisterStep2 extends Fragment implements OnRefreshListener{

    private static final String TAG = "RegisterStep2";
    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS";
    private SharedPreferences appSettings;
    private Backend backend;
    ProgressDialog mProgressDialog;

    ImageButton updateStatus;


    public void updateLayout(){
        // Reload settings to encouter for changes in register step 1
        appSettings = MainActivity.context.getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
        boolean tokenReceived = appSettings.getBoolean("token_received", false);
        boolean tokenActivated = appSettings.getBoolean("token_activated", false);
        boolean registered = appSettings.getBoolean("registered", false);
        View view = this.getView();
        ImageView tokenStatus = (ImageView) view.findViewById(R.id.imageTokenStatus);
        TextView infoText = (TextView) view.findViewById(R.id.infoText);
        Button openBrowser = (Button) view.findViewById(R.id.visitTUMOnlineButton);
        TextView updateText = (TextView) view.findViewById(R.id.textUpdate);
        TextView statusText = (TextView) view.findViewById(R.id.statusText);
        TextView tokenStatusText = (TextView) view.findViewById(R.id.tokenStatusText);

        infoText.setVisibility(View.VISIBLE);
        tokenStatus.setVisibility(View.VISIBLE);
        updateStatus.setVisibility(View.VISIBLE);
        updateText.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.VISIBLE);
        tokenStatusText.setVisibility(View.VISIBLE);

        if (!tokenReceived){
            infoText.setText("Please request a token at Step 1");
            tokenStatus.setImageResource(R.drawable.token_not_activated);
            openBrowser.setVisibility(View.INVISIBLE);
            tokenStatusText.setTextColor(Color.parseColor("#ee100f"));
            tokenStatusText.setText("!Activated");
        }

        if (tokenReceived && !tokenActivated){
            infoText.setText("Please visit TUMonline and activate your token");
            openBrowser.setVisibility(View.VISIBLE);
            tokenStatusText.setTextColor(Color.parseColor("#ee100f"));
            tokenStatusText.setText("!Activated");
            tokenStatus.setImageResource(R.drawable.token_not_activated);
        }
        if(tokenReceived && tokenActivated){
            // Todo: forward to regisiterd fragment
            infoText.setText("Your token is activated");
            tokenStatus.setImageResource(R.drawable.token_activated);
            tokenStatusText.setTextColor(Color.parseColor("#29b530"));
            tokenStatusText.setText("Activated");
            openBrowser.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Triggered if fragment is visible.
     */
    public void onRefresh() {
        Log.d("Lifecycle-"+TAG, "onRefresh");
        updateLayout();
    }

    public void refreshTokenStatus(View v) {
        RotateAnimation ra = new RotateAnimation(0, -360,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        ra.setFillAfter(true);
        ra.setDuration(1000);
        updateStatus.startAnimation(ra);
        // Update status
        String token = appSettings.getString("tumOnlineToken", "");
        String userID = appSettings.getString("tum_id", "");
        new GetUserStatus().execute(token, userID);
    }

    final class GetUserStatus extends AsyncTask<String, Void, Boolean> {
        protected void onPreExecute(){
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMessage("Updating Content...");
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.show();
        }

        protected Boolean doInBackground(String... params) {
            boolean tokenActivated = backend.tokenActivated(params[0]);
            String userCredentials[] = backend.getUserCredentials(params[1]);
            Log.d(TAG, "Token activated: " + tokenActivated);
            SharedPreferences.Editor editor = appSettings.edit();

            editor.putBoolean("token_activated", tokenActivated);

            if (tokenActivated){
                editor.putBoolean("registered", true);
            }

            editor.commit();
            mProgressDialog.dismiss();
            return tokenActivated;
        }

        @Override
        protected void onPostExecute(Boolean tokenActivated) {
            super.onPostExecute(tokenActivated);
            updateLayout();
            if (tokenActivated) {
                if(!appSettings.getBoolean("keys_generated", false)){
                    new KeyGeneratorTask(getActivity()).execute();
                }
            }
        }

    }


    // Store instance variables based on arguments passed
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("Lifecycle-" + TAG, "onCreate");
        super.onCreate(savedInstanceState);
        // Logic bindings
        appSettings = MainActivity.context.getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
        backend = new Backend("www.grandcat.org", "3000");
    }
    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register_step2, container, false);

        // Reference UI fields
        updateStatus = (ImageButton)view.findViewById(R.id.imageUpdate);

        view.findViewById(R.id.imageUpdate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshTokenStatus(v);
            }
        });
        view.findViewById(R.id.visitTUMOnlineButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uriURL = Uri.parse("https://campus.tum.de/tumonline/webnav.ini");
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriURL);
                startActivity(launchBrowser);
            }
        });
        return view;
    }
}
