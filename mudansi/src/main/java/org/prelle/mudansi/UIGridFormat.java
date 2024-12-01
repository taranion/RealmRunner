package org.prelle.mudansi;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.prelle.ansi.ANSIOutputStream;
import org.prelle.ansi.commands.CursorPosition;
import org.prelle.ansi.commands.EraseCharacter;
import org.prelle.ansi.commands.EraseRectangularArea;
import org.prelle.ansi.commands.LeftRightMarginMode;
import org.prelle.ansi.commands.ModeState;
import org.prelle.ansi.commands.kitty.KittyGraphicsFragment;
import org.prelle.ansi.commands.kitty.KittyImageTransmission;
import org.prelle.ansi.control.AreaControls;
import org.prelle.ansi.control.CursorControls;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 *
 * +-----+----------------------------------------------+
 * | Map | Room Description                             |
 * |     |                                              |
 * +-----+----------------------------------------+-----+
 * |     |                                        |Right|
 * | Left|                                        |     |
 * |     |                                        |     |
 * +-----+----------------------------------------+-----+
 * |                                                    |
 * +----------------------------------------------------+
 *
 */
public class UIGridFormat implements BorderElements {

	private final static Logger logger = System.getLogger("mud.ansi");

	public final static String ID_SCROLL = "SCROLL";
	public final static String ID_INPUT = "INPUT";

	public static enum Area {
		TOP_LEFT(0),
		TOP(1),
		TOP_RIGHT(2),
		LEFT(3),
		CENTER(4),
		RIGHT(5),
		BOTTOM_LEFT(6),
		BOTTOM(7),
		BOTTOM_RIGHT(8),
		;
		int id;
		Area(int num) {
			id = num;
		}
		public static Area valueOf(int id) {
			for (Area tmp : Area.values()) {
				if (tmp.id==id) return tmp;
			}
			return null;
		}
	}

	private static enum Line {
		TL_TO_T,
		T_TO_TR,
		TL_TO_L,
		T_TO_C,
		TR_TO_R,
		L_TO_C,
		C_TO_R,
		L_TO_BL,
		C_TO_B,
		R_TO_BR,
		BL_TO_B,
		B_TO_BR
	}

	@AllArgsConstructor
	@ToString
	@Getter
	public static class AreaDefinition {
		int x;
		int y;
		int w;
		int h;
		public AreaDefinition setX(int val) { this.x = val; return this; }
		public AreaDefinition setY(int val) { this.y = val; return this; }
		public AreaDefinition setWidth(int val) { this.w = val; return this; }
		public AreaDefinition setHeight(int val) { this.h = val; return this; }
	}


	/** Screen width */
	private int width;
	/** Screen height */
	private int height;

	@Getter private int leftWidth;
	@Getter private int rightWidth;
	@Getter private int topHeight;
	@Getter private int bottomHeight;

	private int leftX;
	private int leftLineX;
	private int centerX;
	private int rightX;
	private int rightLineX;
	private int topY;
	private int topLineY;
	private int centerY;
	private int bottomLineY;
	private int bottomY;

	private boolean outerBorder;
	private ANSIOutputStream out;
	private boolean supportsRectangular;

	private Map<Area, AreaDefinition> unjoinedAreas = new HashMap<>();
	private List<Line> visibleLines = new ArrayList<>();
	private Map<String, AreaDefinition> namedAreas = new HashMap<>();
	private Map<String, Area[]> joinDefinitions = new HashMap<>();

	//-------------------------------------------------------------------
	public UIGridFormat(ANSIOutputStream out,  int width, int height, boolean supportsRectangular) {
		this.width = width;
		this.height= height;
		this.out   = out;
		this.supportsRectangular = supportsRectangular;

		for (Area area : Area.values()) {
			unjoinedAreas.put(area, new AreaDefinition(0, 0, 0, 0));
		}
		namedAreas.put(ID_SCROLL, new AreaDefinition(0, 0, 0, 0));
		recalculateSizes();
		recalculateAreas();
	}

