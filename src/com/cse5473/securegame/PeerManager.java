package com.cse5473.securegame;

import it.unipr.ce.dsg.s2p.message.parser.BasicParser;
import it.unipr.ce.dsg.s2p.peer.Peer;
import it.unipr.ce.dsg.s2p.sip.Address;

import android.util.Log;

public class PeerManager extends Peer {

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
		checkNAT();
		Log.d("peerManager","after checkNat");
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
		
	}

	@Override
	protected void onDeliveryMsgSuccess(String arg0, Address arg1, String arg2) {
		// TODO Auto-generated method stub

	}

}
