package com.tacoma.uw.erik.minesweeperflags;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;


/**
 * A fragment which is responsible for handling all chat features within the project.
 *
 * @author Erik Tedder
 * @date 8/11/2015
 */
public class ChatFragment extends Fragment {

    /** The URL needed for saving the chat on user inputs. */
    private final static String SAVE_CHAT_URL = "http://cssgate.insttech.washington.edu/~tedderem/saveChat.php";

    /** The URL needed for getting the chat from the web service. */
    private final static String GET_CHAT_URL = "http://cssgate.insttech.washington.edu/~tedderem/getChat.php";

    /** The tag for displaying any debug messages with the log function. */
    private final static String TAG = "CHAT";

    /** The delay between chat updates from the web service in milliseconds. */
    private static final long REFRESH_TIME = 5000;

    /** The board ID that this chat corresponds to. */
    private int myBoardId;

    /** The entire chat log. */
    private String myChatLog;

    /**
     * No-argument default constructor of this fragment.
     */
    public ChatFragment() {
        // Required empty public constructor
    }

    /** Handler responsible for managing the update timer thread. */
    Handler timerHandler = new Handler();

    /** Update timer thread which will constantly retrieves the chat logs from the web server. */
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            //construct the url and execute the web task
            String url = GET_CHAT_URL + "?id=" + myBoardId;
            new RefreshChatWebTask().execute(url);

            timerHandler.postDelayed(this, REFRESH_TIME);
        }
    };

    /**
     * {@inheritDoc}
     *
     * Additionally, makes sure to stop the chat update timer when this fragment is paused.
     */
    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    /** {@inheritDoc} */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    /**
     * Method which sets the text within the chat's TextView widget.
     */
    private void setChatLog() {
        //get the textview that corresponds to the text view that shows the chat log
        final TextView chatBox = (TextView) getActivity().findViewById(R.id.chat_text_view);
        //set the text for this TextView to be the String from myChatLog but with HTML formatting
        chatBox.setText(Html.fromHtml(myChatLog));

        //Get the ScrollView from the layout and scroll it to the bottom
        final ScrollView scrollable = (ScrollView) getActivity().findViewById(R.id.chat_scroll_view);
        scrollable.smoothScrollTo(0, chatBox.getBottom() - 1);
    }

    /**
     * {@inheritDoc}
     *
     * Method constructs the view for the fragment when initially created and set the listeners
     * for the chat button.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //get the board id given to this chat fragment
        myBoardId = getArguments().getInt(getString(R.string.board_bundle));
        View view = getView();

        //ensure the view isn't null
        if (view != null) {
            //get the button and edittext views from the layout
            final Button sendButton = (Button) getActivity().findViewById(R.id.chat_send_button);
            final EditText editText = (EditText) getActivity().findViewById(R.id.chat_edit_text);

            //set what the button does when clicked
            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //get the current user's name
                    SharedPreferences pref = getActivity()
                            .getSharedPreferences(getString(R.string.SHARED_PREFS),
                                    Context.MODE_PRIVATE);
                    String user = pref.getString(getString(R.string.USERNAME),
                            getString(R.string.player_one_label));

                    //retrieve text from edit text view and clear the view
                    String textToEnter = "<b><u>" + user + ":</b></u> "+ editText.getText().toString();
                    editText.setText("");

                    //append to end of chat log
                    myChatLog += textToEnter + "<br/>";
                    setChatLog();

                    String url = null;

                    //construct the URL necessary to save the chat
                    try {
                        url = SAVE_CHAT_URL + "?id=" + myBoardId + "&log="
                                + URLEncoder.encode(myChatLog, "UTF-8");
                        new SaveChatWebTask().execute(url);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        //ensure that the thread responsible for updating the chat is running
        timerHandler.postDelayed(timerRunnable, 0);
    }

    /**
     * Class which is responsible for saving chat logs to the web service. Used by calling the
     * execute(String) method where the String parameter is the URL to execute.
     */
    private class SaveChatWebTask extends AsyncTask<String, Void, String> {

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
            // Only display the first 5000 characters of the retrieved
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

                if (!jsonObject.getString("result").equals("success")) {
                    Toast.makeText(getActivity(), jsonObject.getString("error"),
                            Toast.LENGTH_SHORT).show();
                }
            }
            catch(Exception e) {
                Log.d(TAG, "Parsing JSON Exception " +
                        e.getMessage());
            }
        }
    }

    /**
     * Class responsible for refreshing the chat for the User and ensuring that the chat log is the
     * most up-to-date in relation to the log stored on the offsite database.
     */
    private class RefreshChatWebTask extends AsyncTask<String, Void, String> {

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
            // Only display the first 1000000 characters of the retrieved
            // web page content.
            int len = 10000;

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

                //Use a bufferedreader to capture the data from the URL response
                BufferedReader buffer = new BufferedReader(
                        new InputStreamReader(is));
                String contentAsString = "";
                String s = "";
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

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            // Parse JSON
            try {
                JSONObject o = new JSONObject(s);

                //get the String for the log of this board ID
                String log = o.getString("log");

                //If the log is different from the one already displayed, set it as current and
                //update the chat log view
                if (!log.equals(myChatLog)) {
                    myChatLog = log;
                    setChatLog();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

}