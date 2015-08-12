package com.tacoma.uw.erik.minesweeperflags.control;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * The default web task for doing standard updates or insertion database queries when there is no
 * need for additional code after the execution.
 *
 * @author Erik Tedder
 */
public class UpdateWebTask extends AsyncTask<String, Void, String> {

    /**
     * The tag for this class to allow for debugging messages.
     */
    private final static String TAG = "UpdateWebTask";

    /**
     * {@inheritDoc}
     */
    @Override
    protected String doInBackground(String... urls) {
        try {
            return downloadUrl(urls[0]);
        } catch (IOException e) {
            return "Unable to retrieve web page. URL may be invalid.";
        }
    }

    /**
     * Makes a URL connection based on the given parameter and evaluates the given result.
     *
     * @param myurl The url to be connected to.
     * @return null?
     * @throws IOException
     */
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        // Only display the first 200 characters of the retrieved
        // web page content.
        int len = 200;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(TAG, "The response is: " + response);
            is = conn.getInputStream();

            //utilize a buffered reader to retrieve web service output
            BufferedReader buffer = new BufferedReader(
                    new InputStreamReader(is));

            //concatenate string based on buffered reader's lines
            String contentAsString = "";
            String s;
            while ((s = buffer.readLine()) != null) {
                contentAsString += s;
            }

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

    /**
     * {@inheritDoc}
     *
     * Method for ensuring the update took place correctly.
     */
    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        // Parse JSON
        try {
            JSONObject jsonObject = new JSONObject(s);

            if (!jsonObject.getString("result").equals("success")) {
                Log.d(TAG, jsonObject.getString("error"));
            }
        }
        catch(Exception e) {
            Log.d(TAG, "Parsing JSON Exception " +
                    e.getMessage());
        }
    }
}
