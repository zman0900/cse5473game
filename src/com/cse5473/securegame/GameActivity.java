/*
 * Modified for use in SecureGame
 * 
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cse5473.securegame;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cse5473.securegame.GameView.ICellListener;
import com.cse5473.securegame.GameView.State;
import com.cse5473.securegame.msg.MoveMessage;
import com.cse5473.securegame.msg.VerificationMessage;

/**
 * This is the main activity for any initiated game. This activity gets called
 * by the main activity after the verification messages have been sent along
 * with the Ping and acknowledgement messages have also been sent and recieved
 * accordingly.
 */
public class GameActivity extends Activity {

	/**
	 * The player represetned as player one.
	 */
	public static final String EXTRA_START_PLAYER = "com.cse5473.securegame.GameActivity.EXTRA_START_PLAYER";

	/**
	 * The player represented as player two.
	 */
	public static final String EXTRA_OTHER_ADDRESS = "com.cse5473.securegame.GameActivity.EXTRA_OTHER_ADDRESS";

	/**
	 * ?
	 */
	public static final String EXTRA_PASS = "com.cse5473.securegame.GameActivity.EXTRA_PASS";

	/**
	 * The tag for logging so we know the source of errors.
	 */
	private static final String LOG_TAG = "GameActivity";

	/**
	 * The game view associated with the activity.
	 */
	private GameView mGameView;

	/**
	 * The info panel for telling the user it's their turn or displaying a win
	 * message.
	 */
	private TextView mInfoView;

	/**
	 * The button used for move submission.
	 */
	private Button mButtonNext;

	/**
	 * The private shared key.
	 */
	private static String pass;

	/**
	 * True iff this playere initiated the game.
	 */
	private static boolean isPlayer1;

	/**
	 * The messenger that allows for communication with the peerservices.
	 */
	Messenger mService = null;

	/**
	 * True iff the service is already bound.
	 */
	private boolean serviceIsBound;

	/**
	 * The service connection that allows the GameActivity to communicate with
	 * the PeerManager defined in the MainActivity
	 */
	private ServiceConnection serviceConnection;

	/**
	 * Called when the activity it created. It initialized all variables and
	 * prompts for the person to enter a password for the verification message.
	 */
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		/*
		 * IMPORTANT: all resource IDs from this library will eventually be
		 * merged with the resources from the main project that will use the
		 * library.
		 * 
		 * If the main project and the libraries define the same resource IDs,
		 * the application project will always have priority and override
		 * library resources and IDs defined in multiple libraries are resolved
		 * based on the libraries priority defined in the main project.
		 * 
		 * An intentional consequence is that the main project can override some
		 * resources from the library. (TODO insert example).
		 * 
		 * To avoid potential conflicts, it is suggested to add a prefix to the
		 * library resource names.
		 */
		setContentView(R.layout.lib_game);

		mGameView = (GameView) findViewById(R.id.game_view);
		mInfoView = (TextView) findViewById(R.id.info_turn);
		mButtonNext = (Button) findViewById(R.id.next_turn);

		mGameView.setFocusable(true);
		mGameView.setFocusableInTouchMode(true);
		mGameView.setCellListener(new MyCellListener());

		mButtonNext.setOnClickListener(new MyButtonListener());

