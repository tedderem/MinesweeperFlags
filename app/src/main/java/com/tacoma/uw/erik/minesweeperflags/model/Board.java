package com.tacoma.uw.erik.minesweeperflags.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Random;

import com.tacoma.uw.erik.minesweeperflags.control.BoardEvents;

/**
 * Class which represents the board of the Minesweeper game. Makes up rows and columns of Cells
 * which contain a number representation of how many nearby mines there are, or whether the
 * cell itself is a mine.
 *
 * @author Erik Tedder
 * @version 7/27/2015
 */
public class Board extends Observable implements Serializable {

    /** Seralized Unique ID to allow for this board to be compressed and returned. */
    private static final long serialVersionUID = 18412371239123l;

    /** Default board width. */
    private static final int DEFAULT_WIDTH = 10;

    /** Default board height. */
    private static final int DEFAULT_HEIGHT = 10;

    /** Default number of mines on the board. */
    private static final int DEFAULT_NUMBER_OF_MINES = 30;

    /** Two-dimensional array of Cells to represent the game's board. */
    private Cell[][] myBoard;

    /** Boolean representing the game being over. */
    private boolean myGameOver;

    /** The total number of mines in the current board. */
    private int myMineTotal;

    /** The height of this board. */
    private int myHeight;

    /** The width of this board. */
    private int myWidth;

    /**	Counter for the number of mines found within the game. Used to determine winning. */
    private int myMinesFound;

    /** String array which holds the players of this game. */
    private String[] myPlayers;

    /** Integer array which holds the number of mines found by the players. */
    private int[] myPlayersMinesFound;

    /** The current player for this game. */
    private int myCurrentPlayer;

