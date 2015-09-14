package com.yarmis.core;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.WeakHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.yarmis.core.security.DemandRights;
import com.yarmis.core.security.Right;

public class ModuleManager {

    // No instances.
    private ModuleManager() {
    }

    private static WeakHashMap<String, Module<?>> modules = new WeakHashMap<String, Module<?>>();

    private static HashSet<String> accessibility = new HashSet<String>();

    /**
     * Creates a new Module based on the given class. The resulting Module
     * provides the functionality as defined by {@code functionalityDefinition}.
     * This functionality can be executed on a remote location or on the local
     * machine. To allow for the latter an implementation of the
     * {@code functionalityDefinition} interface needs to be provided.
     * 
     * @param functionalityDefinition
     *            <p>
     *            The interface that defines the functionality. An interface can
     *            be used to create a Module if the following requirements are
     *            met:
     *            <ul>
     *            <li>It is an interface that extends the ModularFunctionality
     *            interface</li>
     *            <li>It contains public methods that have the
     *            {@code @DemandRights} annotation</li>
     *            </ul>
     *            </p>
     * @return A Module version of the the functionality defined by
     *         {@code functionalityDefinition}.
     */
    @SuppressWarnings("unchecked")
    public static <Fuctionality> Fuctionality createModule(
	    Class<Fuctionality> functionalityDefinition) {

	// validate the given class
	validateClass(functionalityDefinition);

	// When it is validated, create a new Module Proxy.
	Module<Fuctionality> module = (Module<Fuctionality>) Proxy
		.newProxyInstance(
			functionalityDefinition.getClassLoader(),
			new Class<?>[] { functionalityDefinition, Module.class },
			new ModuleInvocationHandler<Fuctionality>());

	module.setFunctionalityDefinitionClass(functionalityDefinition);
	
	// Register the Module
	ModuleManager.register(module, functionalityDefinition);

	return (Fuctionality) module;

    }

    private static <Functionality> void register(Module<Functionality> module,
	    Class<Functionality> functionalityDefinition) {
	ModuleManager.modules.put(functionalityDefinition.getSimpleName(),
		module);
    }

    /**
     * <p>
     * Validates that the given class is suited for being used as a Module. If
     * the given class is not suited, then a describing exception will be
     * thrown. If it is suited, the call will terminate without exceptions.
     * </p>
     * <h5>Suitability</h5>
     * <p>
     * A class is suitable for remote execution if all of the following
     * requirements are met:
     * <ul>
     * <li>It is an interface that extends the ModularFunctionality interface</li>
     * <li>It contains public methods that have the {@code @DemandRights}
     * annotation</li>
     * </ul>
     * </p>
     * 
     * @param _class
     *            <p>
     *            The class for which to find out whether it is suited for usage
     *            as a Module.
     *            </p>
     * @return True if the class matches the requirements above, false
     *         otherwise.
     */
    private static void validateClass(Class<?> _class) {

	// Check whether methods exist that can be called if it is used as a
	// Module.
	boolean hasAccessibleMethods = false;

	for (Method method : _class.getMethods())
	    hasAccessibleMethods |= validateMethod(method);

	// If there are no accessible methods, then it is not allowed.
	if (!hasAccessibleMethods)
	    throw new NoAccessibleMethodsException(_class);

    }

    /**
     * Validates the given {@code Method}. A {@code Method} is valid for remote
     * invocation if it meets the following requirements:
     * <ul>
     * <li>It is public</li>
     * <li>It has the {@code @DemandRights} annotation</li>
     * <li>Its parameters are valid</li>
     * </ul>
     * 
     * @param method
     *            The {@code Method} to verify.
     * @return
     */
    private static boolean validateMethod(Method method) {
	if (Modifier.isPublic(method.getModifiers())
		&& method.getAnnotation(DemandRights.class) != null) {
	    for (Class<?> _class : method.getParameterTypes())
		if (!Communication.validParameter(_class))
		    return false;
	    return true;

	} else
	    return false;

    }

