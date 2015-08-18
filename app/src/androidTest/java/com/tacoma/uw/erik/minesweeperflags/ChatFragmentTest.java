package com.tacoma.uw.erik.minesweeperflags;

import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

import java.util.Random;

/**
 * Test for a chat fragment to ensure that users can see each other's text.
 */
public class ChatFragmentTest extends ActivityInstrumentationTestCase2<MainActivity> {

    /**
     * The solo for testing purposes.
     */
    private Solo solo;

    /**
     * Default constructor.
     */
    public ChatFragmentTest() {
        super(MainActivity.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());
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
     * Single test for ensuring that two users can communicate with each other.
     */
    public void testChatSentAndReceived() {
        //construct messages to send that are unique
        String messageOne, messageTwo;
        messageOne = String.valueOf(new Random().nextInt(10000));

        do {
            messageTwo = String.valueOf(new Random().nextInt(10000));
        } while (messageOne.equals(messageTwo));

        //ensure logged out currently
        if(solo.searchText("Your Games")) {
            solo.clickOnMenuItem("Logout");
        }

        //login as one user
        solo.enterText(0, "erik");
        solo.enterText(1, "password");
        solo.clickOnButton("Login");

        //sleep 5 seconds to ensure that the games load
        solo.sleep(5000);

        //click on the game to chat on
        solo.clickOnText("Game #46");

        //sleep to ensure game loads
        solo.sleep(1000);

        //click on chat button and submit message
        solo.clickOnButton("Chat");
        solo.enterText(0, messageOne);
        solo.clickOnButton("SEND");

        //logout and repeat process with player two
        solo.clickOnMenuItem("Logout");

        solo.enterText(0, "erik2");
        solo.enterText(1, "password");
        solo.clickOnButton("Login");

        //sleep 5 seconds to ensure that the games load
        solo.sleep(5000);

        //click on the game to chat on
        solo.clickOnText("Game #46");

        //sleep to ensure game loads
        solo.sleep(1000);

        //click on chat button and submit message
        solo.clickOnButton("Chat");
        solo.enterText(0, messageTwo);
        solo.clickOnButton("SEND");

        //make sure both messages are shown
        assertTrue(solo.searchText(messageOne) && solo.searchText(messageTwo));
    }
}
