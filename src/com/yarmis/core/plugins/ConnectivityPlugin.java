package com.yarmis.core.plugins;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.yarmis.core.Core.Role;
import com.yarmis.core.Plugin;
import com.yarmis.core.Request;
import com.yarmis.core.connectivity.Connection;
import com.yarmis.core.connectivity.LanConnection;
import com.yarmis.core.connectivity.RequestReceiver;
import com.yarmis.core.connectivity.RequestSender;
import com.yarmis.core.connectivity.Response;
import com.yarmis.core.connectivity.Result;

public final class ConnectivityPlugin extends Plugin {

	/**
	 * The RequestHandler that handles requests that are brought in by
	 * Connections.
	 */
	protected final RequestReceiver handler;

	/**
	 * The RequestQueue where all requests that are made but not yet processed
	 * are send to.
	 */
	protected final RequestSender sender;

	/**
	 * The Manager that manages all Connections.
	 */
	protected final ConnectionManager connectionManager;

	/**
	 * The RightsProvider that assigns rights to different connections.
	 */
	private RightsProvider rightsProvider;

	public ConnectivityPlugin() {
		
		this.handler = new RequestReceiver();
		this.sender = new RequestSender();
		this.connectionManager = new ConnectionManager();
		

	}

	/**
	 * Called when a Response has been received over this connection. Note that
	 * this is called from the ConnectionReader thread. <b>Do not do any heavy
	 * stuff in this thread.</b>
	 * 
	 * @param response
	 *            The received response
	 */
	public final void handleRequest(Request request, Connection connection) {
		ConnectivityPlugin.this.handler.handleRequest(request, connection);
	}

	/**
	 * Called when a Request has been received over this connection. Note that
	 * this is called from the ConnectionReader thread. <b>Do not do any heavy
	 * stuff in this thread.</b>
	 * 
	 * @param request
	 *            The received request. This is not sanitized in any way. <b>Do
	 *            not just run it.</b>
	 */
	public void handleResponse(Response response) {
		ConnectivityPlugin.this.sender.report(response);

	}

