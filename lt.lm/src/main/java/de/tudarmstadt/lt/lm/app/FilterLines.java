/*
 *   Copyright 2015
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.utilities.cli.CliUtils;
import de.tudarmstadt.lt.utilities.cli.ExtendedGnuParser;

/**
 * @author Steffen Remus
 *
 */
public class FilterLines implements Runnable {
	
	private final static String USAGE_HEADER = "Expected input format: text <tab> num-ngrams <tab> num-oov <tab> line-prob-log10 <tab> perplexity <tab> line-prob-w/o-oov-log10 <tab> perplexity-w/o-oov  \nOptions";

	private final static Logger LOG = LoggerFactory.getLogger(FilterLines.class);


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new FilterLines(args).run();
	}
	
	@SuppressWarnings("static-access")
	public FilterLines(String args[]) {
		Options opts = new Options();

		opts.addOption(new Option("?", "help", false, "display this message"));
		opts.addOption(OptionBuilder.withLongOpt("file").withArgName("name").hasArg().withDescription("Specify the file or directory that contains '.txt' files that are used as source for testing perplexity with the specified language model. Specify '-' to pipe from stdin. (default: '-').").create("f"));
		opts.addOption(OptionBuilder.withLongOpt("out").withArgName("name").hasArg().withDescription("Specify the output file. Specify '-' to use stdout. (default: '-').").create("o"));
		opts.addOption(OptionBuilder.withLongOpt("runparallel").withDescription("Specify if processing should happen in parallel.").create("p"));
		opts.addOption(OptionBuilder.withLongOpt("maximum-perplexity").withArgName("perplexity-value").hasArg().withDescription("Specify the maximum perplexity value (with oov / 5th col) allowed, everything greater this value will not be printed. (default: '1000').").create("m"));

		try {
			CommandLine cmd = new ExtendedGnuParser(true).parse(opts, args);
			if (cmd.hasOption("help")) 
				CliUtils.print_usage_quit(System.err, StartLM.class.getSimpleName(), opts, USAGE_HEADER, null, 0);

			_file = cmd.getOptionValue("file", "-");
			_out = cmd.getOptionValue("out", "-");
			_parallel = cmd.hasOption("runparallel");
			_max_perp = Double.parseDouble(cmd.getOptionValue("maximum-perplexity", "1000"));

		} catch (Exception e) {
			LOG.error("{}: {}", e.getClass().getSimpleName(), e.getMessage());
			CliUtils.print_usage_quit(System.err, getClass().getSimpleName(), opts, USAGE_HEADER, String.format("%s: %s%n", e.getClass().getSimpleName(), e.getMessage()), 1);
		}
	}

	boolean _parallel;
	String _file;
	String _out;
	PrintStream _pout;
	double _max_perp;
	

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
		_pout = System.out;
		if(!"-".equals(_out)){
			try {
				_pout = new PrintStream(new FileOutputStream(new File(_out), true));
			} catch (FileNotFoundException e) {
				LOG.error("Could not open ouput file '{}' for writing.", _out, e);
				System.exit(1);
			}
		}

		if("-".equals(_file.trim())){
			LOG.info("Processing text from stdin ('{}').", _file);
			try{run(new InputStreamReader(System.in, "UTF-8"));}catch(Exception e){LOG.error("Could not process file '{}'.", _file, e);}
		}else{

			File f_or_d = new File(_file);
			if(!f_or_d.exists())
				throw new Error(String.format("File or directory '%s' not found.", _file));

			if(f_or_d.isFile()){
				LOG.info("Processing file '{}'.", f_or_d.getAbsolutePath());
				try{run(new InputStreamReader(new FileInputStream(f_or_d), "UTF-8"));}catch(Exception e){LOG.error("Could not process file '{}'.", f_or_d.getAbsolutePath(), e);}
			}

			if(f_or_d.isDirectory()){
				File[] txt_files = f_or_d.listFiles(new FileFilter(){
					@Override
					public boolean accept(File f) {
						return f.isFile() && f.getName().endsWith(".txt");
					}});

				for(int i = 0; i < txt_files.length; i++){
					File f = txt_files[i];
					LOG.info("Processing file '{}' ({}/{}).", f.getAbsolutePath(), i + 1, txt_files.length);

					try{ run(new InputStreamReader(new FileInputStream(f), "UTF-8")); }catch(Exception e){LOG.error("Could not process file '{}'.", f.getAbsolutePath(), e);}

				}
			}
		}
	}
	
	void run(Reader r) {
		if(_parallel)
			runParallel(r);
		else
			runSequential(r);
	}
	
	void runParallel(Reader r) {
		BufferedReader br = new BufferedReader(r);
		br.lines().parallel().forEach(line -> processLine(line));
	}
	
	void runSequential(Reader r) {
		
		long l = 0;
		for(LineIterator liter = new LineIterator(r); liter.hasNext(); ){
			if(++l % 5000 == 0)
				LOG.info("processing line {}.", l);
			String line = liter.next();
			processLine(line);
		}
		
	}

	void processLine(String line) {
		
		if(line.trim().isEmpty())
			return;
			
		// Expected format: text <tab> num-ngrams <tab> num-oov <tab> line-prob-log10 <tab> perplexity <tab> line-prob-w/o-oov-log10 <tab> perplexity-w/o-oov
        String[] splits = line.split("\t");
        int num_ngrams = Integer.parseInt(splits[1]);
        int num_oov = Integer.parseInt(splits[2]);
        double perp_w_oov = Double.parseDouble(splits[4]);
        
        if(num_oov >= (num_ngrams / 2d)) // if number of oov term is is half the number of all words we ignore this line 
        	return;
        
        if(perp_w_oov > _max_perp)
        	return;
		
		println(line);

	}
	
	public synchronized void println(String toPrint){
		_pout.println(toPrint);
	}

}
