package com.tacoma.uw.erik.minesweeperflags.model;

/**
 * Simple model class to hold the values of a Board and mines found of a given Minesweeper Flags
 * game. Used primarily for inputting and retreiving information from the SQLite database.
 *
 * Created by Erik on 7/30/2015.
 */
public class GameInfo {

    private int myBoardId;

    private String myPlayerOne;

    private String myPlayerTwo;

    private int myFinished;

    @Override
    public String toString() {
        return "GameInfo{" +
                "myBoardId=" + myBoardId +
                ", myPlayerOne='" + myPlayerOne + '\'' +
                ", myPlayerTwo='" + myPlayerTwo + '\'' +
                ", myFinished=" + myFinished +
                '}';
    }

    public GameInfo (final int boardId, final String playerOne, final String playerTwo,
                     final int finished) {
        myBoardId = boardId;
        myPlayerOne = playerOne;
        myPlayerTwo = playerTwo;
        myFinished = finished;
    }

    public int getFinished() {
        return myFinished;
    }

    public int getBoardId() {
        return myBoardId;
    }

    public String getPlayerOne() {
        return myPlayerOne;
    }

    public String getPlayerTwo() {
        return myPlayerTwo;
    }
}
