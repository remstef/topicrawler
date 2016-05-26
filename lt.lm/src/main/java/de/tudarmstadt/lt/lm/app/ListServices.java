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

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.service.StringProviderMXBean;
import de.tudarmstadt.lt.utilities.cli.CliUtils;
import de.tudarmstadt.lt.utilities.cli.ExtendedGnuParser;

/**
 *
 * @author Steffen Remus
 */
@SuppressWarnings("static-access")
public class ListServices implements Runnable{
	
	private static final Logger LOG = LoggerFactory.getLogger(ListServices.class);
	
	public static void main(String[] args) throws RemoteException {
		new ListServices(args).run();
	}
	
	/**
	 * 
	 */
	public ListServices(String[] args) {
		Options opts = new Options();
		opts.addOption(OptionBuilder.withLongOpt("help").withDescription("Display help message.").create("?"));
		opts.addOption(OptionBuilder.withLongOpt("host").withArgName("hostname").hasArg().withDescription("specifies the hostname on which the rmi registry listens (default: localhost)").create("h"));
		opts.addOption(OptionBuilder.withLongOpt("port").withArgName("port-number").hasArg().withDescription(String.format("specifies the port on which rmi registry listens (default: %d)", Registry.REGISTRY_PORT)).create("p"));
				
		try {
			CommandLine cmd = new ExtendedGnuParser(true).parse(opts, args);
			if (cmd.hasOption("help")) 
				CliUtils.print_usage_quit(StartLM.class.getSimpleName(), opts, null, 0);
			_port = Integer.parseInt(cmd.getOptionValue("port", String.valueOf(Registry.REGISTRY_PORT)));
			_host = cmd.getOptionValue("host", "localhost");
		} catch (Exception e) {
			LOG.error("{}: {}", e.getClass().getSimpleName(), e.getMessage());
			CliUtils.print_usage_quit(StartLM.class.getSimpleName(), opts, String.format("%s: %s%n", e.getClass().getSimpleName(), e.getMessage()), 1);
		}	
	}

	public String _host;
	public int _port;

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		String[] services = new String[]{};
		Registry registry = null;
		try{
			registry = LocateRegistry.getRegistry(_host, _port);
		}catch(Exception e){
			LOG.error("Could not connect to registry.", e);
			System.exit(1);
		}
		try{
			services = registry.list();
		}catch(Exception e){
			LOG.error("Could not lookup services from registry.", e);
			System.exit(1);
		}
		
		LOG.info("{} services available.", services.length);
			
		for (String name : services) {
			Remote r = null;
			try {
				r = registry.lookup(name);
			} catch (NotBoundException | RemoteException e) {
				continue;
			}
			StringBuilder b = new StringBuilder();
			for (Class<?> clazz : r.getClass().getInterfaces())
				b.append(", ").append(clazz.getName());
			String status = "status:unknown";
			try {
			if(r instanceof StringProviderMXBean)
				status = ((StringProviderMXBean)r).getModelReady() ? "status:running" : "status:loading";
			} catch (RemoteException e) {}
			System.out.format("%s\t(%s)\t[%s] %n", name, b.length() > 0 ? b.substring(2) : "", status);
		}
	}
}
