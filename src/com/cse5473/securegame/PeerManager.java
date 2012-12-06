package com.cse5473.securegame;

import java.util.ArrayList;
import java.util.Iterator;

import com.cse5473.securegame.msg.AckMessage;
import com.cse5473.securegame.msg.JoinMessage;
import com.cse5473.securegame.msg.MoveMessage;
import com.cse5473.securegame.msg.PeerListMessage;
import com.cse5473.securegame.msg.PingMessage;
import com.cse5473.securegame.msg.VerificationMessage;

import it.unipr.ce.dsg.s2p.message.parser.BasicParser;
import it.unipr.ce.dsg.s2p.org.json.JSONArray;
import it.unipr.ce.dsg.s2p.org.json.JSONException;
import it.unipr.ce.dsg.s2p.org.json.JSONObject;
import it.unipr.ce.dsg.s2p.peer.NeighborPeerDescriptor;
import it.unipr.ce.dsg.s2p.peer.Peer;
import it.unipr.ce.dsg.s2p.peer.PeerDescriptor;
import it.unipr.ce.dsg.s2p.sip.Address;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This is the peer manager it helps manage all of the peer services generated
 * to and from the bootstrap peer. It allows for connections to and from the
 * bootstrap peer using the SIP2Peer services.
 */
public class PeerManager extends Peer {
	/**
	 * This is the logging tag for the PeerManager. It helped identify the
	 * location of multiple errors.
	 */
	private static final String LOG_TAG = "peerManager";

	/**
	 * @param SBC
	 *            The IP of the SBC service running.
	 */
	public static final String SBC = "66.172.27.236:6066";

	/**
	 * @param BOOTSTRAP
	 *            This holds to local IP of the Bootstrap peer.
	 */
	public static final String BOOTSTRAP = "bootstrap@66.172.27.236:5080";

	/**
	 * @param KEY_PREF_CONTACT_ADDRESS
	 *            Holds the string representation of the contact address
	 */
	public static final String KEY_PREF_CONTACT_ADDRESS = "contactAddress";

	/**
	 * @param RECIEVED_PEER_LIST
	 */
	public static final int RECEIVED_PEER_LIST = 1;

	/**
	 * @param INIT_FAILED
	 */
	public static final int INIT_FAILED = 2;

	/**
	 * @param RECEIVED_PING
	 */
	public static final int RECEIVED_PING = 4;

	/**
	 * @param RECEIVED_ACK
	 */
	public static final int RECEIVED_ACK = 8;

	/**
	 * @param RECEIVED_VERIFY
	 */
	public static final int RECEIVED_VERIFY = 16;

	/**
	 * @param RECEIVED_MOVE
	 */
	public static final int RECEIVED_MOVE = 32;

	/**
	 * This is the handler for the peer manager. It's the object that handles
	 * the handling of multiple peer services.
	 */
	private Handler handler;

	/**
	 * The context of the peer manager. Used to glean information about the
	 * where the peer manager is being run (in what context/activity).
	 */
	private Context context;

	/**
	 * A default constructor for a PeerManager peer.
	 * 
	 * @param key
	 *            The key to be used for authentication and connection
	 * @param peerName
	 *            the name to be used as an identifier for the peermanager.
	 * @param peerPort
	 *            The port it is to be run on.
	 * @param handler
	 *            The handler that will be tied to the peer.
	 * @param context
	 *            The context in which it is being run.
	 */
	public PeerManager(String key, String peerName, int peerPort,
			Handler handler, Context context) {
		super(null, key, peerName, peerPort);
		this.handler = handler;
		this.context = context;
		init();
	}

	/**
	 * A constructor for a PeerManager peer. Uses BasicParser in the extended
	 * peer.
	 * 
	 * @param key
	 *            The key to be used for authentication and connection
	 * @param peerName
	 *            the name to be used as an identifier for the peermanager.
	 * @param peerPort
	 *            The port it is to be run on.
	 * @param handler
	 *            The handler that will be tied to the peer.
	 * @param context
	 *            The context in which it is being run.
	 */
	public PeerManager(String key, String peerName, int peerPort,
			BasicParser parser, Handler handler, Context context) {
		super(null, key, peerName, peerPort, parser);
		this.handler = handler;
		this.context = context;
		init();
	}

