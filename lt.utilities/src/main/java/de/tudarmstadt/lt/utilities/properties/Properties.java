package de.tudarmstadt.lt.utilities.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class Properties extends java.util.Properties {

	protected Properties() {
		super();
	}

	protected Properties(java.util.Properties defaults) {
		super(defaults);
	}

	private static final long serialVersionUID = 3253998311699912113L;

	private final static Logger LOG = LoggerFactory.getLogger(Properties.class);
	protected final static Properties _singleton;

	static {
		_singleton = new Properties();
		// check classpath for defaults
		InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream("project.properties");
		if (in != null) try {
			_singleton.load(in);
			in.close();
		} catch (IOException e) {
			LOG.warn(String.format("Unable to parse property file '%s' in classpath.", "project.properties", e.getMessage()));
		}

		// override defaults with system properties (commandline has precedence)
		_singleton.load(System.getProperties());

		// check if additional propertyfile exists and if so, load properties
		String propertyfilename = propertyfile();
		File propertyfile = new File(propertyfilename);
		if (propertyfile.exists() && propertyfile.isFile()) try {
			_singleton.load(new FileInputStream(propertyfile));
			LOG.info(String.format("Loaded properties from file '%s'.", propertyfile.getAbsolutePath()));
		} catch (FileNotFoundException e) {
			LOG.info(String.format("Property file '%s' not found.", propertyfile.getAbsolutePath()));
		} catch (IOException e) {
			LOG.warn(String.format("Unable to parse property file '%s': %s.", propertyfile.getAbsolutePath(), e.getMessage()));
		}

		// again override properties with system properties (commandline has precedence above all!!!)
		_singleton.load(System.getProperties());
	}

	private static final String _propertyfile_default = "project.properties";
	public static String propertyfile() {
		return _singleton.getProperty("project.properties", _propertyfile_default);
	}

	public Properties load(java.util.Properties properties) {
		putAll(properties);
		return this;
	}

	public static Properties get(){
		return _singleton;
	}

}
