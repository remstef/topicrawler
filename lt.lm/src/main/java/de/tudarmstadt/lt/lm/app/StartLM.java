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
import java.lang.management.ManagementFactory;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.LanguageModel;
import de.tudarmstadt.lt.lm.LanguageModelHelper;
import de.tudarmstadt.lt.lm.PseudoSymbol;
import de.tudarmstadt.lt.lm.berkeleylm.BerkeleyLM;
import de.tudarmstadt.lt.lm.lucenebased.CountingStringLM;
import de.tudarmstadt.lt.lm.lucenebased.KneserNeyLMRecursive;
import de.tudarmstadt.lt.lm.mapbased.CountingLM;
import de.tudarmstadt.lt.lm.mapbased.LaplaceSmoothedLM;
import de.tudarmstadt.lt.lm.perplexity.ModelPerplexity;
import de.tudarmstadt.lt.lm.service.AbstractStringProvider;
import de.tudarmstadt.lt.lm.service.BreakIteratorStringProvider;
import de.tudarmstadt.lt.lm.service.LtSegProvider;
import de.tudarmstadt.lt.lm.service.PreTokenizedStringProvider;
import de.tudarmstadt.lt.lm.service.StringProviderMXBean;
import de.tudarmstadt.lt.lm.util.Properties;
import de.tudarmstadt.lt.utilities.cli.CliUtils;
import de.tudarmstadt.lt.utilities.cli.ExtendedGnuParser;
import de.tudarmstadt.lt.utilities.LogUtils;

/**
 *
 * @author Steffen Remus
 * 
 * 
 */
public class StartLM implements Runnable{

	private final static Logger LOG = LoggerFactory.getLogger(StartLM.class);
	private final static String USAGE_HEADER = "Options:";

	public static void main(String[] args) throws ClassNotFoundException {
		new StartLM(args).run();
	}

	/**
	 * default constructor
	 * 
	 * set necessary variables by using <code>new StartLM(){{ _variable = value; ... }} </code>
	 * 
	 */
	public StartLM() { /* NOTHING TO DO */ }

	@SuppressWarnings("static-access")
	public StartLM(String[] args) {
		Options opts = new Options();
		opts.addOption(new Option("?", "help", false, "display this message"));
		opts.addOption(OptionBuilder.withLongOpt("host").withArgName("hostname").hasArg().withDescription("specifies the hostname on which the rmi registry listens (default: localhost)").create("h"));
		opts.addOption(OptionBuilder.withLongOpt("rmiport").withArgName("port-number").hasArg().withDescription(String.format("specifies the port on which rmi registry listens (default: %d)", Registry.REGISTRY_PORT)).create("rp"));
		opts.addOption(OptionBuilder.withLongOpt("port").withArgName("port-number").hasArg().withDescription("specifies the port on which this service should populate (default: 0, which means a random port will be assigned)").create("p"));
		opts.addOption(OptionBuilder.withLongOpt("lmtype").withArgName("class").hasArg().withDescription("specify the instance of the language model that you want to use: {BerkeleyLM, CountingLM, LaplaceSmoothedLM, CountingStringLM, KneserNeyLM, (experimental: KneserNeyLMRecursive, PoptKneserNeyLMRecursive, ModifiedKNeserNeyLMRecursive)} (default: BerkeleyLM)").create("t"));
		opts.addOption(OptionBuilder.withLongOpt("ptype").withArgName("class").hasArg().withDescription("specify the instance of the language model provider that you want to use: {LtSegProvider, BreakIteratorStringProvider, PreTokenizedStringProvider} (default: LtSegProvider)").create("pt"));
		opts.addOption(OptionBuilder.withLongOpt("dir").withArgName("directory").isRequired().hasArg().withDescription("specify the directory that contains '.txt' files that are used as source for this language model").create("d"));
		opts.addOption(OptionBuilder.withLongOpt("order").withArgName("ngram-order").hasArg().withDescription("specify the order for this language model").create("n"));
		opts.addOption(OptionBuilder.withLongOpt("identifier").withArgName("name").hasArg().withDescription("specify a name/identifier for the language model. If no name is given, a random name will be generated.").create("i"));
		opts.addOption(OptionBuilder.withLongOpt("overwrite").withDescription("Overwrite existing saved or temporary files.").create("w"));
		opts.addOption(OptionBuilder.withLongOpt("discount").withArgName("Discount value in [0,1]").hasArg().withDescription("Uniform discount value for Lucene based Kneser-Ney LM.").create());
		opts.addOption(OptionBuilder.withLongOpt("mincount").withArgName("int").hasArg().withDescription("(Only applicable for Lucene Based LMs) - Specify the number of times an ngram must occur to be considered in further calculations. Ngrams with counts below mincount are filtered. (default: 1).").create("m"));

		try {
			CommandLine cmd = new ExtendedGnuParser(true).parse(opts, args);
			if (cmd.hasOption("help")) 
				CliUtils.print_usage_quit(System.err, StartLM.class.getSimpleName(), opts, USAGE_HEADER, null, 0);

			_rmiRegistryPort = Integer.parseInt(cmd.getOptionValue("rmiport", String.valueOf(Registry.REGISTRY_PORT)));
			_port = Integer.parseInt(cmd.getOptionValue("port", "0"));
			_srcdir = cmd.getOptionValue("dir");
			_n = Integer.parseInt(cmd.getOptionValue("order", "5"));
			_type_lm = cmd.getOptionValue("lmtype", BerkeleyLM.class.getSimpleName());
			_type_provider = cmd.getOptionValue("ptype", LtSegProvider.class.getSimpleName());
			_name = cmd.getOptionValue("identifier", String.valueOf(System.currentTimeMillis()));
			_host = cmd.getOptionValue("host", "localhost");
			_overwrite = cmd.hasOption("overwrite");
			_discount = Double.parseDouble(cmd.getOptionValue("discount", "-1"));
			_mincount = Integer.parseInt(cmd.getOptionValue("mincount", "1"));
			// String[] non_named_args = cmd.getArgs();
			_providerJmxBeanName = new ObjectName("de.tudarmstadt.lt.lm:type=ProviderService");

		} catch (Exception e) {
			LOG.error("{}: {}", e.getClass().getSimpleName(), e.getMessage());
			CliUtils.print_usage_quit(System.err, StartLM.class.getSimpleName(), opts, USAGE_HEADER, String.format("%s: %s%n", e.getClass().getSimpleName(), e.getMessage()), 1);
		}
	}