	//-------------------------------------------------------------------
	/**
	 * Call this method after sizes have been changed
	 */
	private void recalculateSizes() {
		boolean hasTopLine = topHeight>0;
		boolean hasBottomLine = bottomHeight>0;
		boolean hasLeftCol    = leftWidth>0;
		boolean hasRightCol   = rightWidth>0;

		// X Positions
		int pos=1;
		if (outerBorder) pos++;
		leftX = pos;
		pos+=leftWidth;
		if (hasLeftCol)
			leftLineX=pos++;
		centerX = pos;

		pos=width+1;
		if (outerBorder) pos--;
		pos-=rightWidth;
		if (hasRightCol)
			rightX=pos--;
		rightLineX=pos;

		// Y Positions
		pos=1;
		if (outerBorder) pos++;
		topY = pos;
		pos+=topHeight;
		if (hasTopLine)
			topLineY=pos++;
		centerY = pos;

		pos=height+1;
		if (outerBorder) pos--;
		pos-=bottomHeight;
		if (hasBottomLine)
			bottomY=pos--;
		bottomLineY=pos;

		int centerWidth = rightLineX-centerX;
		int centerHeight= bottomLineY-centerY;

		// Line 1
		unjoinedAreas.get(Area.TOP_LEFT)
			.setX( leftX )
			.setY( topY )
			.setWidth(leftWidth)
			.setHeight(topHeight);
		unjoinedAreas.get(Area.TOP)
			.setX( centerX )
			.setY( topY )
			.setWidth( centerWidth)
			.setHeight(topHeight);
		unjoinedAreas.get(Area.TOP_RIGHT)
			.setX( rightX )
			.setY( topY )
			.setWidth(rightWidth)
			.setHeight(topHeight);

		// Line 2
		unjoinedAreas.get(Area.LEFT)
			.setX( leftX )
			.setY( centerY )
			.setWidth(leftWidth)
			.setHeight(topHeight);
		unjoinedAreas.get(Area.CENTER)
			.setX( centerX )
			.setY( centerY )
			.setWidth(centerWidth)
			.setHeight(centerHeight);
		unjoinedAreas.get(Area.RIGHT)
			.setX( rightX )
			.setY( centerY )
			.setWidth(rightWidth)
			.setHeight(centerHeight);

		// Line 3
		unjoinedAreas.get(Area.BOTTOM_LEFT)
			.setX( leftX )
			.setY( bottomY )
			.setWidth(leftWidth)
			.setHeight(bottomHeight);
		unjoinedAreas.get(Area.BOTTOM)
			.setX( centerX )
			.setY( bottomY )
			.setWidth( centerWidth)
			.setHeight(bottomHeight);
		unjoinedAreas.get(Area.BOTTOM_RIGHT)
			.setX( rightX )
			.setY( bottomY )
			.setWidth(rightWidth)
			.setHeight(bottomHeight);
		
		// Update already existing joined areas
		for (String id : joinDefinitions.keySet()) {
			Area[] toJoin = joinDefinitions.get(id);
			AreaDefinition joinedArea = namedAreas.get(id);
			logger.log(Level.DEBUG, "Update {0}:{1} with updated {2}", id, joinedArea, Arrays.toString(toJoin));
			// TODO create AreaDefinition
			AreaDefinition lowestX = null;
			AreaDefinition highestX = null;
			AreaDefinition lowestY = null;
			AreaDefinition highestY = null;
			for (Area key : toJoin) {
				AreaDefinition def = unjoinedAreas.get(key);
				if (def.x>0 && (lowestX==null || def.x<lowestX.x)) lowestX=def;
				if (highestX==null || def.x>highestX.x) highestX=def;
				if (def.y>0 && (lowestY==null || def.y<lowestX.y)) lowestY=def;
				if (highestY==null || def.x>highestY.y) highestY=def;
				namedAreas.remove(key.name());
			}
			int endX = highestX.x+highestX.w;
			int endY = highestY.y+highestY.h;
			joinedArea.setX(lowestX.x);
			joinedArea.setY(lowestY.y);
			joinedArea.setWidth(endX-lowestX.x);
			joinedArea.setHeight(endY-lowestY.y);
		}
	}

