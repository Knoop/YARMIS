package com.yarmis.core;

import org.json.JSONException;
import org.json.JSONObject;



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
	private boolean success;

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
	 * Create a new Result.
	 */
	Result() {
		this.result = null;
		this.hasReleased = false;
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
		if (!this.hasReleased)
			synchronized (this) {
				this.wait();
			}

		// when you get here, result has been set.
		if (this.success)
			throw (Exception) this.result;

		// If no exception has been thrown, just return it.
		return this.result;

	}

	/**
	 * Set the given response as the result. This will also release all threads
	 * waiting for this response.
	 *
	 * @param response
	 */

	void set(JSONObject response) {

		if (this.result != null)
			throw new IllegalStateException(
					"The result has already been set. It can only be set once.");

		// No one is allowed to do anything while this is unpacking.
		synchronized (this) {
			// unpack the response
			this.success = response.getBoolean(Communication.Response.SUCCESS);

			// If it is a success, use the response value.
			if(this.success)
			    this.result = response.get(Communication.Response.VALUE);
			// If it wasn't a success, try to recreate the correct exception. 
			else
			{
			    try {
				this.result = Class.forName(response.getString(Communication.Response.VALUE)).newInstance();
			    } catch (InstantiationException e) {
				e.printStackTrace();
			    } catch (IllegalAccessException e) {
				e.printStackTrace();
			    } catch (ClassNotFoundException e) {
				e.printStackTrace();
			    } catch (JSONException e) {
				this.result = e;
				e.printStackTrace();
			    }
			    
			    if(this.result == null)
				this.result = new UnsuccessfulRequestException(response.getString(Communication.Response.VALUE));
			}
			
			// release at the very last moment.
			this.hasReleased = true;
			this.notify();
		}
	}

	/**
	 * Exception indicating that a Request was unsuccessful. This is only used when no better exception can be thrown. 
	 * @author Maurice
	 *
	 */
	public class UnsuccessfulRequestException extends Exception{

	    public UnsuccessfulRequestException(String string) {
		super(string);
	    }
	
	}

	

}