	Registry _registry;
	int _rmiRegistryPort;
	int _port;
	String _srcdir;
	int _n;
	double _discount;
	String _type_lm;
	String _type_provider;
	String _name;
	String _host;
	boolean _overwrite;
	int _mincount;
	AbstractStringProvider _providerService;
	MBeanServer _mbs;
	ObjectName _providerJmxBeanName;
	boolean _app_serves_rmi_registry = false;
	Thread _shutdown_hook = new Thread(new Runnable() {
		@Override
		public void run() {
			// TODO: implement me
		}
	});

	int _maxresults = 40;
	private Scanner _stdin_scan;
	StringProviderMXBean _lm;

	String readInput(String message){
		String r = null;
		System.out.print(message);
		if(_stdin_scan.hasNextLine()){ // block thread
			r = _stdin_scan.nextLine();
			return r;
		}
		return null;
	}

	private void exit() {
		LOG.info("Exiting.");
		stopLM();
		if(_app_serves_rmi_registry){
			String[] rmi_bounded_apps = new String[0];
			try {
				rmi_bounded_apps = _registry.list();
				if(rmi_bounded_apps.length == 0){
					stopRMI();
					System.exit(0);
				}
			} catch (Exception e) {/* */}

			String input_line = null;
			while((input_line = readInput(String.format("App serves as RMI server for the following apps: %s.%nType 'y' if you really want to quit serving RMI, else type 'n': %n%s $> ", Arrays.toString(rmi_bounded_apps), _name ))) == null);
			if("y".equals(input_line.toLowerCase())){
				stopRMI();
				System.exit(0);
			}

			else 
				return;
		}
		System.exit(0);
	}

