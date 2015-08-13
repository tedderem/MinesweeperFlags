package com.tacoma.uw.erik.minesweeperflags;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tacoma.uw.erik.minesweeperflags.model.Board;


/**
 * A dialog fragment class which shows the end game information to the user.
 *
 * @author Erik Tedder
 */
public class FinishedFragment extends DialogFragment {

    /**
     * A reference to the view for this fragment.
     */
    private View myView;

    /**
     * The details for the game that finished.
     */
    private String myDetails;

    /**
     * Default, no-argument constructor for this dialog fragment.
     */
    public FinishedFragment() {
        // Required empty public constructor
    }

    /**
     * {@inheritDoc}
     *
     * Creates the Dialog for this class and initializes the on click listeners for the different
     * buttons.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        myView = inflater.inflate(R.layout.fragment_finished, null);

        if (myView != null) {
            builder.setView(myView)
                    .setPositiveButton("Game Menu", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //dismiss the dialog
                            getDialog().dismiss();

                            //clear backstack from fragment manager (avoids issues)
                            getActivity().getSupportFragmentManager().popBackStack(null,
                                    FragmentManager.POP_BACK_STACK_INCLUSIVE);

                            //launch the menu
                            ListFragment menu = new MenuFragment();
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, menu)
                                    .commit();
                        }
                    })
                    .setNegativeButton("Return to Game", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getDialog().dismiss();
                        }
                    });

            constructDetailInfo();

            //set the detail view
            TextView detailView = (TextView) myView.findViewById(R.id.score_detail);
            detailView.setText(myDetails);

            ImageButton share = (ImageButton) myView.findViewById(R.id.shareButton);
            share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareContent();
                }
            });

        }

        builder.setTitle("Game Finished");

        return builder.create();
    }

    /**
     * Method which will construct the details of the game.
     */
    private void constructDetailInfo() {
        //get the arguments for this dialog and construct the share string details
        Bundle b = getArguments();
        Board board = (Board) b.getSerializable(getString(R.string.board));

        //retrieve the logged in user
        SharedPreferences pref = getActivity()
                .getSharedPreferences(getString(R.string.SHARED_PREFS),
                        Context.MODE_PRIVATE);
        String user = pref.getString(getString(R.string.USERNAME),
                getString(R.string.player_one_label));

        if (board != null) {
            //retrieve players and their scores
            String playerOne = board.getPlayers()[0];
            String playerTwo = board.getPlayers()[1];
            int playerOneScore = board.getMinesForPlayer(0);
            int playerTwoScore = board.getMinesForPlayer(1);

            //set the overview text view of the two players scores
            TextView score = (TextView) myView.findViewById(R.id.score_overview);
            score.setText(Html.fromHtml("<font color=\"red\">" + playerOneScore + "</font> - "
                    + "<font color=\"red\">" + playerTwoScore));

            //Set the detail information based on player scores
            if (playerOneScore == playerTwoScore) {
                myDetails = "I tied ";
                myDetails += playerOne.equals(user) ? playerTwo : playerOne;
            } else if (playerOneScore > playerTwoScore) {
                myDetails = playerOne.equals(user) ? "I beat " + playerTwo : playerOne + " beat me";
                myDetails += " with a score of " + playerOneScore + " to " + playerTwoScore;
            } else {
                myDetails = playerTwo.equals(user) ? "I beat " + playerOne : playerTwo + " beat me";
                myDetails += " with a score of " + playerTwoScore + " to " + playerOneScore;
            }
            myDetails += " in a game of Minesweeper Flags!";
        }
    }

    /**
     * Method for constructing the content to be shared and preparing the intent.
     */
    private void shareContent() {
        if (myView != null) {
            //prepare the intent and start activity
            Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "I just finished a Minesweeper Flags game!");
            intent.putExtra(android.content.Intent.EXTRA_TEXT, myDetails);

            startActivity(Intent.createChooser(intent, "Share via"));
        }
    }
}