package com.tacoma.uw.erik.minesweeperflags;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

/**
 * Test class for the login fragment functionality.
 */
public class LoginFragmentTest extends ActivityInstrumentationTestCase2<MainActivity> {

    /**
     * The solo for testing purposes.
     */
    private Solo solo;

    /**
     * Default constructor.
     */
    public LoginFragmentTest() {
        super(MainActivity.class);
    }

    /**
     * {@inheritDoc}
     *
     * Additionally, logs out the user to ensure that the tests are not running
     * on a currently logged in account.
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());

        //make sure no users are logged in currently
        if (solo.searchText("Your Games")) {
            solo.clickOnMenuItem("Logout");
        }
    }

    /**
     * {@inheritDoc}
     *
     * Shuts down the application.
     */
    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

    /**
     * Test for invalid login information.
     */
    public void testInvalidLogin() {
        //enter wrong info, testing account is erik - password
        solo.enterText(0, "erik");
        solo.enterText(1, "wrong");
        solo.clickOnButton("Login");

        assertTrue(solo.waitForText("Invalid Username or Password"));
    }

    /**
     * Test for valid login.
     */
    public void testLogin() {
        solo.enterText(0, "erik");
        solo.enterText(1, "password");

        solo.clickOnButton("Login");

        assertTrue(solo.searchText("Your Games"));
    }

    /**
     * Test for ensuring persistent logged in .
     */
    public void testPersistentLogin() {
        //login with correct details
        solo.enterText(0, "erik");
        solo.enterText(1, "password");

        solo.clickOnButton("Login");

        //sleep momentarily and reopen app
        solo.sleep(1000);
        solo.finishOpenedActivities();
        this.launchActivity("com.tacoma.uw.erik.minesweeperflags", MainActivity.class, null);
        solo.sleep(1000);

        assertTrue(solo.searchText("Your Games"));
    }

    /**
     * Test for the register button functionality.
     */
    public void testRegisterButton() {
        solo.clickOnButton("Create a new Account");
        assertTrue(solo.waitForDialogToOpen());

    }
}