		mGameView.setEnabled(false);
		mButtonNext.setEnabled(false);
		// If player 1, started game so prompt to make up password
		isPlayer1 = (State.fromInt(getIntent().getIntExtra(EXTRA_START_PLAYER,
				1)) == State.PLAYER1);
		if (isPlayer1 && pass == null) {
			promptCreatePassword();
		}
	}

	/**
	 * Handles the resuming of the activity should it ever be paused.
	 * Unfortunately this is known to be extremely glitchy when playing two
	 * player games,. It handles resuming just fine, however, if the other
	 * player for some reason deicdes to pause the game as well it gets a tad
	 * screwy.
	 */
	@Override
	protected void onResume() {
		super.onResume();

		isPlayer1 = (State.fromInt(getIntent().getIntExtra(EXTRA_START_PLAYER,
				1)) == State.PLAYER1);
		if (!isPlayer1) {
			pass = getIntent().getStringExtra(EXTRA_PASS);
		}

		doBindService();

		State player = mGameView.getCurrentPlayer();
		if (player == State.UNKNOWN) {
			player = State.fromInt(getIntent().getIntExtra(EXTRA_START_PLAYER,
					1));
		}
		mGameView.setCurrentPlayer(player);
		checkGameFinished(player);
		if (player == State.WIN) {
			setWinState(mGameView.getWinner());
		}
	}

	/**
	 * Handles the unbinding of the service when the game is paused or the user
	 * exits to the main menu for any reason.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		doUnbindService();
	}

	/**
	 * Handler of incoming messages from service.
	 */
	static class IncomingHandler extends Handler {
		private final WeakReference<GameActivity> ga_ref;

		public IncomingHandler(GameActivity gameActivity) {
			ga_ref = new WeakReference<GameActivity>(gameActivity);
		}

		@Override
		public void handleMessage(Message msg) {
			GameActivity ga = ga_ref.get();
			switch (msg.what) {
			case PeerService.MSG_REC_VERIFICATION:
				ga.verifyAndStartAsPlayer1(msg.getData().getByteArray(
						PeerService.DATA_BYTES));
				break;
			case PeerService.MSG_REC_MOVE:
				ga.moveMessageReceived(msg.getData().getByteArray(
						PeerService.DATA_BYTES));
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler(this));

	/**
	 * Binds the service. This is the call that allows communication between the
	 * peerservice, peermanager and the gameactivity. If it wasn't for the
	 * service being bound we'd have trouble with the consistency of the
	 * communications.
	 */
	private void doBindService() {
		serviceConnection = new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name) {
				// This is called when the connection with the service has been
				// unexpectedly disconnected -- that is, its process crashed.
				mService = null;
			}

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				// This is called when the connection with the service has been
				// established, giving us the service object we can use to
				// interact with the service. We are communicating with our
				// service through an IDL interface, so get a client-side
				// representation of that from the raw service object.
				mService = new Messenger(service);
				// We want to monitor the service for as long as we are
				// connected to it.
				try {
					Message msg = Message.obtain(null,
							PeerService.MSG_REGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// In this case the service has crashed before we could even
					// do anything with it; we can count on soon being
					// disconnected (and then reconnected if it can be
					// restarted)
					// so there is no need to do anything here.
				}
			}
		};
		bindService(new Intent(GameActivity.this, PeerService.class),
				serviceConnection, Context.BIND_AUTO_CREATE);
		serviceIsBound = true;
	}

	/**
	 * Unbinds the bound peerservice between two peers.
	 */
	private void doUnbindService() {
		if (serviceIsBound) {
			if (mService != null) {
				try {
					Message msg = Message.obtain(null,
							PeerService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = mMessenger;
					mService.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service
					// has crashed.
				}
			}
			// Detach our existing connection.
			unbindService(serviceConnection);
			serviceIsBound = false;
			serviceConnection = null;
		}
	}

	/**
	 * Prompts the user to create a password, the user then creates a password
	 * and verification message is sent to the bound peer.
	 */
	private void promptCreatePassword() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.app_name);
		alert.setMessage(getString(R.string.create_password));
		final EditText input = new EditText(this);
		alert.setView(input);
		alert.setCancelable(false);
		alert.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						pass = input.getText().toString();
						// Send verification message
						Bundle data = new Bundle(2);
						data.putString(PeerService.DATA_TARGET, getIntent()
								.getStringExtra(EXTRA_OTHER_ADDRESS));
						data.putString(PeerService.DATA_KEY, pass);
						Message m = Message.obtain(null,
								PeerService.MSG_SEND_VERIFICATION);
						m.setData(data);
						try {
							mService.send(m);
							mInfoView.setText(R.string.waiting_for_other);
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
		alert.show();
	}

	/**
	 * Used to verify that the other peer has responded with a valid key (This
	 * means that the key was successfully shared!)
	 */
	private void verifyAndStartAsPlayer1(byte[] bytes) {
		if (VerificationMessage.isValidKey(bytes, pass)) {
			Log.d(LOG_TAG, "verified pass");
			mGameView.setEnabled(true);
			mInfoView.setText(R.string.your_turn);
		} else {
			Log.d(LOG_TAG, "wrong pass");
			Toast.makeText(this, R.string.wrong_pass, Toast.LENGTH_LONG).show();
			finish();
		}
	}

	/**
	 * This updates the game state upon reciving a message stating that the
	 * other player has made a move.
	 */
	private void moveMessageReceived(byte[] bytes) {
		Integer index = MoveMessage.getDecryptedIndex(bytes, pass);
		if (index != null) {
			mGameView.setCell(index, isPlayer1 ? State.PLAYER2 : State.PLAYER1);
			if (!checkGameFinished(mGameView.getCurrentPlayer())) {
				mGameView.setEnabled(true);
				mInfoView.setText(R.string.your_turn);
			}
		} else {
			Log.d(LOG_TAG, "wrong pass");
			Toast.makeText(this, R.string.wrong_pass, Toast.LENGTH_LONG).show();
			finish();
		}
	}

	/**
	 * The cell listener that is to be bound to the GameView for communication
	 * between the activity and the graphics.
	 */
	private class MyCellListener implements ICellListener {
		public void onCellSelected() {
			int cell = mGameView.getSelection();
			Log.d(LOG_TAG, "Clicked cell " + cell);
			mButtonNext.setEnabled(cell >= 0);
		}
	}

	/**
	 * This handles the case that the button is pressed, therefore it basically
	 * sends the move messages, disables the player from touching the game board
	 * then switches turns.
	 */
	private class MyButtonListener implements OnClickListener {
		public void onClick(View v) {
			State player = mGameView.getCurrentPlayer();

			if (player == State.WIN) {
				GameActivity.this.finish();

			} else {
				int cell = mGameView.getSelection();
				if (cell >= 0) {
					mGameView.stopBlink();
					mGameView.setCell(cell, player);

					Bundle data = new Bundle(4);
					data.putString(PeerService.DATA_TARGET, getIntent()
							.getStringExtra(EXTRA_OTHER_ADDRESS));
					data.putInt(PeerService.DATA_STATE, player.getValue());
					data.putInt(PeerService.DATA_INDEX, cell);
					data.putString(PeerService.DATA_KEY, pass);
					Message m = Message.obtain(null, PeerService.MSG_SEND_MOVE);
					m.setData(data);
					try {
						mService.send(m);
						mGameView.setEnabled(false);
						mButtonNext.setEnabled(false);
						mInfoView.setText(R.string.waiting_for_other);
						checkGameFinished(mGameView.getCurrentPlayer());
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * This is the method to check whether or not a player has won the game. It
	 * only returns true if it detects 3 chars in a row col or diagonal.
	 */
	public boolean checkGameFinished(State player) {
		State[] data = mGameView.getData();
		boolean full = true;

		int col = -1;
		int row = -1;
		int diag = -1;

		// check rows
		for (int j = 0, k = 0; j < 3; j++, k += 3) {
			if (data[k] != State.EMPTY && data[k] == data[k + 1]
					&& data[k] == data[k + 2]) {
				row = j;
			}
			if (full
					&& (data[k] == State.EMPTY || data[k + 1] == State.EMPTY || data[k + 2] == State.EMPTY)) {
				full = false;
			}
		}

		// check columns
		for (int i = 0; i < 3; i++) {
			if (data[i] != State.EMPTY && data[i] == data[i + 3]
					&& data[i] == data[i + 6]) {
				col = i;
			}
		}

		// check diagonals
		if (data[0] != State.EMPTY && data[0] == data[1 + 3]
				&& data[0] == data[2 + 6]) {
			diag = 0;
		} else if (data[2] != State.EMPTY && data[2] == data[1 + 3]
				&& data[2] == data[0 + 6]) {
			diag = 1;
		}

		if (col != -1 || row != -1 || diag != -1) {
			setFinished(player, col, row, diag);
			return true;
		}

		// if we get here, there's no winner but the board is full.
		if (full) {
			setFinished(State.EMPTY, -1, -1, -1);
			return true;
		}
		return false;
	}

	/**
	 * Sets the game as finished. The integers are all -1 unless the game WAS
	 * indeed finished, then the row or col must be 0-2 or the diagonal must be
	 * 0-1. This allows for the GameView to paint the winnning lines.
	 */
	private void setFinished(State player, int col, int row, int diagonal) {

		mGameView.setCurrentPlayer(State.WIN);
		mGameView.setWinner(player);
		mGameView.setEnabled(false);
		mGameView.setFinished(col, row, diagonal);

		setWinState(player);
	}

	/**
	 * Sets the player who won in the GameView. This allows for the gameview to
	 * display messages like "YOU WIN!".
	 */
	private void setWinState(State player) {
		mButtonNext.setEnabled(true);
		mButtonNext.setText("Back");

		String text;

		if (player == State.EMPTY) {
			text = getString(R.string.tie);
		} else if (player == State.PLAYER1 && isPlayer1) {
			text = getString(R.string.you_win);
		} else {
			text = getString(R.string.you_lose);
		}
		mInfoView.setText(text);
	}
}
