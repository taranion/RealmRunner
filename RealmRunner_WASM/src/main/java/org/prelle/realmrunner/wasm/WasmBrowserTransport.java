package org.prelle.realmrunner.wasm;

import java.net.URI;
import java.util.function.Consumer;

import org.prelle.realmrunner.transport.MUDTransport;
import org.prelle.realmrunner.transport.MUDTransportListener;

/**
 * WebAssembly browser transport implementation of MUDTransport.
 * Bridges incoming binary frames from JS WebSocket to pipeline decoders,
 * and passes outgoing binary frames to JS binary send listener.
 */
public class WasmBrowserTransport implements MUDTransport {

	private MUDTransportListener listener;
	private Consumer<byte[]> binaryOut;

	public MUDTransportListener getListener() {
		return listener;
	}

	@Override
	public void setListener(MUDTransportListener listener) {
		this.listener = listener;
	}

	public void setBinaryOut(Consumer<byte[]> binaryOut) {
		this.binaryOut = binaryOut;
	}

	private boolean connected = false;

	@Override
	public void connect(URI uri) throws Exception {
		this.connected = true;
		if (listener != null) {
			listener.onConnected("telnet");
		}
	}

	@Override
	public void close() {
		this.connected = false;
		if (listener != null) {
			listener.onDisconnected();
		}
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public void sendBinary(byte[] data) {
		if (binaryOut != null && data != null && data.length > 0) {
			binaryOut.accept(data);
		}
	}

	@Override
	public void sendText(String text) {
		if (text != null) {
			sendBinary(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		}
	}

	@Override
	public String getNegotiatedSubprotocol() {
		return "telnet";
	}
}
