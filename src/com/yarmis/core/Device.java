package com.yarmis.core;

import java.net.InetAddress;

public class Device {
	private final InetAddress address;
	private final String name;

	public Device(InetAddress address, String name) {
		this.address = address;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public InetAddress getAddress() {
		return address;
	}
}