	public UIGridFormat setOuterBorder(boolean value) { this.outerBorder=value; recalculateSizes(); return this; }
	public UIGridFormat setLeftWidth(int value) { this.leftWidth=value; recalculateSizes(); recalculateAreas(); return this; }
	public UIGridFormat setRightWidth(int value) { this.rightWidth=value; recalculateSizes(); recalculateAreas(); return this; }
	public UIGridFormat setTopHeight(int value) { this.topHeight=value; recalculateSizes(); recalculateAreas(); return this; }
	public UIGridFormat setBottomHeight(int value) { this.bottomHeight=value; recalculateSizes(); recalculateAreas(); return this; }

	public boolean hasOuterBorder() { return this.outerBorder; }
	
	//-------------------------------------------------------------------
	private void recalculateAreas() {
		namedAreas.clear();
		namedAreas.put(ID_SCROLL, unjoinedAreas.get(Area.CENTER));
		if (leftWidth>0) namedAreas.put(Area.LEFT.name(), unjoinedAreas.get(Area.LEFT));
		if (rightWidth>0) namedAreas.put(Area.RIGHT.name(), unjoinedAreas.get(Area.RIGHT));
		if (topHeight>0) namedAreas.put(Area.TOP.name(), unjoinedAreas.get(Area.TOP));
		if (bottomHeight>0) namedAreas.put(Area.BOTTOM.name(), unjoinedAreas.get(Area.BOTTOM));
		if (leftWidth>0 && topHeight>0) namedAreas.put(Area.TOP_LEFT.name(), unjoinedAreas.get(Area.TOP_LEFT));
		if (leftWidth>0 && bottomHeight>0) namedAreas.put(Area.BOTTOM_LEFT.name(), unjoinedAreas.get(Area.BOTTOM_LEFT));
		if (rightWidth>0 && topHeight>0) namedAreas.put(Area.TOP_RIGHT.name(), unjoinedAreas.get(Area.TOP_RIGHT));
		if (rightWidth>0 && bottomHeight>0) namedAreas.put(Area.BOTTOM_RIGHT.name(), unjoinedAreas.get(Area.BOTTOM_RIGHT));

		recalculateLines();
	}

	//-------------------------------------------------------------------
	private void recalculateLines() {
		visibleLines.clear();
		for (Line key : Line.values())
			visibleLines.add(key);

		if (leftWidth==0) {
			visibleLines.remove(Line.TL_TO_T);
			visibleLines.remove(Line.TL_TO_L);
			visibleLines.remove(Line.L_TO_C);
			visibleLines.remove(Line.L_TO_BL);
			visibleLines.remove(Line.BL_TO_B);
		}
		if (rightWidth==0) {
			visibleLines.remove(Line.T_TO_TR);
			visibleLines.remove(Line.TR_TO_R);
			visibleLines.remove(Line.C_TO_R);
			visibleLines.remove(Line.R_TO_BR);
			visibleLines.remove(Line.B_TO_BR);
		}
		if (topHeight==0) {
			visibleLines.remove(Line.TL_TO_T);
			visibleLines.remove(Line.T_TO_TR);
			visibleLines.remove(Line.T_TO_C);
			visibleLines.remove(Line.T_TO_TR);
			visibleLines.remove(Line.TR_TO_R);
		}
		if (bottomHeight==0) {
			visibleLines.remove(Line.L_TO_BL);
			visibleLines.remove(Line.BL_TO_B);
			visibleLines.remove(Line.C_TO_B);
			visibleLines.remove(Line.B_TO_BR);
			visibleLines.remove(Line.R_TO_BR);
		}
	}

	//-------------------------------------------------------------------
	public String dump() {
		StringBuffer buf = new StringBuffer();
		buf.append("Size   : Width="+width+", height ="+height+"\n");
		buf.append("Columns: Left="+leftWidth+", right ="+rightWidth+"\n");
		buf.append("Lines  : Top ="+topHeight+", bottom="+bottomHeight+"\n");
		buf.append("Outer Border = "+outerBorder+"\n");
		for (Area key : Area.values()) {
			buf.append(String.format("%12s : %s\n", key, unjoinedAreas.get(key)));
		}

		buf.append("\nVisible Lines: "+visibleLines+"\n");
		buf.append("\nVisible areas\n");
		for (String key : namedAreas.keySet()) {
			buf.append(String.format("%12s : %s\n", key, namedAreas.get(key)));
		}
		return buf.toString();
	}

