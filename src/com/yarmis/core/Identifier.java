package com.yarmis.core;

public class Identifier {

	
	/**
	 * The time at which this identifier was created. This is used to make it unique.
	 */
	private final long timestamp;
	
	/**
	 * Creates a new identifier based 
	 * @param timestamp
	 * @param name
	 */
	private Identifier(long timestamp)
	{
		this.timestamp = timestamp;
	}
	
	/**
	 * Creates a new Identifier 
	 * @return
	 */
	public static synchronized Identifier create()
	{
		return new Identifier(System.currentTimeMillis());
	}
	
	public String toString()
	{
		return "["+timestamp+"]";
	}
	
	public static Identifier from(String source)
	{
		if(!source.matches(REGEX))
			throw new IllegalArgumentException("Given source can;t be parsed to an Identifier");
			
		return new Identifier(Long.parseLong(source.substring(1, source.length())));
		
	}
	
	
	public static final String REGEX = "\\[\\d+\\]"; 
	
}
