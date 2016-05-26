/*
 *   Copyright 2014
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package de.tudarmstadt.lt.utilities;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Steffen Remus
 *
 */
public class LogUtils {
	
	private static final Logger LOG = LoggerFactory.getLogger(LogUtils.class);

	private LogUtils(){ /* DO NOT INSTANTIANTE */ }

	private static ObjectName _logger_name = null;
	
	private static ObjectName logger_name(){
		if(_logger_name == null){
		Set<ObjectName> beans = ManagementFactory.getPlatformMBeanServer().queryNames(null, null);
		for(ObjectName b : beans){
			if(b.getCanonicalName().contains("logback")){
				_logger_name = b;
				break;
			}
		}
		if(_logger_name == null){
			LOG.warn("Could not get log configurator. Are you using logback? Is JMXConfigurator activated in logback.xml? Is JMXSupport active in JVM ('-Dcom.sun.management.jmxremote')?");
			return null;
		}
		}
		return _logger_name;
	}
	
	public static boolean setLogLevel(String logger, String level){
		if(logger_name() == null){
			LOG.warn("Could not change loglevel of logger '{}' to '{}'.", logger, level);
			return false;
		}
		try {
			ManagementFactory.getPlatformMBeanServer().invoke(_logger_name, "setLoggerLevel", new String[]{logger, level}, new String[]{String.class.getName(), String.class.getName()});
		} catch (Exception e) {
			LOG.error("Could not change loglevel of logger '{}' to '{}'.", logger, level, e);
			return false;
		}
		return true;
	}
	
	public static String getLogLevel(String logger){
		if(logger_name() == null){
			LOG.warn("Could not get loglevel for logger '{}'.", logger);
			return null;
		}
		Object result = null;
		try {
			result = ManagementFactory.getPlatformMBeanServer().invoke(_logger_name, "getLoggerLevel", new String[]{logger}, new String[]{String.class.getName()});
		} catch (Exception e) {
			LOG.error("Could get loglevel for logger '{}'.", logger, e);
			return null;
		}
		return (String)result;
	}
	
}