package com.tacoma.uw.erik.minesweeperflags.model;

/**
 * Simple model class to hold the values of a Board and mines found of a given Minesweeper Flags
 * game. Used primarily for inputting and retreiving information from the SQLite database.
 *
 * Created by Erik on 7/30/2015.
 */
public class GameInfo {

    private Board myBoard;
    //private String myMinesFound;

    public GameInfo() {
        //nada
    }

    public GameInfo (Board board) {
        myBoard = board;
        //myMinesFound = minesFound;
    }

//    public String getMyMinesFound() {
//        return myMinesFound;
//    }

    public Board getMyBoard() {
        return myBoard;
    }

    public void setMyBoard(Board myBoard) {
        this.myBoard = myBoard;
    }

//    public void setMyMinesFound(String myMinesFound) {
//        this.myMinesFound = myMinesFound;
//    }
}
