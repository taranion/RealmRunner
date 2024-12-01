package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.prelle.ansi.ANSIOutputStream;
import org.prelle.mudansi.UIGridFormat;
import org.prelle.mudansi.UIGridFormat.Area;
import org.prelle.mudansi.UIGridFormat.AreaDefinition;

/**
 *
 */
public class UIFormatTest {

	//-------------------------------------------------------------------
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		UIGridFormat format = new UIGridFormat(new ANSIOutputStream(System.out), 100, 40, true);
//		format.setOuterBorder(true);
//		format.setBottomHeight(1);
		format.setTopHeight(11);
		format.setLeftWidth(22);
//		format.join(UIGridFormat.ID_INPUT, Area.BOTTOM_LEFT, Area.BOTTOM, Area.BOTTOM_RIGHT);
		format.join("RoomDesc", Area.TOP, Area.TOP_RIGHT);
		format.join(UIGridFormat.ID_SCROLL, Area.LEFT, Area.CENTER, Area.RIGHT);

//		format.recreate();
		System.out.println(format.dump());
		
		AreaDefinition def =format.getArea("TOP_LEFT");

	}

	//-------------------------------------------------------------------
	@Test
	public void testTopCenter() throws IOException {
		UIGridFormat format = new UIGridFormat(new ANSIOutputStream(System.out), 100, 40, true);
		format.setTopHeight(11);
		format.setLeftWidth(22);
		format.join("RoomDesc", Area.TOP, Area.TOP_RIGHT);
		format.join(UIGridFormat.ID_SCROLL, Area.LEFT, Area.CENTER, Area.RIGHT);

//		format.recreate();
		
		AreaDefinition topLeft =format.getArea("TOP_LEFT");
		assertNotNull(topLeft);
		AreaDefinition roomDesc =format.getArea("RoomDesc");
		assertNotNull(roomDesc);
		AreaDefinition scroll =format.getArea(UIGridFormat.ID_SCROLL);
		assertNotNull(scroll);
		assertNull(format.getArea(Area.TOP.name()));
		assertNull(format.getArea(Area.TOP_RIGHT.name()));
		assertNull(format.getArea(Area.LEFT.name()));
		assertNull(format.getArea(Area.CENTER.name()));
		assertNull(format.getArea(Area.RIGHT.name()));
		assertNull(format.getArea(Area.BOTTOM_LEFT.name()));
		assertNull(format.getArea(Area.BOTTOM.name()));
		assertNull(format.getArea(Area.BOTTOM_RIGHT.name()));

		// Top Left
		assertEquals(1, topLeft.getX());
		assertEquals(1, topLeft.getY());
		assertEquals(22, topLeft.getW());
		assertEquals(11, topLeft.getH());

		// Room Desc
		assertEquals(24, roomDesc.getX());
		assertEquals(1, roomDesc.getY());
		assertEquals(77, roomDesc.getW());
		assertEquals(11, roomDesc.getH());

		// Scroll
		assertEquals(1, scroll.getX());
		assertEquals(13, scroll.getY());
		assertEquals(100, scroll.getW());
		assertEquals(28, scroll.getH());
		
		format.setOuterBorder(true);
		System.out.println(format.dump());
		assertNull(format.getArea(Area.TOP.name()));
		assertNull(format.getArea(Area.TOP_RIGHT.name()));
		assertNull(format.getArea(Area.LEFT.name()));
		assertNull(format.getArea(Area.CENTER.name()));
		assertNull(format.getArea(Area.RIGHT.name()));
		assertNull(format.getArea(Area.BOTTOM_LEFT.name()));
		assertNull(format.getArea(Area.BOTTOM.name()));
		assertNull(format.getArea(Area.BOTTOM_RIGHT.name()));
		// Top Left
		assertEquals(2, topLeft.getX());
		assertEquals(2, topLeft.getY());
		assertEquals(22, topLeft.getW());
		assertEquals(11, topLeft.getH());

		// Room Desc
		assertEquals(25, roomDesc.getX());
		assertEquals(2, roomDesc.getY());
		assertEquals(75, roomDesc.getW());
		assertEquals(11, roomDesc.getH());

		// Scroll
		assertEquals(2, scroll.getX());
		assertEquals(14, scroll.getY());
		assertEquals(98, scroll.getW());
		assertEquals(26, scroll.getH());
	}

	//-------------------------------------------------------------------
	@Test
	public void testTopCenterBottom() throws IOException {
		UIGridFormat format = new UIGridFormat(new ANSIOutputStream(System.out), 100, 40, true);
		format.setTopHeight(11);
		format.setLeftWidth(22);
		format.setBottomHeight(1);
		format.join("RoomDesc", Area.TOP, Area.TOP_RIGHT);
		format.join(UIGridFormat.ID_SCROLL, Area.LEFT, Area.CENTER, Area.RIGHT);
		format.join(UIGridFormat.ID_INPUT, Area.BOTTOM, Area.BOTTOM_LEFT, Area.BOTTOM_RIGHT);

//		format.recreate();
		//format.setOuterBorder(true);
		System.out.println(format.dump());
		
		AreaDefinition topLeft =format.getArea("TOP_LEFT");
		assertNotNull(topLeft);
		AreaDefinition roomDesc =format.getArea("RoomDesc");
		assertNotNull(roomDesc);
		AreaDefinition scroll =format.getArea(UIGridFormat.ID_SCROLL);
		assertNotNull(scroll);
		AreaDefinition input =format.getArea(UIGridFormat.ID_INPUT);
		assertNotNull(input);
		assertNull(format.getArea(Area.TOP.name()));
		assertNull(format.getArea(Area.TOP_RIGHT.name()));
		assertNull(format.getArea(Area.LEFT.name()));
		assertNull(format.getArea(Area.CENTER.name()));
		assertNull(format.getArea(Area.RIGHT.name()));
		assertNull(format.getArea(Area.BOTTOM_LEFT.name()));
		assertNull(format.getArea(Area.BOTTOM.name()));
		assertNull(format.getArea(Area.BOTTOM_RIGHT.name()));

		// Top Left
		assertEquals(1, topLeft.getX());
		assertEquals(1, topLeft.getY());
		assertEquals(22, topLeft.getW());
		assertEquals(11, topLeft.getH());

		// Room Desc
		assertEquals(24, roomDesc.getX());
		assertEquals(1, roomDesc.getY());
		assertEquals(77, roomDesc.getW());
		assertEquals(11, roomDesc.getH());

		// Scroll
		assertEquals(1, scroll.getX());
		assertEquals(13, scroll.getY());
		assertEquals(100, scroll.getW());
		assertEquals(26, scroll.getH());
		
	}

}
