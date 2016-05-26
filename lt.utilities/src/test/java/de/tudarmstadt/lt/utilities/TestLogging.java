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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ObjectInstance;
import javax.management.ReflectionException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * @author Steffen Remus
 *
 */
public class TestLogging {

	final static Logger LOG = LoggerFactory.getLogger(TestLogging.class);

	@Test
	public void test() {

		// assume SLF4J is bound to logback in the current environment
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		// print logback's internal status
		StatusPrinter.print(lc);
		
		LOG.trace("this is a trace message");

		LOG.debug("this is a debug message");

		LOG.info("this is an info message");

		LOG.warn("this is a warn message");

		LOG.error("this is an error message");

	}

	@Test
	/**
	 * run with "-Dcom.sun.management.jmxremote", run jconsole and change desired loglevel
	 * 
	 */
	public void testJmx() throws InterruptedException, InstanceNotFoundException, ReflectionException, MBeanException {
		Set<ObjectInstance> beans = ManagementFactory.getPlatformMBeanServer().queryMBeans(null, null);
		ObjectInstance logger = null;
		for(ObjectInstance b : beans){
			if(b.getObjectName().getCanonicalName().contains("logback")){
				System.out.println(b);
				logger = b;
			}
		}
		
		final AtomicBoolean _run_ = new AtomicBoolean(true);
		Thread t = new Thread(new Runnable(){
			@Override
			public void run() {
				while(_run_.get()){
					LOG.trace("this is a trace message");

					LOG.debug("this is a debug message");

					LOG.info("this is an info message");

					LOG.warn("this is a warn message");

					LOG.error("this is an error message");
					
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});

		t.start();
		
		Thread.sleep(2000);
		
		ManagementFactory.getPlatformMBeanServer().invoke(logger.getObjectName(), "setLoggerLevel", new String[]{getClass().getName(), "error"}, new String[]{String.class.getName(), String.class.getName()});
		
		Thread.sleep(2000);
		
		ManagementFactory.getPlatformMBeanServer().invoke(logger.getObjectName(), "setLoggerLevel", new String[]{getClass().getName(), "debug"}, new String[]{String.class.getName(), String.class.getName()});
		
		Thread.sleep(2000);
		
		_run_.set(false);
		
		t.join();
		
	}
	
	@Test
	/**
	 * run with "-Dcom.sun.management.jmxremote", run jconsole and change desired loglevel
	 * 
	 */
	public void testJmx2() throws InterruptedException, InstanceNotFoundException, ReflectionException, MBeanException {
		
		
		final AtomicBoolean _run_ = new AtomicBoolean(true);
		Thread t = new Thread(new Runnable(){
			@Override
			public void run() {
				while(_run_.get()){
					LOG.trace("this is a trace message");

					LOG.debug("this is a debug message");

					LOG.info("this is an info message");

					LOG.warn("this is a warn message");

					LOG.error("this is an error message");
					
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});

		t.start();
		
		Thread.sleep(2000);
		
		LogUtils.setLogLevel(LOG.getName(), "error");
		
		Thread.sleep(2000);
		
		LogUtils.setLogLevel(LOG.getName(), "debug");
		
		Thread.sleep(2000);
		
		_run_.set(false);
		
		t.join();
		
	}
}
