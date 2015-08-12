package com.tacoma.uw.erik.minesweeperflags.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.tacoma.uw.erik.minesweeperflags.model.Board;
import com.tacoma.uw.erik.minesweeperflags.model.GameInfo;

import java.util.ArrayList;
import java.util.List;

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
        myGameInfoDBHelper = new GameInfoDBHelper(context, DB_NAME, null, DB_VERSION);
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
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] columns = {
                "boardID", "playerOne", "playerTwo", "finished"
        };

        String where = "playerOne=? OR playerTwo=?";

        String[] whereArgs = {
            player, player
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

        //create a list for the different game infos
        List<GameInfo> returnList = new ArrayList<>();

        //Iterate through all the returned values
        for (int i=0; i<c.getCount(); i++) {
            int id = c.getInt(0);
            String playerOne = c.getString(1);
            String playerTwo = c.getString(2);
            int finished = c.getInt(3);
            GameInfo temp = new GameInfo(id, playerOne, playerTwo, finished);

            Log.d("DATABASE", temp.toString());

            returnList.add(temp);
            c.moveToNext();
        }

        return returnList;
    }

    public void wipeDB() {
        mySQLiteDatabase.execSQL("DELETE FROM Game");
    }

    public void closeDB() {
        mySQLiteDatabase.close();
    }

}

class GameInfoDBHelper extends SQLiteOpenHelper {

    private static final String CREATE_USER_SQL =
            "CREATE TABLE IF NOT EXISTS Game (boardID INTEGER PRIMARY KEY, playerOne TEXT, " +
                    "playerTwo TEXT, finished INTEGER)";

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