    /**
     * Default, no-argument constructor of a new board with the default height, width, and
     * number of mines.
     */
    public Board(final String thePlayerOne, final String thePlayerTwo) {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_NUMBER_OF_MINES, thePlayerOne, thePlayerTwo);
    }

    /**
     * Constructor of a new Board based on the passed width, height, and total number of mines.
     *
     * @param theWidth The desired width of the board.
     * @param theHeight The desired height of the board.
     * @param theTotalMines The total number of mines on the board.
     */
    public Board(final int theWidth, final int theHeight, final int theTotalMines,
                 final String thePlayerOne, final String thePlayerTwo) {
        myWidth = theWidth;
        myHeight = theHeight;
        myMineTotal = theTotalMines;
        myMinesFound = 0;
        myGameOver = false;
        myPlayers = new String[2];
        myPlayersMinesFound = new int[2];
        myPlayers[0] = thePlayerOne;
        myPlayers[1] = thePlayerTwo;
        myPlayersMinesFound[0] = 0;
        myPlayersMinesFound[1] = 0;
        myCurrentPlayer = 0;

        myBoard = new Cell[myHeight][myWidth];

        for (int i = 0; i < theHeight; i++) {
            for (int j = 0; j < theWidth; j++) {
                myBoard[i][j] = new Cell(i, j);
            }
        }

        populateBoard();
    }

    /**
     * Getter method which returns the current player for this Board.
     *
     * @return The player whose turn it is for this board.
     */
    public String getCurrentPlayer() {
        return myPlayers[myCurrentPlayer];
    }

    /**
     * Getter method which returns a String array with the names of the players for this Board.
     *
     * @return A String array of length 2 which contains the players of this game.
     */
    public String[] getPlayers() {
        return myPlayers;
    }

    /**
     * Getter method which returns the mines found for a given player. The method requires the input
     * of the player number (either 0 or 1).
     *
     * @param playerNum The number of the player where playerOne = 0 and playerTwo = 1.
     * @return The number of mines the requested player has found.
     */
    public int getMinesForPlayer(final int playerNum) {
        return myPlayersMinesFound[playerNum];
    }

    /**
     * Getter method which returns the current state of the game.
     *
     * @return Boolean of the game being over.
     */
    public boolean isGameOver() {
        return myGameOver;
    }

    /**
     * Method to set the current game to be over.
     *
     * @param theGameOver The desired state of the game.
     */
    public void setGameOver(final boolean theGameOver) {
        myGameOver = theGameOver;
    }

    /**
     * Getter method of the total number of mines within the board.
     *
     * @return int of the number of mines within the board.
     */
    public int getMineNumber() {
        return myMineTotal;
    }

    /**
     * Setter method for the number of mines to be in this board.
     *
     * @param theMineNumber The desired number of mines within the board.
     */
    public void setMineNumber(final int theMineNumber) {
        myMineTotal = theMineNumber;
    }

    /**
     * Getter method for the height of the board.
     *
     * @return int of the board's height.
     */
    public int getHeight() {
        return myHeight;
    }

    /**
     * Setter method for the height of the board.
     *
     * @param theHeight The desired height of the board.
     */
    public void setHeight(final int theHeight) {
        myHeight = theHeight;
    }

    /**
     * Getter method which returns the width of the board.
     *
     * @return int of the board width.
     */
    public int getWidth() {
        return myWidth;
    }

    /**
     * Setter method which sets the Width of the current Board.
     *
     * @param theWidth The desired with of the board.
     */
    public void setWidth(final int theWidth) {
        myWidth = theWidth;
    }

    /**
     * Getter method for the number of mines left in the current game.
     *
     * @return the number of mines found.
     */
    public int getMinesLeft() {
        return myMineTotal - myMinesFound;
    }

    /**
     * Method for selecting a cell on the board, making the respective cell being set to
     * selected. If the given cell has a count of 0, then selection is expanded in all
     * directions until no more adjacent empty cells are left unselected.
     *
     * @param theRow The row to the cell.
     * @param theColumn The column of the cell.
     */
    public void selectCell(final int theRow, final int theColumn) {
        if (!myGameOver) {
            myBoard[theRow][theColumn].setSelected();
            setChanged();
            notifyObservers(getCell(theRow, theColumn));

            if (myBoard[theRow][theColumn].isBlank()) {
                blankSelected(theRow, theColumn);
            }

            if (myBoard[theRow][theColumn].isMine()) {
                //increment mines found and increase players mine count
                myMinesFound++;
                myPlayersMinesFound[myCurrentPlayer]++;

                setChanged();
                notifyObservers(BoardEvents.MINE_FOUND);

                //current player has found the majority of mines
                if (myPlayersMinesFound[myCurrentPlayer] > myMineTotal/2) {
                    myGameOver = true;
                    setChanged();
                    notifyObservers(BoardEvents.GAME_OVER);
                }
            } else {
                //no mine found, change current player
                myCurrentPlayer = myCurrentPlayer ^ 1;
            }
        }
    }

    /**
     * Method for retrieving a Cell item based on a given row and column value
     *
     * @param theRow The row of the cell.
     * @param theColumn The column of the cell.
     * @return The Cell at this location.
     */
    public Cell getCell(final int theRow, final int theColumn) {
        return myBoard[theRow][theColumn];
    }

    /**
     * Method which compresses the current Board object into a binary representation as a String
     * for data storage.
     *
     * @return The string representing this board object in its entirety.
     * @throws IOException
     */
    public String getCompressed() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream objOut = new ObjectOutputStream(out);
        objOut.writeObject(this);
        objOut.close();

        byte[] returnBytes = out.toByteArray();
        //Log.d("Board", Arrays.toString(returnBytes));
        out.close();

      //  Log.d("BOARD", new String(returnBytes));
        StringBuilder s = new StringBuilder();
        String b = Arrays.toString(returnBytes);
        for (String c : b.split(", ")) {
            s.append(c);
            s.append(",");
        }
        s.deleteCharAt(s.length() - 1);

       // Log.d("BOARD", s.toString());

        return s.toString();
    }

    /**
     * Method which populates the current Board with mines.
     */
    private void populateBoard() {
        Random r = new Random();
        int minesToPlace = myMineTotal;

        while (minesToPlace > 0) {
            int column = r.nextInt(myWidth);
            int row = r.nextInt(myHeight);

            //place a mine aslong as the current slot isn't already a mine.
            if (!myBoard[row][column].isMine()) {
                myBoard[row][column].setAsMine();
                updateNeighbors(row, column);
                minesToPlace--;
            }
        }
    }

    /**
     * Method that will expand empty spaces of the board when a user selects a cell that is
     * empty.
     *
     * @param theRow The selected empty row value.
     * @param theColumn The selected empty column value.
     */
    private void blankSelected(final int theRow, final int theColumn) {
        ArrayList<Cell> neighbors = (ArrayList<Cell>)getNeighbors(theRow, theColumn);

        for (Cell c : neighbors) {
            if (!c.isSelected())
                blankSelectCell((int)c.getLocation()[0], (int)c.getLocation()[1]);
        }
    }

    /**
     * Method for when a blank has been selected and needs to be expanded out.
     *
     * @param theRow The selected row value.
     * @param theColumn The selected column value.
     */
    private void blankSelectCell(final int theRow, final int theColumn) {
        myBoard[theRow][theColumn].setSelected();
        setChanged();
        notifyObservers(getCell(theRow, theColumn));

        if (myBoard[theRow][theColumn].isBlank()) {
            blankSelected(theRow, theColumn);
        }
    }

    /**
     * Method that finds all the available neighbors of the current cell (denoted by the row
     * and the column of the cell).
     *
     * @param theRow The row of the cell.
     * @param theColumn The column of the cell.
     * @return A list of cells.
     */
    private List<Cell> getNeighbors(final int theRow, final int theColumn) {
        ArrayList<Cell> neighbors = new ArrayList<Cell>();

        if (theRow == 0) {
            neighbors.add(myBoard[theRow + 1][theColumn]);

            if (theColumn == 0) {
                neighbors.add(myBoard[theRow + 1][theColumn + 1]);
                neighbors.add(myBoard[theRow][theColumn + 1]);
            } else if (theColumn == (myWidth - 1)) {
                neighbors.add(myBoard[theRow][theColumn - 1]);
                neighbors.add(myBoard[theRow + 1][theColumn - 1]);
            } else {
                neighbors.add(myBoard[theRow][theColumn - 1]);
                neighbors.add(myBoard[theRow + 1][theColumn - 1]);
                neighbors.add(myBoard[theRow + 1][theColumn + 1]);
                neighbors.add(myBoard[theRow][theColumn + 1]);
            }
        } else if (theRow == (myHeight - 1)) {
            neighbors.add(myBoard[theRow - 1][theColumn]);

            if (theColumn == 0) {
                neighbors.add(myBoard[theRow - 1][theColumn + 1]);
                neighbors.add(myBoard[theRow][theColumn + 1]);
            } else if (theColumn == (myWidth - 1)) {
                neighbors.add(myBoard[theRow][theColumn - 1]);
                neighbors.add(myBoard[theRow - 1][theColumn - 1]);
            } else {
                neighbors.add(myBoard[theRow][theColumn - 1]);
                neighbors.add(myBoard[theRow - 1][theColumn - 1]);
                neighbors.add(myBoard[theRow - 1][theColumn + 1]);
                neighbors.add(myBoard[theRow][theColumn + 1]);
            }
        } else {
            neighbors.add(myBoard[theRow - 1][theColumn]);
            neighbors.add(myBoard[theRow + 1][theColumn]);

            if (theColumn == 0) {
                neighbors.add(myBoard[theRow - 1][theColumn + 1]);
                neighbors.add(myBoard[theRow][theColumn + 1]);
                neighbors.add(myBoard[theRow + 1][theColumn + 1]);
            } else if (theColumn == (myWidth - 1)) {
                neighbors.add(myBoard[theRow][theColumn - 1]);
                neighbors.add(myBoard[theRow - 1][theColumn - 1]);
                neighbors.add(myBoard[theRow + 1][theColumn - 1]);
            } else {
                neighbors.add(myBoard[theRow - 1][theColumn + 1]);
                neighbors.add(myBoard[theRow][theColumn + 1]);
                neighbors.add(myBoard[theRow + 1][theColumn + 1]);
                neighbors.add(myBoard[theRow][theColumn - 1]);
                neighbors.add(myBoard[theRow - 1][theColumn - 1]);
                neighbors.add(myBoard[theRow + 1][theColumn - 1]);
            }
        }

        return neighbors;
    }

    /**
     * Method which will updates the neighboring cells to reflect the number of mines
     * surrounding it. Method is called when initializing mine locations.
     *
     * @param theRow The row of the mine cell.
     * @param theColumn The column of the mine cell.
     */
    private void updateNeighbors(final int theRow, final int theColumn) {
        for (Cell c : getNeighbors(theRow, theColumn)) {
            c.incrementCount();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Returns a string representation of the current Minesweeper Board for debugging purposes.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("  ");
        for(int i = 0; i < myWidth; i++) {
            sb.append(i);
            sb.append(" ");
        }

        sb.append("\n");

        for(int i = 0; i < myHeight; i++) {
            sb.append(i);
            sb.append(" ");
            for (int j = 0; j < myWidth; j++) {
                sb.append(myBoard[i][j]);
                sb.append(" ");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

}
