/*
 *   Copyright 2013
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
package de.tudarmstadt.lt.lm.app;

import java.rmi.NoSuchObjectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.utilities.cli.CliUtils;
import de.tudarmstadt.lt.utilities.cli.ExtendedGnuParser;

/**
 *
 * @author Steffen Remus
 */
public class StartRMI implements Runnable {
	
	private final static Logger LOG = LoggerFactory.getLogger(StartRMI.class);
	private final static String USAGE_HEADER = "Options:";

	public static void main(String[] args) {
		new StartRMI(args).run();
	}
	
	/**
	 * default constructor
	 * 
	 * set necessary variables by using <code>new StartRMI(){{ _variable = value; ... }} </code>
	 * 
	 */
	public StartRMI() { /* NOTHING TO DO */ }

	@SuppressWarnings("static-access")
	public StartRMI(String[] args) {
		LOG.warn("This main is only for convencience and might be deprectated soon. Consider using {}.", StartLM.class.getName());
		Options opts = new Options();
		opts.addOption(OptionBuilder.withLongOpt("help").withDescription("Display help message.").create("?"));
		opts.addOption(OptionBuilder.withLongOpt("port").withArgName("port-number").hasArg().withDescription(String.format("specifies the port on which rmi registry listens (default: %d)", Registry.REGISTRY_PORT)).create("p"));
		
		
		try {
			CommandLine cmd = new ExtendedGnuParser(true).parse(opts, args);
			if (cmd.hasOption("help")) 
				CliUtils.print_usage_quit(System.err, StartLM.class.getSimpleName(), opts, USAGE_HEADER, null, 0);
			_port = Integer.parseInt(cmd.getOptionValue("port", String.valueOf(Registry.REGISTRY_PORT)));
			
		} catch (Exception e) {
			LOG.error("{}: {}", e.getClass().getSimpleName(), e.getMessage());
			CliUtils.print_usage_quit(System.err, StartLM.class.getSimpleName(), opts, USAGE_HEADER, String.format("%s: %s%n", e.getClass().getSimpleName(), e.getMessage()), 1);
		}
	}
	
	
	String readInput(String message){
		String r = null;
		System.out.print(message);
		if(_stdin_scan.hasNextLine()){ // block thread
			r = _stdin_scan.nextLine();
			return r;
		}
		return null;
	}
	
	int _port;
	Scanner _stdin_scan;
	Registry _registry;
	
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
		try{
			// create own registry, alternative: use 'rmiregistry' command in class output folder
			_registry = LocateRegistry.createRegistry(_port);
		}catch(Exception e){
			LOG.error("Could not create regisry. ", e);
			return;
		}

		_stdin_scan = new Scanner(System.in);
		for (String input_line = null; (input_line = readInput(String.format("+++%nRMI Registry running. What do you want to do?%n"
				+ "Type ':l' to list connected services, %n"
				+ "Type ':q' to quit program. %n"
				+ "$> "))) != null;) {
			
			if(input_line.isEmpty())
				continue;
			if(":q".equalsIgnoreCase(input_line))
				exit();
			if(":l".equalsIgnoreCase(input_line))
				try { System.out.format("%s%n", Arrays.toString(_registry.list())); } catch (Exception e) {/* */}
		}
	}
	
	
	void exit() {
		LOG.info("Exiting.");

		String[] rmi_bounded_apps = new String[0];
		try {
			rmi_bounded_apps = _registry.list();
			if(rmi_bounded_apps.length == 0){
				UnicastRemoteObject.unexportObject(_registry, true);
				LOG.info("Stopped RMI server.");
				System.exit(0);
			}
		} catch (Exception e) {/* */}

		String input_line = null;
		while((input_line = readInput(String.format("App serves as RMI server for the following apps: %s.%nType 'y' if you really want to quit serving RMI, else type 'n': %n$> ", Arrays.toString(rmi_bounded_apps) ))) == null);
		if("y".equals(input_line.toLowerCase())){
			try {
				UnicastRemoteObject.unexportObject(_registry, true);
			} catch (NoSuchObjectException e) { /* handle silently */ }
			LOG.info("Stopped RMI server.");
			System.exit(0);
		}
		else 
			return;

		System.exit(0);
	}

	
	


}
