package com.tacoma.uw.erik.minesweeperflags;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tacoma.uw.erik.minesweeperflags.control.BoardEvents;
import com.tacoma.uw.erik.minesweeperflags.control.UpdateWebTask;
import com.tacoma.uw.erik.minesweeperflags.model.Board;
import com.tacoma.uw.erik.minesweeperflags.model.Cell;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;

/**
 * A fragment that will be used to depict a minesweeper game being played.
 *
 * @author Erik Tedder
 */
public class GameFragment extends Fragment implements Observer {

    /**
     * The base URL needed to save the game.
     */
    private static final String SAVE_GAME_URL = "http://cssgate.insttech.washington.edu/~tedderem/saveGame.php";

    /**
     * The base URL needed to get a single game.
     */
    private static final String GET_GAME_URL = "http://cssgate.insttech.washington.edu/~tedderem/getGame.php";

    /**
     * The base URL needed for setting a game to be finished.
     */
    private static final String GAME_FINISHED_URL = "http://cssgate.insttech.washington.edu/~tedderem/setFinished.php?id=";

    /**
     * The constant tag for this particular fragment to allow for logging information.
     */
    private static final String TAG = "GAME";

    /**
     * The intended delay between retrieving new board updates in milliseconds.
     */
    private static final int REFRESH_TIME = 5000;

    /**
     * The ID of the board, used for retrieving the correct board from the web service.
     */
    private int myBoardID;

    /**
     * The Board object that is used to retain the game information for the users.
     */
    private Board myBoard;

    /**
     * The array of Buttons that will represent the Cell array from myBoard.
     */
    private Button[][] myCells;

    /**
     * Custom listener that will be used to notify the main activity when a chat window has been
     * opened.
     */
    private ChatOpenedListener myCallback;

