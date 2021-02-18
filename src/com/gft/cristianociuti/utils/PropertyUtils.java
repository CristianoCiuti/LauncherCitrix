package com.gft.cristianociuti.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyUtils {
	
	private PropertyUtils() {}

	public static Properties readProperties(String filename) {
		Properties prop = new Properties();
		
		try (InputStream input = new FileInputStream(filename)) {
            prop.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
		
		return prop;
	}
	
	public static Integer getPropertyInteger(String propertyName, Properties properties) {
		return getPropertyInteger(propertyName, properties, null);
	}
	
	public static Integer getPropertyInteger(String propertyName, Properties properties, Integer defaultValue) {
		String property = properties.getProperty(propertyName);
		if (property != null)
			try {
				return Integer.parseInt(property);
			}catch (Exception ex) {
				System.err.println(String.format("Not a valid number: %s", property));
			}
		return defaultValue;
	}
}
