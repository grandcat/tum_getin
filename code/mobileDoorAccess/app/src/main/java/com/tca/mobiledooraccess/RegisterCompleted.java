package com.tca.mobiledooraccess;

import android.content.SharedPreferences;
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
 * Created by basti on 09.07.15.
 */
public class RegisterCompleted extends Fragment implements OnRefreshListener {
    private static final String TAG = "RegisterCompl";
    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS";
    private SharedPreferences appSettings;

    public void updateLayout(View view){
        appSettings = MainActivity.context.getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
        boolean tokenReceived = appSettings.getBoolean("token_received", false);
        boolean tokenActivated = appSettings.getBoolean("token_activated", false);
        boolean registered = appSettings.getBoolean("registered", false);


        ImageView imgTokenReceived = (ImageView) view.findViewById(R.id.registered_token_received_image);
        ImageView imgTokenActivated = (ImageView) view.findViewById(R.id.registered_token_activated_image);
        ImageView imgRegistered = (ImageView) view.findViewById(R.id.registered_image);
        ImageView imgNFC = (ImageView) view.findViewById(R.id.nfc_hand_picture);

        if (tokenReceived){
            imgTokenReceived.setImageResource(R.drawable.token_activated);
        }else{
            imgTokenReceived.setImageResource(R.drawable.token_not_activated);
        }
        if (tokenActivated){
            imgTokenActivated.setImageResource(R.drawable.token_activated);
        }else{
            imgTokenActivated.setImageResource(R.drawable.token_not_activated);
        }
        if (registered){
            imgRegistered.setImageResource(R.drawable.token_activated);
            imgNFC.setVisibility(View.VISIBLE);
        }else{
            imgTokenReceived.setImageResource(R.drawable.token_not_activated);
            imgNFC.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public void onRefresh() {
        Log.d("Lifecycle-"+TAG, "onRefresh");
        View view = getView();
        updateLayout(view);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("Lifecycle-"+TAG, "onCreate");
        super.onCreate(savedInstanceState);
        appSettings = MainActivity.context.getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_registered, container, false);
        updateLayout(view);
        return view;
    }


}
