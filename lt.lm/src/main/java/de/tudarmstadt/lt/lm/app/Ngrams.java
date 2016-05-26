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
package de.tudarmstadt.lt.lm.app;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.AbstractLanguageModel;
import de.tudarmstadt.lt.lm.DummyLM;
import de.tudarmstadt.lt.lm.service.AbstractStringProvider;
import de.tudarmstadt.lt.lm.service.LMProviderUtils;
import de.tudarmstadt.lt.lm.service.LtSegProvider;
import de.tudarmstadt.lt.lm.util.Properties;
import de.tudarmstadt.lt.utilities.cli.CliUtils;
import de.tudarmstadt.lt.utilities.cli.ExtendedGnuParser;
import de.tudarmstadt.lt.utilities.collections.FixedSizeFifoLinkedList;

/**
 * TODO: parallelize, merge GenerateNgrams into this 
 * 
 * @author Steffen Remus
 */
public class Ngrams  implements Runnable{

	private final static String USAGE_HEADER = "Options:";

	private static Logger LOG = LoggerFactory.getLogger(Ngrams.class);

	public static void main(String[] args) throws ClassNotFoundException {
		new Ngrams(args).run();
	}
	
	public Ngrams() { /* default constructor, pass options via new Ngrams(){{ param=value; ... }} */ }

	@SuppressWarnings("static-access")
	public Ngrams(String[] args) {

		Options opts = new Options();
		opts.addOption(OptionBuilder.withLongOpt("help").withDescription("Display help message.").create("?"));
		opts.addOption(OptionBuilder.withLongOpt("ptype").withArgName("class").hasArg().withDescription("specify the instance of the language model provider that you want to use: {LtSegProvider, BreakIteratorStringProvider, UimaStringProvider, PreTokenizedStringProvider} (default: LtSegProvider)").create("p"));
		opts.addOption(OptionBuilder.withLongOpt("cardinality").withArgName("ngram-order").hasArg().withDescription("Specify the cardinality of the ngrams (min. 1). Specify a range using 'from-to'. (Examples: 5 = extract 5grams; 1-5 = extract 1grams, 2grams, ..., 5grams; default: 1-5).").create("n"));
		opts.addOption(OptionBuilder.withLongOpt("file").withArgName("filename").hasArg().withDescription("specify the file to read from. Specify '-' to read from stdin. (default: '-')").create("f"));
		opts.addOption(OptionBuilder.withLongOpt("out").withArgName("name").hasArg().withDescription("Specify the output file. Specify '-' to use stdout. (default: '-').").create("o"));
		opts.addOption(OptionBuilder.withLongOpt("accross_sentences").hasOptionalArg().withArgName("{true|false}").withDescription("Generate Ngrams across sentence boundaries.").create("a"));
		
		try {
			CommandLine cmd = new ExtendedGnuParser(true).parse(opts, args);
			if (cmd.hasOption("help")) 
				CliUtils.print_usage_quit(System.err, Ngrams.class.getSimpleName(), opts, USAGE_HEADER, null, 0);

			_provider_type =		 	cmd.getOptionValue("ptype", LtSegProvider.class.getSimpleName());
			_file =			 			cmd.getOptionValue("file", "-");
			_out = 						cmd.getOptionValue("out", "-");
			_accross_sentences = 		cmd.hasOption("accross_sentences");
			String order =				cmd.getOptionValue("cardinality","1-5");
			if(_accross_sentences && cmd.getOptionValue("accross_sentences") != null)
				_accross_sentences = Boolean.parseBoolean(cmd.getOptionValue("accross_sentences"));

			int dash_index = order.indexOf('-');
			_order_to = Integer.parseInt(order.substring(dash_index + 1, order.length()).trim());
			_order_from = _order_to;
			if(dash_index == 0)
				_order_from = 1;
			if(dash_index > 0)
				_order_from = Math.max(1, Integer.parseInt(order.substring(0,dash_index).trim()));

		} catch (Exception e) {
			CliUtils.print_usage_quit(System.err, Ngrams.class.getSimpleName(), opts, USAGE_HEADER, String.format("%s: %s%n", e.getClass().getSimpleName(), e.getMessage()), 1);
		}
	}

	String _provider_type;
	String _file;
	String _out;
	int _order_to;
	int _order_from;
	PrintStream _pout;
	boolean _accross_sentences;
	
	boolean _insert_bos = Properties.insertSentenceTags() == 1 || Properties.insertSentenceTags() == 3;
	boolean _insert_eos = Properties.insertSentenceTags() == 2 || Properties.insertSentenceTags() == 3;
	String _lang = Properties.defaultLanguageCode();
	AbstractStringProvider _prvdr;
	
	List<String> _ngram;
	long _num_ngrams;

	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		_num_ngrams = 0l;
		_ngram = new FixedSizeFifoLinkedList<>(_order_to);
		 
