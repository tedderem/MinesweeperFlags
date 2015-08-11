package com.tacoma.uw.erik.minesweeperflags;


import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * A simple {@link Fragment} subclass.
 */
public class RegisterFragment extends DialogFragment {

    private static final String TAG = "register";

    private static final String url = "http://cssgate.insttech.washington.edu/~tedderem/addUser.php";

    private AlertDialog myDialog;

    private TextView errorText;

    public RegisterFragment() {
        // Required empty public constructor
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final View theView = inflater.inflate(R.layout.fragment_register, null);

        errorText = (TextView) theView.findViewById(R.id.register_error_label);

        builder.setView(theView)
                .setPositiveButton("Create Account", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Nothing for now, over-ride it below to allow for preventing the
                        //dialog from closing
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RegisterFragment.this.getDialog().cancel();
                    }
                });

        builder.setTitle("Register Account");
        myDialog = builder.create();

        //Override the onShow for the dialog to make sure the button doesn't close the window
        //if something is not input correctly
        myDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button b = myDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        //attempt to register the user based on input variables
                        registerUser(theView);
                        //close only if registration was successful
                    }
                });
            }
        });

        return myDialog;
    }

    /**
     * Method for registering the user based on credentials placed into EditText views.
     *
     * @param view The view housing the EditTexts for simplicity.
     */
    private void registerUser(View view) {
        //ensure view isnt null
        if (view != null) {
            //retrieve the values within the edit texts
            //final TextView errorText = (TextView) view.findViewById(R.id.register_error_label);
            final EditText usernameField = (EditText) view.findViewById(R.id.register_username);
            final EditText password = (EditText) view.findViewById(R.id.register_password);
            final EditText passwordCheck = (EditText) view.findViewById(R.id.register_password_redo);

            String username = usernameField.getText().toString();
            String pwd = password.getText().toString();
            String pwdCheck = passwordCheck.getText().toString();

            //Ensure the fields are not empty and the passwords match
            if (username.length() != 0 && pwd.length() != 0 && pwd.equals(pwdCheck)) {
                //hash the password to increase security a bit
                String pwdHash = String.valueOf(password.getText().toString().hashCode());
                String regUrl = url;
                regUrl += "?username=" + username + "&password=" + pwdHash;
                Log.d(TAG, regUrl);
                errorText.setText("Attempting to Register Account!");
                new AddUserWebTask().execute(regUrl);
            }

            //a field is empty
            if (username == null || pwd == null || pwdCheck == null
                    || username.length() == 0 || pwd.length() == 0 || pwdCheck.length() == 0) {
                errorText.setText("Error: Cannot leave a field blank!");
            }

            //passwords do not match
            if (!pwd.equals(pwdCheck)) {
                errorText.setText("Error: Passwords do not match!");
            }
        }
    }

    /**
     * Class for interfacing with MySQL php stuff.
     */
    private class AddUserWebTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }

        // Given a URL, establishes an HttpUrlConnection and retrieves
        // the web page content as a InputStream, which it returns as
        // a string.
        private String downloadUrl(String myurl) throws IOException {
            InputStream is = null;
            // Only display the first 500 characters of the retrieved
            // web page content.
            int len = 500;

            try {
                URL url = new URL(myurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                int response = conn.getResponseCode();
                Log.d(TAG, "The response is: " + response);
                is = conn.getInputStream();

                // Convert the InputStream into a string
                String contentAsString = readIt(is, len);
                Log.d(TAG, "The string is: " + contentAsString);
                return contentAsString;

                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } catch (Exception e) {
                Log.d(TAG, "Something happened" + e.getMessage());
            } finally {
                if (is != null) {
                    is.close();
                }
            }
            return null;

        }

        // Reads an InputStream and converts it to a String.
        public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
            Reader reader = null;
            reader = new InputStreamReader(stream, "UTF-8");
            char[] buffer = new char[len];
            reader.read(buffer);
            return new String(buffer);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            // Parse JSON
            try {
                JSONObject jsonObject = new JSONObject(s);
                String status = jsonObject.getString("result");
                if (status.equalsIgnoreCase("success")) {
                    errorText.setText("Account Successfully Created!");

                    //introduce a small delay to allow for user to know account is created
                    Handler h = new Handler();
                    h.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            myDialog.dismiss();
                        }
                    }, 1500);
                } else {
                    String reason = jsonObject.getString("error");
                    errorText.setText("Error: " + reason);
                }
            }
            catch(Exception e) {
                Log.d(TAG, "Parsing JSON Exception " +
                        e.getMessage());
            }
        }
    }

}
