package de.tudarmstadt.lt.utilities.cli;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;


public class CliUtils {
	
	private CliUtils(){ /* DO NOT INSTANTIATE */ }
	
	public static void print_usage(String cmd, Options opts, String message){
		print_usage(System.err, cmd, opts, "Options:", message);
	}
	
	public static void print_usage_quit(String cmd, Options opts, String message, int exit_code){
		print_usage_quit(System.err, cmd, opts, "Options:", message, exit_code);
	}
	
	public static void print_usage(PrintStream p, String cmd, Options opts, String header, String message){
		if(message != null)
			p.println(message);
		if(opts != null)
			new HelpFormatter().printHelp(new PrintWriter(p, true), 80, "... " + cmd + " <options>", header, opts, 2, 2, "======");
	}
	
	public static void print_usage_quit(PrintStream p, String cmd, Options opts, String header, String message, int exit_code){
		print_usage(p, cmd, opts, header, message);
		System.exit(exit_code);
	}
	
}
