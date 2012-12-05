package com.cse5473.securegame.msg;

import it.unipr.ce.dsg.s2p.message.BasicMessage;
import it.unipr.ce.dsg.s2p.message.Payload;
import it.unipr.ce.dsg.s2p.peer.PeerDescriptor;

public class AckMessage extends BasicMessage {

	public static final String MSG_PEER_ACK = "peer_ack";

	public AckMessage(PeerDescriptor peerDesc) {
		super(MSG_PEER_ACK, new Payload(peerDesc));
	}

}