	@Override
	public void run() {
		Runtime.getRuntime().addShutdownHook(_shutdown_hook);
		try {
			// start connect language model server
			startRMI();
			startLM();

		} catch (Exception e1) {
			LOG.error("Could not start to Language Model Server.");
		}

		_stdin_scan = new Scanner(System.in);

		String status = getStatus();

		for (String input_line = null; (input_line = readInput(String.format("+++%n%s What do you want to do?%n"
				+ "Type ':i' to list LM Server infos, %n"
				+ "Type ':pn' to compute ngram probabilities, %n"
				+ "Type ':ps' to compute sequence probabilities, %n"
				+ "Type ':pw' to predict a sequence of words, %n"
				+ "Type ':l' to list ngrams, %n"
				+ "Type ':s' to stop lm, %n"
				+ "Type ':r' to restart lm, %n"
				+ "Type ':v' to change verbosity. %n"
				+ "Type ':q' to quit program. %n"
				+ "%s $> ", status, _name))) != null;) {

			if(input_line.isEmpty())
				continue;
			String action = input_line.toLowerCase();
			try{
				if(":q".equals(action))
					exit();
				else if(":s".equals(action))
					stopLM();
				else if(":r".equals(action))
					restartLM();
				else if(":i".equals(action) || "i".equals(action))
					showLmInfo();
				else if(":ps".equals(action) || "ps".equals(action))
					computeSequenceProbabilities();
				else if(":pn".equals(action) || "pn".equals(action))
					computeNgramProbabilities();
				else if(":pw".equals(action) || "pw".equals(action))
					predict();
				else if(":l".equals(action) || "l".equals(action))
					listNgrams();
				else if(":v".equals(action) || "v".equals(action))
					changeLogLevel();
				else
					System.out.println(String.format("Unknown action '%s'.", action));
			}catch(Error e){
				e.printStackTrace();
			}

			status = getStatus();
		}

	}

	/**
	 * 
	 */
	void changeLogLevel() {
		String current_level = LogUtils.getLogLevel(Properties.project_name);
		for(String input_line = null; !":q".equalsIgnoreCase((input_line = readInput(String.format("Enter desired log level {trace,debug,info,warn,error} or to change log level (current log level=%s). Hit '<enter>' to leave log level unchanged. %n%s $> ", current_level, _name))));) {
			input_line = input_line.trim();
			if(input_line.isEmpty()){
				System.out.println("Log level unchanged.");
				return;
			}
			if(LogUtils.setLogLevel(Properties.project_name, input_line)){
				if(current_level.equalsIgnoreCase(LogUtils.getLogLevel(Properties.project_name))){
					if(!current_level.equalsIgnoreCase(input_line)){
						System.out.println("Log level unchanged.");
						continue;
					}
				}
				System.out.format("Log level of logger '%s' set to '%s'.%n", Properties.project_name, input_line);
				return;
			}
			LOG.error("Could not change log level. Please try again and see logfiles for details.");
		}
	}

	String getStatus() {
		String rmi_status = "RMI Server connected; ";
		if(_app_serves_rmi_registry)
			rmi_status = "RMI Server published and connected; ";
		String lm_status = "LM server is stopped; ";
		if(_providerService != null)
			lm_status = "LM server is running; ";
		return rmi_status + lm_status;
	}

	void predict(){
		if(_providerService == null){
			System.out.println("LM Server is not runnning.");
			return;
		}
		ModelPerplexity<String> p = new ModelPerplexity<>(_providerService);
		List<String> sequence = new LinkedList<>();
		sequence.add(PseudoSymbol.SEQUENCE_START.asString());
		System.out.format("Initial sequence: '%s'.%n", StringUtils.join(sequence, ' '));
		for (String input_line = null; !":q".equals((input_line = readInput(String.format("Enter space separated sequence to start with, e.g. '<s> The', hit <Enter> to continue current sequence. Type ':q' to quit predicting words: %n%s $> ", _name))));) {
			if(!input_line.isEmpty()){
				p.reset();
				sequence.clear();
				sequence.addAll(Arrays.asList(input_line.split(" ")));
			}
			try {
				String word = _providerService.predictNextWord(sequence);
				sequence.add(word);
				List<String>[] ngrams = _providerService.getNgramSequence(sequence);
				double log10prob = p.addLog10Prob(ngrams[ngrams.length-1]);
				System.out.format(" predicted word: %s (p(%s)=%6.3e) %n", word, ngrams[ngrams.length-1], Math.pow(10, log10prob));
				double sequenceLog10prob = p.getLog10Probs();
				System.out.format(" new sequence: %n%n %s%n%n Prob: %6.3e (log10=%6.3e) / Prob normalized by %d ngrams: %6.3e (log10=%6.3e) %n Perplixity: %6.3e %n%n",
						StringUtils.join(sequence, " "),
						Math.pow(10, sequenceLog10prob), sequenceLog10prob,
						p.getN(), Math.pow(10, sequenceLog10prob/(double)p.getN()), sequenceLog10prob/(double)p.getN(),
						p.get());
			} catch (Exception e) {
				LOG.warn("{}: {}", e.getClass().getSimpleName(), e.getMessage());
			}
		}
	}

