package com.gft.a17u.utils;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

public class LaunchCitrixCabel {
	
	private static Properties readProperties() {
		Properties prop = new Properties();
		
		try (InputStream input = new FileInputStream("config.properties")) {
            prop.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
		
		return prop;
	}
	
	private static Path getRootPath(String rootPath) throws Exception {
		for (Path p : FileSystems.getDefault().getRootDirectories())
			if (p.toString().startsWith(rootPath))
				return p;
		
		throw new Exception(String.format("Root folder not found: %s",rootPath));
	}
	
	private static Path translateUserHome(String rootString) throws Exception {
		Path dir = Paths.get(System.getProperty("user.home"));
		if (Files.exists(dir))
			return dir;
		
		Path root = getRootPath(rootString);
		
		try (
				DirectoryStream<Path> ds = Files.newDirectoryStream(root.resolve("Users"), p -> {
					return p.getFileName().toString().startsWith("A") && p.getFileName().toString().length() == 4;
				})
			) {
			for (Path p : ds) {
				if (Files.isExecutable(p)) {
					return p;
				}
			}
		} catch (Exception ex) {
			throw new Exception("Could not find the user home folder", ex);
		}
		
		throw new Exception("Could not find the user home folder");
	}
	
	private static Path getCitrixFile(Path dir) throws Exception {
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
		
		return citrixFile;
	}
	
	private static void updateCitrixFile(Path citrixFile) throws Exception {
		Path root = getRootPath("C:");
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
	}
	
	private static void launchCitrixFile(Path citrixFile) throws Exception {
		try {
			Desktop.getDesktop().open(citrixFile.toFile());
		} catch (Exception e) {
			throw new Exception("Could not open the new ICA file", e);
		}
	}
	
	private static void logException(Exception e) {
		try (PrintWriter pw = new PrintWriter(Files.newOutputStream(Paths.get(".").resolve("error.log")))) {
			e.printStackTrace(pw);
		} catch (IOException ioe) {
			System.err.println("An error occured while trying to create the error log file");
			ioe.printStackTrace();
		}
	}
	
	private static void deleteCitrixFiles(Path dir) {
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, path -> {return path.toString().endsWith(".ica");})) {
			try {
				Thread.sleep(30000);
			} catch (InterruptedException ie) {}
			for (Path p : ds)
				Files.delete(p);
		} catch (IOException ioe) {
			System.err.println(String.format("Could not delete citrix files from directory: %s", dir.toAbsolutePath()));
		}
	}
	
	private static void showErrorDialog(Exception e) {
		try {
			JOptionPane.showMessageDialog(null, e.getMessage(), "LaunchCitrixCabel - Error", JOptionPane.ERROR_MESSAGE);
		} catch (Exception ee) {
			System.err.println("An error occured while trying to display the error message");
			ee.printStackTrace();
		}
	}
	
	private static void showOpeningMessage () {
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
			System.err.println("An error occured while trying to display the opening message");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		
		Properties properties = readProperties();
		
		System.out.println("====PROPERTIES====");
		String rootPath = properties.getProperty("folder.root", "C:");
		System.out.println(String.format("Root folder: %s", rootPath));
		
		String downloadPath = properties.getProperty("folder.download");
		System.out.println(String.format("Download folder: %s", downloadPath));
		
		Boolean delete = properties.getProperty("utils.autodelete", "true").compareToIgnoreCase("true") == 0;
		System.out.println(String.format("Auto delete citrix files: %s", delete.toString()));
		
		int waitingSecs = 30;
		if (delete) {
			String waitingProp = properties.getProperty("utils.waitingsecs");
			if (waitingProp != null)
				try {
					waitingSecs = Integer.parseInt(waitingProp);
				}catch (Exception ex) {
					System.err.println(String.format("User defined waiting seconds not valid: %s", waitingProp));
				}
			
			System.out.println(String.format("Waiting seconds before deleting citrix files: %d", waitingSecs));
		}
		
		boolean success = true;
		Path download = null;
		
		try {
			
			if (downloadPath != null)
				try {
					download = Paths.get(downloadPath);
				} catch (InvalidPathException ex) {
					System.err.println(String.format("User defined download folder not valid: %s", downloadPath));
				}
			
			if (download == null) {
				Path userHome = translateUserHome(rootPath);
				download = userHome.resolve("Downloads");
			}
			System.out.println(String.format("Using download folder: %s", download.toAbsolutePath()));
			
			Path citrixFile = getCitrixFile(download);
			
			updateCitrixFile(citrixFile);
			
			launchCitrixFile(citrixFile);
			
		}
		catch(Exception e) {
			success = false;
			e.printStackTrace();
			logException(e);
			showErrorDialog(e);
		}
		
		if (success) {
			showOpeningMessage();
			if (delete) 
				deleteCitrixFiles(download);
		}
		
	}
	
}
