package com.cse5473.securegame;

import it.unipr.ce.dsg.s2p.peer.PeerDescriptor;

import java.lang.ref.WeakReference;

import com.cse5473.securegame.GameView.State;
import com.cse5473.securegame.msg.VerificationMessage;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String LOG_TAG = "main";

	private ArrayAdapter<String> adapter;
	private TextView message;
	private ListView listView;

	/** Messenger for communicating with service. */
	Messenger mService = null;
	private boolean serviceIsBound;
	private ServiceConnection serviceConnection;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		message = (TextView) findViewById(R.id.message);
		listView = (ListView) findViewById(R.id.listView);

		startService(new Intent(this, PeerService.class));

		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Log.d(LOG_TAG, "clicked "
						+ ((TextView) view).getText().toString());
				Bundle data = new Bundle();
				data.putString(PeerService.DATA_TARGET, ((TextView) view)
						.getText().toString());
				Message m = Message.obtain(null, PeerService.MSG_SEND_PING);
				m.setData(data);
				try {
					mService.send(m);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		// Set up user name on first run
		if (!SettingsActivity.isUsernameSet(this)) {
			SettingsActivity.promptForUsernameThenSetupPeer(this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(LOG_TAG, "onResume");
		doBindService();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(LOG_TAG, "onPause");
		doUnbindService();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(LOG_TAG, "onDestroy");
	}

	public void userNameSet() {
		try {
			mService.send(Message.obtain(null, PeerService.MSG_USERNAME_SET));
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		case R.id.menu_refresh:
			if (serviceIsBound) {
				message.setVisibility(View.VISIBLE);
				try {
					mService.send(Message.obtain(null, PeerService.MSG_REFRESH));
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return true;
		case R.id.menu_disconnect:
			doUnbindService();
			stopService(new Intent(this, PeerService.class));
			finish();
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Handler of incoming messages from service.
	 */
	static class IncomingHandler extends Handler {
		private final WeakReference<MainActivity> ma_ref;

		public IncomingHandler(MainActivity mainActivity) {
			ma_ref = new WeakReference<MainActivity>(mainActivity);
		}

		@Override
		public void handleMessage(Message msg) {
			MainActivity ma = ma_ref.get();
			switch (msg.what) {
			case PeerService.MSG_INIT_FAILED:
				Log.e(LOG_TAG, "init failed!");
				ma.message.setVisibility(View.INVISIBLE);
				break;
			case PeerService.MSG_REC_PEER_LIST:
				ma.message.setVisibility(View.INVISIBLE);
				ma.adapter = new ArrayAdapter<String>(ma,
						android.R.layout.simple_list_item_1, msg.getData()
								.getStringArrayList(PeerService.DATA_PEER_LIST));
				ma.listView.setAdapter(ma.adapter);
				Log.i(LOG_TAG, "peer list ready");
				break;
			case PeerService.MSG_REC_PING:
				ma.message.setVisibility(View.INVISIBLE);
				ma.adapter = new ArrayAdapter<String>(ma,
						android.R.layout.simple_list_item_1, msg.getData()
								.getStringArrayList(PeerService.DATA_PEER_LIST));
				ma.listView.setAdapter(ma.adapter);
				Log.i(LOG_TAG, "received ping, peer list updated");
				ma.promptStartGame((PeerDescriptor) msg.obj);
				break;
			case PeerService.MSG_REC_ACK:
				ma.startGame((PeerDescriptor) msg.obj);
				break;
			case PeerService.MSG_REC_VERIFICATION:
				// TODO: handle receiving verification and starting game as
				// player 2
				ma.verifyKeyAndJoinGame(
						msg.getData().getByteArray(PeerService.DATA_BYTES), msg
								.getData().getString(PeerService.DATA_SENDER));
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
		bindService(new Intent(MainActivity.this, PeerService.class),
				serviceConnection, Context.BIND_AUTO_CREATE);
		serviceIsBound = true;
	}

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

	private void promptStartGame(final PeerDescriptor pd) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.app_name);
		alert.setMessage(getString(R.string.start_game) + " " + pd.getName()
				+ "?");
		alert.setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Bundle data = new Bundle(1);
						data.putString(PeerService.DATA_TARGET,
								pd.getContactAddress());
						Message m = Message.obtain(null,
								PeerService.MSG_SEND_ACK);
						m.setData(data);
						try {
							mService.send(m);
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
		alert.setNegativeButton(android.R.string.no, null);
		alert.show();
	}

	private void startGame(PeerDescriptor pd) {
		Intent i = new Intent(this, GameActivity.class);
		i.putExtra(GameActivity.EXTRA_START_PLAYER, State.PLAYER1.getValue());
		i.putExtra(GameActivity.EXTRA_OTHER_ADDRESS, pd.getContactAddress());
		startActivity(i);
	}

	private void verifyKeyAndJoinGame(final byte[] bytes,
			final String otherAddress) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.app_name);
		alert.setMessage(getString(R.string.enter_password));
		final EditText input = new EditText(this);
		alert.setView(input);
		alert.setCancelable(false);
		alert.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String pass = input.getText().toString();
						// Send verification message
						Bundle data = new Bundle(2);
						data.putString(PeerService.DATA_TARGET, otherAddress);
						data.putString(PeerService.DATA_KEY, pass);
						Message m = Message.obtain(null,
								PeerService.MSG_SEND_VERIFICATION);
						m.setData(data);
						try {
							mService.send(m);
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						// Check password
						if (VerificationMessage.isValidKey(bytes, pass)) {
							Log.d(LOG_TAG, "verified pass");
							// Start game
							Intent i = new Intent(MainActivity.this,
									GameActivity.class);
							i.putExtra(GameActivity.EXTRA_START_PLAYER,
									State.PLAYER2.getValue());
							i.putExtra(GameActivity.EXTRA_OTHER_ADDRESS,
									otherAddress);
							i.putExtra(GameActivity.EXTRA_PASS, pass);
							startActivity(i);
						} else {
							Log.d(LOG_TAG, "wrong pass");
							Toast.makeText(MainActivity.this,
									R.string.wrong_pass, Toast.LENGTH_LONG)
									.show();
						}
					}
				});
		alert.show();
	}

}
