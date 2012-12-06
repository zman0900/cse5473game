package com.cse5473.securegame.msg;

/*
 * Copyright (C) 2010 University of Parma - Italy
 * 
 * This source code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * Designer(s):
 * Marco Picone (picone@ce.unipr.it)
 * Fabrizio Caramia (fabrizio.caramia@studenti.unipr.it)
 * Michele Amoretti (michele.amoretti@unipr.it)
 * 
 * Developer(s)
 * Fabrizio Caramia (fabrizio.caramia@studenti.unipr.it)
 * 
 * **** NOTE FROM GROUP:
 * PLEASE NOTE, WE ONLY USED THIS BASIC BASIC BASIC OUTLINE.
 * The SIP2Peer had one example of a basic message, and we ended up 
 * using both of them, then creating our own messages. The Javadocs
 * are actually done by us though, there weren't any, aside from the 
 * initial descriptors.
 */

import it.unipr.ce.dsg.s2p.message.BasicMessage;
import it.unipr.ce.dsg.s2p.message.Payload;
import it.unipr.ce.dsg.s2p.peer.PeerDescriptor;

/**
 * Class <code>PingMessage</code> implements a simple message sent by the peer
 * to other peer. The payload of PingMessage contains the peer descriptor.
 * 
 * @author Fabrizio Caramia
 * 
 */
public class PingMessage extends BasicMessage {
	/**
	 * This is the indentifier string for the pin message.
	 */
	public static final String MSG_PEER_PING = "peer_ping";

	/**
	 * Same as the ack message, this message contains the peerdescriptor for the
	 * peer who sent the message, it is accessible through the payload.
	 * 
	 * @param peerDesc
	 *            The peer who sent the message
	 */
	public PingMessage(PeerDescriptor peerDesc) {

		super(MSG_PEER_PING, new Payload(peerDesc));

	}

}
