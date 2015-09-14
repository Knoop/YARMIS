package com.yarmis.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Placeholder for the later coming connectivity facility
 * 
 * @author Maurice
 * 
 */
public class Connection {

    /**
     * Indicates that this Connection has been closed. No new messages can be
     * received if it is closed. Once the Connection is closed it can not be
     * reopened. If the connection is closed it is still possible to send
     * messages.
     */
    boolean isClosed = false;

    private final InputStream inputStream;

    private final OutputStream outputStream;

    /**
     * The identifier for the device to which this is a Connection.
     */
    private final Device device;

    /**
     * Connects to a specific device, based on its address and port
     */
    public Connection(InetAddress identifier, InputStream inputStream,
	    OutputStream outputStream) {

	this.inputStream = inputStream;
	this.outputStream = outputStream;

	this.device = this.identifyDevice(identifier);

	this.setup();
    }

    public Connection(Device device, InputStream inputStream,
	    OutputStream outputStream) {
	this.device = device;
	this.inputStream = inputStream;
	this.outputStream = outputStream;

	this.setup();
    }

    private final void setup() {
	(new Thread(new Reader())).start();
    }

    private final Device identifyDevice(InetAddress identifier) {
	// TODO ask the other device for its name, and other stuff
	return new Device(identifier, "test");
    }

    public final Device getDevice() {
	return this.device;
    }

    public final void close() throws IOException {
	this.isClosed = true;

	this.inputStream.close();
	this.outputStream.close();
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
     * Returns the fingerprint of the public key that is associated with this
     * connection, or @code{null} if no public key is associated with this
     * connection.
     * 
     * @return [description]
     */
    public String getKeyFingerprint() {
	return null;
    }

    /**
     * Sends a JSONObject over the connection.
     * 
     * @param message
     *            The message to send
     * @throws IOException
     */
    protected void send(JSONObject message) throws IOException {
	new BufferedWriter(new OutputStreamWriter(this.outputStream))
		.write(message.toString());
    }

    protected void receive(JSONObject message) {

	CommunicationManager.handleMessage(message, this);

    }

    /**
     * Reads the incoming messages over this connection.
     * 
     * @author Maurice
     * 
     */
    private final class Reader implements Runnable {

	public void run() {
	    BufferedReader reader = null;
	    reader = new BufferedReader(new InputStreamReader(
		    Connection.this.inputStream));

	    JSONTokener tokener = new JSONTokener(reader);
	    while (!Connection.this.isClosed && tokener.more()) {
		try {
		    Connection.this.receive(new JSONObject(tokener));
		} catch (JSONException e) {

		}

	    }

	}

    }

}
