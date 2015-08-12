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

import com.tacoma.uw.erik.minesweeperflags.data.GameInfoDB;
import com.tacoma.uw.erik.minesweeperflags.model.Board;
import com.tacoma.uw.erik.minesweeperflags.model.GameInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
    private static final long REFRESH_THRESHOLD = 5 * 60 * 1000;

    /** */
    private GameSelectedListener myCallback;

    private String myCurrentUser;

    /** */
    private ListView myListView;

    /** */
    private List<GameInfo> myList;

    /** */
    private ArrayAdapter<GameInfo> myArrayAdapter;

    /** */
    private ProgressDialog myProgressDialog;

    private SharedPreferences mySharePrefs;

    /** {@inheritDoc} */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            myCallback = (GameSelectedListener) activity;
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

        mySharePrefs = getActivity().getSharedPreferences(getString(R.string.SHARED_PREFS),
                Context.MODE_PRIVATE);
        myCurrentUser = mySharePrefs.getString(getString(R.string.USERNAME),
                getString(R.string.player_one_label));


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

        if (getView() != null) {
            myListView = (ListView) getView().findViewById(android.R.id.list);
            myList = new ArrayList<>();

            myArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1,
                    android.R.id.text1, myList);

            checkGames(false);
        }
    }

    /**
     * Method which is responsible for checking for games that correspond to the currently logged
     * in user.
     *
     * @param forceCheck Parameter for forcing the retrieval of the games from the off-site database.
     */
    private void checkGames(final boolean forceCheck) {
        //get the last refresh time
        long lastRefresh = mySharePrefs.getLong(getString(R.string.LASTREFRESH), 0);
        long currentTime = System.currentTimeMillis();

        //check if the difference between now and the last refresh is past the threshold
        if (forceCheck || currentTime - lastRefresh > REFRESH_THRESHOLD) {
            Log.d(TAG, "Accessing web service");
            //display a progress dialog so the user isn't stuck at an empty screen
            myProgressDialog = new ProgressDialog(getActivity());
            myProgressDialog.setTitle("Loading");
            myProgressDialog.setMessage("Please wait while we load your games...");
            myProgressDialog.show();

            SharedPreferences.Editor prefEditor = mySharePrefs.edit();
            prefEditor.putLong(getString(R.string.LASTREFRESH), System.currentTimeMillis());
            prefEditor.apply();

            //get the games based on the current user
            new GetGamesWebTask().execute(GET_GAMES_URL + myCurrentUser);
        } else {
            populateGameList();
        }
    }

    private void populateGameList() {
        if (getView() != null) {
            myList.clear();

            GameInfoDB db = new GameInfoDB(getView().getContext());
            List<GameInfo> games = db.getGames(myCurrentUser);
            db.closeDB();

            for (GameInfo g : games) {
                Log.d(TAG, g.toString());
                myList.add(g);
            }

            myListView.setAdapter(myArrayAdapter);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (myCallback != null) {
            myCallback.onGameLoaded(myList.get(position).getBoardId());
        }
    }


    public interface GameSelectedListener {
        void onGameLoaded(Integer board);
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
        public String readIt(InputStream stream, int len) throws IOException {
            Reader reader;
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
                //get the array of users from the web service
                JSONArray jsonUsers = new JSONArray(s);

                //create a new array of users that's 1 less than the total users
                final String[] users = new String[jsonUsers.length() - 1];

                int j = 0;

                //iterate through all the registered users
                for (int i = 0; i < jsonUsers.length(); i++) {
                    JSONObject o = (JSONObject) jsonUsers.get(i);

                    //check if the user is the current user to avoid users challenging themselves
                    String username = (String) o.get("username");
                    if (!myCurrentUser.equals(username)) {
                        users[j++] = username;
                    }
                }

                //construct an alert dialog that will display this altered list of users
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Challenge a Player").setItems(users, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //construct a new board with the current logged in user and the selected
                        //user
                        Board board = new Board(myCurrentUser, users[which]);

                        //attempt to create this game through web services
                        try {
                            String s = CREATE_GAME_URL + "?playerone=" + myCurrentUser + "&playertwo="
                                    + users[which] + "&board="
                                    + board.getCompressed().trim();

                            new CreateGameWebTask().execute(s);
                            checkGames(true);
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
        public String readIt(InputStream stream, int len) throws IOException {
            Reader reader;
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
        public String readIt(InputStream stream, int len) throws IOException {
            Reader reader;
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
                    //get the array of game ids
                    JSONArray jsonGames = new JSONArray(s);
                    //clear the current list to ensure that the games arent duplicated in view
                    //myList.clear();

                    if (getView() != null) {
                        GameInfoDB db = new GameInfoDB(getView().getContext());

                        db.wipeDB();
                        //for each game ID, add this id to the list
                        for (int i = 0; i < jsonGames.length(); i++) {
                            JSONObject o = (JSONObject) jsonGames.get(i);
                            int boardID = o.getInt("id");
                            String playerOne = o.getString("playerOne");
                            String playerTwo = o.getString("playerTwo");
                            int finished = o.getInt("finished");

                            db.insertGame(boardID, playerOne, playerTwo, finished);
                        }
                        db.closeDB();
                    }

                    //set the adapter for the list view.
                    //myListView.setAdapter(myArrayAdapter);
                    populateGameList();
                } catch (Exception e) {
                    Log.d(TAG, "Parsing JSON Exception " +
                            e.getMessage());
                }
            }
            //close out the progress dialog
            myProgressDialog.dismiss();
        }
    }

}
