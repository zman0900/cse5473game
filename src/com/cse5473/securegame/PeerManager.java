package com.cse5473.securegame;

import java.util.ArrayList;
import java.util.Iterator;

import com.cse5473.securegame.msg.JoinMessage;
import com.cse5473.securegame.msg.PeerListMessage;
import com.cse5473.securegame.msg.PingMessage;

import it.unipr.ce.dsg.s2p.message.parser.BasicParser;
import it.unipr.ce.dsg.s2p.org.json.JSONException;
import it.unipr.ce.dsg.s2p.org.json.JSONObject;
import it.unipr.ce.dsg.s2p.peer.NeighborPeerDescriptor;
import it.unipr.ce.dsg.s2p.peer.Peer;
import it.unipr.ce.dsg.s2p.peer.PeerDescriptor;
import it.unipr.ce.dsg.s2p.sip.Address;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

public class PeerManager extends Peer {

	private static final String LOG_TAG = "peerManager";
	public static final String SBC = "66.172.27.236:6066";
	public static final String BOOTSTRAP = "bootstrap@66.172.27.236:5080";
	public static final String KEY_PREF_CONTACT_ADDRESS = "contactAddress";

	public static final int RECEIVED_PEER_LIST = 1;
	public static final int INIT_FAILED = 2;
	public static final int PEER_LIST_UPDATED = 4;
	private Handler handler;
	private Context context;

	public PeerManager(String key, String peerName, int peerPort,
			Handler handler, Context context) {
		super(null, key, peerName, peerPort);
		this.handler = handler;
		this.context = context;
		init();
	}

	public PeerManager(String key, String peerName, int peerPort,
			BasicParser parser, Handler handler, Context context) {
		super(null, key, peerName, peerPort, parser);
		this.handler = handler;
		this.context = context;
		init();
	}

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
			closePublicAddress();
			requestPublicAddress();
		}
	}

	@Override
	protected void onDeliveryMsgSuccess(String arg0, Address arg1, String arg2) {
		// TODO Auto-generated method stub
		Log.d(LOG_TAG, "message delivery success arg0: " + arg0 + " address: "
				+ arg1 + " arg2:" + arg2);
		if (arg0.equals("HELLO")) {
			// successfully set up sbc port, do bootstrap
			doBootstrap();
		}
	}

	@Override
	protected void onReceivedMsg(String peerMsg, Address sender,
			String contentType) {
		super.onReceivedMsg(peerMsg, sender, contentType);
		Log.d(LOG_TAG, "received message: " + peerMsg);
	}

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
				Message m = new Message();
				m.what = RECEIVED_PEER_LIST;
				handler.sendMessage(m);
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
				Message m = new Message();
				m.what = PEER_LIST_UPDATED;
				handler.sendMessage(m);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

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

	public void doBootstrap() {
		JoinMessage msg = new JoinMessage(peerDescriptor);
		send(new Address(BOOTSTRAP), msg);
	}

}