	void computeSequenceProbabilities(){
		if(_providerService == null){
			System.out.println("LM Server is not runnning.");
			return;
		}
		for (String input_line = null; !":q".equals((input_line = readInput(String.format("Enter sequence, e.g. 'hello world'. Type ':q' to quit computing sequence probabilities: %n%s $> ", _name))));) {
			input_line = input_line.trim();
			try {
				_providerService.resetPerplexity();

				double log10_prob_ = _providerService.getSequenceLog10Probability(input_line);
				double prob10_ = Math.pow(10, log10_prob_);
				double log2_prob_ = log10_prob_ / Math.log10(2);
				System.out.format("+++%nprob=%g (log10=%g, log2=%g) %n", prob10_, log10_prob_, log2_prob_);

				double perp = _providerService.getPerplexity(input_line, false);
				System.out.format("perp=%g %n%n", perp);
				perp = _providerService.getPerplexity(input_line, true);
				System.out.format("perp (no-oov)=%g %n%n", perp);
				double log10_prob, prob10,log2_prob;
				List<String>[] ngram_sequence = _providerService.getNgrams(input_line);
				System.out.format("+++ #ngrams= %d +++ %n", ngram_sequence.length);
				System.out.format("[initial cumulative Perp=%g] %n%n", _providerService.getPerplexity());
				for (int i = 0; i < ngram_sequence.length; i++) {
					List<String> ngram = ngram_sequence[i];
					log10_prob = _providerService.getNgramLog10Probability(ngram);
					prob10 = Math.pow(10, log10_prob);
					log2_prob = log10_prob / Math.log10(2);
					int[] ngram_ids = _providerService.getNgramAsIds(ngram);
					List<String> ngram_lm = _providerService.getNgramAsWords(ngram_ids);
					System.out.format("%s %n => %s %n =  %g (log10=%g, log2=%g) %n",
							ngram.toString(),
							ngram_lm.toString(),
							prob10,
							log10_prob,
							log2_prob);
					_providerService.addToPerplexity(ngram);
					System.out.format("   [cumulative perp=%g] %n%n", _providerService.getPerplexity());
				}
				System.out.format("+++ #ngrams= %d +++ %n", ngram_sequence.length);
				System.out.format("prob=%g (log10=%g, log2=%g) %n", prob10_, log10_prob_, log2_prob_);
				System.out.format("perp=%g %n%n", perp);

			} catch (Exception e) {
				LOG.warn("{}: {}", e.getClass().getSimpleName(), e.getMessage());
			}

		}
	}

	void computeNgramProbabilities(){
		if(_providerService == null){
			System.out.println("LM Server is not runnning.");
			return;
		}
		for (String input_line = null; !":q".equals((input_line = readInput(String.format("Enter ngram with space separated tokens, e.g. 'hello world'. Type ':q' to quit computing ngram probabilities: %n%s $> ", _name))));) {
			List<String> ngram = Arrays.asList(input_line.trim().split(" "));
			try {
				if(ngram.size() > _providerService.getLmOrder()){
					System.out.format("%s is too long. Try an ngram with max cardinality %d. ", ngram, _providerService.getLmOrder());
					continue;
				}

				double log10_prob = _providerService.getNgramLog10Probability(ngram);
				double prob10 = Math.pow(10, log10_prob);
				double log2_prob = log10_prob / Math.log10(2);

				int[] ngram_ids = _providerService.getNgramAsIds(ngram);
				List<String> ngram_lm = _providerService.getNgramAsWords(ngram_ids);
				System.out.format("%s %n => %s %n => %s %n =  %g (log10=%g, log2=%g) %n",
						ngram.toString(),
						Arrays.toString(ngram_ids).toString(),
						ngram_lm.toString(),
						prob10,
						log10_prob,
						log2_prob);

			} catch (Exception e) {
				LOG.warn(e.getMessage());
			}

		}
	}

