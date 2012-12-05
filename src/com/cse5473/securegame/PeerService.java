package com.cse5473.securegame;

import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import com.cse5473.securegame.GameView.State;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class PeerService extends Service {

	private static final int DEFAULT_PORT = 12345;
	private static final String LOG_TAG = "PeerService";

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private int NOTIFICATION = R.string.peer_service_connected;
	private NotificationManager mNM;
	/** Keeps track of all current registered clients. */
	ArrayList<Messenger> mClients = new ArrayList<Messenger>();

	/**
	 * Command to the service to register a client, receiving callbacks from the
	 * service. The Message's replyTo field must be a Messenger of the client
	 * where callbacks should be sent.
	 */
	static final int MSG_REGISTER_CLIENT = 1;

	/**
	 * Command to the service to unregister a client, to stop receiving
	 * callbacks from the service. The Message's replyTo field must be a
	 * Messenger of the client as previously given with MSG_REGISTER_CLIENT.
	 */
	static final int MSG_UNREGISTER_CLIENT = 2;
	static final int MSG_INIT_FAILED = 3;
	static final int MSG_REC_PEER_LIST = 4;
	static final int MSG_REC_PING = 5;
	static final int MSG_REC_ACK = 6;

	/**
	 * Command to send a ping. Message's data must contain a bundle with address
	 * in key DATA_TARGET
	 */
	static final int MSG_SEND_PING = 7;
	static final int MSG_REFRESH = 8;

	/**
	 * Command to start peer server if username was not initially set
	 */
	static final int MSG_USERNAME_SET = 9;

	/**
	 * Command to get peer list, refreshing if needed. Responds with
	 * MSG_REC_PEER_LIST
	 */
	static final int MSG_GET_PEER_LIST = 10;

	/**
	 * Command to send ack. Message's data must contain a bundle with address in
	 * key DATA_TARGET
	 */
	static final int MSG_SEND_ACK = 11;

	/**
	 * Command to send verification. Message's data must contain a bundle with
	 * encryption key in key DATA_KEY
	 */
	static final int MSG_SEND_VERIFICATION = 12;
	static final int MSG_REC_VERIFICATION = 13;

	static final int MSG_SEND_MOVE = 14;
	static final int MSG_REC_MOVE = 15;

	static final String DATA_PEER_LIST = "peer_list";
	static final String DATA_TARGET = "target";
	static final String DATA_KEY = "enc_key";
	static final String DATA_BYTES = "bytes";
	static final String DATA_SENDER = "sender";
	static final String DATA_INDEX = "index";
	static final String DATA_STATE = "state";

	private PeerManager peer;
	private WifiLock mWifiLock;

	private boolean bootStrapComplete = false;
	private boolean initFailed = false;

	/**
	 * Handler of incoming messages from clients.
	 */
	static class IncomingHandler extends Handler {
		private final WeakReference<PeerService> ps_ref;

		public IncomingHandler(PeerService peerService) {
			ps_ref = new WeakReference<PeerService>(peerService);
		}

		@Override
		public void handleMessage(Message msg) {
			PeerService ps = ps_ref.get();
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				ps.mClients.add(msg.replyTo);
				// Send peer list if we have it (not first start)
				if (ps.bootStrapComplete) {
					ArrayList<String> peerList = ps.peer.getPeerList();
					if (peerList != null && !peerList.isEmpty()) {
						Log.d(LOG_TAG, "Sending old peer list");
						Message m = Message.obtain(null, MSG_REC_PEER_LIST);
						Bundle data = new Bundle(1);
						data.putStringArrayList(DATA_PEER_LIST, peerList);
						m.setData(data);
						for (int i = ps.mClients.size() - 1; i >= 0; i--) {
							try {
								ps.mClients.get(i).send(m);
							} catch (RemoteException e) {
								Log.d(LOG_TAG,
										"Remote exception sending peer list received");
								// The client is dead. Remove it from the list;
								// we are going through the list from back to
								// front
								// so this is safe to do inside the loop.
								ps.mClients.remove(i);
							}
						}
					}
				}
				break;
			case MSG_UNREGISTER_CLIENT:
				ps.mClients.remove(msg.replyTo);
				break;
			case MSG_SEND_PING:
				ps.peer.pingPeer(msg.getData().getString(DATA_TARGET));
				break;
			case MSG_REFRESH:
				if (ps.bootStrapComplete) {
					ps.bootStrapComplete = false;
					ps.peer.doBootstrap();
				}
				break;
			case MSG_USERNAME_SET:
				ps.startPeer();
				break;
			case MSG_GET_PEER_LIST:
				ArrayList<String> peerList = ps.peer.getPeerList();
				if (peerList != null && !peerList.isEmpty()) {
					Log.d(LOG_TAG, "Using old peer list");
					Message m = Message.obtain(null, MSG_REC_PEER_LIST);
					Bundle data = new Bundle(1);
					data.putStringArrayList(DATA_PEER_LIST, peerList);
					m.setData(data);
					for (int i = ps.mClients.size() - 1; i >= 0; i--) {
						try {
							ps.mClients.get(i).send(m);
						} catch (RemoteException e) {
							Log.d(LOG_TAG,
									"Remote exception sending peer list received");
							// The client is dead. Remove it from the list;
							// we are going through the list from back to front
							// so this is safe to do inside the loop.
							ps.mClients.remove(i);
						}
					}
				} else {
					// Refresh
					Log.d(LOG_TAG, "Refreshing peer list");
					if (ps.bootStrapComplete) {
						ps.bootStrapComplete = false;
						ps.peer.doBootstrap();
					} else if (ps.initFailed) {
						ps.initFailed = false;
						ps.peer.halt();
						ps.setupPeer();
					}
				}
				break;
			case MSG_SEND_ACK:
				ps.peer.ackPeer(msg.getData().getString(DATA_TARGET));
				break;
			case MSG_SEND_VERIFICATION:
				ps.peer.sendVerification(msg.getData().getString(DATA_TARGET),
						msg.getData().getString(DATA_KEY));
				break;
			case MSG_SEND_MOVE:
				ps.peer.sendMovePeer(msg.getData().getString(DATA_TARGET),
						State.fromInt(msg.getData().getInt(DATA_STATE)), msg
								.getData().getInt(DATA_INDEX), msg.getData()
								.getString(DATA_KEY));
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

	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	@Override
	public void onCreate() {
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// Keep wifi awake
		mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
				.createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
		mWifiLock.acquire();
		// Start peer or wait for user name
		if (SettingsActivity.isUsernameSet(this)) {
			startPeer();
		}
	}

	private void startPeer() {
		setupPeer();
		// Display a notification about us starting. We put an icon in the
		// status bar.
		showNotification();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("PeerService", "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		// Cancel the persistent notification.
		mNM.cancel(NOTIFICATION);
		// Stop peer
		peer.halt();
		// Release wifi
		if (mWifiLock.isHeld())
			mWifiLock.release();
		// Tell the user we stopped.
		Toast.makeText(this, R.string.peer_service_disconnected,
				Toast.LENGTH_SHORT).show();
	}

	/**
	 * Show a notification while this service is running.
	 */
	@SuppressWarnings("deprecation")
	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.peer_service_connected);
		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.ic_launcher,
				text, System.currentTimeMillis());
		// The PendingIntent to launch our activity if the user selects this
		// notification
		Intent i = new Intent(this, MainActivity.class);
		i.setAction("android.intent.action.MAIN");
		i.addCategory("android.intent.category.LAUNCHER");
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i,
				Notification.FLAG_ONGOING_EVENT);
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.app_name), text,
				contentIntent);
		// Send the notification.
		mNM.notify(NOTIFICATION, notification);
	}

	/**
	 * Handler of incoming messages from peer manager.
	 */
	static class PeerHandler extends Handler {
		private final WeakReference<PeerService> ps_ref;

		public PeerHandler(PeerService peerService) {
			ps_ref = new WeakReference<PeerService>(peerService);
		}

		@Override
		public void handleMessage(Message msg) {
			PeerService ps = ps_ref.get();
			switch (msg.what) {
			case PeerManager.RECEIVED_PEER_LIST: {
				Log.d(LOG_TAG, "Received peer list");
				ps.bootStrapComplete = true;
				Message m = Message.obtain(null, MSG_REC_PEER_LIST);
				Bundle data = new Bundle(1);
				data.putStringArrayList(DATA_PEER_LIST, ps.peer.getPeerList());
				m.setData(data);
				for (int i = ps.mClients.size() - 1; i >= 0; i--) {
					try {
						ps.mClients.get(i).send(m);
					} catch (RemoteException e) {
						Log.d(LOG_TAG,
								"Remote exception sending peer list received");
						// The client is dead. Remove it from the list;
						// we are going through the list from back to front
						// so this is safe to do inside the loop.
						ps.mClients.remove(i);
					}
				}
				break;
			}
			case PeerManager.INIT_FAILED: {
				Log.d(LOG_TAG, "Init failed");
				ps.initFailed = true;
				for (int i = ps.mClients.size() - 1; i >= 0; i--) {
					try {
						ps.mClients.get(i).send(
								Message.obtain(null, MSG_INIT_FAILED));
					} catch (RemoteException e) {
						Log.d(LOG_TAG, "Remote exception sending init failed");
						// The client is dead. Remove it from the list;
						// we are going through the list from back to front
						// so this is safe to do inside the loop.
						ps.mClients.remove(i);
					}
				}
				break;
			}
			case PeerManager.RECEIVED_PING: {
				Log.d(LOG_TAG, "Received ping");
				// msg.obj should be PeerDescriptor
				Message m = Message.obtain(null, MSG_REC_PING, msg.obj);
				Bundle data = new Bundle(1);
				data.putStringArrayList(DATA_PEER_LIST, ps.peer.getPeerList());
				m.setData(data);
				for (int i = ps.mClients.size() - 1; i >= 0; i--) {
					try {
						ps.mClients.get(i).send(m);
					} catch (RemoteException e) {
						Log.d(LOG_TAG, "Remote exception sending ping received");
						// The client is dead. Remove it from the list;
						// we are going through the list from back to front
						// so this is safe to do inside the loop.
						ps.mClients.remove(i);
					}
				}
				break;
			}
			case PeerManager.RECEIVED_ACK: {
				Log.d(LOG_TAG, "Received ack");
				for (int i = ps.mClients.size() - 1; i >= 0; i--) {
					try {
						ps.mClients.get(i).send(
								Message.obtain(null, MSG_REC_ACK, msg.obj));
					} catch (RemoteException e) {
						Log.d(LOG_TAG, "Remote exception sending ack received");
						// The client is dead. Remove it from the list;
						// we are going through the list from back to front
						// so this is safe to do inside the loop.
						ps.mClients.remove(i);
					}
				}
				break;
			}
			case PeerManager.RECEIVED_VERIFY: {
				Log.d(LOG_TAG, "Received verify");
				Message m = Message.obtain(null, MSG_REC_VERIFICATION);
				m.setData(msg.getData());
				for (int i = ps.mClients.size() - 1; i >= 0; i--) {
					try {
						ps.mClients.get(i).send(m);
					} catch (RemoteException e) {
						Log.d(LOG_TAG,
								"Remote exception sending verify received");
						// The client is dead. Remove it from the list;
						// we are going through the list from back to front
						// so this is safe to do inside the loop.
						ps.mClients.remove(i);
					}
				}
				break;
			}
			case PeerManager.RECEIVED_MOVE: {
				Log.d(LOG_TAG, "Received move");
				Message m = Message.obtain(null, MSG_REC_MOVE);
				m.setData(msg.getData());
				for (int i = ps.mClients.size() - 1; i >= 0; i--) {
					try {
						ps.mClients.get(i).send(m);
					} catch (RemoteException e) {
						Log.d(LOG_TAG, "Remote exception sending move received");
						// The client is dead. Remove it from the list;
						// we are going through the list from back to front
						// so this is safe to do inside the loop.
						ps.mClients.remove(i);
					}
				}
				break;
			}
			default:
				super.handleMessage(msg);
			}
		}
	}

	private void setupPeer() {
		String username = SettingsActivity.getUsername(this);
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			peer = new PeerManager(new String(md.digest(username.getBytes())),
					username, DEFAULT_PORT, new PeerHandler(this), this);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

}
