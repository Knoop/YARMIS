package com.yarmis.core;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.yarmis.core.Communication.CommunicationException;

public class CommunicationManager {

    /**
     * The {@code ConnectivityPlugin} providing network connection capabilities.
     */
    static ConnectivityPlugin connectivity = new ConnectivityPlugin();

    /**
     * A mapping of an identifier to a {@code Connection}.
     */
    private static HashMap<Device, Connection> connections = new HashMap<Device, Connection>();

    /**
     * The device of the host in the mapping of {@code Connection}s.
     */
    private static Device HOST;

    /**
     * The handler that handles messages.
     */
    private static MessageHandler messageHandler;

    /**
     * A Mapping of identifiers to the accompanying result objects.
     */
    private static HashMap<String, Result> results = new HashMap<String, Result>();

    // No instances of CommunicationManager
    private CommunicationManager() {
    }

    /**
     * Verifies that a ConnectivityPlugin has been set properly. If this is not
     * the case an exception will be thrown. Otherwise the method will terminate
     * normally.
     */
    private static void verifyConnectivityPlugin() {
	if (CommunicationManager.connectivity == null)
	    throw new IllegalStateException(
		    "The ConnectivityPlugin was not set");
    }

    /**
     * Registers the given Connection as an active connection.
     * 
     * @param connection
     *            The Connection to register as being active.
     */
    static void register(Connection connection) {

	if (CommunicationManager.connections
		.containsKey(connection.getDevice()))
	    throw new IllegalArgumentException(
		    "A Connection is already registered for device: "
			    + connection.getDevice().getName());

	// TODO further administration regarding registering Connections.
	connections.put(connection.getDevice(), connection);

    }

    /**
     * Drops the {@code Connection} to the given {@code Device} from the active
     * connections.
     * 
     * @param device
     *            The {@code Device} for which the {@code Connection} needs to
     *            be dropped.
     */
    static void drop(Device device) {

	if (!CommunicationManager.connections.containsKey(device))
	    throw new IllegalArgumentException(
		    "No Connection is registered for device: "
			    + device.getName());

	// TODO further administration regarding dropping Connections.
	connections.remove(device);

    }

    /**
     * Drops the {@code Connection} to the Host.
     */
    static void dropHost() {
	CommunicationManager.drop(CommunicationManager.HOST);
    }

    /**
     * Connects to the given {@code Device}. This requires that the
     * CommunicationManager is <b>not</b> hosting.
     * 
     * @param ip
     *            The {@code Device} to connect to
     * @throws IOException
     */
    static void connectTo(Device device) throws IOException {

	if (CommunicationManager.connectivity.isHosting()) {
	    throw new IllegalStateException(
		    "Can't connect to a device when hosting.");
	} else {
	    Connection connection = connectivity.connectTo(device);

	    // if the connection failed, an exception is thrown before reaching
	    // this point.
	    // The connection is already registered
	    CommunicationManager.HOST = device;

	}

    }

    /**
     * Registers a Result for the given identifier.
     * 
     * @param identifier
     * @return
     */
    private static Result register(String identifier) {
	Result result = new Result();
	CommunicationManager.results.put(identifier, result);
	return result;
    }

    /**
     * Requests for the given method to be executed by the Host.
     * 
     * @param m
     *            The Method to execute on the Host.
     * @param args
     *            The arguments to provide to the Host
     * @return A Result object that can be used to retrieve the return value.
     */
    static Result request(Method m, Object... args) {

	verifyConnectivityPlugin();

	String identifier = Communication.getIdentifier();
	Result result = register(identifier);

	try {
	    CommunicationManager.connections.get(HOST).send(
		    makeRequest(identifier, m, args));
	} catch (IOException e) {
	    throw new CommunicationException(e);
	}
	return result;

    }

    /**
     * Creates a request to execute the given method m.
     * 
     * This assumes that the given method m is valid for remote invocation. See
     * {@code ModuleManager.createModule} for the definition of when a method is
     * valid.
     * 
     * @param identifier
     *            The identifier to use for this request
     * @param m
     *            The method that needs to be requested
     * @param args
     *            The arguments that need to be passed to the call of the given
     *            method.
     * @return The created JSONObject, containing a request for the invocation
     *         of the given method, with the given arguments.
     */
    private static JSONObject makeRequest(String identifier, Method m,
	    Object... args) {
	JSONObject obj = new JSONObject();
	obj.put(Communication.Request.IDENTIFIER, identifier);
	obj.put(Communication.Request.METHOD, m.getName());
	obj.put(Communication.Request.MODULE, m.getDeclaringClass()
		.getSimpleName());
	JSONArray arguments = new JSONArray();
	for (int i = 0; i < args.length; ++i)
	    arguments.put((new JSONObject()).put(
		    Communication.Value.TYPE,
		    Communication.convertClassToChar(m.getParameterTypes()[i]))
		    .put(Communication.Value.VALUE, args[i]));

	obj.put(Communication.Request.VALUES, arguments);

	return obj;
    }

