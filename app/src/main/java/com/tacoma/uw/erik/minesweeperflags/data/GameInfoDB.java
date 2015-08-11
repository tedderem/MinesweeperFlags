package com.tacoma.uw.erik.minesweeperflags.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.tacoma.uw.erik.minesweeperflags.model.Board;
import com.tacoma.uw.erik.minesweeperflags.model.GameInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Class for the temporary storage of game information as retrieved from the SQLite database.
 */
public class GameInfoDB {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "GameInfo.db";
    private static final String GAME_TABLE = "Game";

    private final SQLiteDatabase mySQLiteDatabase;
    private final GameInfoDBHelper myGameInfoDBHelper;

    public GameInfoDB(Context context) {
        myGameInfoDBHelper = new GameInfoDBHelper(
                context, DB_NAME, null, DB_VERSION);
        mySQLiteDatabase = myGameInfoDBHelper.getWritableDatabase();
    }

    /**
     * Method which inserts a game into the database in the case of a user not currently having
     * a game present in the database.
     *
     * @param username The user for this game.
     * @param board The board for this user.
     * @param minesFound The number of mines found in the board.
     * @return Boolean as far as if this game was successfully inserted.
     */
    public boolean insertGame(String username, Board board, String minesFound) {
        byte[] data = compressBoard(board);

        //create values to be placed in database
        ContentValues contentValues = new ContentValues();
        contentValues.put("username", username);
        contentValues.put("board", data);
        contentValues.put("mines_found", minesFound);

        long rowId = mySQLiteDatabase.insert("Game", null, contentValues);
        return rowId != -1;
    }

    /**
     * Method for creating a byte array (blob) of the given Board to allow for adding to database.
     *
     * @param board Board object to be compressed into a byte array.
     * @return The byte array representing this Board.
     */
    private byte[] compressBoard(Board board) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objOut = new ObjectOutputStream(out);
            objOut.writeObject(board);
            objOut.close();
        } catch (IOException e) {
            Log.e("serialization", e.getMessage());
        }
        return out.toByteArray();
    }

    /**
     * Method for returning the game information for a specified user.
     *
     * @param username The user needing game information retrieved.
     * @return A GameInfo object which contains the board and mines found for this user. Is null
     * when the user has no saved game in the database.
     */
    public GameInfo getGameInfo(String username) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] columns = {
                "board", "mines_found"
        };

        String where = "username = ?";
        String[] whereArgs = {
                username
        };

        Cursor c = mySQLiteDatabase.query(
                GAME_TABLE,  // The table to query
                columns,                               // The columns to return
                where,                                // The columns for the WHERE clause
                whereArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );
        c.moveToFirst();

        //set GameInfo to null to allow for denoting when a user has no saved game
        GameInfo gameInfo = null;

        //ensure the cursor returned values from the query
        if (c.getCount() > 0) {
            byte[] boardBlob = c.getBlob(0);

            Board board = null;

            //decompress the Board blob
            ByteArrayInputStream in = new ByteArrayInputStream(boardBlob);
            try {
                ObjectInputStream objIn = new ObjectInputStream(in);
                board = (Board) objIn.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            String minesFound = c.getString(1);

            gameInfo = new GameInfo(board);
        }

        return gameInfo;
    }

    /**
     * Update a database entry for a given user.
     *
     * @param username The user to have their game info updated.
     * @param board The board to be placed in the entry.
     * @param minesFound The number of mines found.
     */
    public void updateGame(String username, Board board, String minesFound) {
        ContentValues values = new ContentValues();
        values.put("board", compressBoard(board));
        values.put("mines_found", minesFound);
        mySQLiteDatabase.update("Game", values
                , "username=?",new String[] {username} );
    }

    public void closeDB() {
        mySQLiteDatabase.close();
    }

}

class GameInfoDBHelper extends SQLiteOpenHelper {

    private static final String CREATE_USER_SQL =
            "CREATE TABLE IF NOT EXISTS Game (username TEXT PRIMARY KEY, board TEXT, mines_found TEXT)";

    private static final String DROP_USER_SQL =
            "DROP TABLE IF EXISTS Game";

    public GameInfoDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_USER_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(DROP_USER_SQL);
        onCreate(sqLiteDatabase);
    }
}
