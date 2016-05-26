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
import java.rmi.registry.Registry;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.perplexity.ModelPerplexity;
import de.tudarmstadt.lt.lm.service.AbstractStringProvider;
import de.tudarmstadt.lt.lm.service.StringProviderMXBean;
import de.tudarmstadt.lt.utilities.cli.CliUtils;
import de.tudarmstadt.lt.utilities.cli.ExtendedGnuParser;

/**
 * @author Steffen Remus
 *
 */
public class LineProbPerp implements Runnable {
	
	private final static String USAGE_HEADER = "Output: input-line <tab> num-ngrams <tab> num-oov <tab> line-prob-log10 <tab> perplexity <tab> line-prob-w/o-oov-log10 <tab> perplexity-w/o-oov  \nOptions";

	private final static Logger LOG = LoggerFactory.getLogger(LineProbPerp.class);


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new LineProbPerp(args).run();
	}
	
	@SuppressWarnings("static-access")
	public LineProbPerp(String args[]) {
		Options opts = new Options();

		opts.addOption(new Option("?", "help", false, "display this message"));
		opts.addOption(OptionBuilder.withLongOpt("port").withArgName("port-number").hasArg().withDescription(String.format("Specifies the port on which the rmi registry listens (default: %d).", Registry.REGISTRY_PORT)).create("rp"));
		opts.addOption(OptionBuilder.withLongOpt("host").withArgName("hostname").hasArg().withDescription("Specifies the hostname on which the rmi registry listens (default: localhost).").create("h"));
		opts.addOption(OptionBuilder.withLongOpt("file").withArgName("name").hasArg().withDescription("Specify the file or directory that contains '.txt' files that are used as source for testing perplexity with the specified language model. Specify '-' to pipe from stdin. (default: '-').").create("f"));
		opts.addOption(OptionBuilder.withLongOpt("out").withArgName("name").hasArg().withDescription("Specify the output file. Specify '-' to use stdout. (default: '-').").create("o"));
		opts.addOption(OptionBuilder.withLongOpt("name").withArgName("identifier").isRequired().hasArg().withDescription("Specify the name of the language model provider that you want to connect to.").create("i"));
		opts.addOption(OptionBuilder.withLongOpt("runparallel").withDescription("Specify if processing should happen in parallel.").create("p"));

		try {
			CommandLine cmd = new ExtendedGnuParser(true).parse(opts, args);
			if (cmd.hasOption("help")) 
				CliUtils.print_usage_quit(System.err, StartLM.class.getSimpleName(), opts, USAGE_HEADER, null, 0);

			_host = cmd.getOptionValue("host", "localhost");
			_rmiport = Integer.parseInt(cmd.getOptionValue("port", String.valueOf(Registry.REGISTRY_PORT)));
			_file = cmd.getOptionValue("file", "-");
			_out = cmd.getOptionValue("out", "-");
			_name = cmd.getOptionValue("name");
			_parallel = cmd.hasOption("runparallel");

		} catch (Exception e) {
			LOG.error("{}: {}- {}", _rmi_string, e.getClass().getSimpleName(), e.getMessage());
			CliUtils.print_usage_quit(System.err, getClass().getSimpleName(), opts, USAGE_HEADER, String.format("%s: %s%n", e.getClass().getSimpleName(), e.getMessage()), 1);
		}
		_rmi_string = String.format("rmi://%s:%d/%s", _host, _rmiport, _name);
	}

	boolean _parallel;
	String _rmi_string;
	int _rmiport;
	String _file;
	String _out;
	String _host;
	String _name;
	PrintStream _pout;
	
	ModelPerplexity<String> _perp = null;
	ModelPerplexity<String> _perp_oov = null;
	
	StringProviderMXBean _lm_prvdr = null;

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		_lm_prvdr = AbstractStringProvider.connectToServer(_host, _rmiport, _name);
		if (_lm_prvdr == null){
			LOG.error("Could not connect to language model at '{}'",_rmi_string);
			return;
		}
		
		_perp  = new ModelPerplexity<>(_lm_prvdr);
		_perp_oov = new ModelPerplexity<>(_lm_prvdr);
		
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
			LOG.info("{}: Processing text from stdin ('{}').", _rmi_string, _file);
			try{run(new InputStreamReader(System.in, "UTF-8"));}catch(Exception e){LOG.error("{}: Could not compute perplexity from file '{}'.", _rmi_string, _file, e);}
		}else{

			File f_or_d = new File(_file);
			if(!f_or_d.exists())
				throw new Error(String.format("%s: File or directory '%s' not found.", _rmi_string, _file));

			if(f_or_d.isFile()){
				LOG.info("{}: Processing file '{}'.", _rmi_string, f_or_d.getAbsolutePath());
				try{run(new InputStreamReader(new FileInputStream(f_or_d), "UTF-8"));}catch(Exception e){LOG.error("{}: Could not compute perplexity from file '{}'.", _rmi_string, f_or_d.getAbsolutePath(), e);}
			}

			if(f_or_d.isDirectory()){
				File[] txt_files = f_or_d.listFiles(new FileFilter(){
					@Override
					public boolean accept(File f) {
						return f.isFile() && f.getName().endsWith(".txt");
					}});

				for(int i = 0; i < txt_files.length; i++){
					File f = txt_files[i];
					LOG.info("{}: Processing file '{}' ({}/{}).", _rmi_string, f.getAbsolutePath(), i + 1, txt_files.length);

					try{ run(new InputStreamReader(new FileInputStream(f), "UTF-8")); }catch(Exception e){LOG.error("{}: Could not compute perplexity from file '{}'.", _rmi_string, f.getAbsolutePath(), e);}

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
		br.lines().parallel().forEach(line -> processLine(line, new ModelPerplexity<>(_lm_prvdr), new ModelPerplexity<>(_lm_prvdr)));
	}
	
	void runSequential(Reader r) {
		
		long l = 0;
		for(LineIterator liter = new LineIterator(r); liter.hasNext(); ){
			if(++l % 5000 == 0)
				LOG.info("{}: processing line {}.", _rmi_string, l);
			String line = liter.next();
			processLine(line, _perp, _perp_oov);
		}
		
	}

	void processLine(String line, ModelPerplexity<String> perp, ModelPerplexity<String> perp_oov) {
		
		if(line.trim().isEmpty()){
			println(getOutputLine(line, 0, 0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
			return;
		}
		
		List<String>[] ngrams;
		try {
			List<String> tokens = _lm_prvdr.tokenizeSentence(line);
			if(tokens == null || tokens.isEmpty()){
				println(getOutputLine(line, 0, 0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
				return;
			}
			ngrams = _lm_prvdr.getNgramSequence(tokens);
			if(ngrams == null || ngrams.length == 0){
				println(getOutputLine(line, 0, 0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
				return;
			}
		} catch (Exception e) {
			LOG.error("{}: Could not get ngrams from line '{}'.", _rmi_string, StringUtils.abbreviate(line, 100), e);
			return;
		}

		perp.reset();
		perp_oov.reset();
		for(List<String> ngram : ngrams){
			if(ngram.isEmpty())
				continue;
			try{
				perp_oov.addLog10Prob(ngram);
				if(!_lm_prvdr.ngramEndsWithOOV(ngram))
					perp.addLog10Prob(ngram);	
				
			}catch(Exception e){
				LOG.error("{}: Could not add ngram '{}' to perplexity.", _rmi_string, ngram);
				continue;
			}
		}		
		println(getOutputLine(line, perp_oov.getN(), perp_oov.getN() - perp.getN(), perp_oov.getLog10Probs(), perp_oov.get(), perp.getLog10Probs(), perp.get()));

	}
	
	public String getOutputLine(String line, long num_ngrams, long num_oov, double log10prob, double perp, double log10prob_oov, double perp_oov){
		return String.format("%s\t%d\t%d\t%e\t%e\t%e\t%e", 
				line, 
				num_ngrams,
				num_oov,
				log10prob,
				perp,
				log10prob_oov,
				perp_oov);
		
	}
	
	public synchronized void println(String toPrint){
		_pout.println(toPrint);
	}

}
