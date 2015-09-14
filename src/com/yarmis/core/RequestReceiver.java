package com.yarmis.core;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.yarmis.core.Communication.Request;
import com.yarmis.core.Communication.Response;
import com.yarmis.core.exceptions.FailedExecutionException;
import com.yarmis.core.exceptions.InsufficientRightsException;
import com.yarmis.core.exceptions.InvalidRequestException;
import com.yarmis.core.exceptions.ModuleInaccessibleException;
import com.yarmis.core.exceptions.NoDeclaredRightsException;
import com.yarmis.core.exceptions.UnauthorizedRequestException;

public class RequestReceiver {

    /**
     * An ExecutorService that runs Requests.
     */
    private ExecutorService requestHandlers;

    public RequestReceiver() {

	// use a dynamic amount of threads.
	this.requestHandlers = Executors.newCachedThreadPool();
    }

    /**
     * <p>
     * Adds a request for a connection to the list of requests that are being
     * handled. The execution of the {@code Request}, as well as security
     * checks, happens in a separate thread.
     * </p>
     * <p>
     * If the requester is authorized than the Request will be ran performed on
     * a separate thread. If the Request was already being performed than it
     * will not be done again. Instead the result of that call will be passed
     * on.
     * </p>
     * 
     * @param request
     *            The Request to perform
     * @param requester
     *            The Connection that requested the given Request to be
     *            performed.
     */
    public void handleRequest(JSONObject request, Connection requester) {

	this.requestHandlers.execute(new RequestRunner(request, requester));

    }

    /**
     * The RequestRunner class finds the correct Method to call, checks whether
     * the calling instance is allowed to do so and will perform the request in
     * its run method.
     * 
     * 
     */
    private class RequestRunner implements Runnable {

	/**
	 * The Request that need to be ran.
	 */
	private final JSONObject request;

	/**
	 * The Connection for which the Request is ran.
	 */
	private final Connection connection;

	/**
	 * Creates a new RequestRunner for the given Request. When run is called
	 * on this runner then that Request is send to Core to handle.
	 * 
	 */
	private RequestRunner(JSONObject request, Connection connection) {
	    this.request = request;
	    this.connection = connection;

	}

	@Override
	public void run() {

	    Object returnObj = null;

	    try {

		//
		returnObj = this.performRequest(this.request);

	    } catch (NoSuchMethodException e) {
		// the method referenced by request.method didn't exist
		e.printStackTrace();
		returnObj = new InvalidRequestException(
			"Requested function call was unknown");
	    } catch (SecurityException e) {
		// something bad happened
		e.printStackTrace();
		returnObj = new FailedExecutionException();
	    } catch (IllegalAccessException e) {
		// The rights where okay but something was still inaccessible.
		e.printStackTrace();
		returnObj = new FailedExecutionException();
	    } catch (IllegalArgumentException e) {
		// Everything went okay except for the fact that the wrong
		// arguments were presented.
		e.printStackTrace();
		returnObj = new InvalidRequestException(
			"The parameters applied were incorrect.");
	    } catch (InvocationTargetException e) {
		// Something went wrong during execution. Per documentation, the
		// real exception is .getCause()
		e.printStackTrace();
		returnObj = e.getCause();
	    } catch (Exception e) {
		// Anything else can just be passed on. This includes
		// UnauthorizedAccessExceptions.
		e.printStackTrace();
		returnObj = e;
	    }

	    try {
		connection.sendResponse(new Response(request, returnObj));
	    } catch (IOException e) {
		e.printStackTrace();
	    }

	}

	/**
	 * Gets the method object to which the request refers.
	 * 
	 * @param request
	 *            The Request for which to find the Method
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws Exception
	 */
	// really couldn't care less. It should work as long as we implement
	// everything correctly.
	private Method findMethod(Class<?> clazz, JSONObject request)
		throws NoSuchMethodException, SecurityException {

	    JSONArray arguments = request
		    .getJSONArray(Communication.Request.VALUES);

	    Class<?>[] parameterTypes = new Class[arguments.length()];

	    // Determine for each named parameter what type it has
	    for (int i = 0; i < arguments.length(); ++i)
		parameterTypes[i] = Communication.convertCharToClass(arguments
			.getJSONObject(i).getString(Communication.Value.TYPE)
			.charAt(0));

	    // Obtain the method that is named in the request, using the parameters
	    return (clazz.getMethod(request.getString(Communication.Request.METHOD), parameterTypes));

	}

	/**
	 * Performs a request. This does not include checking whether anyone is
	 * allowed to make a request.
	 * 
	 * If something goes wrong or is non existing or worse, an exception
	 * will be thrown. Different exceptions require different translations
	 * into terms of Responses.
	 * 
	 * @param request
	 *            The Request to perform.
	 * @return The created Response.
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 *             Thrown if the method named in the {@code Request} does
	 *             not exist for the {@code Module} named in the
	 *             {@code Request}.
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 *             Thrown when accessing a Method that isn't visible. As
	 *             long as only public methods are created in Modules, this
	 *             shouldn't happen.
	 * @throws UnauthorizedRequestException
	 *             Thrown if the given Rights are not enough to perform the
	 *             {@code Request}.
	 * @throws InsufficientRightsException
	 * @throws NoDeclaredRightsException
	 * @throws ModuleInaccessibleException
	 */
	@SuppressWarnings("unchecked")
	// This is for the casting in isMethodCallPermitted. Following our
	// pattern, this is safe.
	private Object performRequest(JSONObject request)
		throws IllegalAccessException, IllegalArgumentException,
		InvocationTargetException, NoSuchMethodException,
		SecurityException, UnauthorizedRequestException,
		NoDeclaredRightsException, InsufficientRightsException,
		ModuleInaccessibleException {

	    // Obtain a reference to the module
	    String moduleName = request.getString(Communication.Request.MODULE);

	    // Validate the module
	    SecurityManager.validateModule(moduleName);

	    // Obtain a reference to the method
	    Module<?> module = ModuleManager.getModule(moduleName);
	    Method method = this.findMethod(module.getClass(), request);

	    // Validate the method
	    SecurityManager.validateMethod(method, connection.getDevice());

	    // Execute the method
	    return method.invoke(module, obtainArguments(request));

	}

	/**
	 * Obtains all arguments from the request.
	 * 
	 * @param request
	 *            The request from which the arguments should be obtained.
	 * @return The arguments as a array of objects.
	 */
	private Object[] obtainArguments(JSONObject request) {
	    JSONArray argumentsContainer = request
		    .getJSONArray(Communication.Request.VALUES);
	    Object[] arguments = new Object[argumentsContainer.length()];

	    for (int i = 0; i < argumentsContainer.length(); ++i)
		arguments[i] = Communication.parseValue(argumentsContainer
			.getJSONObject(i));

	    return arguments;

	}
    }
}
