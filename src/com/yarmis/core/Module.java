package com.yarmis.core;

import java.io.Serializable;

public abstract class Module {

	private boolean isInUse;

	public final String type;

	protected Module(String type) {
		this.type = type;
	}

	/**
	 * Call to bind this Module to the Core.
	 */
	final void bind() {
		this.isInUse = true;
		this.onBind();
	}

	/**
	 * Called when the module is bound by the Core.
	 * 
	 */
	protected void onBind() {

	}

	/**
	 * Call to dismiss this Module from the Core.
	 */
	final void dismiss() {
		this.isInUse = false;
		this.onDismiss();
	}

	/**
	 * Called when the module has been dismissed by the core.
	 */
	protected void onDismiss() {

	}

	/**
	 * <p>
	 * Indicates whether this Module is currently being used by the Core. This
	 * means that the Core references to this Module when a Module if its type
	 * is needed.
	 * </p>
	 * 
	 * @return true if the Core currently references to this Module, false
	 *         otherwise.
	 */
	protected final boolean isInUse() {
		return this.isInUse;
	}

	/**
	 * <p>
	 * Turns the previous method call into a Request. Call this method directly
	 * when implementing a RemoteModule.
	 * </p>
	 * @param args
	 *            The provided arguments.
	 * @return A Request that will cause the same method to be invoked remotely.
	 */
	protected Request mkRequest(Serializable...args) {
		
		if(!this.isInUse())
			throw new IllegalStateException("Module isn't in use. Can't send calls to host.");
		
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		
		// Determine which index is of the previous call. This varies over different devices. 
		if(STACKTRACE_MODULE_CALL_INDEX == -1){	
			STACKTRACE_MODULE_CALL_INDEX = 0;
			while(!stack[STACKTRACE_MODULE_CALL_INDEX].getMethodName().equals("mkRequest")){
				++STACKTRACE_MODULE_CALL_INDEX;
			}
			// We've found mkRequest, we want the one that sits underneath it.
			STACKTRACE_MODULE_CALL_INDEX++;
		}
		
		return new Request(type, stack[STACKTRACE_MODULE_CALL_INDEX].getMethodName(), args);	
	}
	
	
	/**
	 * Integer indicating which element in the stacktrace will be the method calling mkRequest. 
	 * This is determined only the first time mkRequest is called.
	 */
	private static int STACKTRACE_MODULE_CALL_INDEX = -1;

}
