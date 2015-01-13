package com.yarmis.core.connectivity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.InputMismatchException;

import com.yarmis.core.security.Crypto;
@SuppressWarnings("unused")
public class Message {
	
	private final int version;
	private final int length;
	private final byte[] header;
	private final byte[] payload;

	private final boolean isSigned;
	private final boolean isDedicated;
	private byte[] nonce;
	private byte[] signature;

	private static final int VERSION = 1;
	private static final int HEADER_SIZE = 4;
	private static final int VERSION_BITS = 4;
	private static final int MAX_VERSION = (1 << VERSION_BITS) - 1;
	private static final int TYPE_BITS = 4;
	private static final int MAX_TYPE = (1 << TYPE_BITS) - 1;
	private static final int LENGTH_BITS = HEADER_SIZE * 8 - VERSION_BITS - TYPE_BITS;
	private static final int MAX_LENGTH = (1 << LENGTH_BITS) - 1;

	private static final int NONCE_SIZE = 8, SIG_SIZE = 32;
	private static final int BASE_SIZE = HEADER_SIZE + NONCE_SIZE + SIG_SIZE;


	/**
	 * Creates a new _unsigned_ message. 
	 * @param  payload                The payload of this message.
	 * @throws InputMismatchException in case the payload length exceeds the permitted length of a single package.
	 */
	public Message(byte[] payload) throws InputMismatchException{
		this(VERSION, payload);
	}
	/**
	 * Creates a new _unsigned_ message.
	 * @param  version                The version of this message.
	 * @param  payload                The payload of this message.
	 * @throws InputMismatchException in case the payload length exceeds the permitted length of a single package.
	 */
	public Message(int version, byte[] payload) throws InputMismatchException{
		this(version, payload, null, null, null);
	}
	
	/**
	 * Creates a new signed message, dedicated for a specific recipient.
	 * @param  payload                The payload of this message.
	 * @param  nonce                  The nonce that is used to guarantee freshness of this message.
	 * @param  publicKey              The public key of the recipient that is used to encrypt the signature.
	 * @param  privateKey             The private key of the sender that is used to sign the message's hash.
	 * @throws InputMismatchException in case the payload length exceeds the permitted length of a single package.
	 */
	public Message(byte[] payload, byte[] nonce, byte[] privateKey, byte[] publicKey) throws InputMismatchException {
		this(VERSION, payload, nonce , privateKey, publicKey);
	}

	/**
	 * Creates a new signed message, dedicated for a specific recipient.
	 * @param  version                The version of this message.
	 * @param  payload                The payload of this message.
	 * @param  nonce                  The nonce that is used to guarantee freshness of this message.
	 * @param  publicKey              The public key of the recipient that is used to encrypt the signature.
	 * @param  privateKey             The private key of the sender that is used to sign the message's hash.
	 * @throws InputMismatchException in case the payload length exceeds the permitted length of a single package.
	 */
	public Message(int version, byte[] payload, byte[] nonce, byte[] privateKey, byte[] publicKey) throws InputMismatchException {
		this.isSigned = nonce != null && privateKey != null;
		this.isDedicated = isSigned && publicKey != null;
		this.version = version;
		this.length = payload.length;
		if(length >= MAX_LENGTH) {
			throw new InputMismatchException("The length of the payload exceeds the limit of one message.\nPayload length: " + length + "\nLength limit: " + MAX_LENGTH);
		}
		this.header = makeHeader(version, this.length, isSigned, isDedicated);
		this.payload = payload;
		if(isSigned) {
			this.nonce = nonce;
			this.signature = Crypto.encrypt(hash(), privateKey);
			if(isDedicated) {
				this.signature = Crypto.encrypt(this.signature, publicKey);
			}
		}
	}

	/**
	 * Creates a new signed message that is not dedicated to a specific recipient.
	 * @param  payload    The payload of this message.
	 * @param  nonce      The nonce that is used to guarantee freshness of this message.
	 * @param  privateKey The private key that is used to sign this message.
	 * @throws InputMismatchException in case the payload length exceeds the permitted length of a single package.
	 */
	public Message(byte[] payload, byte[] nonce, byte[] privateKey) {
		this(VERSION, payload, nonce, privateKey);
	}
	
