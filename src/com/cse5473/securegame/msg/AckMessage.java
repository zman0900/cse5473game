package com.cse5473.securegame.msg;

import it.unipr.ce.dsg.s2p.message.BasicMessage;
import it.unipr.ce.dsg.s2p.message.Payload;
import it.unipr.ce.dsg.s2p.peer.PeerDescriptor;

/**
 * Ack message is the basic response message when the user attempts to initiate
 * a game with any other player. This message gets sent back the the user who
 * first initiated the connection after the verification message has been
 * authenticated.
 * 
 * @author Caruso
 */
public class AckMessage extends BasicMessage {
	/**
	 * The label for the message.
	 */
	public static final String MSG_PEER_ACK = "peer_ack";

	/**
	 * Takes in the peer descriptor for the player sending this message.
	 * 
	 * @param peerDesc
	 *            Loaded directly into the payload.
	 */
	public AckMessage(PeerDescriptor peerDesc) {
		super(MSG_PEER_ACK, new Payload(peerDesc));
	}
}
