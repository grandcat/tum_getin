package com.tca.mobiledooraccess;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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

    public void updateLayout(){
        appSettings = MainActivity.context.getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
        boolean registered = appSettings.getBoolean("registered", false);

        View view = this.getView();

        ImageView imgComplete = (ImageView)view.findViewById(R.id.img_registered_status);
        TextView txtComplete = (TextView) view.findViewById(R.id.txt_registered_status);
        Button complete = (Button) view.findViewById(R.id.btn_registered_status);

        if (registered){
            imgComplete.setImageResource(R.drawable.token_activated);
            txtComplete.setTextColor(Color.parseColor("#29b530"));
            txtComplete.setText("OK");
            complete.setVisibility(View.VISIBLE);

        }else{
            imgComplete.setImageResource(R.drawable.token_not_activated);
            txtComplete.setTextColor(Color.parseColor("#ffee100f"));
            complete.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onRefresh() {
        Log.d("Lifecycle-"+TAG, "onRefresh");
        updateLayout();
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
        Button complete = (Button) view.findViewById(R.id.btn_registered_status);
        complete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean registered_done = appSettings.getBoolean("registered_done", false);
                SharedPreferences.Editor editor = appSettings.edit();
                editor.putBoolean("registered_done", true);
                editor.commit();
                Intent intent = new Intent(getActivity(), UnlockProgressActivity.class);
                startActivity(intent);
            }
        });
        return view;
    }


}
