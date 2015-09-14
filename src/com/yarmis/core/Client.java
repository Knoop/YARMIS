package com.yarmis.core;

public final class Client {

    public static void connect(Device device) throws Exception
    {
	if(CommunicationManager.connectivity.isHosting())
	    throw new IllegalStateException("Can not perform Client operation when hosting.");
	
	CommunicationManager.connectTo(device);
    }
    
    public static void disconnect()
    {
	if(CommunicationManager.connectivity.isHosting())
	    throw new IllegalStateException("Can not perform Client operation when hosting.");
	
	CommunicationManager.dropHost();
    }
    
}
