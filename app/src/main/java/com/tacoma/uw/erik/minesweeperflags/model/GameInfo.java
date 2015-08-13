package com.tacoma.uw.erik.minesweeperflags.model;

/**
 * Simple model class to hold the values of a Board and mines found of a given Minesweeper Flags
 * game. Used primarily for inputting and retreiving information from the SQLite database.
 *
 * @author Erik Tedder
 */
public class GameInfo {

    /** The id for the given board. */
    private final int myBoardId;

    /** The name of player one. */
    private final String myPlayerOne;

    /** The name of player two. */
    private final String myPlayerTwo;

    /** Whether or not this game is finished (0 = no, 1 = yes). */
    private final int myFinished;

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Game #" + myBoardId +
                "\nPlayerOne : " + myPlayerOne +
                "  PlayerTwo : " + myPlayerTwo +
                "\nFinished : " + (myFinished == 1);
    }

    /**
     * Constructor of a new GameInfo object based on information about a Minesweeper Flags game.
     *
     * @param boardId The id number of this board.
     * @param playerOne The name of player one.
     * @param playerTwo The name of player two.
     * @param finished Whether or not this game is finished.
     */
    public GameInfo (final int boardId, final String playerOne, final String playerTwo,
                     final int finished) {
        myBoardId = boardId;
        myPlayerOne = playerOne;
        myPlayerTwo = playerTwo;
        myFinished = finished;
    }

    /**
     * Method which returns whether or not this game is finished.
     *
     * @return Boolean depicting this game's finished state.
     */
    public int getFinished() {
        return myFinished;
    }

    /**
     * Method which returns the id number of the board in question.
     *
     * @return Integer value representing the boards id number.
     */
    public int getBoardId() {
        return myBoardId;
    }

    /**
     * Getter method for the name of player one.
     *
     * @return The name of player one.
     */
    public String getPlayerOne() {
        return myPlayerOne;
    }

    /**
     * Getter method for the name of player two.
     *
     * @return The name of player two.
     */
    public String getPlayerTwo() {
        return myPlayerTwo;
    }
}