	void startRMI(){
		// create own registry, alternative: use 'rmiregistry' command in class output folder
		try {
			_registry = LocateRegistry.getRegistry(_host, _rmiRegistryPort);
			_registry.lookup(_name);
		} catch (RemoteException e) {
			try {
				_registry = LocateRegistry.createRegistry(_rmiRegistryPort);
				LOG.info("Publishing RMI.");
			} catch (RemoteException e1) {
				e1.printStackTrace();
				exit();
			}
			_app_serves_rmi_registry = true;
		} catch (NotBoundException e) {
			// That's what we want
		}
		LOG.info("Connected to RMI.");
	}

	void stopRMI(){
		if(_app_serves_rmi_registry)
			try {
				UnicastRemoteObject.unexportObject(_registry, true);
				LOG.info("Stopped RMI server.");
			} catch (NoSuchObjectException e) { /* handle silently */ }
	}

	void startLM(){
		try {

			_providerService = getStringProviderInstance(_type_provider);

			_providerService.publish(_registry, _name, _port);

			File srcdir = new File(_srcdir);
			try {
				LOG.info("Loading '{}' languagemodel from '{}'... ", _type_lm, srcdir.getAbsolutePath());
				long begin_ms = System.currentTimeMillis();
				LanguageModel<String> lm = getLanguageModelInstance(_type_lm, _providerService,  srcdir, _n, _mincount, _discount, _overwrite);

				_providerService.setLanguageModel(lm);

				long end_ms = System.currentTimeMillis();
				long duration_ms = end_ms - begin_ms;
				long duration_s = duration_ms / 1000;
				String duration_str = String.format("%04dh:%02dm:%02ds", duration_s / 3600, (duration_s % 3600) / 60, (duration_s % 60));
				LOG.info("... finished loading language model {}. Took {}.", _name, duration_str);
			} catch (Exception e) {
				e.printStackTrace();
				LOG.error("{}: {}", e.getClass(), e.getMessage(), e);
				try {
					_registry.unbind(_name);
				} catch (Exception e1) {
					/* handle silently */
				}
				CliUtils.print_usage_quit(System.err, StartLM.class.getSimpleName(), null, USAGE_HEADER, String.format("%s: %s%n", e.getClass().getName(), e.getMessage()), 1);
			}

			if(_mbs == null)
				_mbs = ManagementFactory.getPlatformMBeanServer();
			_mbs.registerMBean(_providerService, _providerJmxBeanName);

		} catch (Exception e) {
			LOG.error("Could not start language model server. {}: {}", e.getClass(), e.getMessage());
			try {
				_registry.unbind(_name);
			} catch (Exception e1) {
				/* handle silently */
			}
			_providerService = null;
			CliUtils.print_usage_quit(System.err, StartLM.class.getSimpleName(), null, USAGE_HEADER, String.format("%s: %s%n", e.getClass().getName(), e.getMessage()), 1);
		}

		_overwrite = false;

	}

	void stopLM(){
		try {
			_registry.unbind(_name);
			_mbs.unregisterMBean(_providerJmxBeanName);
			LOG.info("Stopped LM.");
			_providerService = null;
		} catch (Exception e1) {/* handle silently */ }
	}

	void restartLM(){
		if(_providerService != null)
			stopLM();
		startLM();
	}

	void showLmInfo(){
		if(_providerService == null){
			System.out.println("LM Server is not runnning.");
			return;
		}

		System.out.format("%n####%nLanguage Model Information: %n");
		System.out.format("%-50.50s %s ", "App serves registry:", _app_serves_rmi_registry );
		if(_app_serves_rmi_registry)
			try { System.out.format("%s", Arrays.toString(_registry.list())); } catch (Exception e) {/* */}
		System.out.println();
		System.out.format("%-50.50s %s %n", "RMI registry host:", _host );
		System.out.format("%-50.50s %s %n", "RMI registry port:", _rmiRegistryPort );
		System.out.format("%-50.50s %s %n", "LM port:",_port );
		System.out.format("%-50.50s %s %n", "LM identifier:", _name );
		System.out.format("%-50.50s %s %n", "LM order:", _n );
		System.out.format("%-50.50s %s %n", "LM source Directory:", new File(_srcdir).getAbsolutePath() );
		System.out.format("%-50.50s %s [%s] %n", "LM type:", _type_lm, _providerService.getLanguageModel().getClass().getSimpleName());
		System.out.format("%-50.50s %s [%s] %n", "LM type:", _type_provider,  _providerService.getClass().getSimpleName() );
		System.out.format("---%nLM specific properties:%n");
		for(Entry<Object, Object> prop : Properties.get().entrySet())
			if(prop.getKey().toString().startsWith("lt.lm"))
				System.out.format("%-50.50s %s %n", prop.getKey(), prop.getValue() );
		System.out.format("####%n%n");

	}

