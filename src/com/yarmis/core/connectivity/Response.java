package com.yarmis.core.connectivity;

import java.io.Serializable;

import com.yarmis.core.Request;
import com.yarmis.core.connectivity.exceptions.UnauthorizedRequestException;

/**
 * Used to package data from a remote source.
 * 
 * @author Maurice
 * 
 */
public class Response implements Serializable {

	/**
	 * Generated Serial version.
	 */
	private static final long serialVersionUID = -6344447188440565228L;

	/**
	 * The flags set along with the contained result.
	 */
	public final int flags;

	/**
	 * The result returned by the Response.
	 */
	public final Object result;

	/**
	 * The Request for which this Response was created.
	 */
	public final Request request;

	/**
	 * Create a new empty response for the given Request. This only acknowledges
	 * the fact that the given request has been dealt with.
	 * 
	 * @param request
	 */
	Response(Request request) {
		this(request, null, CLEAR);
	}

	/**
	 * Create a new response with a default value for all flags. If the result
	 * is an exception then the FLAG_THROW_EXCEPTION will be set. This can be
	 * prevented by calling the same constructor with the additional CLEAR
	 * value.
	 * 
	 * @param request
	 *            The request to which this is a response.
	 * @param result
	 *            The Object that was the returned result for the response.
	 */
	Response(Request request, Object result) {
		this(
				request,
				result,
				(result instanceof Exception) ? (result instanceof UnauthorizedRequestException ? FLAG_UNAUTHORIZED_REQUEST
						: CLEAR)
						| FLAG_THROW_EXCEPTION
						: CLEAR);
	}

	/**
	 * Create a new response with a the given flags.
	 * 
	 * @param request
	 *            The request to which this is a response.
	 * @param result
	 *            The Object that was the returned result for the response.
	 * @param flags
	 *            The flags that should be used for this response. If you have
	 *            no particular flags to set, leave this out.
	 */
	Response(Request request, Object result, int flags) {

		this.request = request;
		this.result = result;
		this.flags = flags;
	}

	/**
	 * <p>
	 * Indicates whether the given flags are set. Works for single flags but
	 * also for combinations of flags.
	 * </p>
	 * <p>
	 * If you want to check for multiple flags, separate them by a |. For
	 * instance isSet(FLAG_THROW_EXCEPTION | FLAG_UNAUTHORIZED_REQUEST)
	 * </p>
	 * 
	 * @param flags
	 * @return
	 */
	public boolean isSet(int flags) {
		return (this.flags & flags) == flags;
	}

	/*
	 * Used to give every flag a unique value. Is otherwise useless.
	 */
	private static int flagIndex = 0;
	/**
	 * <p>
	 * The FLAG that the contained result is an exception that was thrown. If
	 * this flag is set then the receiver should throw the received Exception as
	 * well.
	 * </p>
	 * <p>
	 * If it isn't set but the contained result is an exception, then this
	 * exception should be returned rather than thrown. The outcome would then
	 * be wanted.
	 * </p>
	 */
	public static int FLAG_THROW_EXCEPTION = 1 << flagIndex++;

	/**
	 * <p>
	 * The FLAG indicating that the request was invalid and couldn't be
	 * executed.
	 * </p>
	 */
	public static int FLAG_INVALID_REQUEST = 1 << flagIndex++;

	/**
	 * <p>
	 * The FLAG indicating that the request was unauthorized and won't be
	 * executed.
	 * </p>
	 */
	public static int FLAG_UNAUTHORIZED_REQUEST = 1 << flagIndex++;

	/**
	 * <p>
	 * The FLAG indicating that the request to which this is a response has been
	 * treated as spam. This means that the host hasn't treated this request.
	 */
	public static int FLAG_TREATED_AS_SPAM = 1 << flagIndex++;

	/**
	 * The value of the flags if none of the flags have been set.
	 */
	public static int CLEAR = 0;

}
