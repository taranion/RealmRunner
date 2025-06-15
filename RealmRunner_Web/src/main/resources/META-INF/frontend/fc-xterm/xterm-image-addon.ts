import { ImageAddon, IImageAddonOptions } from '@xterm/addon-image';
import { XTermElement } from './xterm-element';

(function () { 
    console.debug("_____________HELLO____________________\r\n");
  (window as any).Vaadin.Flow.fcXtermConnector = (window as any).Vaadin.Flow.fcXtermConnector || {};
  (window as any).Vaadin.Flow.fcXtermConnector.load_images = (name:string, node: XTermElement) => {
    console.debug("_____________WORLD____________________\r\n");
    // customize as needed (showing addon defaults)
    const customSettings: IImageAddonOptions = {
      enableSizeReports: true,    // whether to enable CSI t reports (see below)
      pixelLimit: 16777216,       // max. pixel size of a single image
      sixelSupport: true,         // enable sixel support
      sixelScrolling: true,       // whether to scroll on image output
      sixelPaletteLimit: 256,     // initial sixel palette size
      sixelSizeLimit: 25000000,   // size limit of a single sixel sequence
      storageLimit: 128,          // FIFO storage limit in MB
      showPlaceholder: true,      // whether to show a placeholder for evicted images
      iipSupport: true,           // enable iTerm IIP support
      iipSizeLimit: 20000000      // size limit of a single IIP sequence
    }

    const addon = new ImageAddon(customSettings);
    node.terminal.loadAddon(addon);
    node.addons[name]=addon;
    console.debug("_____________Add on loaded\r\n");
  };
})();