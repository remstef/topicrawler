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

import de.tudarmstadt.lt.lm.perplexity.ModelPerplexity;
import de.tudarmstadt.lt.lm.service.AbstractStringProvider;
import de.tudarmstadt.lt.lm.service.StringProviderMXBean;
import de.tudarmstadt.lt.utilities.cli.CliUtils;
import de.tudarmstadt.lt.utilities.cli.ExtendedGnuParser;

/**
 *
 * @author Steffen Remus
 */
public class PerplexityClient implements Runnable {

	private final static String USAGE_HEADER = "Options";

	private final static Logger LOG = LoggerFactory.getLogger(PerplexityClient.class);

	public static void main(String[] args) throws Exception {
		new PerplexityClient(args).run();
	}

	/**
	 * 
	 */
	@SuppressWarnings("static-access")
	public PerplexityClient(String args[]) {
		Options opts = new Options();

		opts.addOption(new Option("?", "help", false, "display this message"));
		opts.addOption(OptionBuilder.withLongOpt("port").withArgName("port-number").hasArg().withDescription(String.format("Specifies the port on which the rmi registry listens (default: %d).", Registry.REGISTRY_PORT)).create("p"));
		opts.addOption(OptionBuilder.withLongOpt("selftest").withDescription("Run a selftest, compute perplexity of ngrams in specified LM.").create("s"));
		opts.addOption(OptionBuilder.withLongOpt("quiet").withDescription("Run with minimum outout on stdout.").create("q"));
		opts.addOption(OptionBuilder.withLongOpt("noov").hasOptionalArg().withArgName("{true|false}").withDescription("Do not consider oov terms, i.e. ngrams that end in an oov term. (default: false)").create());
		opts.addOption(OptionBuilder.withLongOpt("oovreflm").withArgName("identifier").hasArg().withDescription("Do not consider oov terms with respect to the provided lm, i.e. ngrams that end in an oov term in the referenced lm. (default use current lm)").create());
		opts.addOption(OptionBuilder.withLongOpt("host").withArgName("hostname").hasArg().withDescription("Specifies the hostname on which the rmi registry listens (default: localhost).").create("h"));
		opts.addOption(OptionBuilder.withLongOpt("file").withArgName("name").hasArg().withDescription("Specify the file or directory that contains '.txt' files that are used as source for testing perplexity with the specified language model. Specify '-' to pipe from stdin. (default: '-').").create("f"));
		opts.addOption(OptionBuilder.withLongOpt("out").withArgName("name").hasArg().withDescription("Specify the output file. Specify '-' to use stdout. (default: '-').").create("o"));
		opts.addOption(OptionBuilder.withLongOpt("id").withArgName("identifier").isRequired().hasArg().withDescription("Specify the name of the language model provider that you want to connect to.").create("i"));
		opts.addOption(OptionBuilder.withLongOpt("one_ngram_per_line").hasOptionalArg().withArgName("{true|false}").withDescription("Specify if the input file contains one ngram per line or sentences. (default: false)").create());

		try {
			CommandLine cmd = new ExtendedGnuParser(true).parse(opts, args);
			if (cmd.hasOption("help")) 
				CliUtils.print_usage_quit(System.err, StartLM.class.getSimpleName(), opts, USAGE_HEADER, null, 0);

			_host = cmd.getOptionValue("host", "localhost");
			_rmiport = Integer.parseInt(cmd.getOptionValue("port", String.valueOf(Registry.REGISTRY_PORT)));
			_file = cmd.getOptionValue("file", "-");
			_out = cmd.getOptionValue("out", "-");
			_id = cmd.getOptionValue("id");
			_host = cmd.getOptionValue("host", "localhost");
			_selftest = cmd.hasOption("selftest");
			_quiet = cmd.hasOption("quiet");
			_no_oov = cmd.hasOption("noov");
			if(_no_oov && cmd.getOptionValue("noov") != null)
				_no_oov = Boolean.parseBoolean(cmd.getOptionValue("noov"));
			_one_ngram_per_line = cmd.hasOption("one_ngram_per_line");
			if(_one_ngram_per_line && cmd.getOptionValue("one_ngram_per_line") != null)
				_one_ngram_per_line = Boolean.parseBoolean(cmd.getOptionValue("one_ngram_per_line"));
			_oovreflm_name = cmd.getOptionValue("oovreflm");

		} catch (Exception e) {
			LOG.error("{}: {}- {}", _rmi_string, e.getClass().getSimpleName(), e.getMessage());
			CliUtils.print_usage_quit(System.err, StartLM.class.getSimpleName(), opts, USAGE_HEADER, String.format("%s: %s%n", e.getClass().getSimpleName(), e.getMessage()), 1);
		}
		_rmi_string = String.format("rmi://%s:%d/%s", _host, _rmiport, _id);
	}