	//-------------------------------------------------------------------
	public void resetJoinedAreas() {
		unjoinedAreas.clear();
		namedAreas.clear();

		boolean hasTopLine = topHeight>0;
		boolean hasBottomLine = bottomHeight>0;
		boolean hasLeftCol    = leftWidth>0;
		boolean hasRightCol   = rightWidth>0;

		int centerWidth = width-2 -leftWidth -(hasLeftCol?1:0) -rightWidth -(hasRightCol?1:0);

		unjoinedAreas.put(Area.TOP_LEFT, new AreaDefinition(2, 2, leftWidth, topHeight));
		unjoinedAreas.put(Area.TOP, new AreaDefinition(leftWidth+2+((hasLeftCol)?1:0), 2, centerWidth, topHeight));
		unjoinedAreas.put(Area.TOP_RIGHT, new AreaDefinition(width-2-rightWidth,2, rightWidth, topHeight));

		unjoinedAreas.put(Area.LEFT  , new AreaDefinition(2, topHeight+2, leftWidth, topHeight));
		unjoinedAreas.put(Area.CENTER, new AreaDefinition(3+leftWidth, 2, width-2-rightWidth, topHeight));
		unjoinedAreas.put(Area.RIGHT , new AreaDefinition(width-2-rightWidth,2, rightWidth, topHeight));
	}

	//-------------------------------------------------------------------
	/**
	 * Join multiple areas to a named area.
	 * @param identifier
	 * @param areas
	 */
	public void join(String identifier, Area...areas) {
		List<Integer> ids = List.of(areas).stream().map(a -> a.id).toList();
		// Mark all "lines" and "columns" used
		List<Integer> lines = ids.stream().map(i -> i/3).toList();
		List<Integer> columns= ids.stream().map(i -> i%3).toList();
		// Validate that combination of areas makes sense
		List<Integer> required = new ArrayList<>();
		for (int l : lines) {
			for (int c : columns) {
				required.add(l*3+c);
			}
		}
		// Validate that all needed areas should be joined
		List<Integer> missing = required.stream().filter(r -> !ids.contains(r)).toList();
		if (!missing.isEmpty())
			throw new IllegalArgumentException("Missing "+missing.stream().map(m -> Area.valueOf(m)).toList());

		if (!ids.contains(Area.CENTER.id) && ID_SCROLL.equals(identifier))
			throw new IllegalArgumentException("All areas which include CENTER must use identifier "+ID_SCROLL);

		// TODO create AreaDefinition
		AreaDefinition lowestX = null;
		AreaDefinition highestX = null;
		AreaDefinition lowestY = null;
		AreaDefinition highestY = null;
		for (Area key : areas) {
			AreaDefinition def = unjoinedAreas.get(key);
			if (def.x>0 && (lowestX==null || def.x<lowestX.x)) lowestX=def;
			if (highestX==null || def.x>highestX.x) highestX=def;
			if (def.y>0 && (lowestY==null || def.y<lowestX.y)) lowestY=def;
			if (highestY==null || def.x>highestY.y) highestY=def;
			namedAreas.remove(key.name());
		}
		int endX = highestX.x+highestX.w;
		int endY = highestY.y+highestY.h;
		AreaDefinition joined = new AreaDefinition(lowestX.x, lowestY.y, endX-lowestX.x, endY-lowestY.y);
		namedAreas.put(identifier, joined);
		joinDefinitions.put(identifier, areas);

		removeLinesBetween(List.of(areas));

	}

