package com.yarmis.core.exceptions;

public class ModuleInaccessibleException extends Exception {

    public ModuleInaccessibleException(String module) {
	super("Module "+module+" is not accessible for remote invocation");
	// TODO Auto-generated constructor stub
    }

    

}