	void listNgrams(){
		if(_providerService == null){
			System.out.println("LM Server is not runnning.");
			return;
		}

		try {
			LanguageModel<String> lm = _providerService.getLanguageModel();
			Iterator<List<String>> iter = lm.getNgramIterator();
			if(!iter.hasNext()){
				System.out.println("No ngrams in Language Model.");
				return;
			}

			double log10_prob, prob10, log2_prob;
			int i = 0;
			for(List<String> ngram = null; iter.hasNext();){
				ngram = iter.next();
				if(++i % 30 == 0)
					if(":q".equals(readInput(String.format("press <enter> to show next 30 ngrams, type ':q' if you want to quit showing ngrams: %n%s $> ", _name))))
						break;

				log10_prob = _providerService.getNgramLog10Probability(ngram);
				prob10 = Math.pow(10, log10_prob);
				log2_prob = log10_prob / Math.log10(2);

				System.out.format("%-50.50s [%g (log10=%g, log2=%g)] %n",
						StringUtils.abbreviate(StringUtils.join(ngram, ' '), 50),
						prob10,
						log10_prob,
						log2_prob);
			}

		} catch (Exception e) {
			LOG.warn(e.getMessage());
		}

	}

	//TODO: realize with java reflections
	@SuppressWarnings("unchecked")
	public static LanguageModel<String> getLanguageModelInstance(String type, AbstractStringProvider stringProvider, File srcdir, int order, int mincount, double discount, boolean overwrite) throws Exception {
		LanguageModel<String> lm = null;

		if (BerkeleyLM.class.getSimpleName().equals(type))
			lm = LanguageModelHelper.createBerkelyLmFromTxtFilesInDirectory(stringProvider, srcdir, order, Properties.knUnkLog10Prob(), discount, mincount, overwrite);
		else if (CountingLM.class.getSimpleName().equals(type))
			lm = LanguageModelHelper.createCountingLmTxtFilesInDirectory(stringProvider, srcdir, order);
		else if (LaplaceSmoothedLM.class.getSimpleName().equals(type))
			lm = new LaplaceSmoothedLM<String>((CountingLM<String>)LanguageModelHelper.createCountingLmTxtFilesInDirectory(stringProvider, srcdir, order));
		else if (CountingStringLM.class.getSimpleName().equals(type))
			lm = LanguageModelHelper.createLuceneBasedCountingLmFromTxtFilesInDirectory(stringProvider, srcdir, order, mincount, overwrite);
		else if (KneserNeyLMRecursive.class.getSimpleName().equals(type))
			lm = LanguageModelHelper.createLuceneBasedKneserNeyLMFromTxtFilesInDirectory(stringProvider, srcdir, order, mincount, discount, overwrite);
		else{
			Class<?> lmtype = Class.forName(String.format("%s.%s", CountingStringLM.class.getPackage().getName(), type));
			if(CountingStringLM.class.isAssignableFrom(lmtype))
				lm = LanguageModelHelper.createLuceneBasedLMFromTxtFilesInDirectory((Class<? extends CountingStringLM>)lmtype, stringProvider, srcdir, order, mincount, discount, overwrite);
		}

		if (lm == null)
			throw new IllegalArgumentException(String.format("Unkown language model type '%s'.", type));
		return lm;
	}

	//TODO: realize with java reflections
	public static AbstractStringProvider getStringProviderInstance(String type) throws Exception {
		AbstractStringProvider strprvdr = null;
		if (BreakIteratorStringProvider.class.getSimpleName().equals(type))
			strprvdr = new BreakIteratorStringProvider();
		if (PreTokenizedStringProvider.class.getSimpleName().equals(type))
			strprvdr = new PreTokenizedStringProvider();
		if (LtSegProvider.class.getSimpleName().equals(type))
			strprvdr = new LtSegProvider();
		if (strprvdr == null)
			throw new IllegalArgumentException(String.format("Unkown StringProvider type '%s'.", type));

		return strprvdr;
	}

}