	String _rmi_string;
	int _rmiport;
	String _file;
	String _out;
	String _host;
	String _id;
	String _oovreflm_name;
	boolean _selftest;
	boolean _quiet;
	boolean _no_oov;
	boolean _one_ngram_per_line;
	PrintStream _pout;

	double _min_prob = Double.MAX_VALUE;
	double _max_prob = -Double.MAX_VALUE;
	List<String> _min_ngram = null;
	List<String> _max_ngram = null;
	StringProviderMXBean _lm_prvdr = null;
	StringProviderMXBean _lm_prvdr_oovref = null;
	ModelPerplexity<String> _perplexity_all = null;
	ModelPerplexity<String> _perplexity_file = null;

	long _oov_terms = 0;
	long _oov_ngrams = 0;
	long _num_ngrams = 0;

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		_lm_prvdr = AbstractStringProvider.connectToServer(_host, _rmiport, _id);
		if (_lm_prvdr == null){
			LOG.error("Could not connect to language model at '{}'",_rmi_string);
			return;
		}

		if(_oovreflm_name != null){
			_lm_prvdr_oovref = AbstractStringProvider.connectToServer(_host, _rmiport, _oovreflm_name);
			if (_lm_prvdr_oovref == null){
				LOG.error("Could not connect to language model at '{}'",_oovreflm_name);
				return;
			}
		}else
			_lm_prvdr_oovref = _lm_prvdr;
		
		_perplexity_all  = new ModelPerplexity<>(_lm_prvdr);
		_perplexity_file = new ModelPerplexity<>(_lm_prvdr);
		
		_pout = System.out;
		if(!"-".equals(_out.trim())){
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
					_perplexity_file.reset();
					try{ run(new InputStreamReader(new FileInputStream(f), "UTF-8")); }catch(Exception e){LOG.error("{}: Could not compute perplexity from file '{}'.", _rmi_string, f.getAbsolutePath(), e);}
					String o = String.format("%s: (intermediate results) \t %s \tPerplexity (file): %6.3e \tPerplexity (cum): %6.3e \tMax: log_10(p(%s))=%6.3e \tMin: log_10(p(%s))=%6.3e \tngrams (cum): %d \tOov-terms (cum): %d \tOov-ngrams (cum): %d", 
							_rmi_string, f.getAbsoluteFile(), _perplexity_file.get(), _perplexity_all.get(), _max_ngram, _max_prob, _min_ngram, _min_prob,
							_num_ngrams, _oov_terms, _oov_ngrams);
					LOG.info(o);
					if(!_quiet)
						write(String.format("%s%n", o));
				}
			}
		}

		String o = String.format("%s\t%s\tPerplexity: %6.3e \tMax: log_10(p(%s))=%6.3e \tMin: log_10(p(%s))=%6.3e \tngrams: %d \tOov-terms: %d \tOov-ngrams: %d", 
				_rmi_string, _file, _perplexity_all.get(), _max_ngram, _max_prob, _min_ngram, _min_prob, 
				_num_ngrams, _oov_terms, _oov_ngrams);
		LOG.info(o);
		if(!_quiet)
			write(String.format("%s%n", o));
		else
			write(String.format("%s\t%s\t%6.3e%n", _rmi_string, _file, _perplexity_all.get()));
	}

	@SuppressWarnings("unchecked")
	void run(Reader r) {
		long l = 0;
		for(LineIterator liter = new LineIterator(r); liter.hasNext(); ){
			if(++l % 5000 == 0)
				LOG.info("{}: processing line {}.", _rmi_string, l);

			String line = liter.next();
			if(line.trim().isEmpty())
				continue;

			List<String>[] ngrams;
			try {
				if(_one_ngram_per_line)
					ngrams = new List[]{Arrays.asList(line.split(" "))};
				else
					ngrams = _lm_prvdr.getNgrams(line);
				if(ngrams == null || ngrams.length == 0)
					continue;
			} catch (Exception e) {
				LOG.error("{}: Could not get ngrams from line {}: '{}'.", _rmi_string, l, StringUtils.abbreviate(line, 100), e);
				continue;
			}

			for(List<String> ngram : ngrams){
				if(ngram.isEmpty())
					continue;
				_num_ngrams++;
				try{

					if(_lm_prvdr_oovref.ngramContainsOOV(ngram)){
						_oov_ngrams++;
						if(_lm_prvdr_oovref.ngramEndsWithOOV(ngram)){
							_oov_terms++;
							if(_no_oov)
								continue;
						}
					}
					
					double log10prob = _perplexity_all.addLog10Prob(ngram);
					_perplexity_file.addLog10Prob(ngram);
					if(log10prob < _min_prob){
						_min_prob = log10prob;
						_min_ngram = ngram;
					}
					if(log10prob > _max_prob){
						_max_prob = log10prob;
						_max_ngram = ngram;
					}
				}catch(Exception e){
					LOG.error("{}: Could not add ngram '{}' to perplexity.", _rmi_string, ngram);
					continue;
				}
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
