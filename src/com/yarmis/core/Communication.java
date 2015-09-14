package com.yarmis.core;

import org.json.JSONObject;

import com.yarmis.core.exceptions.UnsuccessfulRequestException;

class Communication {

    public static final int PORT = 4223;
    private static final char EXCEPTION_SEPARATOR = ':';

    public static final String NOTIFICATION = "notification";

    public static final class Notification {
	public static final String NOTIFICATION = "notification";
	public static final String VALUES = "values";
	public static final String STATE_HASH = "state_hash";
    }

    public static final String REQUEST = "request";

    public static final class Request {
	public static final String MODULE = "module";
	public static final String METHOD = "method";
	public static final String VALUES = "values";
	public static final String IDENTIFIER = "identifier";
    }

    public static final String RESPONSE = "response";

    public static class Value {
	public static final String TYPE = "type";
	public static final String VALUE = "value";
    }

    public static final class Response {
	public static final String IDENTIFIER = "identifier";
	public static final String SUCCESS = "success";
	public static final String VALUE = "value";
    }

    public static final String TYPE = "type";

    public static final char convertClassToChar(Class<?> _class) {
	if (_class.equals(Boolean.class))
	    return 'b';
	else if (_class.equals(Character.class))
	    return 'c';
	else if (_class.equals(Double.class))
	    return 'd';
	else if (_class.equals(Float.class))
	    return 'f';
	else if (_class.equals(Integer.class))
	    return 'i';
	else if (_class.equals(Long.class))
	    return 'l';
	else if (_class.equals(String.class))
	    return 's';
	else if (Throwable.class.isAssignableFrom(_class))
	    return EXCEPTION_CHAR;
	else
	    return DEFAULT_CHAR;
    }

    public static final Class<?> convertCharToClass(char _char) {
	switch (_char) {
	case 'b':
	    return Boolean.class;
	case 'c':
	    return Character.class;
	case 'd':
	    return Double.class;
	case 'f':
	    return Float.class;
	case 'i':
	    return Integer.class;
	case 'l':
	    return Long.class;
	case 's':
	    return String.class;
	case EXCEPTION_CHAR:
	    return Exception.class;
	default:
	    return null;
	}
    }

    /**
     * Parses a value from a JSONObject. It expects that the JSONObject contains
     * the keys {@code Communication.Value.VALUE} and
     * {@code Communication.Value.TYPE}.
     * 
     * @param value
     * @return
     */
    public static final Object parseValue(JSONObject value) {
	String rawVal = value.getString(Communication.Value.VALUE);

	switch ((char) value.getInt(Communication.Value.TYPE)) {
	case 'b':
	    return Boolean.parseBoolean(rawVal);
	case 'c':
	    return (char) Integer.parseInt(rawVal);
	case 'd':
	    return Double.parseDouble(rawVal);
	case 'f':
	    return Float.parseFloat(rawVal);
	case 'i':
	    return Integer.parseInt(rawVal);
	case 'l':
	    return Long.parseLong(rawVal);
	case 's':
	    return rawVal;
	case EXCEPTION_CHAR:
	    return parseException(rawVal);
	default:
	    return null;
	}

    }

    /**
     * <p>
     * Tries to parse an Exception from the given raw value. The raw value is
     * expected to be formed as
     * {@code com.package.exceptions.MyException:Some error message, which can contain the char :.}
     * . This would then be inflated into a new instance of MyException, with
     * {@code Some error message ... } as its message.
     * </p>
     * <p>
     * If the creation of such an exception fails, an
     * UnsuccesfulRequestException will be thrown instead, containing the entire
     * raw value.
     * </p>
     * 
     * @param rawVal
     *            The raw value from which to parse the exception.
     * @return The created exception.
     */
    private static final Exception parseException(String rawVal) {
	int indexOfSplit = rawVal.indexOf(EXCEPTION_SEPARATOR);

	try {
	    return (Exception) Class.forName(rawVal.substring(0, indexOfSplit))
		    .getConstructor(String.class)
		    .newInstance(rawVal.substring(indexOfSplit + 1));
	} catch (Exception e) {
	    e.printStackTrace();
	}

	return new UnsuccessfulRequestException(rawVal);
    }

    /**
     * Converts an Exception to a String, such that it can be used by
     * parseException to be read into an Exception.
     * 
     * @param exception
     * @return
     */
    public static final String convertException(Exception exception) {
	return exception.getClass().getName() + EXCEPTION_SEPARATOR
		+ exception.getMessage();
    }

    /**
     * Creates an identifier. This consists of the IP address, followed by an @
     * and a time stamp. This makes it unique across devices and across time.
     * 
     * @return
     */
    public static final String getIdentifier() {
	return CommunicationManager.connectivity.localAddress() + "@"
		+ System.currentTimeMillis();
    }

    /**
     * Indicates whether the given class is a valid parameter. This is checked
     * by comparing the result of converting that class to a character to the
     * default return value for that conversion.
     * 
     * @param _class
     *            The class to verify as a parameter.
     * @return {@code true} if it can be used as a parameter, {@code false}
     *         otherwise.
     */
    public static boolean validParameter(Class<?> _class) {

	switch (convertClassToChar(_class)) {
	case DEFAULT_CHAR:
	case EXCEPTION_CHAR:
	    return false;
	default:
	    return true;

	}
    }

    /**
     * The default character to use in the conversion from a class to a
     * representing character.
     */
    private static final char DEFAULT_CHAR = 0;
    /**
     * The character to use to express an Exception.
     */
    private static final char EXCEPTION_CHAR = 'E';

    public static final class CommunicationException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5863516554277940503L;

	public CommunicationException(String arg0, Throwable arg1,
		boolean arg2, boolean arg3) {
	    super(arg0, arg1, arg2, arg3);
	    // TODO Auto-generated constructor stub
	}

	public CommunicationException(String arg0, Throwable arg1) {
	    super(arg0, arg1);
	    // TODO Auto-generated constructor stub
	}

	public CommunicationException(Throwable arg0) {
	    super(arg0);
	    // TODO Auto-generated constructor stub
	}

    }

}