	//-------------------------------------------------------------------
	private void removeLinesBetween(List<Area> areas) {
		if (areas.contains(Area.TOP_LEFT) && areas.contains(Area.TOP)) visibleLines.remove(Line.TL_TO_T);
		if (areas.contains(Area.LEFT) && areas.contains(Area.CENTER)) visibleLines.remove(Line.L_TO_C);
		if (areas.contains(Area.BOTTOM_LEFT) && areas.contains(Area.BOTTOM)) visibleLines.remove(Line.BL_TO_B);

		if (areas.contains(Area.TOP) && areas.contains(Area.TOP_RIGHT)) visibleLines.remove(Line.T_TO_TR);
		if (areas.contains(Area.CENTER) && areas.contains(Area.RIGHT)) visibleLines.remove(Line.C_TO_R);
		if (areas.contains(Area.BOTTOM) && areas.contains(Area.BOTTOM_RIGHT)) visibleLines.remove(Line.B_TO_BR);

		if (areas.contains(Area.TOP_LEFT) && areas.contains(Area.LEFT)) visibleLines.remove(Line.TL_TO_L);
		if (areas.contains(Area.TOP) && areas.contains(Area.CENTER)) visibleLines.remove(Line.T_TO_C);
		if (areas.contains(Area.TOP_RIGHT) && areas.contains(Area.RIGHT)) visibleLines.remove(Line.TR_TO_R);

		if (areas.contains(Area.LEFT) && areas.contains(Area.BOTTOM_LEFT)) visibleLines.remove(Line.L_TO_BL);
		if (areas.contains(Area.CENTER) && areas.contains(Area.BOTTOM)) visibleLines.remove(Line.C_TO_B);
		if (areas.contains(Area.RIGHT) && areas.contains(Area.BOTTOM_RIGHT)) visibleLines.remove(Line.R_TO_BR);
	}

