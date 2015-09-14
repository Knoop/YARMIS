package com.yarmis.core;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import com.yarmis.core.exceptions.InsufficientRightsException;
import com.yarmis.core.exceptions.ModuleInaccessibleException;
import com.yarmis.core.exceptions.NoDeclaredRightsException;
import com.yarmis.core.security.DemandRights;

import com.yarmis.core.security.Right;

public class SecurityManager {

    static List<Right> getRights(Device device) {
	return Arrays.asList(Right.values());
    }

    /**
     * Validates the given module. If the given module is not accessible for
     * remote invocation, an exception will be thrown. If the module is
     * accessible, the call to this method will terminate normally.
     * 
     * @param module
     * @throws ModuleInaccessibleException
     */
    static void validateModule(String module)
	    throws ModuleInaccessibleException {
	// First, is the intended module accessible?
	if (!ModuleManager.isAccessible(module))
	    throw new ModuleInaccessibleException(module);
    }

    /**
     * Validates the given method call. If the given device is not allowed to
     * make the given request, an exception will be thrown. If the device is
     * allowed to make the request, the call to this method will terminate
     * normally.
     * 
     * @throws InsufficientRightsException
     * @throws NoDeclaredRightsException
     * 
     */
    static void validateMethod(Method method, Device device)
	    throws NoDeclaredRightsException, InsufficientRightsException {
	List<Right> rights = SecurityManager.getRights(device);

	// First, a basic check: does the device have rights?
	if (rights == null || rights.size() == 0)
	    throw new InsufficientRightsException();

	// Second, are the rights the device has enough to perform the call?
	SecurityManager.validateMethodCall(method, rights);

    }

    /**
     * <p>
     * Checker to see whether the given Connection is allowed to call the given
     * Method. This will check whether the rights that the method demands are a
     * subset of all rights assigned to the connection.
     * </p>
     * <p>
     * Do not that a method call is also not permitted if there are no rights
     * (null), however, that is not checked here.
     * </p>
     * 
     * @param method
     * @param connection
     * @return
     * @throws NoDeclaredRightsException
     * @throws InsufficientRightsException
     */
    private static void validateMethodCall(Method method, List<Right> rights)
	    throws NoDeclaredRightsException, InsufficientRightsException {

	// Get the Rights that have been demanded.
	DemandRights requiredRightAnnotation = method
		.getAnnotation(DemandRights.class);

	// If no rights have been demanded, then disallow calling to
	// prevent
	// illegal access.
	if (requiredRightAnnotation == null)
	    throw new NoDeclaredRightsException(
		    "The intended method "
			    + method.getName()
			    + " is not callable. Did you forget to add @DemandRights to the method?");

	// Create a list of all missing rights
	List<Right> insufficient = Arrays.asList(requiredRightAnnotation
		.value());
	insufficient.removeAll(rights);

	// If that list contains any elements, then it is not allowed to be
	// executed.
	if (insufficient.size() > 0)
	    throw new InsufficientRightsException(insufficient);

    }

}
