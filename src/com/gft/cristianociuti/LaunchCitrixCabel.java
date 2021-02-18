package com.gft.cristianociuti;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

import com.gft.cristianociuti.utils.ApplicationUtils;
import com.gft.cristianociuti.utils.CitrixUtils;
import com.gft.cristianociuti.utils.FileUtils;
import com.gft.cristianociuti.utils.PropertyUtils;

public class LaunchCitrixCabel {
	
	private static void logException(Exception e) {
		try (PrintWriter pw = new PrintWriter(Files.newOutputStream(Paths.get(".").resolve("error.log")))) {
			e.printStackTrace(pw);
		} catch (IOException ioe) {
			System.err.println("An error occured while trying to create the error log file");
			ioe.printStackTrace();
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
	
	private static void showOpeningMessage (String citrixExe, boolean waitViewer) {
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
			
			int animationCount = 1;
			boolean completeAnimation = false;
			while(!completeAnimation) {
				try {
					System.out.println(String.format("Waiting animation: %d msecs", animationCount * 500));
					Thread.sleep(500);
				} catch (InterruptedException e) {}
				if (ext.charAt(2) == '.')
					ext = "   ";
				else
					ext = ext.replaceFirst(" ", ".");
				l.setText(base + ext);
				completeAnimation = (animationCount > (120 * 2) || (animationCount > 6 && waitViewer && ApplicationUtils.isProcessRunning(citrixExe))) && animationCount > 12;
				animationCount++;
			}
			try {
				l.setText("About to close...");
				Thread.sleep(2000);
			} catch (InterruptedException e) {}
			
			f.setVisible(false);
			f.dispose();
			
			System.out.println("Opening animation complete");
		} catch (Exception e) {
			System.err.println("An error occured while trying to display the opening message");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		
		Properties properties = PropertyUtils.readProperties("config.properties");
		
		System.out.println("==========PROPERTIES==========");
		String rootPath = properties.getProperty("folder.root", "C:");
		System.out.println(String.format("Root folder: %s", rootPath));
		
		String downloadPath = properties.getProperty("folder.download");
		System.out.println(String.format("Download folder: %s", downloadPath != null ? downloadPath : "computed"));
		
		String citrixExe = properties.getProperty("utils.citrixexe", CitrixUtils.CITRIX_VIEWER_EXE);
		System.out.println(String.format("Citrix viewer exe: %s", citrixExe));
		
		Boolean waitViewer = properties.getProperty("utils.waitviewer", "true").compareToIgnoreCase("true") == 0;
		System.out.println(String.format("Wait citrix viewer to open: %s", waitViewer.toString()));
		
		int waitingSecsClose = PropertyUtils.getPropertyInteger("utils.waitviewer.waitingsecs", properties, 60);
		System.out.println(String.format("Waiting seconds after citrix viewer opened: %d", waitingSecsClose));
		
		Boolean delete = properties.getProperty("utils.autodelete", "true").compareToIgnoreCase("true") == 0;
		System.out.println(String.format("Auto delete citrix files: %s", delete.toString()));
		
		int waitingSecsDelete = PropertyUtils.getPropertyInteger("utils.autodelete.waitingsecs", properties, 30);
		System.out.println(String.format("Waiting seconds before deleting citrix files: %d", waitingSecsDelete));
		System.out.println("==============================");
		
		boolean success = true;
		Path download = null;
		
		try {
			
			if (ApplicationUtils.isProcessRunning(citrixExe)) {
				throw new Exception(String.format("Citrix viewer process (%s) already running. Please close citrix viewer and run LauncherCitrixCabel again.", citrixExe));
			}
			
			if (downloadPath != null)
				try {
					download = Paths.get(downloadPath);
				} catch (InvalidPathException ex) {
					System.err.println(String.format("User defined download folder not valid: %s", downloadPath));
				}
			
			if (download == null) {
				Path userHome = FileUtils.translateUserHome(rootPath);
				download = userHome.resolve("Downloads");
			}
			System.out.println(String.format("Using download folder: %s", download.toAbsolutePath()));
			
			Path citrixFile = CitrixUtils.getCitrixFile(download);
			
			CitrixUtils.updateCitrixFile(citrixFile);
			
			CitrixUtils.launchCitrixFile(citrixFile);
			
		}
		catch(Exception e) {
			success = false;
			e.printStackTrace();
			logException(e);
			showErrorDialog(e);
		}
		
		if (success) {
			showOpeningMessage(citrixExe, waitViewer);
			
			Thread deleteThread = null, closeThread = null;
			if (delete)
				deleteThread = CitrixUtils.deleteCitrixFiles(download, waitingSecsDelete);
			if (waitViewer)
				closeThread = CitrixUtils.waitViewer(waitingSecsClose);
			
			try {
				if (deleteThread != null)
					deleteThread.join();
				if (closeThread != null)
					closeThread.join();
			} catch (InterruptedException e) {
				System.err.println("An error occured while waiting final threads");
				e.printStackTrace();
				System.exit(1);
			}
			
			System.exit(0);
		}
		else
			System.exit(1);
		
	}
	
}
