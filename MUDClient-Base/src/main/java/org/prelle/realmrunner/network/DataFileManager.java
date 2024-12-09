package org.prelle.realmrunner.network;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringTokenizer;

/**
 *
 */
public class DataFileManager {

	private final static Logger logger = System.getLogger("mud.client");

	private static MainConfig mainConfig;
	private static Config activeWorldConfig;

	private static Path mainDataDir;
	private static Path currentDataDir;
	private static HttpClient http;

	//-------------------------------------------------------------------
	public static void configure(MainConfig config) throws IOException {
		DataFileManager.mainConfig = config;
		// Determine main data directory
		mainDataDir = MainConfig.CONFIG_DIR.resolve("worlds");
		if (config.getDataDir()!=null) {
			String tmp = config.getDataDir().trim();
			String sep = FileSystems.getDefault().getSeparator();
			mainDataDir = Paths.get(tmp);
		}
		logger.log(Level.INFO, "Global world data directory: {0}", mainDataDir.toAbsolutePath());
		// Ensure a data directory exists
		if (!Files.exists(mainDataDir)) {
			Files.createDirectories(mainDataDir);
		}

		// Prepare HTTP downloading
		http = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(3))
				.followRedirects(Redirect.NORMAL)
				.build();
	}

	//-------------------------------------------------------------------
	public static void setActiveMUD(String worldId, Config worldConfig) throws IOException {
		currentDataDir = mainDataDir.resolve(worldId);
		logger.log(Level.INFO, "World data directory: {0}", currentDataDir.toAbsolutePath());
		// Ensure a data directory exists
		if (!Files.exists(currentDataDir)) {
			Files.createDirectories(currentDataDir);
		}
	}

	//-------------------------------------------------------------------
	public static Path getCurrentDataDir() {
		return currentDataDir;
	}

	//-------------------------------------------------------------------
	public static Path downloadFileTo(String filePath, URI uri) throws IOException {
		logger.log(Level.DEBUG, "ENTER: downloadFileTo({0}, {1})", filePath, uri);
		try {
			StringTokenizer tok = new StringTokenizer(filePath, "/");
			Path targetFile = currentDataDir.resolve(tok.nextToken());
			boolean checkParent =false;
			while (tok.hasMoreTokens()) {
				targetFile = targetFile.resolve(tok.nextToken());
				// A subdirectory was given
				checkParent=true;
			}
			// Ensure given directory exists
			if (checkParent) {
				Path parent = targetFile.getParent();
				if (!Files.exists(parent)) {
					Files.createDirectories(parent);
				}
			}
			// Know we know where to load file to
			// Maybe it does already exist - in this case compare file dates
			FileTime modifiedSince = FileTime.fromMillis(0);
			if (Files.exists(targetFile)) {
				modifiedSince = Files.readAttributes(targetFile, BasicFileAttributes.class).lastModifiedTime();
			}
			 ZonedDateTime zdt = ZonedDateTime.ofInstant(modifiedSince.toInstant(), java.time.ZoneOffset.UTC);

			HttpRequest request = HttpRequest.newBuilder(uri)
					.GET()
					.header("User-Agent", System.getProperty("app.name", "MUDClient"))
					.header("If-Modified-Since", zdt.format(DateTimeFormatter.RFC_1123_DATE_TIME))
					.build();
			try {
				HttpResponse<InputStream> resp = http.send(request, BodyHandlers.ofInputStream());
				if (resp.statusCode()==200) {
					logger.log(Level.INFO, "Download file {0}", targetFile);
					String lastModifiedHeader = resp.headers().firstValue("Last-Modified").orElse(null);
					if (Files.exists(targetFile)) {
						Files.delete(targetFile);
					} else {
					}
					Files.copy(resp.body(), targetFile);

		            if (lastModifiedHeader != null) {
		                zdt = ZonedDateTime.parse(lastModifiedHeader, DateTimeFormatter.RFC_1123_DATE_TIME);
		                FileTime fileTime = FileTime.from(zdt.toInstant());
		                Files.setLastModifiedTime(targetFile, fileTime);
		            }
					resp.body().close();
					return targetFile;
				} else if (resp.statusCode()==304) {
					logger.log(Level.DEBUG, "Already up to date {0}", targetFile);
					return targetFile;
				} else if (resp.statusCode()==404) {
					throw new FileNotFoundException("Code "+resp.statusCode());
				}
				logger.log(Level.ERROR, "Got status code "+resp.statusCode());
				throw new IOException("Code "+resp.statusCode());
			} catch (InterruptedException e) {
				logger.log(Level.ERROR, "Failed downloading from "+uri,e);
				throw new IOException("Download interrupted",e);
			}
		} finally {
			logger.log(Level.DEBUG, "LEAVE: downloadFileTo({0}, {1})", filePath, uri);
		}
	}

}
