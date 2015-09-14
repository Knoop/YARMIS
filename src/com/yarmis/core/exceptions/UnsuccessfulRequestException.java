package com.yarmis.core.exceptions;

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