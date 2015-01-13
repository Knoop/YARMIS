package com.yarmis.core.connectivity;

import java.util.LinkedList;
import java.util.List;

/**
 * Result is a class that helps you to get a value from any Connection. It
 * allows your thread to wait for the result that is send over a Connection. If
 * the result is an exception, then this exception will be thrown.
 * 
 * @author Maurice
 * 
 */
public class Result {

	/**
	 * The object that is the result of a request.
	 */
	private Object result;

	/**
	 * <p>
	 * Indicates whether an exception should be thrown if available. The value
	 * is equal to the FLAG_THROW_EXCEPTION flag in Response.
	 * </p>
	 */
	private boolean exceptionNeedsThrowing;

	/**
	 * <p>
	 * Indicates whether this Result has already been released. If it has
	 * already been released when get() is called, that thread doesn't have to
	 * wait.
	 * </p>
	 * <p>
	 * This is not entirely equal to checking whether result is null. It is very
	 * well possible that the actual result is null.Use this variable instead of
	 * that check to make sure that you have the correct information.
	 * </p>
	 */
	private boolean hasReleased;

	/**
	 * The List of listeners that need to be called when the result is known.
	 * This is used in lieu of .get()
	 */
	private List<OnResultReceivedListener> callbacks;

	/**
	 * Create a new Result.
	 */
	Result() {
		this.result = null;
		this.hasReleased = false;
		this.callbacks = new LinkedList<OnResultReceivedListener>();
	}

	/**
	 * <p>
	 * Waits for the result and returns it as soon as it is available. The
	 * returned Object can be casted safely to the type that you expect.
	 * </p>
	 * <p>
	 * Use this method if you can't continue working until you have a result. If
	 * you however can not or don't want to block the thread waiting for the
	 * result, use addOnResultReceivedListener instead.
	 * </p>
	 * 
	 * @return
	 * @throws Exception
	 */
	public Object get() throws Exception {

		// wait for the result to be set but only it hasn't released before.
		if (!this.hasReleased){
			synchronized (this) {
				this.wait();
			}
		}
		// when you get here, result has been set.
		if (this.result instanceof Exception && this.exceptionNeedsThrowing)
			throw (Exception) this.result;

		// If no exception has been thrown, just return it.
		return this.result;

	}

	/**
	 * <p>
	 * Add a OnResultReceivedListener to this Result. The Listener will be
	 * called when the Result has been received.
	 * </p>
	 * <p>
	 * This listener is meant as a way of adding a callback such that you do not
	 * have to wait for the result to continue your work. If you can not
	 * continue until the result is known, call .get instead.
	 * </p>
	 * 
	 * @param listener
	 */
	public void addOnResultReceivedListener(OnResultReceivedListener listener) {
		this.callbacks.add(listener);
	}

	/**
	 * Set the given response as the result. This will also release all threads
	 * waiting for this response.
	 * 
	 * @param response
	 */
	void set(Response response) {

		if (this.result != null)
			throw new IllegalStateException(
					"The result has already been set. It can only be set once.");

		// No one is allowed to do anything while this is unpacking.
		synchronized (this) {
			// unpack the response
			this.result = response.result;
			this.exceptionNeedsThrowing = response
					.isSet(Response.FLAG_THROW_EXCEPTION);

			// release at the very last moment.
			this.hasReleased = true;
			this.notify();
		}

		// Now that everything is unpacked and threads have been released, call
		// all listeners. This is done in a separate thread.
		(new Thread(new Runnable() {

			@Override
			public void run() {
				Object result;
				try {
					result = get();

				} catch (Exception e) {
					// Notify every waiter of the exception. Can't throw them
					// directly. Also catch everything one of the listeners
					// throws. Don't want one listener to screw up for the rest.
					for (OnResultReceivedListener listener : callbacks) {
						try {
							listener.onResultFailed(e);
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
					return;

				}
				for (OnResultReceivedListener listener : callbacks)
					listener.onResultReceived(result);
			}
		})).start();
	}

	/**
	 * 
	 * @author Maurice
	 * 
	 */
	public static interface OnResultReceivedListener {
		public void onResultReceived(Object result);

		public void onResultFailed(Exception exceptionThrown);
	}

}