		_pout = System.out;
		if(!"-".equals(_out.trim())){
			try {
				if(_out.endsWith(".gz"))
					_pout = new PrintStream(new GZIPOutputStream(new FileOutputStream(new File(_out))));
				else
					_pout = new PrintStream(new FileOutputStream(new File(_out), true));
			} catch (IOException e) {
				LOG.error("Could not open ouput file '{}' for writing.", _out, e);
				System.exit(1);
			}
		}
		
		try{
			if(_prvdr == null){
				_prvdr = StartLM.getStringProviderInstance(_provider_type);
				_prvdr.setLanguageModel(new DummyLM<>(_order_to));
			}
		}catch(Exception e){
			LOG.error("Could not initialize Ngram generator. {}: {}", e.getClass(), e.getMessage(), e);
		}
		
		if("-".equals(_file.trim())){
			LOG.info("Processing text from stdin ('{}').", _file);
			try{run(new InputStreamReader(System.in, "UTF-8"), _file);}catch(Exception e){LOG.error("Could not generate ngram from from file '{}'.", _file, e);}
		}else{

			File f_or_d = new File(_file);
			if(!f_or_d.exists())
				throw new Error(String.format("File or directory '%s' not found.", _file));

			if(f_or_d.isFile()){
				LOG.info("Processing file '{}'.", f_or_d.getAbsolutePath());
				try{
					run(new InputStreamReader(new FileInputStream(f_or_d), "UTF-8"), _file);
				}catch(Exception e){
					LOG.error("Could not generate ngrams from file '{}'.", f_or_d.getAbsolutePath(), e);
				}
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
					try{ 
						run(new InputStreamReader(new FileInputStream(f), "UTF-8"), f.getAbsolutePath()); 
					}catch(Exception e){
						LOG.error("Could not generate ngrams from file '{}'.", f.getAbsolutePath(), e);
					}
				}
			}
		}
		LOG.info("Generated {} ngrams.", _num_ngrams);
		if(!"-".equals(_out.trim()))
			_pout.close();
		
	}
	
	public void run(Reader r, String f){
		if(!_accross_sentences)
			run_within_sentences(r, f);
		else
			run_across_sentences(r, f);
	}
	
	public void run_within_sentences(Reader r, String f) {

		LineIterator liter = new LineIterator(r);
		for(long lc = 0; liter.hasNext();){
			if(++lc % 1000 == 0)
				LOG.info("Processing line {}:{}", f, lc);
			try{
				String line = liter.next();
				if(line.trim().isEmpty())
					continue;
				List<String> sentences = _prvdr.splitSentences(line);
				if(sentences == null || sentences.isEmpty())
					continue;
				for(String sentence : sentences){
					if(sentence == null || sentence.trim().isEmpty())
						continue;
					for(int n = _order_from; n <= _order_to; n++){
						List<String>[] ngrams = null;
						try{
							List<String> tokens = _prvdr.tokenizeSentence(sentence);
							if(tokens == null || tokens.isEmpty())
								continue;
							ngrams = _prvdr.getNgramSequence(tokens, n);
							if(ngrams == null || ngrams.length < 1)
								continue;
						}
						catch(Exception e){
							LOG.warn("Could not get ngram of cardinality {} from String '{}' in line '{}' from file '{}'.", n, StringUtils.abbreviate(line, 100), lc, f);
							continue;
						}
						for(List<String> ngram : ngrams){
							if(ngram == null || ngram.isEmpty())
								continue;
							_pout.println(StringUtils.join(ngram, " "));
						}
						_pout.flush();
						_num_ngrams += ngrams.length;
					}
				}
			}catch(Exception e){
				LOG.warn("Could not process line '{}' in file '{}'.", lc, f);
			}
		}
	}

	public void run_across_sentences(Reader r, String f) {
		LineIterator liter = new LineIterator(r);
		for(long lc = 0; liter.hasNext();){
			if(++lc % 1000 == 0)
				LOG.info("Processing line {}:{}", f, lc);
			try{
				String line = liter.next();
				if(line.trim().isEmpty())
					continue;
				List<String> sentences = _prvdr.splitSentences(line);
				if(sentences == null || sentences.isEmpty())
					continue;
				for(String sentence : sentences){
					if(sentence == null || sentence.isEmpty())
						continue;
					List<String> tokens = null;
					try{
						tokens = _prvdr.tokenizeSentence(sentence);
						if(tokens == null || tokens.isEmpty())
							continue;
					}
					catch(Exception e){
						LOG.warn("Could not get tokens from from String '{}' in line '{}' from file '{}'.", StringUtils.abbreviate(line, 100), lc, f);
						continue;
					}
					for(String word : tokens){
						if(word == null || word.trim().isEmpty())
							continue;
						_ngram.add(word);
						for(int n = Math.max(_ngram.size()-_order_to,0); n <= Math.min(_ngram.size() - _order_from, _ngram.size()-1); n++)
							_pout.println(StringUtils.join(_ngram.subList(n, _ngram.size()), " "));
						_num_ngrams++;
					}
					_pout.flush();
				}
			}catch(Exception e){
				LOG.warn("Could not process line '{}' in file '{}'.", lc, f);
			}
		}
	}

}
