package org.prelle.realmrunner.web.views.play;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import com.flowingcode.vaadin.addons.xterm.ClientTerminalAddon;
import com.flowingcode.vaadin.addons.xterm.XTermBase;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

@NpmPackage(value = "@xterm/addon-image", version = "0.8.0")
@JsModule("./fc-xterm/xterm-image-addon.ts")
public class ImageAddOn extends ClientTerminalAddon {
	
	private final static Logger logger = System.getLogger("client.web");

  private final static String NAME = "addon-image";

  public ImageAddOn(XTermBase xterm) {
    super(xterm);
    logger.log(Level.ERROR, "ImageAddOn<init2>********************************************************");
    xterm.getElement().executeJs("Vaadin.Flow.fcXtermConnector.load_images($0, this)", NAME);
  }

  //just as an example on how to set a property on WebLinksAddon 
  public void setFoo(String foo) {
    executeJs("this.foo=$0;", foo);
  }

  @Override
  public String getName() {
    return NAME;
  }

}