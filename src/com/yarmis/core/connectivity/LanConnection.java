package com.yarmis.core.connectivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;


/**
 * This class is a thin wrapper for a WiFi socket.
 */
public class LanConnection extends Connection {
	private Socket s;

	public LanConnection(String host, int port, String publicKeyFingerprint) throws UnknownHostException, IOException {
		this(new Socket(host, port), publicKeyFingerprint);
	}
	
	public LanConnection(Socket s, String publicKeyFingerprint) {
		super(publicKeyFingerprint);
		this.s = s;
		this.s.setPerformancePreferences(0, 1, 2);
		
		try {
			this.s.setTcpNoDelay(true);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void closeInner() throws IOException {
		s.close();		
	}

	@Override
	protected InputStream getInputStream() throws IOException {
		return s.getInputStream();
	}

	@Override
	protected OutputStream getOutputStream() throws IOException {
		return s.getOutputStream();
	}
	
}