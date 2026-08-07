package org.prelle.realmrunner.wasm;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.prelle.realmrunner.event.GMCPEvent;
import org.prelle.realmrunner.event.MUDEventBus;
import org.prelle.realmrunner.event.SoundEvent;
import org.prelle.realmrunner.event.StreamLineEvent;
import org.prelle.realmrunner.network.MUDSession;
import org.prelle.realmrunner.network.bridge.TelnetOptionManager;
import org.prelle.realmrunner.network.pipeline.SessionPipelineFactory;

/**
 * WebAssembly Session Bridge for bridging Java MUDClient-Base event bus and Telnet pipeline
 * to JavaScript in GraalVM Web Image environment.
 */
public class WasmSessionBridge {

	@FunctionalInterface
	public interface LineListener {
		void onLine(String text);
	}

	@FunctionalInterface
	public interface GMCPListener {
		void onGMCP(String command, String jsonPayload);
	}

	@FunctionalInterface
	public interface SoundListener {
		void onSound(String type, String filename, String fullUrl);
	}

	@FunctionalInterface
	public interface BinarySendListener {
		void onBinarySend(byte[] data);
	}

	private static MUDSession session;
	private static WasmBrowserTransport transport;
	private static LineListener lineListener;
	private static GMCPListener gmcpListener;
	private static SoundListener soundListener;
	private static BinarySendListener sendListener;

	//-------------------------------------------------------------------
	public static void initSession() {
		session = new MUDSession();
		transport = new WasmBrowserTransport();

		MUDEventBus eventBus = session.getEventBus();

		eventBus.subscribe(StreamLineEvent.class, event -> {
			if (lineListener != null) {
				lineListener.onLine(event.getPlainText());
			}
		});

		eventBus.subscribe(GMCPEvent.class, event -> {
			if (gmcpListener != null && event.getCommand() != null) {
				gmcpListener.onGMCP(
					event.getCommand().getName(),
					String.valueOf(event.getCommand().getPayload())
				);
			}
		});

		eventBus.subscribe(SoundEvent.class, event -> {
			if (soundListener != null) {
				soundListener.onSound(
					event.getSoundType().name(),
					event.getFilename(),
					event.getFullUrl()
				);
			}
		});

		TelnetOptionManager optionMgr = new TelnetOptionManager();
		SessionPipelineFactory.buildPipeline(session, transport, optionMgr);
	}

	//-------------------------------------------------------------------
	public static void setLineListener(LineListener listener) {
		lineListener = listener;
	}

	public static void setGMCPListener(GMCPListener listener) {
		gmcpListener = listener;
	}

	public static void setSoundListener(SoundListener listener) {
		soundListener = listener;
	}

	public static void setSendListener(BinarySendListener listener) {
		sendListener = listener;
		if (transport != null) {
			transport.setBinaryOut(data -> {
				if (sendListener != null) {
					sendListener.onBinarySend(data);
				}
			});
		}
	}

	//-------------------------------------------------------------------
	public static void feedBinaryData(byte[] data) {
		if (transport != null && transport.getListener() != null) {
			transport.getListener().onBinaryFrame(data);
		}
	}

	public static void sendText(String text) {
		if (transport != null && text != null) {
			transport.sendText(text);
		}
	}
}
