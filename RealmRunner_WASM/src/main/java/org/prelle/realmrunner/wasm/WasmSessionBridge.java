package org.prelle.realmrunner.wasm;

import java.util.function.Consumer;

import org.graalvm.webimage.api.JS;
import org.graalvm.webimage.api.JSValue;
import org.prelle.mudevents.MUDEventPipeline;
import org.prelle.realmrunner.network.MUDSession;

/**
 * WebAssembly Session Bridge connecting Java MUDClient-Base event bus and Telnet pipeline
 * to JavaScript in GraalVM Web Image environment via official @JS JavaScript interop.
 */
public class WasmSessionBridge {

	private static MUDSession session;
	private static WasmBrowserTransport transport;

	//-------------------------------------------------------------------
	@JS("""
		(function() {
			globalThis.__wasm_onLine = null;
			globalThis.__wasm_onGMCP = null;
			globalThis.__wasm_onSound = null;
			globalThis.__wasm_onSend = null;

			globalThis.__wasm_dispatchSendHex = function(hex) {
				if (!globalThis.__wasm_onSend || !hex) return;
				var len = hex.length / 2;
				var u8 = new Uint8Array(len);
				for (var i = 0; i < len; i++) {
					u8[i] = parseInt(hex.substring(i * 2, i * 2 + 2), 16);
				}
				globalThis.__wasm_onSend(u8);
			};

			globalThis.WasmSessionBridge = {
				setLineListener: function(fn) {
					console.log("[WasmSessionBridge] Registered lineListener");
					globalThis.__wasm_onLine = fn;
				},
				setGMCPListener: function(fn) {
					console.log("[WasmSessionBridge] Registered gmcpListener");
					globalThis.__wasm_onGMCP = fn;
				},
				setSoundListener: function(fn) {
					console.log("[WasmSessionBridge] Registered soundListener");
					globalThis.__wasm_onSound = fn;
				},
				setSendListener: function(fn) {
					console.log("[WasmSessionBridge] Registered sendListener");
					globalThis.__wasm_onSend = fn;
				},
				feedBinaryData: function(data) {
					var arr;
					if (data instanceof Uint8Array) {
						arr = data;
					} else if (data instanceof ArrayBuffer) {
						arr = new Uint8Array(data);
					} else if (Array.isArray(data)) {
						arr = new Uint8Array(data);
					} else {
						arr = new Uint8Array(0);
					}
					var hex = "";
					for (var i = 0; i < arr.length; i++) {
						var h = arr[i].toString(16);
						if (h.length < 2) h = "0" + h;
						hex += h;
					}
					if (globalThis.__wasm_feedHex) {
						try {
							if (typeof globalThis.__wasm_feedHex === 'function') {
								globalThis.__wasm_feedHex(hex);
							} else if (globalThis.__wasm_feedHex.accept) {
								globalThis.__wasm_feedHex.accept(hex);
							}
						} catch (e) {
							console.error("[WASM Bridge] Error feeding hex:", e);
						}
					}
				},
				sendText: function(text) {
					if (globalThis.__wasm_sendText) {
						if (typeof globalThis.__wasm_sendText === 'function') {
							globalThis.__wasm_sendText(text);
						} else if (globalThis.__wasm_sendText.accept) {
							globalThis.__wasm_sendText.accept(text);
						}
					}
				}
			};
			console.log("[WASM JS Interop] Registered globalThis.WasmSessionBridge successfully.");
		})();
	""")
	public static native void installJavaScriptBridge();

	@JS(value = "globalThis.__wasm_feedHex = fn;", args = {"fn"})
	public static native void setFeedHexHandler(Consumer<JSValue> fn);

	@JS(value = "globalThis.__wasm_sendText = fn;", args = {"fn"})
	public static native void setSendTextHandler(Consumer<JSValue> fn);

	@JS(value = "if (globalThis.__wasm_onLine) globalThis.__wasm_onLine(text);", args = {"text"})
	public static native void dispatchLineToJS(String text);

	@JS(value = "if (globalThis.__wasm_onGMCP) globalThis.__wasm_onGMCP(cmd, payload);", args = {"cmd", "payload"})
	public static native void dispatchGMCPToJS(String cmd, String payload);

	@JS(value = "if (globalThis.__wasm_onSound) globalThis.__wasm_onSound(type, filename, fullUrl);", args = {"type", "filename", "fullUrl"})
	public static native void dispatchSoundToJS(String type, String filename, String fullUrl);

	@JS(value = "if (globalThis.__wasm_dispatchSendHex) globalThis.__wasm_dispatchSendHex(hex);", args = {"hex"})
	public static native void dispatchSendHexToJS(String hex);

	//-------------------------------------------------------------------
	public static void initSession() {
		System.out.println("[WasmSessionBridge] initSession() called");
		installJavaScriptBridge();

		setFeedHexHandler(val -> {
			if (val != null) {
				feedHex(val.asString());
			}
		});

		setSendTextHandler(val -> {
			if (val != null) {
				sendText(val.asString());
			}
		});

		session = new MUDSession();
		transport = new WasmBrowserTransport();

		MUDEventPipeline eventBus = session.getEventBus();

		eventBus.subscribe(StreamLineEvent.class, event -> {
			System.out.println("[WasmSessionBridge] StreamLineEvent: " + event.getPlainText());
			dispatchLineToJS(event.getPlainText());
		});

		eventBus.subscribe(GMCPEvent.class, event -> {
			if (event.getCommand() != null) {
				System.out.println("[WasmSessionBridge] GMCP: " + event.getCommand().getName());
				dispatchGMCPToJS(
					event.getCommand().getName(),
					String.valueOf(event.getCommand().getPayload())
				);
			}
		});

		eventBus.subscribe(SoundEvent.class, event -> {
			System.out.println("[WasmSessionBridge] Sound: " + event.getFilename());
			dispatchSoundToJS(
				event.getSoundType().name(),
				event.getFilename(),
				event.getFullUrl()
			);
		});

		transport.setBinaryOut(data -> {
			if (data != null && data.length > 0) {
				StringBuilder sb = new StringBuilder(data.length * 2);
				for (byte b : data) {
					sb.append(Character.forDigit((b >> 4) & 0xF, 16));
					sb.append(Character.forDigit(b & 0xF, 16));
				}
				System.out.println("[WasmSessionBridge] Sending " + data.length + " bytes to JS WebSocket");
				dispatchSendHexToJS(sb.toString());
			}
		});

		TelnetOptionManager optionMgr = new TelnetOptionManager();
		SessionPipelineFactory.buildPipeline(session, transport, optionMgr);
		System.out.println("[WasmSessionBridge] Pipeline built. Transport listener = " + transport.getListener());
	}

	//-------------------------------------------------------------------
	public static void feedHex(String hex) {
		try {
			if (hex != null && transport != null && transport.getListener() != null) {
				int len = hex.length() / 2;
				byte[] b = new byte[len];
				for (int i = 0; i < len; i++) {
					int high = Character.digit(hex.charAt(i * 2), 16);
					int low = Character.digit(hex.charAt(i * 2 + 1), 16);
					b[i] = (byte) ((high << 4) | low);
				}
				transport.getListener().onBinaryFrame(b);
			}
		} catch (Throwable t) {
			System.out.println("[WasmSessionBridge] Exception in feedHex: " + t);
			t.printStackTrace(System.out);
		}
	}

	public static void sendText(String text) {
		System.out.println("[WasmSessionBridge] sendText called: " + text);
		if (transport != null && text != null) {
			transport.sendText(text);
		}
	}
}
