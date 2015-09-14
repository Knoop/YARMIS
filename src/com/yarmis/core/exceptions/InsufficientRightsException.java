package com.yarmis.core.exceptions;

import java.util.List;

import com.yarmis.core.security.Right;

public class InsufficientRightsException extends Exception {

    public InsufficientRightsException(List<Right> insufficient) {
	super(InsufficientRightsException.convertToText(insufficient));
    }

    public InsufficientRightsException() {
	super("Device doesn't have any rights");
    }

    private static String convertToText(List<Right> insufficient) {
	StringBuilder sb = new StringBuilder();
	sb.append("Device is missing the following right(s): ");
	for (Right right : insufficient)
	    sb.append(right.name()).append(", ");

	sb.delete(sb.length() - 2, 2);
	return sb.toString();
    }

}
