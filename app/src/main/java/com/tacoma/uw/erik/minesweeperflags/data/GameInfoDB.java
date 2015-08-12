package com.tacoma.uw.erik.minesweeperflags.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.tacoma.uw.erik.minesweeperflags.model.GameInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for the temporary storage of game information as retrieved from the SQLite database.
 *
 * @author Erik Tedder
 */
public class GameInfoDB {

    /** The version of the database. */
    private static final int DB_VERSION = 1;

    /** The name of the database to be stored. */
    private static final String DB_NAME = "GameInfo.db";

    /** The primary table being used within the database. */
    private static final String GAME_TABLE = "Game";

    /** A reference to the SQLiteDatabase object. */
    private final SQLiteDatabase mySQLiteDatabase;

    /**
     * Constructor of this database tool based on the given context.
     *
     * @param context The context for this database.
     */
    public GameInfoDB(Context context) {
        GameInfoDBHelper myGameInfoDBHelper = new GameInfoDBHelper(context, DB_NAME, null, DB_VERSION);
        mySQLiteDatabase = myGameInfoDBHelper.getWritableDatabase();
    }

    /**
     * Method which inserts a game into the database in the case of a user not currently having
     * a game present in the database.
     *
     * @return Boolean as far as if this game was successfully inserted.
     */
    public boolean insertGame(final int boardID, final String playerOne,
                              final String playerTwo, final int finished) {
        //create values to be placed in database
        ContentValues contentValues = new ContentValues();
        contentValues.put("boardID", boardID);
        contentValues.put("playerOne", playerOne);
        contentValues.put("playerTwo", playerTwo);
        contentValues.put("finished", finished);

        long rowId = mySQLiteDatabase.insert("Game", null, contentValues);
        return rowId != -1;
    }

    /**
     * Method for returning the game information for a specified user.
     *
     * @param player The player currently logged in needing their games from the sqlite database.
     * @return Returns a GameInfo object which contains all the information for the various games.
     */
    public List<GameInfo> getGames(final String player) {
        //The values we are wanting from the database
        String[] columns = {
                "boardID", "playerOne", "playerTwo", "finished"
        };

        //The where clause
        String where = "playerOne=? OR playerTwo=?";

        //the where arguments
        String[] whereArgs = {
            player, player
        };

        //Create and execute the database query
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

        //create a list for the different GameInfos
        List<GameInfo> returnList = new ArrayList<>();

        //Iterate through all the returned values
        for (int i=0; i<c.getCount(); i++) {
            //retrieve the ID, players, and game status
            int id = c.getInt(0);
            String playerOne = c.getString(1);
            String playerTwo = c.getString(2);
            int finished = c.getInt(3);
            GameInfo temp = new GameInfo(id, playerOne, playerTwo, finished);

            Log.d("DATABASE", temp.toString());

            //add to return list and increment to next query result
            returnList.add(temp);
            c.moveToNext();
        }

        return returnList;
    }

    /**
     * Method for wiping the database to allow for maintaining fresh game information in the system.
     */
    public void wipeDB() {
        mySQLiteDatabase.execSQL("DELETE FROM Game");
    }

    /**
     * Method for closing the connection to the database.
     */
    public void closeDB() {
        mySQLiteDatabase.close();
    }

}

/**
 * Helper method for the GameInfoDB class. Extends SQLiteOpenHelper and manages the creation and
 * tear down of the database.
 *
 * @author Erik Tedder
 */
class GameInfoDBHelper extends SQLiteOpenHelper {

    /** SQL create method for constructing the necessary database. */
    private static final String CREATE_GAME_SQL =
            "CREATE TABLE IF NOT EXISTS Game (boardID INTEGER PRIMARY KEY, playerOne TEXT, " +
                    "playerTwo TEXT, finished INTEGER)";

    /** SQL command for dropping the game database table. */
    private static final String DROP_GAME_SQL =
            "DROP TABLE IF EXISTS Game";

    /**
     * Constructor of a new helper object.
     *
     * @param context The context to be used.
     * @param name The name of the database.
     * @param factory The SQLiteDatabase factory type.
     * @param version The database version being used.
     */
    public GameInfoDBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    /** {@inheritDoc} */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_GAME_SQL);
    }

    /** {@inheritDoc} */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(DROP_GAME_SQL);
        onCreate(sqLiteDatabase);
    }
}
