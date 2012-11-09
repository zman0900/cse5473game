package com.cse5473.securegame;

import it.unipr.ce.dsg.s2p.message.parser.BasicParser;
import it.unipr.ce.dsg.s2p.peer.Peer;
import it.unipr.ce.dsg.s2p.sip.Address;

public class PeerManager extends Peer {

	public PeerManager(String key, String peerName,
			int peerPort) {
		super(null, key, peerName, peerPort);
		// TODO Auto-generated constructor stub
	}

	public PeerManager(String key, String peerName,
			int peerPort, BasicParser parser) {
		super(null, key, peerName, peerPort, parser);
		// TODO Auto-generated constructor stub
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