	//-------------------------------------------------------------------
	public void recreate(Charset charset) throws IOException {
		logger.log(Level.DEBUG,"UIGridFormat.recreate: Charset is {0}",charset);
		AreaControls.clearScreen(out);
		out.reset();
		
		char[] border = BorderElements.ASCII.toCharArray();
		if (charset==StandardCharsets.UTF_8) {
			border = BorderElements.UTF_8.toCharArray();
		}
		
//		ANSIOutputStream out = new ANSIOutputStream(System.out);
		/*
		 * Create outer border if necessary
		 */
		if (outerBorder) {
			CursorControls.setCursorPosition(out, 1, 1);
			StringBuffer line = new StringBuffer();
			// Top
			line.append(String.valueOf(border[CORNER_TL]));
			line.append(String.valueOf(border[OUTER_HORI]).repeat(width-2));
			line.append(String.valueOf(border[CORNER_TR]));
			out.write(line.toString());
			line.delete(0, line.length());
			// Middle
			for (int i=0; i<height-2; i++) {
				out.write(new CursorPosition(1, 2+i));
				out.write(String.valueOf(border[OUTER_VERT]));
				out.write(new CursorPosition(width, 2+i));
				out.write(String.valueOf(border[OUTER_VERT]));
			}
			// Bottom
			line.append(String.valueOf(border[CORNER_BL]));
			line.append(String.valueOf(border[OUTER_HORI]).repeat(width-2));
			line.append(String.valueOf(border[CORNER_BR]));
			out.write(line.toString());
			line.delete(0, line.length());
		}
		// Color
		for (Entry<String,AreaDefinition> entry : namedAreas.entrySet()) {
			AreaDefinition def = entry.getValue();
			AreaControls.fillArea(out, ' ', def.x, def.y, def.w-1, def.h-1);
			if (def.x>2) {
				for (int y=def.y; y<(def.y+def.h); y++) {
					CursorControls.setCursorPosition(out, def.x-1, y);
					out.write(String.valueOf(border[INNER_VERT]));
				}
			}
			if (def.y>2) {
				CursorControls.setCursorPosition(out, def.x, def.y-1);
				out.write(String.valueOf(border[INNER_HORI]).repeat(def.w));
			}
		}

		// Draw outer connects
		AreaDefinition center = namedAreas.get(ID_SCROLL);
		if (outerBorder) {
			// Top connects
			if (visibleLines.contains(Line.TL_TO_T) || (topHeight==0 && visibleLines.contains(Line.L_TO_C))) {
				CursorControls.setCursorPosition(out, leftLineX, 1);
				out.write(String.valueOf(border[TOP_HORI_INNER_VERT]));
			}
			if (visibleLines.contains(Line.T_TO_TR) || (topHeight==0 && visibleLines.contains(Line.C_TO_R))) {
				CursorControls.setCursorPosition(out, rightLineX, 1);
				out.write(String.valueOf(border[TOP_HORI_INNER_VERT]));
			}
			// Bottom connects
			if (visibleLines.contains(Line.BL_TO_B) || (bottomHeight==0 && visibleLines.contains(Line.C_TO_R))) {
				CursorControls.setCursorPosition(out, leftLineX, height);
				out.write(String.valueOf(border[BOT_HORI_INNER_VERT]));
			}
			if (visibleLines.contains(Line.B_TO_BR) || (bottomHeight==0 && visibleLines.contains(Line.B_TO_BR))) {
				CursorControls.setCursorPosition(out, rightLineX, height);
				out.write(String.valueOf(border[BOT_HORI_INNER_VERT]));
			}
			// Left connects
			if (visibleLines.contains(Line.TL_TO_L) || (leftWidth==0 && visibleLines.contains(Line.T_TO_C))) {
				CursorControls.setCursorPosition(out, 1, topLineY);
				out.write(String.valueOf(border[LEF_VERT_INNER_HORI]));
			}
			if (visibleLines.contains(Line.L_TO_BL) || (leftWidth==0 && visibleLines.contains(Line.C_TO_B))) {
				CursorControls.setCursorPosition(out, 1, bottomLineY);
				out.write(String.valueOf(border[LEF_VERT_INNER_HORI]));
			}
			// Right connects
			if (visibleLines.contains(Line.TR_TO_R) || (rightWidth==0 && visibleLines.contains(Line.T_TO_C))) {
				CursorControls.setCursorPosition(out, width, topLineY);
				out.write(String.valueOf(border[RGT_VERT_INNER_HORI]));
			}
			if (visibleLines.contains(Line.R_TO_BR) || (rightWidth==0 && visibleLines.contains(Line.C_TO_B))) {
				CursorControls.setCursorPosition(out, width, bottomLineY);
				out.write(String.valueOf(border[RGT_VERT_INNER_HORI]));
			}
		}

		/*
		 * Connection points
		 */
		// TOP LEFT
		CursorControls.setCursorPosition(out, leftLineX, topLineY);
		if (visibleLines.containsAll(List.of(Line.TL_TO_T,Line.TL_TO_L,Line.T_TO_C, Line.L_TO_C))) {
			out.write(String.valueOf(border[INNER_CROSS]));
		} else if (visibleLines.containsAll(List.of(Line.TL_TO_T,Line.TL_TO_L,Line.T_TO_C, Line.L_TO_C))) {
			out.write(String.valueOf(border[INNER_HORI_VERT_UP]));
		} else if (visibleLines.containsAll(List.of(Line.TL_TO_T,Line.TL_TO_L,Line.L_TO_C))) {
			out.write(String.valueOf(border[INNER_VERT_HORI_LEFT]));
		} else if (visibleLines.containsAll(List.of(Line.TL_TO_T,Line.T_TO_C, Line.L_TO_C))) {
			out.write(String.valueOf(border[INNER_VERT_HORI_RIGHT]));
		} else if (visibleLines.containsAll(List.of(Line.TL_TO_L,Line.T_TO_C, Line.L_TO_C))) {
			out.write(String.valueOf(border[INNER_HORI_VERT_DOWN]));
		}
		// TOP RIGHT
		CursorControls.setCursorPosition(out, rightLineX, topLineY);
		if (visibleLines.containsAll(List.of(Line.T_TO_TR,Line.TR_TO_R,Line.C_TO_R, Line.T_TO_C))) {
			out.write(String.valueOf(border[INNER_CROSS]));
		} else if (visibleLines.containsAll(List.of(Line.T_TO_TR,Line.TR_TO_R,Line.T_TO_C))) {
			out.write(String.valueOf(border[INNER_HORI_VERT_UP]));
		} else if (visibleLines.containsAll(List.of(Line.T_TO_TR,Line.C_TO_R, Line.T_TO_C))) {
			out.write(String.valueOf(border[INNER_VERT_HORI_LEFT]));
		} else if (visibleLines.containsAll(List.of(Line.T_TO_TR,Line.TR_TO_R,Line.C_TO_R))) {
			out.write(String.valueOf(border[INNER_VERT_HORI_RIGHT]));
		} else if (visibleLines.containsAll(List.of(Line.TR_TO_R,Line.C_TO_R, Line.T_TO_C))) {
			out.write(String.valueOf(border[INNER_HORI_VERT_DOWN]));
		}
		// BOTTOM LEFT
		CursorControls.setCursorPosition(out, leftLineX, bottomLineY);
		if (visibleLines.containsAll(List.of(Line.BL_TO_B,Line.L_TO_BL,Line.C_TO_B, Line.L_TO_C))) {
			out.write(String.valueOf(border[INNER_CROSS]));
		} else if (visibleLines.containsAll(List.of(Line.L_TO_BL,Line.C_TO_B, Line.L_TO_C))) {
			out.write(String.valueOf(border[INNER_HORI_VERT_UP]));
		} else if (visibleLines.containsAll(List.of(Line.BL_TO_B,Line.L_TO_BL,Line.L_TO_C))) {
			out.write(String.valueOf(border[INNER_VERT_HORI_LEFT]));
		} else if (visibleLines.containsAll(List.of(Line.BL_TO_B,Line.C_TO_B, Line.L_TO_C))) {
			out.write(String.valueOf(border[INNER_VERT_HORI_RIGHT]));
		} else if (visibleLines.containsAll(List.of(Line.BL_TO_B,Line.L_TO_BL,Line.C_TO_B))) {
			out.write(String.valueOf(border[INNER_HORI_VERT_DOWN]));
		}
		// BOTTOM RIGHT
		CursorControls.setCursorPosition(out, rightLineX, bottomLineY);
		if (visibleLines.containsAll(List.of(Line.C_TO_B,Line.C_TO_R,Line.R_TO_BR, Line.B_TO_BR))) {
			out.write(String.valueOf(border[INNER_CROSS]));
		} else if (visibleLines.containsAll(List.of(Line.C_TO_B,Line.C_TO_R,Line.R_TO_BR))) {
			out.write(String.valueOf(border[INNER_HORI_VERT_UP]));
		} else if (visibleLines.containsAll(List.of(Line.C_TO_B,Line.C_TO_R,Line.B_TO_BR))) {
			out.write(String.valueOf(border[INNER_VERT_HORI_LEFT]));
		} else if (visibleLines.containsAll(List.of(Line.C_TO_R,Line.R_TO_BR, Line.B_TO_BR))) {
			out.write(String.valueOf(border[INNER_VERT_HORI_RIGHT]));
		} else if (visibleLines.containsAll(List.of(Line.C_TO_B,Line.R_TO_BR, Line.B_TO_BR))) {
			out.write(String.valueOf(border[INNER_HORI_VERT_DOWN]));
		}

		out.write(new LeftRightMarginMode(ModeState.SET));
		AreaControls.setTopAndBottomMargins(out, centerY, bottomLineY-1);
		AreaControls.setLeftAndRightMargins(out, centerX, rightLineX-1);


		logger.log(Level.INFO, "Set cursor at {0}", center);
		AreaDefinition input = namedAreas.get(ID_SCROLL);
		CursorControls.setCursorPosition(out, input.x, input.y);
		out.flush();
	}

