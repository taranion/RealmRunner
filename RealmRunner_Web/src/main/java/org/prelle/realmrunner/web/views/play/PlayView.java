package org.prelle.realmrunner.web.views.play;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.prelle.ansi.commands.iterm.SendITermImage;
import org.prelle.realmrunner.network.Config;
import org.prelle.realmrunner.network.LineBufferListener;
import org.prelle.realmrunner.network.MUDSession;
import org.prelle.realmrunner.network.ReadFromConsoleTask;
import org.prelle.realmrunner.network.ReadFromMUDTask;
import org.prelle.realmrunner.network.SessionConfig;
import org.prelle.realmrunner.network.SessionConfig.SessionConfigBuilder;
import org.prelle.realmrunner.web.data.AudioPlayer;
import org.prelle.telnet.TelnetOption;
import org.prelle.telnet.TelnetSocket;
import org.prelle.telnet.TelnetSocket.State;
import org.prelle.telnet.TelnetSocketListener;
import org.prelle.telnet.mud.AardwolfMushclientProtocol.AardwolfMushclientListener;
import org.prelle.terminal.TerminalEmulator;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.flowingcode.vaadin.addons.xterm.ITerminalOptions.CursorStyle;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
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
public class PlayView extends Composite<VerticalLayout> implements TelnetSocketListener, LineBufferListener {
	
	private final static Logger logger = System.getLogger("client.web");
	private Charset charset = StandardCharsets.UTF_8;
	
	private WebTerminal xterm;
	private MessageInput messageInput;
	private MUDSession session;

