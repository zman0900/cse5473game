package com.cse5473.securegame;

import java.util.Iterator;

import com.cse5473.securegame.msg.JoinMessage;
import com.cse5473.securegame.msg.PeerListMessage;
import com.cse5473.securegame.msg.PingMessage;

import it.unipr.ce.dsg.s2p.message.parser.BasicParser;
import it.unipr.ce.dsg.s2p.org.json.JSONException;
import it.unipr.ce.dsg.s2p.org.json.JSONObject;
import it.unipr.ce.dsg.s2p.peer.Peer;
import it.unipr.ce.dsg.s2p.peer.PeerDescriptor;
import it.unipr.ce.dsg.s2p.sip.Address;

import android.util.Log;
import android.widget.Toast;

public class PeerManager extends Peer {

	public static String LOG_TAG = "peerManager";
	public static String SBC = "66.172.27.236:6066";
	public static String BOOTSTRAP = "bootstrap@66.172.27.236:5080";

	public interface PeerListReadyCallback {
		void peerListReady();
	}

	private PeerListReadyCallback cb;

	public PeerManager(String key, String peerName, int peerPort,
			PeerListReadyCallback cb) {
		super(null, key, peerName, peerPort);
		this.cb = cb;
		init();
	}

	public PeerManager(String key, String peerName, int peerPort,
			BasicParser parser, PeerListReadyCallback cb) {
		super(null, key, peerName, peerPort, parser);
		this.cb = cb;
		init();
	}

	private void init() {
		nodeConfig.test_address_reachability = "yes";
		nodeConfig.keepalive_time = 600;
		nodeConfig.sbc = SBC;
		// checkNAT(); slim chance of not being on nat
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
		// Got address, do bootstrap
		JoinMessage msg = new JoinMessage(peerDescriptor);
		send(new Address(BOOTSTRAP), msg);
	}

	@Override
	protected void onDeliveryMsgFailure(String arg0, Address arg1, String arg2) {
		// TODO Auto-generated method stub
		Log.d(LOG_TAG, "message delivery failure arg0: " + arg0 + " address: "
				+ arg1 + " arg2:" + arg2);
	}

	@Override
	protected void onDeliveryMsgSuccess(String arg0, Address arg1, String arg2) {
		// TODO Auto-generated method stub
		Log.d(LOG_TAG, "message delivery success arg0: " + arg0 + " address: "
				+ arg1 + " arg2:" + arg2);
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
				/*
				 * PeerActivity.handler.post(new Runnable() { public void run()
				 * { Toast toast = Toast.makeText(
				 * peerActivity.getBaseContext(), "Received: " +
				 * PeerListMessage.MSG_PEER_LIST, Toast.LENGTH_LONG);
				 * toast.show(); } });
				 */
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
					//Integer size = Integer.valueOf(this.peerList.size());
				}
				// Let main know
				cb.peerListReady();
			} else if (jsonMsg.get("type").equals(PingMessage.MSG_PEER_PING)) {
				/*
				 * PeerActivity.handler.post(new Runnable() { public void run()
				 * { Toast toast = Toast.makeText(
				 * peerActivity.getBaseContext(), "Received: " +
				 * PingMessage.MSG_PEER_PING, Toast.LENGTH_LONG); toast.show();
				 * } });
				 */
				Log.i(LOG_TAG,
						"received ping message from "
								+ params.get("contactAddress"));
				PeerDescriptor neighborPeerDesc = new PeerDescriptor(params
						.get("name").toString(), params.get("address")
						.toString(), params.get("key").toString(), params.get(
						"contactAddress").toString());
				addNeighborPeer(neighborPeerDesc);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
