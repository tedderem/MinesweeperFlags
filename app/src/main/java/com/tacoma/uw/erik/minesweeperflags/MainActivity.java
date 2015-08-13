package com.tacoma.uw.erik.minesweeperflags;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;

/**
 * The Main activity for the Minesweeper Flags game. This class will maintain all the fragments
 * as well as manage showing of games and displaying of chat rooms.
 *
 * @author Erik Tedder
 */
public class MainActivity extends FragmentActivity
        implements  MenuFragment.GameSelectedListener, GameFragment.ChatOpenedListener {

    /** The shared preferences to allow for retrieving stored data. */
    private SharedPreferences myPreferences;

    /** {@inheritDoc} */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(findViewById(R.id.fragment_container) != null) {

            if (savedInstanceState != null) {
                return;
            }

            myPreferences = getSharedPreferences(getString(R.string.SHARED_PREFS), Context.MODE_PRIVATE);
            boolean loggedIn = myPreferences.getBoolean(getString(R.string.LOGGEDIN), false);

            Fragment fragment;

            //check if user is currently logged in
            if (!loggedIn) { //not logged in, goto launch fragment
                fragment = new LaunchFragment();
            } else { //logged in, goto menu fragment
                fragment = new MenuFragment();
            }

            FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0 ){
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (myPreferences == null) {
            myPreferences = getSharedPreferences(getString(R.string.SHARED_PREFS), Context.MODE_PRIVATE);
        }

        if (id == R.id.action_goto_menu) {
            if (myPreferences.getBoolean(getString(R.string.LOGGEDIN), false)) {
                MenuFragment fragment = new MenuFragment();
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit();
            }
        } else {
            //check if user is logged in currently
            if (myPreferences.getBoolean(getString(R.string.LOGGEDIN), false)) {
                SharedPreferences.Editor prefEditor = myPreferences.edit();
                prefEditor.putBoolean(getString(R.string.LOGGEDIN), false);
                prefEditor.putString(getString(R.string.USERNAME), "");
                prefEditor.putLong(getString(R.string.LASTREFRESH), 0);
                prefEditor.commit();

                LaunchFragment fragment = new LaunchFragment();
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Helper method which loads a passed fragment and board id to launch the fragment and
     * set the fragment to be accessible through the back button.
     *
     * @param fragment The fragment to be loaded.
     * @param board The board identifier that the fragment requires.
     */
    private void loadFragment(final Fragment fragment, final int board) {
        Bundle args = new Bundle();
        args.putInt(getString(R.string.board_bundle), board);

        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /** {@inheritDoc} */
    @Override
    public void onGameLoaded(Integer board) {
        //create a new game fragment and replace the current fragment
        Fragment fragment = new GameFragment();

        loadFragment(fragment, board);
    }

    /** {@inheritDoc} */
    @Override
    public void onChatOpened(Integer board) {
        Fragment fragment = new ChatFragment();

        loadFragment(fragment, board);
    }
}
