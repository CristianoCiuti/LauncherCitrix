package com.gft.a17u.utils;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import javax.swing.JOptionPane;

public class LaunchCitrixCabel {

	public static void main(String[] args) {
		
		Path root = null;
		Path dir = null;
		
		try {
			// Get the root C directory
			for (Path p : FileSystems.getDefault().getRootDirectories())
				if (p.toString().startsWith("C:"))
					root = p;
			// To get to the use home directory, get the first accessible, 4-character, starting with 'A' folder in Users
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(root.resolve("Users"), p -> {
				return p.getFileName().toString().startsWith("A") && p.getFileName().toString().length() == 4;
			})) {
				for (Path p : ds) {
					if (Files.isExecutable(p)) {
						dir = p;
						break;
					}
				}
			} catch (IOException ioe) {
				throw new IOException("Could not find the user home folder", ioe);
			}
			// Get the Downloads folder
			dir = dir.resolve("Downloads");
			
			// Get the last modified ICA file in Downloads
			List<Path> fileList = new ArrayList<>();
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, path -> {return path.toString().endsWith(".ica");})) {
				for (Path p : ds) {
					fileList.add(p);
				}
			} catch (IOException ioe) {
				throw new IOException("Could not access the Downloads folder", ioe);
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
				throw new IllegalStateException("No ICA file found", ioobe);
			}
			
			// Get the hosts file
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
			
			try {
				Desktop.getDesktop().open(citrixFile.toFile());
			} catch (Exception e) {
				System.out.println("Could not open the new ICA file");
			}
		} catch (Exception e) {
			try {
				JOptionPane.showMessageDialog(null, e.getMessage(), "LaunchCitrixCabel - Error", JOptionPane.ERROR_MESSAGE);
			} catch (Exception ee) {
				System.err.println("An error occured while trying to display the error message");
				System.err.println(e.getMessage());
			}
		}
	}

}
