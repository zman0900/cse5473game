package com.cse5473.securegame.msg;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.cse5473.securegame.Encryption;
import com.cse5473.securegame.GameView.State;

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
	private static final String MSG_PEER_MOVE = "peer_move", PARAM_STATE = "STATE", PARAM_INDEX = "INDEX", PARAM_PEER = "PEER";
	
	/**
	 * 
	 * @param peer
	 * @param pos
	 */
	public MoveMessage(PeerDescriptor peer, State state, int index, String key) {
		super (MoveMessage.MSG_PEER_MOVE, new Payload(generateParamMap(peer, state, index, key)));
	}
	
	/**
	 * 
	 * @param peer
	 * @param pos
	 * @param timeStamp
	 * @return
	 */
	private static final Map<String, Object> generateParamMap(PeerDescriptor peer, State state, int index, String key) {
		Map<String, Object> params = new HashMap<String, Object>(0);
		byte[] byteKey = null;
		try {
			byteKey = Encryption.GenerateAES128Key(key);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		
		try {
			params.put(MoveMessage.PARAM_STATE, Encryption.EncryptAES128(state.toString(), byteKey));
			params.put(MoveMessage.PARAM_INDEX, Encryption.EncryptAES128(Integer.toString(index), byteKey));
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
	public final State getDecryptedState(String key) {
		byte[] msg = (byte[]) this.getPayload().getParams().get(MoveMessage.PARAM_STATE);
		byte[] byteKey = null;
		try {
			byteKey = Encryption.GenerateAES128Key(key);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		try {
			return State.valueOf(new String(Encryption.DecryptAES128(msg, byteKey)));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public final Integer getDecryptedIndex(String key) {
		byte[] msg = (byte[]) this.getPayload().getParams().get(MoveMessage.PARAM_INDEX);
		byte[] byteKey = null;
		try {
			byteKey = Encryption.GenerateAES128Key(key);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		try {
			return Integer.valueOf(new String(Encryption.DecryptAES128(msg, byteKey)));
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
