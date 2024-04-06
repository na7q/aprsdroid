package com.jazzido.PacketDroid;

public interface PacketCallback {
	public void received(byte[] packet);
	public void peak(short peak_value);
}
