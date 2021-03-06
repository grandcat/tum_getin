package com.tca.mobiledooraccess;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tca.mobiledooraccess.utils.KeyGeneratorTask;

/**
 * Register Step 1 Fragment
 *
 * Asks for a user ID
 *
 * Checks the userinput if its valid
 * If the id is valid, the backend answers with a user token that can be stored
 * The fragment will forward the user to the next registration step when backend communication
 * is finished
 *
 */

public class RegisterStep1 extends Fragment implements OnRefreshListener{
    private static final String TAG = "RegisterStep1";
    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS";
    private Backend backend;
    private SharedPreferences appSettings;
    ProgressDialog mProgressDialog;


    public void onRefresh(){
        Log.d("Lifecycle-"+TAG, "onRefresh");
    }

    // Store instance variables based on arguments passed
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("Lifecycle-"+TAG, "onCreate");
        super.onCreate(savedInstanceState);
        backend = new Backend("www.grandcat.org", "3000");
        appSettings = getActivity().getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
        Log.d(TAG, "Test pseudo ID: " + appSettings.getString("pseudo_id", ""));
    }

    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        final View view = inflater.inflate(R.layout.fragment_register_step1, container, false);
        final Button button = (Button) view.findViewById(R.id.buttonSendTUMID);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Clicked Send TUM ID");
                EditText tumIDEditText = (EditText) view.findViewById(R.id.editTextTUMID);
                final String tumID = tumIDEditText.getText().toString();

                if (tumID.matches("")) {
                    Log.d(TAG, "TUM ID field empty");
                    Toast.makeText(getActivity(), "You did not enter a TUM-ID", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    new SendTUMID().execute(tumID); //calls onServerResponse after execution
                                                    // which will set the next page
                }
            }
        });


        //Supports posting the id over the keyboard action button on the right lower corner
        EditText editText = (EditText) view.findViewById(R.id.editTextTUMID);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    button.performClick(); // calls the button onClick method
                    handled = true;
                }
                return handled;
            }
        });
        return view;
    }

    //Called as an PostExecute Method of the AsyncTask that communicates with the backend
    public void onServerResponse(){
        EditText editText = (EditText) getView().findViewById(R.id.editTextTUMID);
        editText.setText("");

        if (appSettings.getBoolean("token_received", false)){
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
            MainActivity.viewPager.setCurrentItem(1);
        } else {
            Toast.makeText(getActivity(), "Your TUM ID is invalid or already registered.", Toast.LENGTH_LONG).show();
        }
    }

    final class SendTUMID extends AsyncTask<String, Void, Void> {
        protected void onPreExecute(){
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMessage("Connecting to server...");
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.show();
        }

        protected Void doInBackground(String... params) {
            String result[] = backend.getUserCredentials(params[0]);
            SharedPreferences.Editor editor = appSettings.edit();

            switch (result[0]){
                //all kind of error codes, break if one of those are the answer of the backend
                case "20":
                case "21":
                case "30":
                case "31":
                case "39":
                    break;
                default:
                    editor.putString("tum_id", params[0]);
                    editor.putString("tumOnlineToken", result[0]);
                    editor.putString("pseudo_ID", result[1]);
                    editor.putString("salt", result[2]);
                    editor.putBoolean("token_received", true);
                    editor.commit();
            }
            mProgressDialog.dismiss();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            onServerResponse();
        }

    }

}
