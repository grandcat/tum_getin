package com.tca.mobiledooraccess;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by basti on 07.07.15.
 */
public class RegisterStep2 extends Fragment implements OnRefreshListener{

    private static final String TAG = "RegisterStep2";
    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS";
    private SharedPreferences appSettings;
    private Backend backend;
    ProgressDialog mProgressDialog;


    private void updateLayout(){
        appSettings = MainActivity.context.getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
        boolean tokenReceived = appSettings.getBoolean("token_received", false);
        boolean tokenActivated = appSettings.getBoolean("token_activated", false);
        boolean registered = appSettings.getBoolean("registered", false);
        View view = this.getView();
        ImageView tokenStatus = (ImageView) view.findViewById(R.id.imageTokenStatus);
        TextView infoText = (TextView) view.findViewById(R.id.infoText);
        Button openBrowser = (Button) view.findViewById(R.id.visitTUMOnlineButton);

        if (!tokenReceived){
            infoText.setText("Please request a token at Step 1");
            tokenStatus.setImageResource(R.drawable.token_not_activated);
            openBrowser.setVisibility(View.INVISIBLE);
        }

        if (tokenReceived && !tokenActivated){
            infoText.setText("Please visit TUM-Online and activate your token");
            openBrowser.setVisibility(View.VISIBLE);
            tokenStatus.setImageResource(R.drawable.token_not_activated);
        }
        if(tokenReceived && tokenActivated){
            infoText.setText("Your token is activated");
            tokenStatus.setImageResource(R.drawable.token_activated);
            openBrowser.setVisibility(View.INVISIBLE);
        }
    }

    public void onRefresh(){
        Log.d(TAG, "Refresh");
        appSettings = MainActivity.context.getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
        String token = appSettings.getString("tumOnlineToken", null);
        String userID = appSettings.getString("tum_id", null);
        new GetUserStatus().execute(token,userID);

    }
    final class GetUserStatus extends AsyncTask<String, Void, Void> {
        protected void onPreExecute(){
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMessage("Connecting to server...");
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.show();
        }

        protected Void doInBackground(String... params) {
            boolean tokenActivated = backend.tokenActivated(params[0]);
            String userCredentials[] = backend.getUserCredentials(params[1]);
            Log.d(TAG, "Token activated: " + tokenActivated);
            SharedPreferences.Editor editor = appSettings.edit();

            editor.putBoolean("token_activated", tokenActivated);

            if (userCredentials[0].equals("31")){
                editor.putBoolean("token_received",true);
            }else{
                editor.putBoolean("token_received",false);
            }
            editor.commit();
            mProgressDialog.dismiss();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            updateLayout();
        }

    }


    // Store instance variables based on arguments passed
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appSettings = getActivity().getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
        backend = new Backend("www.grandcat.org", "3000");
    }
    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register_step2, container, false);

        view.findViewById(R.id.imageUpdate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRefresh();
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
