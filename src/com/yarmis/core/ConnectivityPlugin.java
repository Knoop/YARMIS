package com.yarmis.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ConnectivityPlugin {

    private ServerSocket server = null;

    /**
     * @throws IOException
     * 
     */
    protected void startHosting() throws IOException {
	this.server = new ServerSocket(Communication.PORT);
	this.startAccepting();
    }

    private void startAccepting() {
	(new Thread(new Runnable() {

	    @Override
	    public void run() {

		try {
		    while (true) {
			Socket socket = ConnectivityPlugin.this.server.accept();
			Connection connection = new Connection(socket.getInetAddress(), socket.getInputStream(), socket.getOutputStream());
			CommunicationManager.register(connection);
		    }
		}catch(SocketException e)
		{
		    // Socket is closed 
		    System.out.println("Closed socket.");
		}
		catch (IOException e) {
		    e.printStackTrace();

		}
	    }

	})).start();

    }

    /**
     * @throws IOException
     * 
     */
    protected void stopHosting() throws IOException {
	this.server.close();
    }

    /**
     * Directly connects to the given {@code Device}. This should also call
     * {@code registerConnection(Connection)} if the connection was made.
     * 
     * @param device
     *            The device to connect to.
     * @return The created Connection.
     * @throws IOException 
     */
    protected Connection connectTo(Device device) throws IOException {

	Socket socket = new Socket(device.getAddress(), Communication.PORT);
	
	Connection connection = new Connection(device, socket.getInputStream(), socket.getOutputStream());
	CommunicationManager.register(connection);
	
	return connection;
	
	
    }

    /**
     * Call to register a Connection that has been set up.
     * 
     * @param connection
     *            The Connection to register
     */

    /**
     * Indicates whether the {@code ConnectivityPlugin} is currently hosting
     * 
     * @return true if the {@code ConnectivityPlugin} is hosting, false
     *         otherwise.
     */
    protected boolean isHosting() {
	return this.server != null && !this.server.isClosed();
    }

    public String localAddress() {
	// TODO Auto-generated method stub
	return null;
    }

}