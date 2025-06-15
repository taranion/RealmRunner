import { WebLinksAddon } from '@xterm/addon-web-links';
import { XTermElement } from './xterm-element';

(function () { 
  (window as any).Vaadin.Flow.fcXtermConnector = (window as any).Vaadin.Flow.fcXtermConnector || {};
  (window as any).Vaadin.Flow.fcXtermConnector.load_weblinks = (name:string, node: XTermElement) => {
    const addon = new WebLinksAddon();
    node.terminal.loadAddon(addon);
    node.addons[name]=addon;
  };
})();