    /**
     * The progress dialog that will be displayed to the user when a game board is being loaded
     * for the first time.
     */
    private ProgressDialog myProgressDialog;

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game, container, false);
    }

    /**
     * A handler object that will be used to manage the timer that is refreshing the game board.
     */
    private final Handler timerHandler = new Handler();

    /**
     * A runnable thread that is used to consistently poll the web service for updates to the game
     * board.
     */
    private final Runnable timerRunnable = new Runnable() {

        /**
         * {@inheritDoc}
         *
         * Overridden run method which polls the web service for updated  game boards.
         */
        @Override
        public void run() {
            //retrieve the current logged in user.
            SharedPreferences pref = getActivity().getSharedPreferences(getString(R.string.SHARED_PREFS),
                    Context.MODE_PRIVATE);
            String username = pref.getString(getString(R.string.USERNAME),
                    getString(R.string.player_one_label));

            //aslong as the current board isn't null and it isn't the logged in user's turn
            if (myBoard != null && !myBoard.getCurrentPlayer().equals(username)
                    && !myBoard.isGameOver()) {
                //retrieve the game from the web service
                getGame();
            }
            timerHandler.postDelayed(this, REFRESH_TIME);
        }
    };

    /**
     * Method which is responsible for polling the web service using GET_GAME_URL constant
     * to retrieve the board object that is stored on the offsite database.
     */
    private void getGame() {
        //Log the current ID of the board for debugging
        Log.d(TAG, String.valueOf(myBoardID));
        //concatenate the GET_GAME_URL with the board id
        String s = GET_GAME_URL + "?id=" + myBoardID;
        //execute a refresh game web task with the constructed url
        new RefreshGameWebTask().execute(s);

    }


    /**
     * {@inheritDoc}
     *
     * Also initializes the callback listener based on the passed activity.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            myCallback = (ChatOpenedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement GameSelectedListener");
        }
    }

    /**
     * {@inheritDoc}
     *
     * Method also responsible for retrieving the game board information to be able to display the
     * game to the user.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //retrieve board ID and get game values
        myBoardID = getArguments().getInt(getString(R.string.board_bundle));
        getGame();

        View view = getView();

        if (view != null) {
            //get the chat button from the view and set its appropriate listener
            Button chatButton = (Button) view.findViewById(R.id.chat_button);
            chatButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    myCallback.onChatOpened(myBoardID);
                }
            });
        }

        //check if the board hasn't been loaded yet (null). Show progress dialog if is null
        if (myBoard == null) {
            myProgressDialog = new ProgressDialog(getActivity());
            myProgressDialog.setTitle("Loading");
            myProgressDialog.setMessage("Loading selected game...");
            myProgressDialog.show();
        }

        //start update thread
        timerHandler.postDelayed(timerRunnable, 0);
    }

    /**
     * {@inheritDoc}
     *
     * Overridden to ensure the timer thread for refresh game board gets stopped.
     */
    @Override
    public void onPause() {
        super.onPause();
        //stop the refresh thread
        timerHandler.removeCallbacks(timerRunnable);
    }

    /**
     * Constructor of a new GameFragment.
     */
    public GameFragment() {
        //empty for now
    }

    /**
     * The custom interface for a listener for whenever a chat window gets opened.
     */
    public interface ChatOpenedListener {
        void onChatOpened(Integer board);
    }

    /**
     * Method for constructing the buttons responsible for displaying the Board information.
     */
    private void constructBoardView() {
        //set this to be an observer of the board
        myBoard.addObserver(this);
        //initialize the button array
        myCells = new Button[myBoard.getHeight()][myBoard.getWidth()];

        //ensure that the view is not null to allow adding to the display
        if (getView() != null) {
            //get the layout responsible for holding the button rows
            LinearLayout outer = (LinearLayout) getView().findViewById(R.id.game_board_layout);
            //remove all the views from this layout
            outer.removeAllViews();
            //set the weight to allow for the buttons to show correctly on any display size
            outer.setWeightSum(myBoard.getHeight());

            //Set the player labels
            TextView playerOne = (TextView) getView().findViewById(R.id.player_one_label);
            TextView playerTwo = (TextView) getView().findViewById(R.id.player_two_label);

            playerOne.setText(myBoard.getPlayers()[0]);
            playerTwo.setText(myBoard.getPlayers()[1]);

            //ensure the player scores are correct
            updatePlayerScore();

            //for the number of rows, create a new linear layout and add butttons to this row
            for (int i = 0; i < myBoard.getHeight(); i++) {
                LinearLayout inner = new LinearLayout(getActivity());
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                p.weight = 1;
                inner.setLayoutParams(p);
                inner.setWeightSum(myBoard.getWidth());

                //add buttons to this row that represent the different cells in the game
                for (int j = 0; j < myBoard.getWidth(); j++) {
                    inner.addView(createCell(i, j));
                }
                outer.addView(inner);
            }
        }

        //display if game is over
        if (myBoard.isGameOver()) {
            gameOverDisplay();
        }
    }

    /**
     * Creates the Cells for the minesweeper flag game. Each Button represents an individual cell
     * which will trigger the correct cell on the board.
     *
     * @param theRow The row this cell will be located
     * @param theColumn The column this cell will be located
     * @return The button representing this cell
     */
    private Button createCell(final int theRow, final int theColumn) {
        //create a new button based on the current activity
        final Button b = new Button(getActivity());

        //set layout and display attributes of the button
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.weight = 1;
        b.setLayoutParams(params);
        b.setMaxWidth(b.getHeight());

        //set on click listener to trigger a game movement
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get the current logged in user
                SharedPreferences pref = getActivity().getSharedPreferences(getString(R.string.SHARED_PREFS),
                        Context.MODE_PRIVATE);
                String user = pref.getString(getString(R.string.USERNAME), getString(R.string.player_one_label));
                //check to make sure that the game is not over and it is currently this user's turn
                if (myBoard.getCurrentPlayer().equals(user) && !myBoard.isGameOver()) {
                    //select the clicked cell within the board
                    myBoard.selectCell(theRow, theColumn);

                    //attempt to save the game in the off-site database
                    try {
                        String s = SAVE_GAME_URL + "?id=" + myBoardID + "&board="
                                + myBoard.getCompressed().trim();

                        new UpdateWebTask().execute(s);
                    } catch (IOException e) {
                        Toast.makeText(getActivity(), "Error saving game!", Toast.LENGTH_SHORT).show();
                    }
                } else if (myBoard.isGameOver()) { //game is over so display information
                    gameOverDisplay();
                } else { //Not this player's turn, let them know
                    Toast.makeText(getActivity(), "It is currently the other player's turn",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        myCells[theRow][theColumn] = b;

        checkCell(theRow, theColumn);
        return b;
    }

    /**
     * Method which displays the information when a game has been finished.
     */
    private void gameOverDisplay() {
        //create a bundle and pass necessary game details
        Bundle b = new Bundle();
        b.putSerializable(getString(R.string.board), myBoard);
        DialogFragment gameOverDialog = new FinishedFragment();
        gameOverDialog.setArguments(b);
        gameOverDialog.show(getActivity().getSupportFragmentManager(), "Game Over");
    }

    /**
     * Method for checking each cell of a board to see if it is selected. This is primarily only
     * used for games that had been loaded. Without a call to this method, the buttons on the view
     * will not be shown as selected to the user.
     *
     * @param theRow The cell's row.
     * @param theColumn The cell's column.
     */
    private void checkCell(int theRow, int theColumn) {
        //Check if cell is selected
        if (myBoard.getCell(theRow, theColumn).isSelected()) {
            //If so, either set its icon or its text
            if (myBoard.getCell(theRow, theColumn).isMine()) {
                myCells[theRow][theColumn].setBackgroundResource(R.mipmap.ic_bomb);
            } else {
                myCells[theRow][theColumn].setText(myBoard.getCell(theRow, theColumn).toString());
            }

            //select and disable the button
            myCells[theRow][theColumn].setSelected(true);
            myCells[theRow][theColumn].setEnabled(false);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param observable The object being observed.
     * @param data The data being passed, if applicable.
     */
    @Override
    public void update(Observable observable, Object data) {
        //A cell was selected in the Board. Select given button, update it's text, and
        //disable to avoid it being clicked again.
        if(data instanceof Cell) {
            Cell c = (Cell) data;
            int row = c.getLocation()[0];
            int column = c.getLocation()[1];
            String text = myBoard.getCell(row, column).toString();
            if (myBoard.getCell(row, column).isMine()) {
                myCells[row][column].setBackgroundResource(R.mipmap.ic_bomb);
            } else {
                myCells[row][column].setText(text);
            }
            myCells[row][column].setSelected(true);

            myCells[row][column].setEnabled(false);
        } else if(data == BoardEvents.MINE_FOUND) { //A mine was found, update mines found label
            //update player mines found count
            if (getView() != null) {
                updatePlayerScore();
            }
            updateMinesFound();
        } else if(data == BoardEvents.GAME_OVER) {
            new UpdateWebTask().execute(GAME_FINISHED_URL + myBoardID);
            gameOverDisplay();
        }
    }

    /**
     * Helper method which updates the views for the player scores.
     */
    private void updatePlayerScore() {
        if (getView() != null) {
            TextView playerOneMines = (TextView) getView().findViewById(R.id.player_one_mine_label);
            TextView playerTwoMines = (TextView) getView().findViewById(R.id.player_two_mine_label);

            playerOneMines.setText(String.valueOf(myBoard.getMinesForPlayer(0)));
            playerTwoMines.setText(String.valueOf(myBoard.getMinesForPlayer(1)));
        }
    }

    /**
     * Method for updating the mines found label.
     */
    private void updateMinesFound() {
        if (getView() != null) {
            //Get the textview for mines left and update its value
            TextView mines = (TextView) getView().findViewById(R.id.mines_left_label);
            mines.setText(String.valueOf(myBoard.getMinesLeft()));
            //if mines left is less than 5, set color to red
            if (myBoard.getMinesLeft() <= 5) {
                mines.setTextColor(Color.RED);
            }
        }
    }

    /**
     * A custom AsyncTask class that is responsible for retrieving minesweeper flag boards from the
     * web service. Class requires the use of .execute(String) method where the String parameter
     * is the URL required to access the board info.
     */
    private class RefreshGameWebTask extends AsyncTask<String, Void, String> {

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

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            // Parse JSON
            try {
                JSONObject o = new JSONObject(s);

                //retrieve the blob of text that is stored in the board key.
                String boardBlob = o.getString("board");
                //take off the first and last index characters of the blob (these correspond to
                //'[' and ']'
                boardBlob = boardBlob.substring(1, boardBlob.length() - 1);
                //split on ',' since blob is organized as a comma separated array represenation
                String[] strArray = boardBlob.split(",");
                //create a byte array that will hold the Board in binary form
                byte[] bytes = new byte[strArray.length];

                //go through each string of the strArray, convert the string to a byte and add
                //to byte array
                for (int j = 0; j < strArray.length; j++) {
                    bytes[j] = Byte.decode(strArray[j]);
                }

                Board tempBoard = null;

                //decompress the Board blob
                try {
                    ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                    ObjectInputStream objIn = new ObjectInputStream(in);

                    tempBoard = (Board) objIn.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }

                //ensure that the retrieved board is not null
                if (tempBoard != null) {
                    //dismiss the progress dialog in the case of the board not being loaded already
                    if (myBoard == null) {
                        myProgressDialog.dismiss();
                    }
                    myBoard = tempBoard;

                    //construct views
                    constructBoardView();
                    updatePlayerScore();
                    updateMinesFound();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
