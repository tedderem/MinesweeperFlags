package com.tacoma.uw.erik.minesweeperflags;


import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
public class LaunchFragment extends Fragment {

    private static final String TAG = "launch";

    private static final String url = "http://cssgate.insttech.washington.edu/~tedderem/validate.php";

    private View myView;

    public LaunchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        myView = inflater.inflate(R.layout.fragment_launch, container, false);
        return myView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();

        //Set login and register button on click methods.
        if (view != null) {
            Button loginButton = (Button) view.findViewById(R.id.login_button);
            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    validateLogin();
                }
            });

            Button registerButton = (Button) view.findViewById(R.id.register_button);
            registerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //ensure there is a connection before openning registration fragment
                    ConnectivityManager connMgr = (ConnectivityManager)
                            getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.isConnected()) {
                        DialogFragment regFrag = new RegisterFragment();
                        regFrag.show(getActivity().getSupportFragmentManager(), "Register");
                    } else {
                        Toast.makeText(getActivity()
                                , "No network connection available.", Toast.LENGTH_LONG)
                                .show();
                    }
                }
            });
        }
    }

    /**
     * Method for validating a user's login credentials against the web services' information.
     */
    private void validateLogin() {
        if (myView != null) {
            final EditText usernameField = (EditText) myView.findViewById(R.id.username_input);

            String username = usernameField.getText().toString();

            String validateUrl = url;
            validateUrl += "?username=" + username;

            //ensure connection before attempting to validate login credentials
            ConnectivityManager connMgr = (ConnectivityManager)
                    getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                new AddUserWebTask().execute(validateUrl);
            } else {
                Toast.makeText(getActivity()
                        , "No network connection available.", Toast.LENGTH_LONG)
                        .show();
            }


        }

    }

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
                String status = jsonObject.getString("password");
                if (status.equalsIgnoreCase("error")) {
                    Toast.makeText(getActivity(), "Invalid username",
                            Toast.LENGTH_SHORT)
                            .show();
                } else {
                    //while view isnt null
                    if (myView != null) {
                        //retrieve input password and database password
                        final EditText passwordField = (EditText) myView.findViewById(R.id.password_input);
                        final String password = String.valueOf(passwordField.getText().toString().hashCode());

                        //if equal, enter into menu fragment
                        if (status.equals(password)) {
                            //update the preferences file to denote being logged in
                            SharedPreferences mSharedPreferences = getActivity().getSharedPreferences(
                                    getString(R.string.SHARED_PREFS), Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor =  mSharedPreferences.edit();
                            editor.putBoolean(getString(R.string.LOGGEDIN), true);

                            //update pref file to denote the username logged in
                            EditText username = (EditText) myView.findViewById(R.id.username_input);
                            editor.putString(getString(R.string.USERNAME), username.getText().toString());
                            editor.commit();

                            MenuFragment fragment = new MenuFragment();
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, fragment)
                                    .commit();
                        } else { //display error to user
                            Toast.makeText(getActivity(), "Invalid Username or Password",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                }
            }
            catch(Exception e) {
                Log.d(TAG, "Parsing JSON Exception " +
                        e.getMessage());
            }
        }
    }
}
