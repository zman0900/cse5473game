package com.cse5473.securegame;

import com.cse5473.securegame.msg.JoinMessage;

import it.unipr.ce.dsg.s2p.message.parser.BasicParser;
import it.unipr.ce.dsg.s2p.peer.Peer;
import it.unipr.ce.dsg.s2p.sip.Address;

import android.util.Log;

public class PeerManager extends Peer {
	
	public static String SBC = "66.172.27.236:6066";
	public static String BOOTSTRAP = "bootstrap@66.172.27.236:5080";

	public PeerManager(String key, String peerName,
			int peerPort) {
		super(null, key, peerName, peerPort);
		init();
	}

	public PeerManager(String key, String peerName,
			int peerPort, BasicParser parser) {
		super(null, key, peerName, peerPort, parser);
		init();
	}
	
	private void init() {
		nodeConfig.test_address_reachability = "yes";
		nodeConfig.keepalive_time = 600;
		nodeConfig.sbc = SBC;
		checkNAT();
		Log.d("peerManager","after checkNat");
		JoinMessage msg = new JoinMessage(peerDescriptor);
		send(new Address(BOOTSTRAP), msg);
		Log.d("peerManager","after bootstrap send");
	}
	
	@Override
	protected void onReceivedSBCMsg(String SBCMsg) {
		super.onReceivedSBCMsg(SBCMsg);
		Log.d("peerManager", "received sbc msg: " + SBCMsg);
	}
	
	@Override
	protected void onReceivedSBCContactAddress(Address cAddress) {
		super.onReceivedSBCContactAddress(cAddress);
		Log.d("peerManager", "received sbc contact address: " + cAddress);
	}

	@Override
	protected void onDeliveryMsgFailure(String arg0, Address arg1, String arg2) {
		// TODO Auto-generated method stub
		Log.d("peerManager", "message delivery failure");
	}

	@Override
	protected void onDeliveryMsgSuccess(String arg0, Address arg1, String arg2) {
		// TODO Auto-generated method stub
		Log.d("peerManager", "message delivery success");
	}
	
	@Override
	protected void onReceivedMsg(String peerMsg, Address sender, String contentType) {
		super.onReceivedMsg(peerMsg, sender, contentType);
		Log.d("peerManager", "received message: " + peerMsg);
	}

}
