package com.cse5473.securegame.msg;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.cse5473.securegame.Coordinate;
import com.cse5473.securegame.Encryption;

import it.unipr.ce.dsg.s2p.message.BasicMessage;
import it.unipr.ce.dsg.s2p.message.Payload;
import it.unipr.ce.dsg.s2p.peer.PeerDescriptor;

/**
 * 
 * @author Caruso
 *
 */
public final class MoveMessage extends BasicMessage {
	/**
	 * 
	 */
	private static final String MSG_PEER_MOVE = "peer_move", PARAM_POSITION = "POSITION", PARAM_PEER = "PEER";
	
	/**
	 * 
	 * @param peer
	 * @param pos
	 */
	public MoveMessage(PeerDescriptor peer, Coordinate pos, String key) {
		super (MoveMessage.MSG_PEER_MOVE, new Payload(generateParamMap(peer, pos, key)));
	}
	
	/**
	 * 
	 * @param peer
	 * @param pos
	 * @param timeStamp
	 * @return
	 */
	private static final Map<String, Object> generateParamMap(PeerDescriptor peer, Coordinate pos, String key) {
		Map<String, Object> params = new HashMap<String, Object>(0);
		byte[] byteKey = null;
		try {
			byteKey = Encryption.GenerateAES128Key(key);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		
		try {
			params.put(MoveMessage.PARAM_POSITION, Encryption.EncryptAES128(pos.toString(), byteKey));
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			return null;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
			return null;
		} catch (BadPaddingException e) {
			e.printStackTrace();
			return null;
		}
		params.put(MoveMessage.PARAM_PEER, peer);
		return params;
	}
	
	/**
	 * 
	 * @param key
	 * @return
	 */
	public final Coordinate getDecryptedCoordinate(String key) {
		byte[] msg = (byte[]) this.getPayload().getParams().get(MoveMessage.PARAM_POSITION);
		byte[] byteKey = null;
		try {
			byteKey = Encryption.GenerateAES128Key(key);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		try {
			return new Coordinate(new String(Encryption.DecryptAES128(msg, byteKey)));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public final PeerDescriptor getPeerDescriptor() {
		return (PeerDescriptor)this.getPayload().getParams().get(MoveMessage.PARAM_PEER);
	}
}
