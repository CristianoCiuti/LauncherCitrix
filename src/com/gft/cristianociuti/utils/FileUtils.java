package com.gft.cristianociuti.utils;

import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtils {
	
	private FileUtils() {}
	
	public static Path getRootPath(String rootPath) throws Exception {
		for (Path p : FileSystems.getDefault().getRootDirectories())
			if (p.toString().startsWith(rootPath))
				return p;
		
		throw new Exception(String.format("Root folder not found: %s",rootPath));
	}
	
	public static Path translateUserHome(String rootString) throws Exception {
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
}
