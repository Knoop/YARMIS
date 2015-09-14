package com.yarmis.core.exceptions;

import java.util.List;

import com.yarmis.core.security.Right;

public class UnauthorizedRequestException extends Exception {

	/**
	 * A list of missing Rights.
	 */
	private final List<Right> missing;

	/**
	 * Creates a new UnauthorizedRequestException with the given list of Rights
	 * indicating the Rights that were missing to be able to make the Request.
	 * 
	 * @param missing
	 */
	public UnauthorizedRequestException(List<Right> missing) {
		super();
		this.missing = missing;
	}

	/**
	 * Creates a new UnauthorizedRequestException indicating that no
	 */
	public UnauthorizedRequestException() {

		this(null);

	}

	/**
	 * Get an Array of all Rights that were missing, causing this
	 * UnauthorizedRequestException to be thrown.
	 * 
	 * @return
	 */
	public Right[] getMissingRights() {

		if (this.wasCallable())
			return this.missing.toArray(new Right[missing.size()]);
		else
			return null;
	}

	/**
	 * Indicates whether there are any Rights that would allow you to make the
	 * Request that bounced. If false, then no set of Rights exist that would
	 * allow you to make the Request this was thrown for.
	 * 
	 * @return true if a set of Rights existed to be allowed to perform the Request, false if the Request would never be allowed. 
	 */
	public boolean wasCallable() {
		return this.missing == null;
	}

}
