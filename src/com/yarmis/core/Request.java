package com.yarmis.core;

import java.io.Serializable;


/**
 * Used to send to remotely ask for data.
 * 
 *
 */
public class Request implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2180161495362429412L;

	/**
	 * The id of the request
	 */
	public final String id;
	
	/**
	 * The Name of the intended module. Note that this should be one of the abstract classes in com.duke.core.modules.
	 */
	public final String module;
	
	/**
	 * The name of the intended method
	 * @return
	 */
	public final String method;
	
	/**
	 * The arguments given to the request
	 */
	public final Serializable[] arguments;
	
	/**
	 * Creates a new Request
	 * @param module
	 * @param method
	 */
	Request(String module, String method, Serializable... arguments)
	{
		//TODO implement functionality to get a new id
		//TODO Add Object... for arguments to the function
		this.id = "test";
		this.module = module;
		this.method = method;
		this.arguments = arguments;
			
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < arguments.length; i++) {
			Serializable s = arguments[i];
			sb.append(s.toString());
			if(i < arguments.length - 1) {
				sb.append(", ");
			}
		}
		return "Request: " + module + "." + method + "(" + sb.toString() + ")";
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Request && this.hashCode() == o.hashCode();
	}

	@Override
	public int hashCode() {
		int hash = 0;
		hash = 31 * hash + id.hashCode();
		hash = 31 * hash + module.hashCode();
		hash = 31 * hash + method.hashCode();
		for(Object o: arguments) {
			hash = 31 * hash + o.toString().hashCode();
		}
		return hash;
	}

	
}
