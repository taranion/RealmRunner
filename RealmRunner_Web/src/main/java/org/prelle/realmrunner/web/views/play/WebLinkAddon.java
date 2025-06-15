package org.prelle.realmrunner.web.views.play;

import com.flowingcode.vaadin.addons.xterm.ClientTerminalAddon;
import com.flowingcode.vaadin.addons.xterm.XTermBase;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

@NpmPackage(value = "@xterm/addon-web-links", version = "0.11.0")
@JsModule("./fc-xterm/xterm-weblinks-addon.ts")
public class WebLinkAddon extends ClientTerminalAddon {

  private final static String NAME = "addon-web-links";

  public WebLinkAddon(XTermBase xterm) {
    super(xterm);
    xterm.getElement().executeJs("Vaadin.Flow.fcXtermConnector.load_weblinks($0, this)", NAME);
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