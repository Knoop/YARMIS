package com.yarmis.core;



public interface Module<RemoteClass> {

	
	public void setRemote();
	
	public void setLocal();
	
	public void useLocalImplementation(RemoteClass localInstance);
	

	public void setFunctionalityDefinitionClass(Class<?> functionalityDefinitionClass);
	
	public Class<?> getFunctionalityDefinitionClass();
}
