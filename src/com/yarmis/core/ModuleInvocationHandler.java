package com.yarmis.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

class ModuleInvocationHandler<RemoteClass> implements InvocationHandler,
	Module<RemoteClass> {

    // Prevent other classes from creating this ModuleInvocationHandler.
    ModuleInvocationHandler() {

    }

    /**
     * Boolean indicating whether the methods will be executed remotely.
     */
    // While ModuleInvocationHandler.implementation equals null, isRemote should
    // be true.
    private boolean isRemote = true;

    private HashMap<Method, Method> methodCache;

    /**
     * The implementation to use for local usage.
     */
    private RemoteClass implementation = null;

    /**
     * The functionalityDefinitionClass
     */
    private Class<?> functionalityDefinitionClass;

    public Object invoke(Object object, Method method, Object[] arguments)
	    throws Throwable {

	// TODO see whether the following three statements can be executed
	// within a try-catch statement in this method.

	// Method is defined by Module, always treat as local
	if (method.getDeclaringClass().equals(Module.class))
	    return this.invokeAsModuleMethod(method, arguments);

	// Current state is to execute remotely, so do that
	else if (isRemote)
	    return this.invokeRemotely(method, arguments);

	// Method is to be executed locally, so do that
	else
	    return this.invokeLocally(method, arguments);

    }

    /**
     * Set this up to let calls be handled remotely. This checks whether all
     * security requirements are in check.
     */
    public void setRemote() {
	// Shortcut!
	if (this.isRemote)
	    return;

	this.performSetRemote();
    }

    /**
     * Performs what is required to set this as remote. <b>DO NOT CALL THIS
     * METHOD ANYWHERE OTHER THAN IN {@code setRemote}</b>. This method requires
     * safety/synchronization features which are verified and done by {@code setRemote}.
     */
    private void performSetRemote() {
	assert (!this.isRemote());

	this.isRemote = true;
    }

    /**
     * Set this up to let calls be handled locally. This checks whether all
     * security requirements are in check.
     */
    public void setLocal() {
	// Shortcut!
	if (!this.isRemote)
	    return;

	// No implementation -> can't be local
	if (this.implementation == null)
	    throw new IllegalStateException(
		    "Can't be set to local as there is no local implementation available.");

	// When everything is checked: do it.
	this.performSetLocal();

    }

    /**
     * Performs what is required to set this as local. <b>DO NOT CALL THIS
     * METHOD ANYWHERE OTHER THAN IN {@code setLocal}</b>. This method requires
     * safety/synchronization features which are verified and done by {@code setLocal}.
     */
    private void performSetLocal() {
	assert (this.isRemote());

	// Set it as a local.
	this.isRemote = false;

	// Make sure the method cache exists.
	if (methodCache == null)
	    methodCache = new HashMap<Method, Method>();

    }

    public void useLocalImplementation(RemoteClass localInstance) {
	if (Proxy.isProxyClass(localInstance.getClass()))
	    throw new IllegalArgumentException(
		    "The provided local implementation can not be a proxy class.");
	else
	    this.implementation = localInstance;

    }

    /**
     * Indicates whether all method calls will be executed remotely.
     * 
     * @return
     */
    boolean isRemote() {
	return this.isRemote;
    }

    /**
     * Call to let the method be executed as if it was defined by Module. This
     * asserts that the given Method is indeed declared by the Module interface.
     * 
     * The call to any method is passed onto the correct Method in this class,
     * which implements Module.
     * 
     * @param method
     *            The method to invoke.
     * @param arguments
     *            The arguments to pass to the call of the method.
     */
    private Object invokeAsModuleMethod(Method method, Object[] arguments)
	    throws Throwable {

	assert (method.getDeclaringClass().equals(Module.class));

	return this.getClass()
		.getMethod(method.getName(), method.getParameterTypes())
		.invoke(this, arguments);
    }

    /**
     * Call to let the method be executed remotely.
     * 
     * @param method
     *            The method to be executed remotely.
     * @param arguments
     *            The arguments provided to the method.
     */
    private Object invokeRemotely(Method method, Object[] arguments)
	    throws Throwable {

	assert (this.isRemote);
	return CommunicationManager.request(method, arguments).get();
    }

    /**
     * <p>
     * Looks up the Method that is the local implementation of the given method.
     * These Methods are looked up in a cache in case the Method was retrieved
     * before. If it has not been retrieved before, then it is searched in the
     * class of attribute {@code implementation}. If the cache exists, then the
     * found Method is stored in the cache.
     * </p>
     * 
     * <p>
     * If a method is cached, the Method can always be found. If it has not been
     * cached, then a local implementation of {@code RemoteClass} must be
     * assigned to be able to find the method.
     * </p>
     * 
     * <p>
     * If a cache is available, but does not contain the requested
     * {@code Method}, then the {@code Method} that was found will be added to
     * the cache.
     * </p>
     * 
     * @param method
     *            The method of which an implementation in {@code RemoteClass}
     *            must be found.
     * @return The Method as implemented in {@code RemoteClass}. This is never
     *         {@code null}.
     * @throws IllegalStateException
     *             If the Method has not been cached and there is no value set
     *             for attribute {@code implementation}.
     * @throws NoSuchMethodException
     *             If the given Method has no implementation in
     *             {@code RemoteClass}.
     */
    private Method lookupImplementingMethod(Method method)
	    throws NoSuchMethodException {

	/* Simple case: the method was cached, return the result */
	if (this.methodCache != null && this.methodCache.containsKey(method)) {
	    return this.methodCache.get(method);
	}
	/* Harder case: the method was not cached, go fetch */
	else {
	    /* Local implementation is required for its class. */
	    if (this.implementation == null)
		throw new IllegalStateException(
			"Method was not cached and no local implementation was set.");

	    /*
	     * Lookup the method, if it doesn't exist a NoSuchMethodException is
	     * thrown.
	     */
	    Method foundMethod = implementation.getClass().getMethod(
		    method.getName(), method.getParameterTypes());

	    /* Cache the result if a cache is present. */
	    if (this.methodCache != null)
		methodCache.put(method, foundMethod);

	    /* Return the result. */
	    return foundMethod;
	}

    }

    /**
     * Call to let the method be executed locally.
     * 
     * @param method
     *            The method to be executed locally.
     * @param arguments
     *            The arguments provided to the method.
     */
    private Object invokeLocally(Method method, Object[] arguments)
	    throws Throwable {

	// Assert that a local invocation is indeed wanted.
	assert (this.implementation != null && !this.isRemote);

	return lookupImplementingMethod(method).invoke(implementation,
		arguments);
    }

    @Override
    public Class<?> getFunctionalityDefinitionClass() {
	return this.functionalityDefinitionClass;
    }

    @Override
    public void setFunctionalityDefinitionClass(Class<?> functionalityDefinitionClass) {
	if(this.functionalityDefinitionClass!=null)
	    throw new IllegalStateException("FunctionalityDefinitionClass is already set");
	this.functionalityDefinitionClass = functionalityDefinitionClass;
    }
    
    
    

}