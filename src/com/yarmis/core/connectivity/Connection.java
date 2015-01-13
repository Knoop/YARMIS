package com.yarmis.core.connectivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.text.ParseException;

import com.yarmis.core.Core;
import com.yarmis.core.Request;
import com.yarmis.core.connectivity.Message.MessageReader;

/**
 * <p>
 * This class is used to create a uniform interface for BlueTooth and WiFi
 * connections.
 * </p>
 * <p>
 * It already handles all tasks like sending Requests and getting Responses.
 * </p>
 */
public abstract class Connection {


	/**
	 * Indicates that this Connection has been closed. No new messages can be
	 * received if it is closed. Once the Connection is closed it can not be
	 * reopened. If the connection is closed it is still possible to send
	 * messages.
	 */
	boolean isClosed = false;

	/**
	 * Indicates that this Connection has been blocked. New messages will be
	 * received but they will not be converted or used in any way. A Connection
	 * can be blocked and unblocked as often as required. If the connection is
	 * blocked it is still possible to send messages.
	 */
	boolean isBlocked = false;

	/**
	 * The device that we are connected to is identified by the fingerprint of
	 * its public key.
	 */
	private final String publicKeyFingerprint;

	/**
	 * Creates a new connection to a Device that is identified by the given
	 * public key fingerprint.
	 */

	public Connection(String publicKeyFingerprint) {
		this.publicKeyFingerprint = publicKeyFingerprint;
		(new Thread(new ConnectionReader())).start();

	}

	protected abstract InputStream getInputStream() throws IOException;

	protected abstract OutputStream getOutputStream() throws IOException;

	protected abstract void closeInner() throws IOException;

	public final String getPublicKeyFingerprint() {
		return publicKeyFingerprint;
	}

	public final void close() throws IOException {
		this.isClosed = true;
		closeInner();
	}

	/**
	 * Blocks this connection. This does not close the connection but lets all
	 * incoming messages be ignored. Undo this by calling unblock.
	 */
	public final void block() {
		this.isBlocked = true;
	}

	/**
	 * Unblocks this connection. New messages will be send on to the connection
	 * controller until block or close is called.
	 */
	public final void unblock() {
		this.isBlocked = false;
	}

	/**
	 * Indicates whether this Connection is currently blocked. It is possible to
	 * unblock a Connection.
	 * 
	 * @return
	 */
	public final boolean isBlocked() {
		return this.isBlocked;
	}

	/**
	 * Indicates whether this Connection is closed. It is not possible to reopen
	 * a closed Connection.
	 * 
	 * @return
	 */
	public final boolean isClosed() {
		return this.isClosed;
	}

	/**
	 * <p>
	 * Send a byte array of this connection. Do not use this to send anything
	 * other than the byte array of a Request or Response. This is not enforced.
	 * </p>
	 * <p>
	 * Please use this rather than sendResponse if you have a Response that
	 * needs sending to a lot of Connections.
	 * </p>
	 * 
	 * @param payload
	 * @throws IOException
	 */

	private final synchronized void send(byte[] payload){
		try {
			this.getOutputStream().write((new Message(payload)).toByteArray());
		} catch(IOException e) {
			Core.connectivity.handleFailedConnection(this, e);
		}
	}

	/**
	 * Send a Request over this connection. This will wrap the request in a Message.
	 * 
	 * @param request
	 *            The request to be sent
	 */
	public final void sendRequest(Request request) throws IOException {
		byte[] convertedRequest = turnToByteArray(request);
		send(convertedRequest);
	}

	/**
	 * Send a Response over this connection. This will wrap the response in a Message.
	 * 
	 * @param response
	 *            The response to be sent
	 */
	public final void sendResponse(Response response) throws IOException {
		
		send(turnToByteArray(response));
		
	}

	/**
	 * Turns an object into a ByteArray.
	 * 
	 * @param object
	 * @return
	 * @throws IOException
	 */
	public static final byte[] turnToByteArray(Object object)
			throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		byte[] result;
		try {
			out = new ObjectOutputStream(bos);
			out.writeObject(object);
			result = bos.toByteArray();

		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException ex) {
				// ignore close exception
			}
			try {
				bos.close();
			} catch (IOException ex) {
				// ignore close exception
			}
		}

		return result;
	}

	/**
	 * Turns a byte array into an Object.
	 * 
	 * @param bytes
	 *            The byte array to turn into an Object
	 * @return The converted Object
	 * @throws IOException
	 *             If there is something wrong with the byte array
	 * @throws ClassNotFoundException
	 */
	public final static Object turnToObject(byte[] bytes) throws IOException,
			ClassNotFoundException {

		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		ObjectInput in = null;
		Object result = null;
		try {
			in = new ObjectInputStream(bis);

			result = in.readObject();

		} finally {
			try {
				bis.close();
			} catch (IOException ex) {
				// ignore close exception
			}
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				// ignore close exception
			}

		}

		return result;
	}

	/**
	 * A Runnable that performs checks on the inputStream of this Connection.
	 */

	private class ConnectionReader implements Runnable {

		private MessageReader mr;

		public ConnectionReader() {
			mr = new MessageReader(publicKeyFingerprint.getBytes());
		}

		/**
		 * Checks the connection for input until closed. If the Connection is
		 * blocked than the received input will not be propagated.
		 */
		@Override
		public void run() {

			Message input;
			Object received;

			// See if there still is a need to continue
			while (!isClosed) {

				input = safeGetInput();

				// check if that input needs inspection. if not, continue
				if (input == null || isBlocked)
					continue;

				received = safeConvertInput(input.getPayload());

				// check if the result can be used
				if (received == null)
					continue;

				// check how to treat the input.
				if (received instanceof Request)
					Core.connectivity.handleRequest((Request) received, Connection.this);
				else if (received instanceof Response)
					Core.connectivity.handleResponse((Response) received);
				// else it is not something to do anything with.

			}
		}

		private Message safeGetInput() {
			try {
				// Get new input
				// return IOUtils.toByteArray(getInputStream());
				Message m = mr.parse(getInputStream());
				return m;
			} catch (ParseException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				Core.connectivity.handleFailedConnection(Connection.this, e);
			}
			return null;

		}

		private Object safeConvertInput(byte[] input) {
			try {
				try {
					return Connection.turnToObject(input);
				} catch (EOFException e) {
					Core.connectivity.handleFailedConnection(Connection.this, e);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return null;
		}
	}

}