	/**
	 * Creates a new signed message that is not dedicated to a specific recipient.
	 * @param  version    The version of this message.
	 * @param  payload    The payload of this message.
	 * @param  nonce      The nonce that is used to guarantee freshness of this message.
	 * @param  privateKey The private key that is used to sign this message.
	 * @throws InputMismatchException in case the payload length exceeds the permitted length of a single package.
	 */
	public Message(int version, byte[] payload, byte[] nonce, byte[] privateKey) {
		this(version, payload, nonce, privateKey, null);
	}

	/**
	 * Creates a Message instance from a raw byte array.
	 * @param  raw The byte array that contains the message.
	 * @param  publicKey The public key of the sender.
	 * @return     The message that was constructed from the byte array.
	 */
	public static Message fromByteArray(byte[] raw, byte[] publicKey) throws ParseException, IOException {
		MessageReader mp = new MessageReader(publicKey);
		ByteArrayInputStream bis = new ByteArrayInputStream(raw);
		Message m = mp.parse(bis);
		return m;
	}

	/**
	 * Turns the content of this message into a single
	 * byte array.
	 */
	public byte[] toByteArray() {
		ByteBuffer bb;
		if(isSigned) {
			bb = ByteBuffer.wrap(new byte[BASE_SIZE + length]);
		} else {
			bb = ByteBuffer.wrap(new byte[HEADER_SIZE + length]);
		}
		bb.put(header);
		bb.put(payload);
		if(isSigned) {
			bb.put(nonce);
			bb.put(signature);
		}
		return bb.array();
	}

	/**
	 * Checks whether the signature of this signed message is valid, given the public key of the
	 * potential signer.
	 * @param  publicKey The public key that corresponds to the private key used for signing.
	 * @return           True iff the signature is valid.
	 * @throws  IllegalStateException in case that this message is not signed.
	 */
	public boolean isSignatureValid(byte[] publicKey) throws IllegalStateException {
		if(!isSigned) {
			throw new IllegalStateException("This message is not signed; checking the signature is not possible.");
		}
		return Crypto.isSignatureValid(hash(), signature, publicKey);
	}

	/**
	 * Checks whether the signature of this signed, dedicated message is valid, given the public key of the
	 * potential signer, as well as the private key of the recipient.
	 * @param  publicKey The public key that corresponds to the private key used for signing.
	 * @param  privateKey The private key that corresponds to the public key used for encrypting the signature.
	 * @return           True iff the signature is valid.
	 * @throws  IllegalStateException in case that this message is not signed.
	 */
	public boolean isSignatureValid(byte[] publicKey, byte[] privateKey) throws IllegalStateException {
		if(!isSigned) {
			throw new IllegalStateException("This message is not signed; checking the signature is not possible.");
		}
		return Crypto.isSignatureValid(hash(), Crypto.encrypt(signature, privateKey), publicKey);
	}


	public byte[] getPayload() {
		return payload;
	}

	/**
	 * Calculates the hash of the header, the payload, and the nonce.
	 */
	private byte[] hash() {
		ByteBuffer bb = ByteBuffer.wrap(new byte[HEADER_SIZE + NONCE_SIZE + length]); // Signature is not included ..obviously
		bb.put(header);
		bb.put(payload);
		bb.put(nonce);
		return Crypto.hash(bb.array());
	}

