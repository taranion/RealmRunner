package org.prelle.realmrunner.web.views.play;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.prelle.realmrunner.web.data.AudioPlayer;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.flowingcode.vaadin.addons.xterm.ITerminalClipboard.UseSystemClipboard;
import com.flowingcode.vaadin.addons.xterm.ITerminalOptions.CursorStyle;
import com.flowingcode.vaadin.addons.xterm.XTerm;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;

import jakarta.annotation.security.PermitAll;

@PageTitle("Play")
@Route("play")
@Menu(order = 1, icon = LineAwesomeIconUrl.PENCIL_RULER_SOLID)
@PermitAll
@AnonymousAllowed
public class PlayView extends Composite<VerticalLayout> {
	
	private WebTerminal xterm;
	private MessageInput messageInput;

    //-------------------------------------------------------------------
    public PlayView() {
    	initComponents();
    	xterm.writeln("Hello world.\n\n");
    	xterm.setCursorBlink(true);
    	xterm.setCursorStyle(CursorStyle.UNDERLINE);
    	    	
    	 
        HorizontalLayout layoutRow = new HorizontalLayout();
        VerticalLayout layoutColumn2 = new VerticalLayout();
        VerticalLayout layoutColumn3 = new VerticalLayout();
        VerticalLayout layoutColumn4 = new VerticalLayout();
        HorizontalLayout layoutRow2 = new HorizontalLayout();
        //getContent().getStyle().set("flex-grow", "1");
        layoutRow.addClassName(Gap.MEDIUM);
        layoutRow.setWidth("100%");
        layoutRow.getStyle().set("flex-grow", "1");
        layoutColumn2.getStyle().set("flex-grow", "1");
        layoutColumn3.setWidth("100%");
        layoutColumn3.getStyle().set("flex-grow", "1");
        layoutColumn4.getStyle().set("flex-grow", "1");
        layoutRow2.addClassName(Gap.MEDIUM);
        layoutRow2.setWidth("100%");
        layoutRow2.setHeight("min-content");
        getContent().add(layoutRow);
        layoutRow.add(layoutColumn2);
        layoutRow.add(layoutColumn3);
        layoutRow.add(layoutColumn4);
        getContent().add(layoutRow2);
        layoutRow2.add(messageInput);
        
        initLayout();
        initInteractivity();
        
        AudioPlayer player=new AudioPlayer();
        player.getElement().setAttribute("autoplay",true);
        player.getElement().setAttribute("loop",true);
        player.getElement().setAttribute("preload",true);
        
        String soundfile = "src/main/resources/file_example_MP3_700KB.mp3";
        StreamResource stream = new StreamResource("foo", () -> {
            byte[] data = getBytesFromFileMP3(soundfile);
            System.err.println("Stream "+data.length+" bytes");
            return new ByteArrayInputStream(data); 
           })
           .setContentType("audio/mpeg")
           ; // For MP3
        player.setSource(stream);
//        player.setSource("https://file-examples.com/storage/fef2c10964681ca2d97e203/2017/11/file_example_MP3_700KB.mp3");
        getContent().add(player);
    }

    //-------------------------------------------------------------------
    private void initComponents() {
    	xterm = new WebTerminal();
    	messageInput = new MessageInput();
    }

    //-------------------------------------------------------------------
    private void initLayout() {
    	xterm.setSizeFull();
    	xterm.focus();
    	xterm.fit();
    	getContent().add(xterm);
    	getContent().getStyle().set("flex-grow", "10");
        messageInput.getStyle().set("flex-grow", "1");
        getContent().setWidth("100%");
   }

    //-------------------------------------------------------------------
    private void initInteractivity() {
        messageInput.addSubmitListener(event -> {
        	System.out.println("PlayView: "+event);
        	xterm.writeln(event.getValue());
        });
    }
   

    //-------------------------------------------------------------------
    public static byte[] getBytesFromFileMP3(String filename) {
    	Path file = Paths.get(filename);
    	try {
			return Files.readAllBytes(file);
		} catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
    }
}
