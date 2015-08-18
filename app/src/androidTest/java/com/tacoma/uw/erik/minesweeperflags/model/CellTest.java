package com.tacoma.uw.erik.minesweeperflags.model;

import junit.framework.TestCase;

/**
 * Test for the Cell model class.
 */
public class CellTest extends TestCase {

    private static final int MAX_NEIGHBORS = 8;

    /**
     * Test case for ensuring the cell is in the correct position.
     */
    public void testConstructor() {
        Cell cell = new Cell(5, 6);

        assertTrue("True assertion failed", cell.getLocation()[0] == 5
                && cell.getLocation()[1] == 6);
    }

    /**
     * Test to ensure the cell increments correctly.
     */
    public void testIncrement() {
        Cell cell = new Cell(5, 6);

        //since not incremented, cell should return blank
        assertTrue("True assertion failed", cell.isBlank());

        for (int i = 0; i < MAX_NEIGHBORS; i++) {
            cell.incrementCount();
            assertTrue("True assertion failed", cell.getCount() == i + 1);
        }
    }

    /**
     * Test to ensure the cell correctly identifies as a mine when set.
     */
    public void testIsMine() {
        Cell cell = new Cell(4, 6);

        cell.setAsMine();

        assertTrue("True assertion failed", cell.isMine());
    }

    /**
     * Test selection method.
     */
    public void testSelected() {
        Cell cell = new Cell(5, 6);

        //should be false to begin
        assertFalse("False assertion failed", cell.isSelected());

        //set as selected and test
        cell.setSelected();
        assertTrue("True assertion failed", cell.isSelected());
    }

    /**
     * Test for toString method.
     */
    public void testToString() {
        final String mineMark = "X";

        Cell cell = new Cell(5, 6);

        //check blank toString
        assertEquals("Equality of blank string failed", " ", cell.toString());

        //check counted toString
        for (int i = 0; i < MAX_NEIGHBORS; i++) {
            cell.incrementCount();
            assertEquals("Equality of numbered string failed", String.valueOf(i + 1), cell.toString());
        }

        //check mine toString
        cell.setAsMine();
        assertEquals("Mine string equality failed", mineMark, cell.toString());
    }
}