	//-------------------------------------------------------------------
	public void clear(AreaDefinition def) throws IOException {
		if (supportsRectangular) {
			out.write(new EraseRectangularArea(def.y, def.x, def.y+def.h, def.x+def.w));
		} else {
			for (int y=0; y<def.h; y++) {
				CursorControls.setCursorPosition(out, def.x, def.y+y);
				out.write(new EraseCharacter(def.w));
			}
		}
		
	}

	//-------------------------------------------------------------------
	public void showMarkupIn(String id, String markup, boolean memorizeCursor) throws IOException {
		List<MarkupElement> elements = MarkupParser.convertText(markup);
		AreaDefinition def = namedAreas.get(id);
		if (def==null)
			throw new NoSuchElementException("Unknown area '"+id+"' - valid are "+namedAreas.keySet());
		int areaWidth = def.w;
		List<String> lines = FormatUtil.convertText(elements, areaWidth);
		logger.log(Level.DEBUG, "Show {0} lines in {1} = {2}", lines.size(), id, def);

		// Clear current content
		if (memorizeCursor)
			CursorControls.savePositionDEC(out);
		clear(def);
		int count=0;
		for (String line : lines) {
			CursorControls.setCursorPosition(out, def.x, def.y+count);
			out.write(line);
			count++;
		}
		if (memorizeCursor)
			CursorControls.restorePositionDEC(out);
	}

