package de.log4j.mudclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.prelle.ansi.ANSIInputStream;
import org.prelle.ansi.ANSIInputStreamFilter;
import org.prelle.ansi.AParsedElement;
import org.prelle.ansi.commands.SelectGraphicRendition;
import org.prelle.mxp.MXPInputStreamFilter;

/**
 * 
 */
class MXPInputStreamTest {
	
	private static class CSIFilter implements ANSIInputStreamFilter {
		
		private Consumer<AParsedElement> listener;
		public CSIFilter(Consumer<AParsedElement> listener) {
			this.listener = listener;
		}

		@Override
		public boolean handles(AParsedElement event) {
			return event instanceof SelectGraphicRendition;
		}

		@Override
		public List<AParsedElement> process(AParsedElement event) {
			listener.accept(event);
			return List.of();
		}
		
	}

	//-------------------------------------------------------------------
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	//-------------------------------------------------------------------
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
	}


	//-------------------------------------------------------------------
	/**
	 * When reading from a stream with an active MXPFilterInputStream,
	 * the first byte after returning to MXP locked mode get swallowed in the past.
	 * This test verifies that this does not happen.
	 */
	@Test
	public void testNoSwallowAfterMXP() throws IOException {
		byte[] buf = new byte[] {'H','e','l','l','o', 0x1b, '[', '1', 'z','<','B','>','W','o','r','l','d', (byte)0xff ,(byte)0xfb};
		ByteArrayInputStream bais = new ByteArrayInputStream(buf);

		MXPInputStreamFilter mxpFilter = new MXPInputStreamFilter(null);
		mxpFilter.setMXPActive(true);
		
		List<AParsedElement> parsedElements = new ArrayList<>();
		ANSIInputStream in = new ANSIInputStream(bais);
		in.addFilter(mxpFilter);
		in.addFilter(new CSIFilter( pe -> parsedElements.add(pe)));
		byte[] tmp  = new byte[100];
		
		var expect = "Hello";
		for (int i=0; i<5; i++) {
			int read = in.read(tmp);
			assertEquals(1, read);
			assertEquals(expect.charAt(i), tmp[0]);
		}
		in.close();
		assertEquals(1, parsedElements.size());
	}

}