    /**
     * Creates a JSON response for the request with the same identifier.
     * 
     * @param identifier
     *            The identifier of this message.
     * @param success
     *            Indicates whether the execution was a success. The execution
     *            of a request is a success if it terminated normally, and thus
     *            no exceptions were thrown.
     * @param value
     *            The resulting value of the execution. This is either the
     *            return value (or null if void), or an exception.
     * @return The created JSONObject, containing a response for the execution
     *         of the request with the same identifier.
     */
    private static JSONObject makeResponse(String identifier, boolean success,
	    Object value) {
	JSONObject obj = new JSONObject();
	obj.put(Communication.Response.IDENTIFIER, identifier);
	obj.put(Communication.Response.SUCCESS, success);

	if (value == null)
	    obj.put(Communication.Response.VALUE, (Object) null);
	else
	    obj.put(Communication.Response.VALUE,
		    new JSONObject().put(Communication.Value.TYPE,
			    Communication.convertClassToChar(value.getClass()))
			    .put(Communication.Value.VALUE,
				    (success) ? value : Communication.convertException((Exception)value)));

	return obj;
    }

    /**
     * Sends a response to the given request message, based on whether the
     * request was executed successfully.
     * 
     * @param message
     *            The message for which a response needs to be send
     * @param connection
     *            The connection to which the response needs to be send
     * @param success
     *            Indicates whether the execution of the given request message
     *            was successful.
     * @param value
     *            The value that came from the execution of the given request
     *            message
     */
    private static void respondToMessage(JSONObject message,
	    Connection connection, boolean success, Object value) {
	try {
	    connection.send(makeResponse(
		    message.getString(Communication.Request.IDENTIFIER),
		    success, value));

	} catch (IOException e) {
	    e.printStackTrace();
	}

    }

    /**
     * Entry point for incoming messages. This needs to be called by a
     * Connection to indicate that it received a message. This checks the
     * message, lets it be handled by the right instance, and then responds to
     * the message if necessary.
     * 
     * @param message
     *            The message that needs to be handled
     * @param connection
     *            The {@code Connection} that received the given message.
     */
    static void handleMessage(JSONObject message, Connection connection) {

	// Non existing messages are of no use
	if (message == null)
	    return;

	CommunicationManager.messageHandler.handleIncomingMessage(message,
		connection);
    }

    private static class MessageHandler {

	/**
	 * Distributor for incoming messages. Reads the message type, and then
	 * handles the message how it is supposed to be handled.
	 * 
	 * @param message
	 *            The {@code Message} that was received
	 * @param receiver
	 *            The {@code Connection} over which it was received.
	 */
	private void handleIncomingMessage(JSONObject message,
		Connection receiver) {

	    try {
		String type = message.getString(Communication.TYPE);
		// Notifications
		if (Communication.NOTIFICATION.equalsIgnoreCase(type))
		    this.handleNotification(message);
		// Response
		else if (Communication.RESPONSE.equalsIgnoreCase(type))
		    this.handleResponse(message);
		// Request - May throw an Exception
		else if (Communication.REQUEST.equalsIgnoreCase(type))
		    this.handleRequest(message, receiver);
		// Unknown
		else
		    // The type is unexpected
		    throw new IllegalArgumentException("Message type " + type
			    + " is unexpected.");

	    } catch (Throwable throwable) {
		CommunicationManager.respondToMessage(message, receiver, false,
			throwable);
	    }
	}

	/**
	 * Handle for dealing with notifications. This is currently only a
	 * placeholder.
	 * 
	 * @param notification
	 *            The notification that was received.
	 */
	private void handleNotification(JSONObject notification) {
	    // Let the NotificationManager handle this
	}

	/**
	 * Handle for dealing with responses. This will release the
	 * {@code Result} waiting for this response.
	 * 
	 * @param response
	 *            THe response that was received.
	 */
	private void handleResponse(JSONObject response) {
	    String identifier = response
		    .getString(Communication.Response.IDENTIFIER);
	    if (CommunicationManager.results.containsKey(identifier))
		CommunicationManager.results.get(identifier).set(response);
	    else
		throw new IllegalStateException("Request " + identifier
			+ " is not known as an outstanding request.");
	}

	/**
	 * Handle for dealing with requests.
	 * 
	 * @param request
	 * @param connection
	 * @throws Throwable
	 */
	private void handleRequest(JSONObject request, Connection connection)
		throws Throwable {
	    if (CommunicationManager.connectivity.isHosting())
		CommunicationManager.respondToMessage(request, connection,
			true, ModuleManager.handleRequest(request, SecurityManager.getRights(connection.getDevice())));
	    else
		throw new IllegalStateException(
			"Can't handle a request when not hosting");
	}
    }
}
