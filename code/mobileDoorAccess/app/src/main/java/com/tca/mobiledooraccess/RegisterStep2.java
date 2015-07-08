package com.tca.mobiledooraccess;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by basti on 07.07.15.
 */
public class RegisterStep2 extends Fragment implements OnRefreshListener{

    private static final String TAG = "RegisterStep2";


    public void onRefresh(){
        Log.d(TAG, "Refresh");
    }

    // Store instance variables based on arguments passed
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register_step2, container, false);
        return view;
    }
}
