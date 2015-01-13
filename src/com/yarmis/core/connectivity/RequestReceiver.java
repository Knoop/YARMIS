package com.yarmis.core.connectivity;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.yarmis.core.Core;
import com.yarmis.core.Module;
import com.yarmis.core.Request;
import com.yarmis.core.connectivity.exceptions.FailedExecutionException;
import com.yarmis.core.connectivity.exceptions.InvalidRequestException;
import com.yarmis.core.connectivity.exceptions.UnauthorizedRequestException;
import com.yarmis.core.security.DemandRights;

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
	public void handleRequest(Request request, Connection requester) {

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
		private final Request request;

		/**
		 * The Connection for which the Request is ran.
		 */
		private final Connection connection;

		/**
		 * Creates a new RequestRunner for the given Request. When run is called
		 * on this runner then that Request is send to Core to handle.
		 * 
		 */
		private RequestRunner(Request request, Connection connection) {
			this.request = request;
			this.connection = connection;

		}

		@Override
		public void run() {

			Object returnObj = null;

			try {

				returnObj = this.performRequest(this.request,
						Core.connectivity.getRights(connection));

			} catch (NoSuchMethodException e) {
				// the method referenced by request.method didn't exist
				e.printStackTrace();
				returnObj = new InvalidRequestException(
						"Requested function call was unknown");
			} catch (SecurityException e) {
				// something bad happened
				e.printStackTrace();
				returnObj = new FailedExecutionException(e);
			} catch (IllegalAccessException e) {
				// The rights where okay but something was still inaccessible.
				e.printStackTrace();
				returnObj = new FailedExecutionException(e);
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
		 * <p>
		 * Checker to see whether the given Connection is allowed to call the
		 * given Method. This will check whether the rights that the method
		 * demands are a subset of all rights assigned to the connection.
		 * </p>
		 * <p>
		 * Do not that a method call is also not permitted if there are no
		 * rights (null), however, that is not checked here.
		 * </p>
		 * 
		 * @param method
		 * @param connection
		 * @return
		 */
		private boolean isMethodCallPermitted(Method method, List<String> rights) {
	
			// Get the Rights that have been demanded.
			DemandRights requiredRightAnnotation = method
					.getAnnotation(DemandRights.class);

			// If no rights have been demanded, then disallow calling to prevent
			// illegal access.
			if (requiredRightAnnotation == null) 				
				return false;
			

			// If one of the demanded rights is not present in the connection's
			// rights, it is not allowed. Otherwise it is.
			return rights.containsAll(Arrays.asList(requiredRightAnnotation
					.value()));

		}

		/**
		 * Get a List of {@code Right}s that the calling connection misses to
		 * execute the given method.
		 * 
		 * @param method
		 *            The Method for which to find the missing set of
		 *            {@code Right}s.
		 * @param rights
		 *            The {@code Right}s that the calling {@code Connection}
		 *            does have.
		 * @return
		 */
		private List<String> getMissingRights(Method method, List<String> rights) {
			DemandRights requiredRightAnnotation = method
					.getAnnotation(DemandRights.class);
			List<String> missing = Arrays
					.asList(requiredRightAnnotation.value());
			missing.removeAll(rights);

			return missing;
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
		private Method findMethod(Class<? extends Module> clazz, Request request)
				throws NoSuchMethodException, SecurityException {
			Class<?>[] parameterTypes = new Class[request.arguments.length];

			int index = 0;
			for (Object argument : request.arguments)
				parameterTypes[index++] = argument.getClass();

			return (clazz.getMethod(request.method, parameterTypes));

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
		 */
		@SuppressWarnings("unchecked")
		// This is for the casting in isMethodCallPermitted. Following our
		// pattern, this is safe.
		private Object performRequest(Request request, List<String> rights)
				throws IllegalAccessException, IllegalArgumentException,
				InvocationTargetException, NoSuchMethodException,
				SecurityException, UnauthorizedRequestException {

			// First perform basic security check.
			if (rights == null) {
				throw new UnauthorizedRequestException();
			}

			// The referenced Module, supplied by Core.
			Module module = (Module) Core.getModule(request.module);

			// The referenced Method, found through reflection
			Method method = this.findMethod(module.getClass(), request);

			// Now perform more advanced security check: check for the rights
			// defined in the abstract Module class.
			if (isMethodCallPermitted(
					this.findMethod((Class<? extends Module>) module.getClass()
							.getSuperclass(), request), rights)) {
				return method.invoke(module, (Object[]) request.arguments);
			} else {
				throw new UnauthorizedRequestException(this.getMissingRights(
						method, rights));
			}
		}
	}
}