	/**
	 * Called when a Connection has failed to transmit its data.
	 * 
	 * @param connection
	 *            The Connection that has failed.
	 * @param e
	 *            The Exception that was thrown.
	 */
	public void handleFailedConnection(Connection connection, Exception e) {
		try {
			ConnectivityPlugin.this.connectionManager
					.removeConnection(connection);
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}

	/**
	 * Send a {@code Request} to the given {@code Connection}.
	 * 
	 * @param request
	 *            The Request to send.
	 * @param connection
	 *            The Connection to send the Request over.
	 */
	public Result send(Request request, Connection connection) {

		if (connection == null)
			throw new IllegalArgumentException("Connection can not be null");
		if (request == null)
			throw new IllegalArgumentException("Request can not be null");

		return this.sender.send(request, connection);
	}

	/**
	 * Send a {@code Request} to the host.
	 * 
	 * @param request
	 *            The {@code Request} to send to the hosting device.
	 * @throws IllegalArgumentException
	 *             If the device doesn't have a host to send the message to.
	 */

	public Result send(Request request) {

		// always let it (indirectly) run through send(Request, Connection).
		return this.send(request, ALIAS_HOST);
	}

	/**
	 * Send a {@code Request} to the {@code Connection} with the given alias.
	 * 
	 * @param request
	 *            The {@code Request} to send to the device connected under the
	 *            given alias.
	 * @param alias
	 *            The alias under which the {@code Connection} is stored that
	 *            should be used to send the message to.
	 * @throws IllegalArgumentException
	 *             If the device doesn't have a connection with the given alias
	 *             to send the message to.
	 */
	public Result send(Request request, String alias) {
		Connection connection = this.getConnection(alias);

		if (connection == null)
			throw new IllegalArgumentException("No connection exists for \""
					+ alias + "\"");
		// always let it (indirectly) run through send(Request, Connection).
		return this.send(request, connection);
	}

	/**
	 * Gets the Rights associated with the given Connection.
	 * 
	 * @param connection
	 *            The Connection for which to get the associated rights.
	 * @return The List of Rights that the given Connection has on this device.
	 */
	public List<String> getRights(Connection connection) {

		if (this.rightsProvider == null)
			this.setRightsProvider(createDefaultRightsProvider());

		return this.rightsProvider.getRights(connection,
				this.connectionManager.isHosting);

	}

	/**
	 * Set the RightsProvider to use to determine the rights for a Connection.
	 * 
	 * @param rightsProvider
	 *            An instance of RightsProvider or null if you want to use the
	 *            default. By default everything is allowed when hosting, and
	 *            nothing when not hosting.
	 */
	public void setRightsProvider(RightsProvider rightsProvider) {
		this.rightsProvider = rightsProvider;
	}

	
	
	/**
	 * Removes the connection under the given alias from all connections. This
	 * Connections is attempted to be closed. If it fails to close, then it is
	 * not removed.
	 * 
	 * @param alias
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public void disconnect(String alias) throws IllegalArgumentException,
			IOException {
		this.connectionManager.removeConnection(alias);
	}

	/**
	 * Disconnect from the host. This is only possible if the ConnctivityPlugin
	 * isn't hosting and is connected to a Host.
	 * 
	 * @throws IOException
	 */
	public void disconnectFromHost() throws IOException {
		if (this.connectionManager.isHosting)
			throw new IllegalStateException(
					"A Host doesn't have a Host to disconnect from");
		else if (this.getConnectionCount() <= 0)
			throw new IllegalStateException("No Host to disconnect from");
		else
			this.connectionManager.removeConnection("host");
	}

	/**
	 * Disconnects all current connections.
	 * 
	 * @param force
	 */
	public void disconnectFromAll(boolean force) {
		this.connectionManager.removeAllConnections(force);
	}

	/**
	 * <p>
	 * Creates a new Connection with a Host. This is only possible if the
	 * ConnectivityPlugin isn't hosting, and also no host has been set yet.
	 * </p>
	 * <p>
	 * The communication will be set up using the default port as defined by
	 * {@code ConnectivityPlugin.PORT}.
	 * </p>
	 * 
	 * @param host
	 *            The IP address of the host with which to connect.
	 */
	public void connectToHost(String host) {
		this.connectToHost(host, PORT);
	}

	/**
	 * Creates a new Connection with a Host. This is only possible if the
	 * ConnectivityPlugin isn't hosting, and also no host has been set yet.
	 * 
	 * @param host
	 *            The IP address of the host with which to connect
	 * @param port
	 *            The port to use for communication.
	 */
	public void connectToHost(String host, int port) {
		if (this.connectionManager.isHosting)
			throw new IllegalStateException("A Host can not setup connections.");
		else if (this.getConnectionCount() > 0)
			throw new IllegalStateException("A Host has already been set.");

		try {
			Socket s = new Socket(host, port);
			Connection c = new LanConnection(s, readPKF(s));
			this.connectionManager.addConnection(ALIAS_HOST, c);

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Get the Connection that is stored under the given alias.
	 * 
	 * @param alias
	 * @return
	 */
	public Connection getConnection(String alias) {
		return this.connectionManager.connections.get(alias);
	}

	/**
	 * Get all Connections by both their aliases and the actual connections.
	 * 
	 * @return
	 */
	public Set<Entry<String, Connection>> getConnections() {
		return this.connectionManager.getConnections();
	}

	/**
	 * Get the number of existing connections.
	 * 
	 * @return
	 */
	public int getConnectionCount() {
		return this.connectionManager.connections.size();
	}

	/**
	 * Let the ConnectivityPlugin fulfill the given R
	 * 
	 * @param newRole
	 */
	public void fulfillRole(Role newRole) {

		switch (newRole) {
		case Client:
			if (this.connectionManager.isHosting)
				this.connectionManager.stopHosting();
			break;
		case Host:
			if (!this.connectionManager.isHosting)
				this.connectionManager.startHosting();
			
			break;
		default:
			break;

		}

	}

	/**
	 * NYI. Will later read the public key from a connection.
	 * 
	 * @param s
	 *            The connection socket that will transmit the key
	 * @return The obtained key.
	 */
	protected String readPKF(Socket s) {
		return s.getInetAddress().getHostName();
	}

	private class ConnectionManager {

		/**
		 * Indicates whether the ConnectionManager is hosting.
		 */
		private boolean isHosting = false;
		/**
		 * Indicates whether the hosting process should stop as soon as
		 * possible.
		 */
		private boolean stop = false;
		/**
		 * The mapping of Requests to Connections. This should only be updated
		 * from one thread, namely the one on which the controller usually runs.
		 */
		private final Map<String, Connection> connections;

		private ConnectionManager() {
			this.connections = new HashMap<String, Connection>();
		}

		/**
		 * Removes all existing connections. Will attempt to close all of them
		 * before discarding them. If a connection fails to close, it is only
		 * discarded if {@code force} is true.
		 */
		private void removeAllConnections(boolean force) {
			synchronized (this.connections) {
				Iterator<Entry<String, Connection>> iterator = getConnections()
						.iterator();
				while (iterator.hasNext()) {
					try {
						iterator.next().getValue().close();
						iterator.remove();
					} catch (IOException ex) {
						if (force)
							iterator.remove();
					}
				}
			}

		}

		/**
		 * Adds a connection under the given alias. It is assumed that someone
		 * has opened the connection, or is going to. The ConnectionManager will
		 * not change the Connection when storing it.
		 * 
		 * @param alias
		 *            The alias by which the connection should be identified.
		 * @param connection
		 *            The Connection to add.
		 * @throws IllegalArgumentException
		 *             If the alias is already in use.
		 */
		private void addConnection(String alias, Connection connection)
				throws IllegalArgumentException {

			if (connection.isClosed())
				throw new IllegalArgumentException(
						"Can't add a closed connection");

			synchronized (this.connections) {

				if (this.connections.containsKey(alias)) {
					throw new IllegalArgumentException(
							"There is already a connection associated with this alias. Choose a different alias or close the existing connection.");
				} else {
					this.connections.put(alias, connection);
				}
			}
		}

		/**
		 * Removes the connection under the given alias from all connections.
		 * This Connections is attempted to be closed. If it fails to close,
		 * then it is not removed.
		 * 
		 * @param alias
		 * @throws IllegalArgumentException
		 * @throws IOException
		 */
		private void removeConnection(String alias)
				throws IllegalArgumentException, IOException {
			synchronized (this.connections) {
				if (!this.connections.containsKey(alias)) {
					throw new IllegalArgumentException(
							"There is no connection associated with this alias");
				} else {
					Connection c = this.connections.get(alias);
					c.close();
					this.connections.remove(alias);
				}
			}
		}

		/**
		 * Removes the given connection from all connections.
		 * 
		 * @param c
		 *            The Connection to remove.
		 * @throws IllegalArgumentException
		 *             If the given Connection was not stored by the
		 *             ConnectionManager.
		 * @throws IOException
		 *             If the Connection couldn't be closed.
		 */
		private void removeConnection(Connection c)
				throws IllegalArgumentException, IOException {
			synchronized (this.connections) {
				Iterator<Entry<String, Connection>> i = connections.entrySet()
						.iterator();
				while (i.hasNext()) {
					Entry<String, Connection> e = i.next();
					if (c.equals(e.getValue())) {
						c.close();
						i.remove();
						return;
					}
				}
			}
			throw new IllegalArgumentException(
					"The provided connection is unknown.");
		}

		private Set<Entry<String, Connection>> getConnections() {
			return this.connections.entrySet();
		}

		/**
		 * Hosting functionality. Calling this will start a hostingprocess in
		 * the Thread it was called in. <b>Do not call this on the main Thread
		 * or any other Thread that isn't dedicated for hosting!</b>
		 * 
		 */
		private void host() {

			try {

				ServerSocket ss = new ServerSocket(PORT);
				this.isHosting = true;

				while (!this.stop) {
					Socket s = ss.accept();
					// Second check because {@code ssc.accept()} can block for a
					// long time.
					// Nontheless, this is not nice since the thread will be
					// occupied and blocked
					// until a client tries to connect, even if the hosting
					// service is shut down already.
					if (this.stop) {
						if (s != null)
							s.close();
					} else {
						String pkf = readPKF(s);
						synchronized (this) {
							Connection c = new LanConnection(s, pkf);
							connectionManager.addConnection(s.getInetAddress()
									.toString(), c);
						}
					}
				}
				ss.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			isHosting = false;
			connectionManager.stop = false;

		}

		/**
		 * Requests the hosting process to be started.
		 * 
		 * @return {@code true} if the hosting process is now started.
		 *         {@code false} if it was already started.
		 */
		private boolean startHosting() {
			
			
			synchronized (this) {

				if (!this.isHosting) 
				{
					this.removeAllConnections(true);
					(new Thread(new Runnable() {
						@Override
						public void run() {
							ConnectionManager.this.host();
						}
					})).start();
					return true;
				} else
					return false;
			}

		}

		/**
		 * Requests the hosting process to be stopped.
		 * 
		 * @return whether the hosting process has been asked to stop. This will
		 *         only be true if this call has caused the hosting process to
		 *         be asked to stop. If the process has been asked to stop
		 *         before, but hasn't stopped yet, then false is returned. False
		 *         is also returned in any other case that isn't described
		 *         above.
		 */
		private boolean stopHosting() {
			synchronized (this) {
				if (this.isHosting) {
					this.stop = true;
					this.removeAllConnections(true);
					return true;
				} else {
					return false;
				}
			}
		}

	}

	public static interface RightsProvider {
		/**
		 * Indicates which rights are assigned to the given Connection. The
		 * Rights indicate which methods can be executed.
		 * 
		 * @param connection
		 *            The Connection for whom the rights need to be obtained.
		 * @param isHost
		 *            Indicates whether the connection should be treated as if
		 *            this machine is hosting.
		 * @return The list of rights that the given Connection has to execute
		 *         methods on this machine. Return an empty list or null to
		 *         indicate that the given connection has no rights.
		 */
		public List<String> getRights(Connection connection, boolean isHost);

		public static final List<String> DEFAULT_RIGHTS = new LinkedList<String>();
		
	}

	/**
	 * The port to use for communication. This is the same on all devices.
	 */
	public final int PORT = 4223;

	/**
	 * The alias to use for the host.
	 */
	public final String ALIAS_HOST = "host";

	/**
	 * Creates a default RightsProvider that allows everything when hosting, and
	 * nothing when not hosting.
	 * 
	 * @return
	 */
	private RightsProvider createDefaultRightsProvider() {
		return new RightsProvider() {
			@Override
			public List<String> getRights(Connection connection, boolean isHost) {
				if (isHost) {
					return RightsProvider.DEFAULT_RIGHTS;
				} else {
					return null;
				}
			}

		};
	}
}
