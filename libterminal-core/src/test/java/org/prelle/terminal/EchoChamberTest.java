package org.prelle.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;

public class EchoChamberTest {

	private SwitchableInputStream inPipe;
	private ByteArrayOutputStream serverOut;
	private EchoChamber echoChamber;

	@Before
	public void setUp() {
		inPipe = new SwitchableInputStream();
		serverOut = new ByteArrayOutputStream();
		echoChamber = new EchoChamber(serverOut, inPipe);
	}

	@Test
	public void testEchoEnabledInjectsData() throws IOException {
		echoChamber.setEchoEnabled(true);
		assertTrue(echoChamber.isEchoEnabled());

		byte[] typedInput = "hello\n".getBytes(StandardCharsets.UTF_8);
		echoChamber.write(typedInput);

		// Verify bytes sent to server output
		assertEquals("hello\n", serverOut.toString(StandardCharsets.UTF_8));

		// Verify bytes injected into inPipe
		byte[] readBack = new byte[typedInput.length];
		int len = inPipe.read(readBack);
		assertEquals(typedInput.length, len);
		assertEquals("hello\n", new String(readBack, StandardCharsets.UTF_8));
	}

	@Test
	public void testEchoDisabledDoesNotInjectData() throws IOException {
		echoChamber.setEchoEnabled(false);
		assertFalse(echoChamber.isEchoEnabled());

		byte[] typedInput = "password123\n".getBytes(StandardCharsets.UTF_8);
		echoChamber.write(typedInput);

		// Verify bytes sent to server output
		assertEquals("password123\n", serverOut.toString(StandardCharsets.UTF_8));

		// Verify inPipe has NO injected bytes available
		assertEquals(0, inPipe.available());
	}

	@Test
	public void testSwitchableInputStreamMultiplexing() throws IOException {
		ByteArrayInputStream serverStream = new ByteArrayInputStream("Server Message\n".getBytes(StandardCharsets.UTF_8));
		inPipe.setSource(serverStream);

		// Inject local echo
		inPipe.inject("Local Echo\n".getBytes(StandardCharsets.UTF_8));

		// Local injected data must be read first
		byte[] buf = new byte[100];
		int n1 = inPipe.read(buf);
		assertEquals("Local Echo\n", new String(buf, 0, n1, StandardCharsets.UTF_8));

		// Then server data is read
		int n2 = inPipe.read(buf);
		assertEquals("Server Message\n", new String(buf, 0, n2, StandardCharsets.UTF_8));
	}
}
