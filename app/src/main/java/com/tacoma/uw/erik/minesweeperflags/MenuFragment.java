package com.tacoma.uw.erik.minesweeperflags;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.tacoma.uw.erik.minesweeperflags.model.Board;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class MenuFragment extends ListFragment {

    /** */
    private static final String TAG = "menu";

    /** */
    private static final String GET_USERS_URL = "http://cssgate.insttech.washington.edu/~tedderem/users.php";

    /** */
    private static final String CREATE_GAME_URL = "http://cssgate.insttech.washington.edu/~tedderem/createGame.php";

    /** */
    private static final String GET_GAMES_URL = "http://cssgate.insttech.washington.edu/~tedderem/getGames.php?player=";


    /** */
    private GameSelectedListener mCallback;

    /** */
    private ListView mListView;

    /** */
    private List<Integer> mList;

    /** */
    private ArrayAdapter<Integer> mArrayAdapter;

    /** */
    private ProgressDialog mProgressDialog;

    /** {@inheritDoc} */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (GameSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement GameSelectedListener");
        }
    }

    /**
     * Default constructor of a new menu fragment.
     */
    public MenuFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();

        //ensure that the view is not null
        if (view != null) {
            Button gameButton = (Button) view.findViewById(R.id.new_game_button);
            gameButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new GetUserWebTask().execute(GET_USERS_URL);
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        mListView = (ListView) getView().findViewById(android.R.id.list);
        mList = new ArrayList<Integer>();

        mArrayAdapter = new ArrayAdapter<Integer>(getActivity(), android.R.layout.simple_list_item_1,
                android.R.id.text1, mList);

        checkGames();
    }

    private void checkGames() {
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setTitle("Loading");
        mProgressDialog.setMessage("Please wait while we load your games...");
        mProgressDialog.show();

        SharedPreferences pref = getActivity().getSharedPreferences(getString(R.string.SHARED_PREFS),
                Context.MODE_PRIVATE);
        String currentUser = pref.getString(getString(R.string.USERNAME), getString(R.string.player_one_label));
        new GetGamesWebTask().execute(GET_GAMES_URL + currentUser);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (mCallback != null) {
            mCallback.onGameLoaded(mList.get(position));
        }
    }


    public interface GameSelectedListener {
        public void onGameLoaded(Integer board);
    }

    private class GetUserWebTask extends AsyncTask<String, Void, String> {

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
            int len = 5000;

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
                //JSONObject jsonObject = new JSONObject(s);

                JSONArray jsonUsers = new JSONArray(s);

                final String[] users = new String[jsonUsers.length() - 1];

                int j = 0;

                for (int i = 0; i < jsonUsers.length(); i++) {
                    JSONObject o = (JSONObject) jsonUsers.get(i);
                    SharedPreferences pref = getActivity().getSharedPreferences(getString(R.string.SHARED_PREFS),
                            Context.MODE_PRIVATE);
                    String currentUser = pref.getString(getString(R.string.USERNAME), getString(R.string.player_one_label));

                    String username = (String) o.get("username");

                    if (!currentUser.equals(username)) {
                        users[j++] = username;
                    }
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Challenge a Player").setItems(users, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences pref = getActivity().getSharedPreferences(getString(R.string.SHARED_PREFS),
                                Context.MODE_PRIVATE);
                        String currentUser = pref.getString(getString(R.string.USERNAME), getString(R.string.player_one_label));
                        Board board = new Board(currentUser, users[which]);

                        try {
//                            StringBuilder b = new StringBuilder();
//                            b.append(CREATE_GAME_URL);
//                            b.append("?playerone=");
//                            b.append(currentUser);
//                            b.append("&playertwo=");
//                            b.append(users[which]);
//                            b.append("&board=");
//                            b.append(new String(Base64.encode(board.getCompressed(), Base64.URL_SAFE)));

//                            String s = CREATE_GAME_URL + "?playerone=" + currentUser + "&playertwo="
//                                    + users[which] + "&board="
//                                    + new String(Base64.encode(board.getCompressed(), Base64.NO_WRAP));
                            String s = CREATE_GAME_URL + "?playerone=" + currentUser + "&playertwo="
                                    + users[which] + "&board="
                                    + board.getCompressed().trim();

                            //Log.d(TAG, s);
                            new CreateGameWebTask().execute(s);
                            checkGames();
                            //mCallback.onGameLoaded(board);
                        } catch (IOException e) {
                            Log.e(TAG, "Error creating game!");
                        }
                    }
                });
                builder.create().show();

            }
            catch(Exception e) {
                Log.d(TAG, "Parsing JSON Exception " +
                        e.getMessage());
            }
        }
    }

    private class CreateGameWebTask extends AsyncTask<String, Void, String> {

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

                if (jsonObject.getString("result").equals("success")) {
                    Toast.makeText(getActivity(), "Successfully created game!",
                            Toast.LENGTH_SHORT).show();
                } else {
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

    private class GetGamesWebTask extends AsyncTask<String, Void, String> {

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
            int len = 1000;

            try {
                URL url = new URL(myurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(20000 /* milliseconds */);
                conn.setConnectTimeout(30000 /* milliseconds */);
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

            if (s != null) {
                // Parse JSON
                try {
                    JSONArray jsonUsers = new JSONArray(s);
                    mList.clear();

                    for (int i = 0; i < jsonUsers.length(); i++) {
                        JSONObject o = (JSONObject) jsonUsers.get(i);
                        int boardID = o.getInt("id");
                        mList.add(boardID);

//                        String boardBlob = o.getString("board");
//                        boardBlob = boardBlob.substring(1, boardBlob.length() - 1);
//                        String[] strArray = boardBlob.split(",");
//                        byte[] bytes = new byte[strArray.length];
//
//                        for (int j = 0; j < strArray.length; j++) {
//                            bytes[j] = Byte.decode(strArray[j]);
//                        }
//
//                        Log.d(TAG, boardBlob);
//                        //Log.d(TAG, boardBlob.getBytes(Charset.forName("UTF-8")).toString());
//                        //byte[] bytes = Base64.decode(boardBlob.getBytes(), Base64.NO_WRAP);
//
//                        Board board = null;
//
//                        //decompress the Board blob
//                        try {
//                            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
//                            ObjectInputStream objIn = new ObjectInputStream(in);
//
//                            //Log.d(TAG, "AFTER INPUT STREAM");
//                            board = (Board) objIn.readObject();
//
//                            //Log.d(TAG, board.toString());
//                            //Log.d(TAG, "AFTER READ OBJECT");
//                            //Log.d(TAG, board.getPlayers()[0]);
//                            //Log.d(TAG, board.getPlayers()[1]);
//                            //mList.add(new GameInfo(board));
//                        } catch (IOException | ClassNotFoundException e) {
//                            e.printStackTrace();
//                        }
                    }

                    mListView.setAdapter(mArrayAdapter);

                } catch (Exception e) {
                    Log.d(TAG, "Parsing JSON Exception " +
                            e.getMessage());
                }
            }
            mProgressDialog.dismiss();
        }
    }

}
