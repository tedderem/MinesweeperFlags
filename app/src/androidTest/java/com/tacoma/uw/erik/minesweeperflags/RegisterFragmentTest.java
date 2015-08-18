package com.tacoma.uw.erik.minesweeperflags;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import java.util.Random;

/**
 * Test class of the register fragment.
 */
public class RegisterFragmentTest  extends ActivityInstrumentationTestCase2<MainActivity> {

    /**
     * The solo for testing.
     */
    private Solo solo;

    /**
     * Default constructor.
     */
    public RegisterFragmentTest() {
        super(MainActivity.class);
    }

    /**
     * {@inheritDoc}
     *
     * Additionally makes sure to log out the user and open a new register dialog.
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());

        //make sure no users are logged in currently
        if (solo.searchText("Your Games")) {
            solo.clickOnMenuItem("Logout");
        }
        solo.clickOnButton("Create a new Account");
    }

    /**
     * Ensure that the application is closed after each test.
     * @throws Exception The exception being thrown.
     */
    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    /**
     * Test for when a duplicate name is entered.
     */
    public void testDuplicateName() {
        //ensure dialog opens
        if (solo.waitForDialogToOpen()) {
            solo.enterText(0, "erik");
            solo.enterText(1, "password1");
            solo.enterText(2, "password1");

            solo.clickOnButton("Create Account");
        }
        //ensure generic error showing
        solo.searchText("Error");
    }

    /**
     * Test for mismatched passwords.
     */
    public void testPasswordMismatch() {
        //ensure dialog opens
        if (solo.waitForDialogToOpen()) {
            solo.enterText(0, "testingAccount");
            solo.enterText(1, "password1");
            solo.enterText(2, "password2");

            solo.clickOnButton("Create Account");
        }
        //ensure generic error showing
        solo.searchText("Error");
    }

    /**
     * Test that short passwords are rejected.
     */
    public void testShortPassword() {
        //ensure the dialog opens and attempt short password
        if (solo.waitForDialogToOpen()) {
            solo.enterText(0, "testingAccount");
            solo.enterText(1, "pw");
            solo.enterText(2, "pw");

            solo.clickOnButton("Create Account");
        }
        //ensure generic error showing
        solo.searchText("Error");
    }

    /**
     * Test for creating a brand new account and being able to log in with it.
     */
    public void testCreateAccount() {
        int suffix;

        //ensure that the dialog opens
        if (solo.waitForDialogToOpen()) {
            //get random integer to add to end of account
            suffix = new Random().nextInt(1000);
            solo.enterText(0, "testingAccount" + suffix);
            solo.enterText(1, "password");
            solo.enterText(2, "password");
            //click create button
            solo.clickOnButton("Create Account");

            //ensure dialog closes and attempt to log in with info
            if (solo.waitForDialogToClose()) {
                solo.enterText(0, "testingAccount" + suffix);
                solo.enterText(1, "password");
                solo.clickOnButton("Login");
            }
        }

        assertTrue(solo.searchText("Your Games"));


    }
}
