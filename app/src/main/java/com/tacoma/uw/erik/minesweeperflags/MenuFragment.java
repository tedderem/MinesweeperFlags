package com.tacoma.uw.erik.minesweeperflags;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.tacoma.uw.erik.minesweeperflags.control.UpdateWebTask;
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
 * Fragment that is responsible for displaying the game menu to the user. This game menu displays
 * all the current games associated with this user as well as giving the user the opportunity to
 * challenge another player to a game.
 *
 * @author Erik Tedder
 */
public class MenuFragment extends ListFragment {

    /** Tag value for debugging purposes. */
    private static final String TAG = "menu";

    /** The url for retrieving all the uses from the system. */
    private static final String GET_USERS_URL = "http://cssgate.insttech.washington.edu/~tedderem/users.php";

    /** The url for creating a new game. */
    private static final String CREATE_GAME_URL = "http://cssgate.insttech.washington.edu/~tedderem/createGame.php";

    /** The url for getting all games associated with a player. */
    private static final String GET_GAMES_URL = "http://cssgate.insttech.washington.edu/~tedderem/getGames.php?player=";

    /**
     * The time in which the fragment waits in order to pull
     * new game information from the server.
     */
    private static final long REFRESH_THRESHOLD = 5 * 60 * 1000;

    /** Listener responsible for notifying the activity a game was selected. */
    private GameSelectedListener myCallback;

    /** The current logged in user. */
    private String myCurrentUser;

    /** The list view used for displaying game information. */
    private ListView myListView;

    /** The list containing all the games associated with a user. */
    private List<GameInfo> myList;

    /** Adapter for facilitating the loading of game information into the ListView. */
    private ArrayAdapter<GameInfo> myArrayAdapter;

    /** The progress dialog view which keeps the user notified on retrieving games. */
    private ProgressDialog myProgressDialog;

    /** Reference to the shared preferences file. */
    private SharedPreferences mySharePrefs;

    /**
     * {@inheritDoc}
     *
     * Additionally initializes the call back listener to the given activity.
     */
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

    /** {@inheritDoc} */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    /**
     * {@inheritDoc}
     *
     * Initializes the various class fields as well as setting the challenge button listener.
     */
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

    /**
     * {@inheritDoc}
     *
     * Additionally ensures that the games are pulled down from the various databases when started.
     */
    @Override
    public void onStart() {
        super.onStart();

        //ensure the view is not null
        if (getView() != null) {
            //get the list view and initialize the list to contain game information
            myListView = (ListView) getView().findViewById(android.R.id.list);
            myList = new ArrayList<>();

            myArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1,
                    android.R.id.text1, myList);

            //check for games but not forced
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
        //or if this is a forced retrieval
        if (forceCheck || currentTime - lastRefresh > REFRESH_THRESHOLD) {
            Log.d(TAG, "Accessing web service");
            //display a progress dialog so the user isn't stuck at an empty screen
            myProgressDialog = new ProgressDialog(getActivity());
            myProgressDialog.setTitle("Loading");
            myProgressDialog.setMessage("Please wait while we load your games...");
            myProgressDialog.show();

            //store the current time in the shared preferences to allow for determining later
            //data retrievals
            SharedPreferences.Editor prefEditor = mySharePrefs.edit();
            prefEditor.putLong(getString(R.string.LASTREFRESH), System.currentTimeMillis());
            prefEditor.apply();

            //get the games based on the current user
            new GetGamesWebTask().execute(GET_GAMES_URL + myCurrentUser);
        } else {
            populateGameList();
        }
    }

    /**
     * Private method used to populate the game list on the List View object.
     */
    private void populateGameList() {
        //ensure view is not null
        if (getView() != null) {
            //clear the current items in the list to avoid duplicates
            myList.clear();

            //retrieve all the current games from the SQLite database
            GameInfoDB db = new GameInfoDB(getView().getContext());
            List<GameInfo> games = db.getGames(myCurrentUser);
            db.closeDB();

            //insert each game from the SQLite database into the array list for displaying
            for (GameInfo g : games) {
                Log.d(TAG, g.toString());
                myList.add(g);
            }

            myListView.setAdapter(myArrayAdapter);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Sends a onGameLoaded command to the main activity whenever a game has been selected.
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        //Notify the main activity that a game has been selected so it can be displayed
        if (myCallback != null) {
            myCallback.onGameLoaded(myList.get(position).getBoardId());
        }
    }

    /**
     * Standard interface to allow for communicating with the main activity when a game has been
     * selected by the user.
     */
    public interface GameSelectedListener {
        /**
         * A game has been selected and needs to be loaded into the fragment manager.
         *
         * @param board The ID for the board to be loaded.
         */
        void onGameLoaded(Integer board);
    }

    /**
     * AsyncTask class to allow for the retrieval of all the registered users from the off-site
     * database. Class also is responsible for constructing the dialog which lists these users
     * and allows the currently logged in user to challenge them.
     */
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

                            new UpdateWebTask().execute(s);
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

    /**
     * AsyncTask that takes care of getting all the games for the given logged in user.
     */
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