    /**
     * <p>
     * Exception that indicates that the class mentioned in the exception does
     * not contain any methods that can be accessed when used remotely.
     * </p>
     * <p>
     * A method is available for remote execution if all of the following
     * requirements are met:
     * <ul>
     * <li>The method is publicly available;</li>
     * <li>The method has the {@code @DemandRights} annotation (the demanded
     * {@code Rights} are not important);</li>
     * </ul>
     * </p>
     * 
     * @author Maurice
     * 
     */
    public static final class NoAccessibleMethodsException extends
	    RuntimeException {

	/**
	 * Generated serial version universal identifier.
	 */
	private static final long serialVersionUID = -2805803932827960855L;

	// Only the ModuleManager may create a new instance of this exception.
	private NoAccessibleMethodsException(Class<?> _class) {
	    super(
		    _class.getName()
			    + " doesn't have any accessible methods for remote execution.");
	}

    }

    public static final class IllegalAccessibleMethodException extends
	    RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1277651592679680465L;

	// Only the ModuleManager may create a new instance of this exception.
	private IllegalAccessibleMethodException(Method method) {
	    super(
		    method.toString()
			    + " is set available for remote execution but has invalid parameters.");
	}

    }

    public static final class NotDeclaredAsModularFunctionalityException extends
	    RuntimeException {

	/**
	 * Generated serial version universal identifier.
	 */
	private static final long serialVersionUID = -176165158865175565L;

	// Only the ModuleManager may create a new instance of this exception.
	private NotDeclaredAsModularFunctionalityException(Class<?> _class) {
	    super(_class.getName()
		    + " doesn't extend the ModularFunctionality interface.");
	}

    }

    static Object handleRequest(JSONObject message, List<Right> assignedRights)
	    throws Throwable {

	String recipient = message.getString(Communication.Request.MODULE);

	// Check whether the module has been cleared for remote invocation.
	if (!ModuleManager.accessibility.contains(recipient))
	    throw new IllegalAccessException("Module " + recipient
		    + " is not accessible for remote invocation.");

	JSONArray args = message.getJSONArray(Communication.Request.VALUES);

	Class<?>[] argumentTypes = new Class<?>[args.length()];
	Object[] arguments = new Object[args.length()];

	for (int i = 0; i < args.length(); ++i) {
	    argumentTypes[i] = Communication.convertCharToClass(args
		    .getJSONObject(i)
		    .getString(Communication.Value.TYPE).charAt(0));
	    arguments[i] = args.getJSONObject(i).get(
		    Communication.Value.VALUE);
	}

	return ModuleManager.modules
		.get(recipient)
		.getClass()
		.getMethod(message.getString(Communication.Request.METHOD),
			argumentTypes)
		.invoke(ModuleManager.modules.get(recipient), arguments);

    }

    /**
     * Makes the given module accessible for executing code remotely.
     * 
     * @param module
     *            The Module to make accessible
     */
    public static void makeAccessible(String module) {
	ModuleManager.accessibility.add(module);
    }

    /**
     * Makes the given module inaccessible for executing code remotely. When the
     * given module receives a Request, an exception will be thrown.
     * 
     * @param module
     *            The module to make inaccessible
     */
    public static void makeInaccessible(String module) {
	ModuleManager.accessibility.remove(module);
    }

    /**
     * Makes all modules accessible for executing code remotely
     */
    public static void makeAllAccessible() {
	ModuleManager.accessibility.addAll(modules.keySet());
    }

    /**
     * Makes all modules inaccessible for executing code remotely. When any
     * module receives a Request, an exception will be thrown.
     */
    public static void makeAllInaccessible() {
	ModuleManager.accessibility.clear();
    }

    /**
     * Indicates whether the module for the given name is accessible for remote
     * invocation.
     * 
     * @param module
     *            The module for which to check accessibility
     * @return true if the module can be used for remote invocation, false
     *         otherwise.
     */
    public static boolean isAccessible(String module) {
	return ModuleManager.accessibility.contains(module);
    }

}