    //-------------------------------------------------------------------
    @SuppressWarnings("resource")
	public PlayView() {
    	initComponents();
    	xterm.writeln("Hello world.");
    	try {
//			byte[] sixel = (new FileInputStream("/home/prelle/lady-of-shalott.six").readAllBytes());
//			xterm.writeln(new String(sixel));

			byte[] data = (new FileInputStream("/home/prelle/git/RealmRunner/Hybrid.png").readAllBytes());
			SendITermImage iterm = new SendITermImage();
			iterm.setSize(data.length);
			iterm.setFileName("Hybrid.png");
			iterm.setWidth(1116);
			iterm.setHeight(446);
			iterm.setImgData(data);
			try {
	        		ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        		iterm.encode(baos, true);
	        		xterm.write(new String(baos.toByteArray()));
	        		xterm.write("\r\n");
	            	xterm.writeln("Hallo welt.");
			} catch (Exception e) {
				logger.log(Level.ERROR, "Failed sending image",e);
			}

    	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	xterm.writeln("\u001B]8;;http://example.com\u001B\\This is a link\u001B]8;;\u001B\\\n");
    	xterm.setCursorBlink(true);
    	xterm.setCursorStyle(CursorStyle.UNDERLINE);
   	 
        HorizontalLayout layoutRow = new HorizontalLayout();
        VerticalLayout layoutColumn2 = new VerticalLayout();
        layoutColumn2.add("LC2");
        VerticalLayout layoutColumn3 = new VerticalLayout();
        layoutColumn3.add("LC3");
        VerticalLayout layoutColumn4 = new VerticalLayout();
        layoutColumn4.add("LC4");
        HorizontalLayout layoutRow2 = new HorizontalLayout();
        layoutRow2.add("LR2");
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
        layoutRow.add(layoutColumn2);
        layoutRow.add(layoutColumn3);
        layoutRow.add(layoutColumn4);
        getContent().add(layoutRow2);
        layoutRow2.add(messageInput);
        
        initLayout();
        getContent().add(layoutRow);
        initInteractivity();
        
        AudioPlayer player=new AudioPlayer();
        player.getElement().setAttribute("autoplay",false);
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
        
		SessionConfigBuilder builder = SessionConfig.builder();
		builder.server("eden-test.rpgframework.de").port(4000);	
//		builder.server("dragonfiremud.com").port(1999);	
//		builder.server("rom.mud.de").port(400);	
		SessionConfig config = builder.build();
		
		Config activeConfig = new Config();
		
		TerminalEmulator console = xterm;

		try {
			setupSession(config, activeConfig);
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  }

    //-------------------------------------------------------------------
    private void initComponents() {
    	xterm = new WebTerminal();
    	ImageAddOn add = new ImageAddOn(xterm);
    	WebLinkAddon links = new WebLinkAddon(xterm);
    	messageInput = new MessageInput();
    }

    //-------------------------------------------------------------------
    private void initLayout() {
    	xterm.setSizeFull();
    	xterm.focus();
//    	xterm.fit();
    	getContent().add(xterm);
    	xterm.getStyle().set("flex-grow", "20");
        messageInput.getStyle().set("flex-grow", "1");
        xterm.setHeight("800px");
   }

    //-------------------------------------------------------------------
    private void initInteractivity() {
        messageInput.addSubmitListener(event -> {
        	System.out.println("PlayView: "+event);
        	xterm.writeln(event.getValue());
        	try {
				session.getStreamToMUD().write(event.getValue()+"\r\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        });
    }
   
    //-------------------------------------------------------------------
    /**
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
       logger.log(Level.WARNING, "onAttach############################################## "+attachEvent.getUI());
       xterm.setUI( attachEvent.getUI() );
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

	//-------------------------------------------------------------------
	private void setupSession(SessionConfig config, Config activeConfig) throws IOException, InterruptedException {
		TerminalEmulator console = xterm;
		session = new MUDSession(config, this, console.getConsoleSize(), charset);
//		session.getSocket().setOptionListener(TelnetOption.MUSHCLIENT, (AardwolfMushclientListener)this);
//		session.getSocket().setOptionListener(TelnetOption.MSP, sound);
//		session.setGmcpListener(this);
//		readFromConsole.setForwardTo(session.getSocket());
		
		Charset useCharset = charset;
		if (activeConfig.getServerEncoding()!=null)
			useCharset = Charset.forName(activeConfig.getServerEncoding());

		if ((activeConfig instanceof Config)  &&((Config)activeConfig).getServerEncoding()!=null) {
			console.getInputStream().setEncoding(useCharset);
		}

		logger.log(Level.INFO, "Read from MUD with charset {0}", useCharset);
		ReadFromMUDTask readTask = new ReadFromMUDTask(session.getSocket(), console.getOutputStream(), activeConfig, useCharset);
//		readTask.setControlSequenceFilter( frag -> filterFragmentFromMUD(frag));
		session.getStreamToMUD().setLoggingListener( (type,text) -> {if (!"PRINT".equals(type)) logger.log(Level.INFO, "MUD --> {0} = {1}", type,text);});
		Thread readThread = new Thread(readTask,"ReadFromMUDTask");
		readThread.start();

		ReadFromConsoleTask readFromConsole = new ReadFromConsoleTask(console, activeConfig, (LineBufferListener)this);
		readFromConsole.setForwardMode(true);
		readFromConsole.setForwardTo(session.getSocket());
		Thread readFromTerminal = new Thread(readFromConsole, "ReadFromConsole");
		readFromTerminal.start();

		
		if (config.getLogin()!=null) {
			session.getStreamToMUD().write( (config.getLogin()+"\r\n").getBytes(StandardCharsets.UTF_8));
			if (config.getPasswd()!=null) {
				session.getStreamToMUD().write( (config.getPasswd()+"\r\n").getBytes(StandardCharsets.UTF_8));
			}
		}
	}

	@Override
	public void telnetSocketChanged(TelnetSocket nvt, State oldState, State newState) {
		logger.log(Level.WARNING, "telnetSocketChanged: "+newState);
		// TODO Auto-generated method stub
		
	}

	@Override
	public void lineBufferChanged(String content, int cursorPosition) {
		logger.log(Level.WARNING, "lineBufferChanged: "+content);
		
	}

	@Override
	public String processCommandTyped(String typed) {
		logger.log(Level.WARNING, "processCommand: "+typed);
		return typed;
	}

}