	/**
	 * Initiates the private data for peer manager. Sets the basics like how
	 * long to keep the peer manager alive. Also logs possible initiation errors
	 * to LogCat.
	 */
	private void init() {
		nodeConfig.test_address_reachability = "yes";
		nodeConfig.keepalive_time = 600;
		nodeConfig.sbc = SBC;
		// checkNAT(); slim chance of not being on nat
		// check if existing contact address is still up
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String contact = sharedPref.getString(KEY_PREF_CONTACT_ADDRESS, "");
		if (!contact.equals("")) {
			Log.d(LOG_TAG, "destroying possible old contact address");
			peerDescriptor.setContactAddress(contact);
			closePublicAddress();
			SharedPreferences.Editor e = sharedPref.edit();
			e.putString(KEY_PREF_CONTACT_ADDRESS, "");
			e.commit();
		}
		requestPublicAddress();
		Log.d(LOG_TAG, "init done");
	}

	@Override
	protected void onReceivedSBCMsg(String SBCMsg) {
		super.onReceivedSBCMsg(SBCMsg);
		Log.d(LOG_TAG, "received sbc msg: " + SBCMsg);
	}

	/**
	 * Handles the case of recieving an SBC address from a peer.
	 */
	@Override
	protected void onReceivedSBCContactAddress(Address cAddress) {
		super.onReceivedSBCContactAddress(cAddress);
		Log.d(LOG_TAG, "received sbc contact address: " + cAddress);
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor e = sharedPref.edit();
		e.putString(KEY_PREF_CONTACT_ADDRESS, cAddress.getURL());
		e.commit();
		// Got address, wait for HELLO to send
	}

	/**
	 * Handles the case of a failed message delivery.
	 */
	@Override
	protected void onDeliveryMsgFailure(String arg0, Address arg1, String arg2) {
		// TODO Auto-generated method stub
		Log.d(LOG_TAG, "message delivery failure arg0: " + arg0 + " address: "
				+ arg1 + " arg2:" + arg2);
		if (arg0.equals("REQUEST_PORT")) {
			// Init failed, try again?
			Message m = new Message();
			m.what = INIT_FAILED;
			handler.sendMessage(m);
		} else if (arg0.equals("HELLO")) {
			// failed setting up sbc, try again
			Log.e(LOG_TAG, "sbc failed, retrying");
			closePublicAddress();
			requestPublicAddress();
		}
	}

	/**
	 * Handles the case of a successful message delivery.
	 */
	@Override
	protected void onDeliveryMsgSuccess(String arg0, Address arg1, String arg2) {
		// TODO Auto-generated method stub
		Log.d(LOG_TAG, "message delivery success arg0: " + arg0 + " address: "
				+ arg1 + " arg2:" + arg2);
		if (arg0.equals("HELLO")) {
			// successfully set up sbc port, do bootstrap
			Log.d(LOG_TAG, "sbc success, doing bootstrap");
			doBootstrap();
		}
	}

	/**
	 * Handles the case of a received message from a peer.
	 */
	@Override
	protected void onReceivedMsg(String peerMsg, Address sender,
			String contentType) {
		super.onReceivedMsg(peerMsg, sender, contentType);
		Log.d(LOG_TAG, "received message: " + peerMsg);
	}