	//-------------------------------------------------------------------
	public void showRawIn(String id, List<String> lines) throws IOException {
		AreaDefinition def = namedAreas.get(id);
		if (def==null)
			throw new NoSuchElementException("Unknown area '"+id+"' - valid are "+namedAreas.keySet());
		showRawIn(def, lines);
	}

	//-------------------------------------------------------------------
	public void showRawIn(AreaDefinition def, List<String> lines) throws IOException {
		logger.log(Level.INFO, "ENTER: showRawIn({1}, {0} lines)", lines.size(), def);

		// Clear current content
		CursorControls.savePositionDEC(out);
		clear(def);
		CursorControls.setCursorPosition(out, def.x, def.y);
		out.write(new EraseCharacter(def.w));
		int count=0;
		for (String line : lines) {
			CursorControls.setCursorPosition(out, def.x, def.y+count);
			out.write(line);
			out.write("\u001b[0m");
			count++;
		}
		CursorControls.restorePositionDEC(out);
		logger.log(Level.INFO, "LEAVE: showRawIn()");
	}

	//-------------------------------------------------------------------
	private static List<String> splitIntoKittyChunks(byte[] imgData) {
		String encoded = Base64.getEncoder().encodeToString(imgData);
		int offset=0;
		List<String> ret = new ArrayList<>();
		do {
			int to = Math.min(offset+4096, encoded.length());
			ret.add( encoded.substring(offset,to) );
			offset += 4096;
		} while (offset<encoded.length());
		return ret;
	}


	//-------------------------------------------------------------------
	public void sendKittyImage(String id, byte[] data) throws IOException {
		AreaDefinition def = namedAreas.get(id);
		if (def==null)
			throw new NoSuchElementException("Unknown area '"+id+"' - valid are "+namedAreas.keySet());
		boolean isFirst = true;
		CursorControls.savePositionDEC(out);
		CursorControls.setCursorPosition(out, def.x, def.y);
		for (Iterator<String> it = splitIntoKittyChunks(data).iterator(); it.hasNext(); ) {
			String chunk = it.next();
			KittyImageTransmission kitty = new KittyImageTransmission();
			kitty.setPayload(chunk);
			if (isFirst) {
				kitty.set('a',KittyGraphicsFragment.ACION_TRANSMIT_AND_DISPLAY);
				kitty.setFormat(KittyImageTransmission.FORMAT_PNG);
				kitty.set(KittyImageTransmission.KEY_ID, 33);
				kitty.setMedium(KittyImageTransmission.MEDIUM_DIRECT);
				kitty.set('c',def.w);
				kitty.set('r',def.h);
				isFirst=false;
			}
			kitty.setMoreChunksFollow(it.hasNext());
			logger.log(Level.DEBUG, "Send Kitty PNG APC");
			out.write(kitty);
		}
		CursorControls.restorePositionDEC(out);

	}

	//-------------------------------------------------------------------
	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
		recalculateSizes();
		recalculateAreas();
	}

	//-------------------------------------------------------------------
	public AreaDefinition getArea(String id) {
		return namedAreas.get(id);
	}
	
}