	/**
	 * Composes the header of the message from the version
	 * number and the length of the payload.
	 * This function is extremely ugly since we potentially 
	 * need to mix two numbers into one byte.
	 */
	private byte[] makeHeader(int version, int length, boolean isSigned, boolean isDedicated) {
		assert VERSION_BITS < 8 && TYPE_BITS < 8; //Otherwise we'll have to rewrite this.
		assert LENGTH_BITS < 31; //Otherwise we can't even fit the length into one integer in the first place. At least on 32 bits machines.
		ByteBuffer bb = ByteBuffer.wrap(new byte[HEADER_SIZE]);
		int shift = 7;
		byte b = 0;
		
		for(int i = VERSION_BITS - 1; i > 0; i--) {
			b |= (version / (1 << i))<<shift--;
			version %= (1 << i);
			if(shift < 0) {
				bb.put(b);
				b = 0;
				shift = 7;
			}
		}
		b |= version << shift--;
		if(shift < 0) {
			bb.put(b);
			b = 0;
			shift = 7;
		}
		int type = 0;
		type |= 0x01 & (isSigned?1:0);
		type |= 0x02 & (isDedicated?1:0);

		for(int i = TYPE_BITS - 1; i > 0; i--) {
			b |= (type / (1 << i))<<shift--;
			type %= (1 << i);
			if(shift < 0) {
				bb.put(b);
				b = 0;
				shift = 7;
			}
		}
		b |= type << shift--;
		if(shift < 0) {
			bb.put(b);
			b = 0;
			shift = 7;
		}

		for(int i = LENGTH_BITS - 1; i > 0; i--) {
			b |= (length / (1 << i))<<shift--;
			length %= (1 << i);
			if(shift < 0) {
				bb.put(b);
				b = 0;
				shift = 7;
			}
		}
		b |= length << shift--;
		if(shift < 0) {
			bb.put(b);
			b = 0;
			shift = 7;
		}

		return bb.array();
	}
	
	private static boolean isSigned(int type) {
		return (type & 0x01) != 0;
	}
	
	private static boolean isDedicated(int type) {
		return (type & 0x02) != 0;
	}

	public static class MessageReader {
		private int version;
		private int length;
		private int type;
		private byte[] payload;
		private byte[] nonce;
		private byte[] signature;
		
		// The public key that does supposedly match the signature of any
		// read message.
		private final byte[] publicKey;
		
		public MessageReader(byte[] publicKey) {
			this.publicKey = publicKey;
		}

		public Message parse(InputStream bb) throws ParseException, IOException {
			parseHeader(bb);
			payload = new byte[length];
			bb.read(payload);
			if (isSigned(type)) {
				nonce = new byte[NONCE_SIZE];
				bb.read(nonce);
				signature = new byte[SIG_SIZE];
				bb.read(signature);
				if(isDedicated(type)) {
					return new Message(version, payload, nonce, signature, publicKey);
				} else {
					return new Message(version, payload, nonce, signature);									
				}
			} else {
				return new Message(version, payload);
			}
		}
		
		private void parseHeader(InputStream bb) throws ParseException, IOException{
			assert HEADER_SIZE > 0; //Otherwise we'll have to rewrite this.
			assert LENGTH_BITS < 31; //Otherwise we can't even fit the length into one integer in the first place. At least on 32 bits machines.
			
			int[] numBits = new int[]{VERSION_BITS, TYPE_BITS, LENGTH_BITS};
			int[] values = getBitBlocks(numBits, bb);
			int i = 0;
			this.version = values[i++];
			this.type = values[i++];
			this.length = values[i++];
		}

		private int[] getBitBlocks(int[] numBits, InputStream bb) throws IOException{
			int pos = 0;
			int b = 0;
			int value[] = new int[numBits.length];
			for(int i = 0; i < numBits.length; i++) {
				int remaining = numBits[i];
				while(remaining > 0) {
					if(pos == 0) {
						int read = bb.read();
						if (read == -1) {
							throw new IOException("Stream closed");
						}
						b = 0xFF & read;
						pos = 8;
					}
					int maskSize = ((remaining-1) % 8)+1 > pos? pos : ((remaining-1) % 8)+1;
					byte mask = (byte) (0xFF >> (8 - maskSize));
					mask <<= pos - maskSize;
					int val = b & mask;
					pos -= maskSize;
					val >>= pos;
					remaining -= maskSize;
					val <<= remaining;
					value[i] |= val;
				}
			}
			return value;
		}
	}


}