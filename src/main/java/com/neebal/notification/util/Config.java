package com.neebal.notification.util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.springframework.stereotype.Component;

@Component
public class Config {
	Properties properties;

	public Config() {
		properties = new Properties();

//		try(InputStream stream = new FileInputStream(new File("C:\\Program Files (x86)\\Apache Software Foundation\\Tomcat 8.5\\Sequelstring\\QA\\Properties\\consumer.properties"))) {
		try(InputStream stream = new FileInputStream(new File("C:\\Program Files\\Apache Software Foundation\\Tomcat 8.5\\Sequelstring\\QA\\Properties\\consumer.properties"))) {
			properties.load(stream);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}

	}

	public String getProperty(String propName) {
		if (properties != null) {
			return properties.getProperty(propName);
		} else {
			return null;
		}
	}
	
}
