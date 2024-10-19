package org.prelle.mudclient.jfx;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import org.prelle.mud.map.SymbolMap;
import org.prelle.mud.map.ViewportMap;
import org.prelle.mud.symbol.Symbol;
import org.prelle.mud.symbol.SymbolSet;
import org.prelle.mud.symbol.TileGraphicService;
import org.prelle.mud.symbol.jfx.JavaFXTileGraphicLoader;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 *
 */
public class MapView extends Canvas {

	private final static Logger logger = System.getLogger(MapView.class.getPackageName());

	private TileGraphicService service;
	private SymbolSet symbolSet;

	//-------------------------------------------------------------------
	public MapView(TileGraphicService service, SymbolSet set) {
		super(352,352);
		this.service = service;
		this.symbolSet = set;
		this.getGraphicsContext2D().fillText("Hallo", 40, 40);
//		symbolSet = new SymbolSet(-1);
//		Function<Integer,Integer> frames = (f) -> 1;
////		InputStream in = ClassLoader.getSystemResourceAsStream("static/symbols/SymbolSet_0");
//		InputStream in = ClassLoader.getSystemResourceAsStream("static/symbols/SymbolSet_0.jpg");
//		try {
////			OldFormatSymbolSetLoader.builder()
////				.build()
////				.load(symbolSet, in, 0, frames);
//			(new AWTImageSymbolSetLoader(32,32)).load(symbolSet, in, 0, frames);
//			System.out.println("Successfully loaded "+symbolSet.size()+" symbols");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	//-------------------------------------------------------------------
	public void setData(int[][] data) {
		System.out.println("Called MapView.setData");

		byte[] imgBytes = service.renderMap(new SymbolMap(data,symbolSet), symbolSet);
		logger.log(Level.INFO,"Read {0} bytes ",imgBytes.length);
		ByteArrayInputStream bins = new ByteArrayInputStream(imgBytes);
		Image img = new Image(bins);
		logger.log(Level.INFO,"Loaded image has "+img.getWidth()+"x"+img.getHeight()+"   error="+img.isError());
		if (img.isError()) {
			logger.log(Level.ERROR, "image loading had error  "+img.getException());
			System.exit(1);
		}
		GraphicsContext gc = this.getGraphicsContext2D();
		gc.drawImage(img, 0, 0);
		try {
			bins.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


//		GraphicsContext gc = this.getGraphicsContext2D();
//
//		for (int y=0; y<data.length; y++) {
//			for (int x=0; x<data[y].length; x++) {
//				int id = data[y][x];
//				AWTImageSymbol symbol = (AWTImageSymbol) symbolSet.getSymbol(id);
//				Image img = SwingFXUtils.toFXImage(symbol.getImageR(),null);
//				gc.drawImage(img, x*32, y*32, 32,32);
////				gc.fillText(""+id, x*32, y*32);
//			}
//		}
	}
}