	/**
	 * This is the powerhouse of our main message interpreting system. It
	 * interprets every type of message as a JSON message. This is extremely
	 * useful for parsing messages and was set forth by the underlying structure
	 * of the SIP2peer interfaces. The messages in the peers use JSON message as
	 * an underlying format to standardize most messages between peers. This
	 * just parses the separate message formats accordingly.
	 */
	@Override
	protected void onReceivedJSONMsg(JSONObject jsonMsg, Address sender) {
		try {
			JSONObject params = jsonMsg.getJSONObject("payload").getJSONObject(
					"params");
			if (jsonMsg.get("type").equals(PeerListMessage.MSG_PEER_LIST)) {
				Log.i(LOG_TAG, "received peer list");
				@SuppressWarnings("unchecked")
				Iterator<String> iter = params.keys();
				while (iter.hasNext()) {
					String key = (String) iter.next();
					JSONObject keyPeer = params.getJSONObject(key);
					PeerDescriptor neighborPeerDesc = new PeerDescriptor(
							keyPeer.get("name").toString(), keyPeer.get(
									"address").toString(), keyPeer.get("key")
									.toString());
					if (keyPeer.get("contactAddress").toString() != "null")
						neighborPeerDesc.setContactAddress(keyPeer.get(
								"contactAddress").toString());
					addNeighborPeer(neighborPeerDesc);
					// Integer size = Integer.valueOf(this.peerList.size());
				}
				// Let main know
				handler.sendMessage(Message.obtain(null, RECEIVED_PEER_LIST));
			} else if (jsonMsg.get("type").equals(PingMessage.MSG_PEER_PING)) {
				Log.i(LOG_TAG,
						"received ping message from "
								+ params.get("contactAddress"));
				PeerDescriptor neighborPeerDesc = new PeerDescriptor(params
						.get("name").toString(), params.get("address")
						.toString(), params.get("key").toString(), params.get(
						"contactAddress").toString());
				addNeighborPeer(neighborPeerDesc);
				// Notify of new peer
				handler.sendMessage(Message.obtain(null, RECEIVED_PING,
						neighborPeerDesc));
			} else if (jsonMsg.get("type").equals(AckMessage.MSG_PEER_ACK)) {
				Log.i(LOG_TAG,
						"received ack message from "
								+ params.get("contactAddress"));
				PeerDescriptor neighborPeerDesc = new PeerDescriptor(params
						.get("name").toString(), params.get("address")
						.toString(), params.get("key").toString(), params.get(
						"contactAddress").toString());
				// Notify of ack
				handler.sendMessage(Message.obtain(null, RECEIVED_ACK,
						neighborPeerDesc));
			} else if (jsonMsg.get("type").equals(
					VerificationMessage.MSG_PEER_VERIFY)) {
				Log.i(LOG_TAG,
						"received verification message from " + sender.getURL());
				JSONArray arr = (JSONArray) params
						.get(VerificationMessage.PARAM_MESSAGE);
				byte[] bytes = new byte[arr.length()];
				for (int i = 0; i < arr.length(); i++) {
					bytes[i] = (byte) arr.getInt(i);
				}
				Bundle data = new Bundle();
				data.putByteArray(PeerService.DATA_BYTES, bytes);
				data.putString(PeerService.DATA_SENDER, sender.getURL());
				Message m = Message.obtain(null, RECEIVED_VERIFY);
				m.setData(data);
				handler.sendMessage(m);
			} else if (jsonMsg.get("type").equals(MoveMessage.MSG_PEER_MOVE)) {
				Log.i(LOG_TAG, "received move message from " + sender.getURL());
				// state isn't important
				JSONArray arr = (JSONArray) params.get(MoveMessage.PARAM_INDEX);
				byte[] bytes = new byte[arr.length()];
				for (int i = 0; i < arr.length(); i++) {
					bytes[i] = (byte) arr.getInt(i);
				}
				Bundle data = new Bundle();
				data.putByteArray(PeerService.DATA_BYTES, bytes);
				data.putString(PeerService.DATA_SENDER, sender.getURL());
				Message m = Message.obtain(null, RECEIVED_MOVE);
				m.setData(data);
				handler.sendMessage(m);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This allows for the software to get the list of peers from the bootstrap
	 * server. Used for the main activity.
	 */
	public ArrayList<String> getPeerList() {
		ArrayList<String> addressList = new ArrayList<String>();
		Iterator<NeighborPeerDescriptor> iter = this.peerList.values()
				.iterator();
		NeighborPeerDescriptor npd;
		while (iter.hasNext()) {
			npd = iter.next();
			addressList.add(npd.getContactAddress());
		}
		return addressList;
	}

	/**
	 * Sends a message to the bootstrap from the given peer to initiate a
	 * connection.
	 */
	public void doBootstrap() {
		JoinMessage msg = new JoinMessage(peerDescriptor);
		send(new Address(BOOTSTRAP), msg);
	}

	/**
	 * Sends the ping message to the given address.
	 * 
	 * @param address
	 *            The address to send the ping message to.
	 */
	public void pingPeer(String address) {
		PingMessage ping = new PingMessage(peerDescriptor);
		send(new Address(address), ping);
	}

	/**
	 * Sends the acknowledgement message to the given address.
	 * 
	 * @param address
	 *            The address to send to acknowledgement message to.
	 */
	public void ackPeer(String address) {
		AckMessage ack = new AckMessage(peerDescriptor);
		send(new Address(address), ack);
	}

	/**
	 * Sends a verification message to the given address with the given key.
	 * 
	 * @param address
	 *            The address to send the verification to.
	 * @param key
	 *            The key to encrypt the random data with.
	 */
	public void sendVerification(String address, String key) {
		VerificationMessage msg = new VerificationMessage(key);
		send(new Address(address), msg);
	}

	/**
	 * Send a move message to the given address with the given state, index and
	 * key to encrypt the message with
	 * 
	 * @param address
	 *            The address to send the verification to.
	 * @param state
	 *            The state to change the square to.
	 * @param index
	 *            The index to change.
	 * @param key
	 *            The key to encrypt this move message with.
	 */
	public void sendMovePeer(String address, GameView.State state, int index,
			String key) {
		MoveMessage m = new MoveMessage(state, index, key);
		send(new Address(address), m);
	}

}
