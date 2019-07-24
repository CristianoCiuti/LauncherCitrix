package com.gft.a17u.utils;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

public class LaunchCitrixCabel {

	public static void main(String[] args) {
		
		Path root = null;
		Path dir = null;
		boolean success = true;
		
		try {
			// Get the root C directory
			for (Path p : FileSystems.getDefault().getRootDirectories())
				if (p.toString().startsWith("C:"))
					root = p;
			// Get the user home directory by looking at system properties
			dir = Paths.get(System.getProperty("user.home"));
			if (Files.notExists(dir))
				throw new IllegalStateException("The directory specified in the user.home system property does not exist");
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
			} catch (NullPointerException npe) {
				throw new IllegalStateException("The user.home system property has not been found", npe);
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
				throw new IllegalStateException("No ICA file found in the Downloads folder", ioobe);
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
				throw new Exception("Could not open the new ICA file", e);
			}
		} catch (Exception e) {
			success = false;
			e.printStackTrace();
			// Write the error in a log file
			try (PrintWriter pw = new PrintWriter(Files.newOutputStream(Paths.get(".").resolve("error.log")))) {
				e.printStackTrace(pw);
			} catch (IOException ioe) {
				System.err.println("An error occured while trying to create the error log file");
				ioe.printStackTrace();
			}
			// Show the error in a message dialog
			try {
				JOptionPane.showMessageDialog(null, e.getMessage(), "LaunchCitrixCabel - Error", JOptionPane.ERROR_MESSAGE);
			} catch (Exception ee) {
				System.err.println("An error occured while trying to display the error message");
				ee.printStackTrace();
			}
		}
		
		// Display the window saying that the VM is starting
		// This is all in a try block because it is entirely cosmetic
		if (success) {
			try {
				final JFrame f = new JFrame();
				f.setLayout(new BorderLayout());
				f.setSize(680, 100);
				f.setLocationRelativeTo(null);
				f.setUndecorated(true);
				final String base = "The connection to the remote desktop is being established";
				String ext = "   ";
				final JLabel l = new JLabel(base + ext, SwingConstants.CENTER);
				l.setFont(l.getFont().deriveFont(20f));
				f.add(l, BorderLayout.CENTER);
				f.setVisible(true);
				for (byte i = 0; i < 10; i++) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {}
					if (ext.charAt(2) == '.')
						ext = "   ";
					else
						ext = ext.replaceFirst(" ", ".");
					l.setText(base + ext);
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {}
				f.setVisible(false);
				f.dispose();
			} catch (Exception e) {
				System.err.println("An error occured while trying to display the 'Opening...' message");
				e.printStackTrace();
			}
		}
		
	}

}
