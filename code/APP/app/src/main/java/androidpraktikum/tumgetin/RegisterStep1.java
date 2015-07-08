package androidpraktikum.tumgetin;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by basti on 07.07.15.
 */
public class RegisterStep1 extends Fragment {
    public static final String TUM_GETIN_PREFERENCES = "TGI_PREFS";
    public static final String TAG = "NotRegisteredActivity";
    private Backend backend;
    private SharedPreferences appSettings;
    ProgressDialog mProgressDialog;

    // Store instance variables based on arguments passed
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        backend = new Backend("www.grandcat.org", "3000");
        appSettings = getActivity().getSharedPreferences(TUM_GETIN_PREFERENCES, 0);
    }

    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_register_step1, container, false);
        Button button = (Button) view.findViewById(R.id.buttonSendTUMID);
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
                }else{
                    new SendTUMID().execute(tumID);
                    ((EditText) view.findViewById(R.id.editTextTUMID)).setText("");
                    if (appSettings.getBoolean("token_received", false)){
                        MainActivity.viewPager.setCurrentItem(1);
                    }else{
                        view.findViewById(R.id.imageView).requestFocus();
                        Toast.makeText(getActivity(), "Your TUM ID is invalid", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        return view;
    }

    final class SendTUMID extends AsyncTask<String, Void, Integer>{
        protected void onPreExecute(){
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMessage("Connecting to server...");
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.show();
        }

        protected Integer doInBackground(String... params) {
            String result[] = backend.getTokenPseudoID(params[0]);
            Log.d(TAG, "Result from server: " + result[0] + " | " + result[1]);
            SharedPreferences.Editor editor = appSettings.edit();

            switch (result[0]){
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
                    editor.putBoolean("token_received", true);
                    editor.commit();
            }
            mProgressDialog.dismiss();
            return 0;
        }
    }

}
