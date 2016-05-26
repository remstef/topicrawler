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

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.service.AbstractStringProvider;
import de.tudarmstadt.lt.lm.service.StringProviderMXBean;
import de.tudarmstadt.lt.utilities.cli.CliUtils;
import de.tudarmstadt.lt.utilities.cli.ExtendedGnuParser;

/**
 *
 * Get probabilities for provided ngrams 
 *
 * @author Steffen Remus
 */
public class NgramProbs implements Runnable {

	private final static String USAGE_HEADER = "Options";

	private final static Logger LOG = LoggerFactory.getLogger(NgramProbs.class);

	public static void main(String[] args) throws Exception {
		new NgramProbs(args).run();
	}

	/**
	 * 
	 */
	@SuppressWarnings("static-access")
	public NgramProbs(String args[]) {
		Options opts = new Options();

		opts.addOption(new Option("?", "help", false, "display this message"));
		opts.addOption(OptionBuilder.withLongOpt("port").withArgName("port-number").hasArg().withDescription(String.format("Specifies the port on which the rmi registry listens (default: %d).", Registry.REGISTRY_PORT)).create("p"));
		opts.addOption(OptionBuilder.withLongOpt("host").withArgName("hostname").hasArg().withDescription("Specifies the hostname on which the rmi registry listens (default: localhost).").create("h"));
		opts.addOption(OptionBuilder.withLongOpt("file").withArgName("name").hasArg().withDescription("Specify the file or directory that contains '.txt' with one ngram per line. Specify '-' to pipe from stdin. (default: '-').").create("f"));
		opts.addOption(OptionBuilder.withLongOpt("out").withArgName("name").hasArg().withDescription("Specify the output file. Specify '-' to use stdout. (default: '-').").create("o"));
		opts.addOption(OptionBuilder.withLongOpt("name").withArgName("identifier").isRequired().hasArg().withDescription("Specify the name of the language model provider that you want to connect to.").create("i"));

		try {
			CommandLine cmd = new ExtendedGnuParser(true).parse(opts, args);
			if (cmd.hasOption("help")) 
				CliUtils.print_usage_quit(System.err, StartLM.class.getSimpleName(), opts, USAGE_HEADER, null, 0);

			_host = cmd.getOptionValue("host", "localhost");
			_rmiport = Integer.parseInt(cmd.getOptionValue("port", String.valueOf(Registry.REGISTRY_PORT)));
			_file = cmd.getOptionValue("file", "-");
			_out = cmd.getOptionValue("out", "-");
			_name = cmd.getOptionValue("name");
			_host = cmd.getOptionValue("host", "localhost");

		} catch (Exception e) {
			LOG.error("{}: {}- {}", _rmi_string, e.getClass().getSimpleName(), e.getMessage());
			CliUtils.print_usage_quit(System.err, StartLM.class.getSimpleName(), opts, USAGE_HEADER, String.format("%s: %s%n", e.getClass().getSimpleName(), e.getMessage()), 1);
		}
		_rmi_string = String.format("rmi://%s:%d/%s", _host, _rmiport, _name);
	}

	String _rmi_string;
	int _rmiport;
	String _file;
	String _out;
	String _host;
	String _name;
	PrintStream _pout;

	double _min_prob = Double.MAX_VALUE;
	double _max_prob = -Double.MAX_VALUE;
	List<String> _min_ngram = null;
	List<String> _max_ngram = null;
	StringProviderMXBean _lm_prvdr = null;

	long _oov_terms = 0;
	long _oov_ngrams = 0;
	long _num_ngrams = 0;

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
		
		String o = String.format("%s\t%s\tMax=%s (%6.3e)\tMin=%s (%6.3e)",
				_rmi_string,
				_file,
				_max_ngram, Math.pow(10, _max_prob),
				_min_ngram, Math.pow(10, _min_prob));
		LOG.info(o);
		
	}

	void run(Reader r) {
		long l = 0;
		for(LineIterator liter = new LineIterator(r); liter.hasNext(); ){
			if(++l % 5000 == 0)
				LOG.info("{}: processing line {}:{}.", _rmi_string, _file, l);

			String line = liter.next();
			if(line.trim().isEmpty())
				continue;
			
			List<String> ngram = Arrays.asList(line.split(" "));
			
			if(ngram.isEmpty())
				continue;
			_num_ngrams++;
			try{
				if(_lm_prvdr.ngramContainsOOV(ngram)){
					_oov_ngrams++;
					if(_lm_prvdr.ngramEndsWithOOV(ngram)){
						_oov_terms++;
					}
				}

				double log10prob = _lm_prvdr.getNgramLog10Probability(ngram);

				write(String.format("%s\t%6.3e\t%s\t%s\t%s%n",
						StringUtils.join(ngram, ' '),
						Math.pow(10, log10prob),
						_rmi_string,
						_file,
						StringUtils.join(_lm_prvdr.getNgramAsWords(_lm_prvdr.getNgramAsIds(ngram)), ' ')));
				
				if(log10prob > _max_prob){
					_max_prob = log10prob;
					_max_ngram = ngram;
				}
				if(log10prob < _min_prob){
					_min_prob = log10prob;
					_min_ngram = ngram;
				}	
			}catch(Exception e){
				LOG.error("{}: Could not add ngram '{}' to perplexity.", _rmi_string, ngram);
				continue;
			}
		}
	}
	
	void write(String towrite){
		if("-".equals(_out))
			_pout.print(towrite);
		else{
			File lock = new File(_out + ".lock");
			while(lock.exists())
				try { Thread.sleep(1000); } catch (InterruptedException e) { }
			try {
				lock.createNewFile();
			} catch (IOException e) {
				LOG.warn("Could not create lock '{}'.", lock, e);
			}
			_pout.print(towrite);
			lock.delete();
		}
	}
	
}
