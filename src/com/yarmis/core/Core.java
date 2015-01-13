package com.yarmis.core;


import java.util.HashMap;
import java.util.Map;

import com.yarmis.core.plugins.ConnectivityPlugin;

/**
 * Core is a central module that is in charge of distributing Requests and
 * Responses among other modules. It also keeps track of whether a certain type
 * of response is handled locally or remotely.
 */
public class Core {
	/*
	 * Modules
	 */
	private static final Map<String, Module> modules = new HashMap<String, Module>();

	public static final ConnectivityPlugin connectivity = new ConnectivityPlugin();

	public enum Role {
		Host, Client
	};

	private static Role currentRole = null;

	public static void setRole(Role newRole) {
		if (newRole == currentRole)
			return;
		
		//
		currentRole = newRole;
		connectivity.fulfillRole(newRole);

	}

	public static Role getCurrentRole() {
		return currentRole;
	}

	// No instance of Core can be created.
	private Core() {
	};

	

	/**
	 * Sets the given Module as the new Module for that module's Type. This will
	 * dismiss the old Module as well as bind the new one.
	 * 
	 * @param module
	 */
	public static void setModule(Module module) {
		String type = module.type;
		Core.setModule(type, module);
	}

	private static void setModule(String type, Module module) {
		synchronized (modules) {
			Module old = modules.remove(type);
			if(old!=null)
				old.dismiss();
			modules.put(type, module);
			if (module != null)
				module.bind();
		}
	}

	public static Module getModule(String type) {

		synchronized (modules) {
			return modules.get(type);
		}

	}

	public static void clearModule(String type) {
		Core.setModule(type, null);
	}

	public static final String DEBUG_TAG = "Core";

	
}
