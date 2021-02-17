package com.gft.cristianociuti.utils;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

public class CitrixUtils {
	
	private CitrixUtils() {}
	
	public static final String CITRIX_VIEWER_EXE = "CDViewer.exe";
	
	public static Path getCitrixFile(Path dir) throws Exception {
		List<Path> fileList = new ArrayList<>();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, path -> {return path.toString().endsWith(".ica");})) {
			for (Path p : ds) {
				fileList.add(p);
			}
		} catch (IOException ioe) {
			throw new IOException(String.format("Could not access the download folder: %s", dir.toAbsolutePath()), ioe);
		}
		
		fileList.sort((a, b) -> {
			try {
				return -1 * Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b));
			} catch (IOException e) {
				return 0;
			}
		});
		
		Path citrixFile;
		try {
			citrixFile = fileList.get(0);
		} catch (IndexOutOfBoundsException ioobe) {
			throw new IllegalStateException(String.format("No ICA file found in the download folder: %s",dir), ioobe);
		}
		
		System.out.println(String.format("Citrix file found: %s", citrixFile.toAbsolutePath()));
		return citrixFile;
	}
	
	public static void updateCitrixFile(Path citrixFile) throws Exception {
		Path root = FileUtils.getRootPath("C:");
		Path hostsFile = root.resolve("Windows/System32/drivers/etc/hosts");
		
		StringBuilder out = new StringBuilder(1500);
		
		try (BufferedReader hosts = Files.newBufferedReader(hostsFile);
				BufferedReader citrix = Files.newBufferedReader(citrixFile);
				Formatter formatter = new Formatter(out)) {
			
			for (String line = citrix.readLine(); line != null; line = citrix.readLine()) {
				if (line.startsWith("Address=")) {
					String[] addressParts = line.substring(8).split(":");
					String ipLastPart = addressParts[0].split("\\.")[3];
					String ip = null;
					for (String l = hosts.readLine(); l != null; l = hosts.readLine()) {
						if (l.startsWith("10.55") && l.substring(0, 15).trim().split("\\.")[3].equals(ipLastPart)) {
							ip = l.substring(0, 15).trim();
							break;
						}
					}
					formatter.format("Address=%s:%s%n", ip, addressParts[1]);
				} else {
					formatter.format("%s%n", line);
				}
			}
		} catch (IOException ioe) {
			throw new IOException("Error while reading the hosts file and computing the new ICA file", ioe);
		}
		
		try(BufferedWriter writer = Files.newBufferedWriter(citrixFile)) {
			writer.write(out.toString());
		} catch (IOException ioe) {
			throw new IOException("Error while writing the new ICA file", ioe);
		}
		
		System.out.println(String.format("Citrix file updated: %s", citrixFile.toAbsolutePath()));
	}
	
	public static void launchCitrixFile(Path citrixFile) throws Exception {
		try {
			Desktop.getDesktop().open(citrixFile.toFile());
		} catch (Exception e) {
			throw new Exception(String.format("Could not open the new ICA file: %s", citrixFile.toAbsolutePath()), e);
		}
		System.out.println(String.format("Citrix file opened: %s", citrixFile.toAbsolutePath()));
		
		new Thread() {
			public void run() {
				try {
					Thread.sleep(60 * 1000);
					System.out.println("Stop waiting citrix viewer exe");
				} catch (InterruptedException ignored) {}
			}
		}.start();
	}
	
	public static void deleteCitrixFiles(Path dir, int waitingSecs) {
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, path -> {return path.toString().endsWith(".ica");})) {
			try {
				Thread.sleep(waitingSecs * 1000);
			} catch (InterruptedException ie) {}
			for (Path p : ds)
				Files.delete(p);
			System.out.println(String.format("Citrix files deleted from directory %s", dir.toAbsolutePath()));
		} catch (IOException ioe) {
			System.err.println(String.format("Could not delete citrix files from directory: %s", dir.toAbsolutePath()));
		}
	}
}
