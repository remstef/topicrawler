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
package de.tudarmstadt.lt.seg.app;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Spliterator;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.LineIterator;

import de.tudarmstadt.lt.seg.SegmentType;
import de.tudarmstadt.lt.seg.sentence.ISentenceSplitter;
import de.tudarmstadt.lt.seg.sentence.RuleSplitter;
import de.tudarmstadt.lt.seg.token.ITokenizer;
import de.tudarmstadt.lt.seg.token.RuleTokenizer;
import de.tudarmstadt.lt.utilities.cli.CliUtils;
import de.tudarmstadt.lt.utilities.cli.ExtendedGnuParser;

/**
 * @author Steffen Remus
 *
 */
@SuppressWarnings("static-access")
public class Segmenter implements Runnable{

	private static String USAGE_HEADER = String.format("+++%nSplit sentences and tokenize documents. Supports piped input. %nUses default encoding and locale. Specify '-Dfile.encoding' for changing default encoding, specify '-Duser.language', '-Duser.country', '-Duser.script', '-Duser.variant' for changing default locale. E.g. '-Dfile.encoding=UTF-8 -Duser.language=en -Duser.country=US'! %nSupported RuleSets for RuleSplitter: %s %nSupported RuleSets for RuleTokenizer: %s%n+++%nOptions:", de.tudarmstadt.lt.seg.sentence.rules.RuleSet.getAvailable(), de.tudarmstadt.lt.seg.token.rules.RuleSet.getAvailable());

	private static boolean DEBUG = false;

	public static void main(String[] args) throws ClassNotFoundException {
		new Segmenter(args).run();
	}

	/**
	 * default constructor
	 * 
	 * set necessary variables by using <code>new Segmenter(){{ _variable = value; ... }} </code>
	 * 
	 */
	public Segmenter() {/* NOTHING TO DO */}

	static Options opts;


	static{
		opts = new Options();
		opts.addOption(new Option("?", "help", false, "display this message"));

		opts.addOption(OptionBuilder.withLongOpt("file").withArgName("filename").hasArg().withDescription("Specify the file you want to read from. Specify '-' to read from stdin. (default: '-').").create("f"));
		opts.addOption(OptionBuilder.withLongOpt("out").withArgName("filename").hasArg().withDescription("Specify the file you want to write to. Specify '-' to write to stdout. (default: '-').").create("o"));
		opts.addOption(OptionBuilder.withLongOpt("sentence-separator").withArgName("separator").hasArg().withDescription("Specify the separator for sentences. (default: '\\n').").create("seps"));
		opts.addOption(OptionBuilder.withLongOpt("token-separator").withArgName("separator").hasArg().withDescription("Specify the separator for tokens. (default: ' ').").create("sept"));
		opts.addOption(OptionBuilder.withLongOpt("source-separator").withArgName("separator").hasArg().withDescription("Specify the separator for the source description. (default: '\\t').").create("sepd"));
		opts.addOption(OptionBuilder.withLongOpt("sentencesplitter").withArgName("class").hasArg().withDescription("Specify the class of the sentence splitter that you want to use: {BreakSplitter, LineSplitter, RuleSplitter, NullSplitter} (default: RuleSplitter)").create("s"));
		opts.addOption(OptionBuilder.withLongOpt("tokenizer").withArgName("class").hasArg().withDescription("Specify the class of the word tokinzer that you want to use: {BreakTokenizer, DiffTokenizer, EmptySpaceTokenizer, NullTokenizer} (default: DiffTokenizer)").create("t"));
		opts.addOption(OptionBuilder.withLongOpt("parallel").withArgName("num").hasArg().withDescription("Specify the number of parallel threads. (Note: output might be genereated in a different order than provided by input, specify 1 if you need to keep the order. Parallel mode requires one document per line [ -l ] (default: 1).").create());
		opts.addOption(OptionBuilder.withLongOpt("normalize").withDescription("Specify the degree of token normalization [0...4] (default: 0).").hasArg().withArgName("level").create("nl"));
		opts.addOption(OptionBuilder.withLongOpt("filter").withDescription("Specify the degree of token filtering [0...5] (default: 2).").hasArg().withArgName("level").create("fl"));
		opts.addOption(OptionBuilder.withLongOpt("merge").withDescription("Specify the degree of merging conscutive items {0,1,2} (default: 0).").hasOptionalArg().withArgName("level").create("ml"));
		opts.addOption(OptionBuilder.withLongOpt("onedocperline").withDescription("Specify if you want to process documents linewise and preserve document ids, i.e. map line numbers to sentences.").create("l"));
		opts.addOption(OptionBuilder.withLongOpt("sentence-ruleset").withArgName("languagecode").hasArg().withDescription(String.format("Specify the ruleset that you want to use together with RuleSplitter (avaliable: %s) (default: 'default')", de.tudarmstadt.lt.seg.sentence.rules.RuleSet.getAvailable())).create());
		opts.addOption(OptionBuilder.withLongOpt("token-ruleset").withArgName("languagecode").hasArg().withDescription(String.format("Specify the ruleset that you want to use together with RuleTokenizer (avaliable: %s) (default: 'default')", de.tudarmstadt.lt.seg.token.rules.RuleSet.getAvailable())).create());
		opts.addOption(OptionBuilder.withLongOpt("boundary-as-part-of-sentence").withDescription("Specify if sentence boundaries should be part of the sentence segment (default: true).").hasArg().withArgName("true|false").create("bps"));
		opts.addOption(OptionBuilder.withLongOpt("debug").withDescription("Enable debugging.").create());
	}


	public Segmenter(String[] args) {
		try {
			CommandLine cmd = new ExtendedGnuParser(true).parse(opts, args);
			if (cmd.hasOption("help")) 
				CliUtils.print_usage_quit(System.err, Segmenter.class.getSimpleName(), opts, USAGE_HEADER, null, 0);

			_sentence_splitter_type = 	cmd.getOptionValue("sentencesplitter", 	RuleSplitter.class.getSimpleName());
			_tokenizer_type = 			cmd.getOptionValue("tokenizer", 		RuleTokenizer.class.getSimpleName());
			_filename_in =		 		cmd.getOptionValue("file", 				"-");
			_filename_out =		 		cmd.getOptionValue("out", 				"-");
			_separator_sentence =		cmd.getOptionValue("seps", 				"\n");
			_separator_token =	 		cmd.getOptionValue("sept", 				" ");
			_separator_desc =	 		cmd.getOptionValue("sepd", 				"\t");

			_level_normalize =			Integer.parseInt(cmd.getOptionValue("normalize","0"));
			_level_filter =				Integer.parseInt(cmd.getOptionValue("filter","2"));

			int level_merge =				cmd.hasOption("merge") ? 1 : 0;
			if(cmd.hasOption("merge") && cmd.getOptionValue("merge") != null)
				level_merge = Integer.parseInt(cmd.getOptionValue("merge","1"));
			_merge_tokens = level_merge >= 2;
			_merge_types = level_merge >= 1;

			_parallelism = 				Integer.parseInt(cmd.getOptionValue("parallel", "1" ));//Runtime.getRuntime().availableProcessors()
			_one_doc_per_line =			cmd.hasOption("l");
			_ruleset_sentence =			cmd.getOptionValue("sentence-ruleset");
			_ruleset_token =			cmd.getOptionValue("token-ruleset");
			_boundary_as_part_of_sentence = Boolean.parseBoolean(cmd.getOptionValue("boundary-as-part-of-sentence","true"));

			DEBUG =						cmd.hasOption("debug");
			if(DEBUG){
				_separator_sentence = "\n";
				_separator_token = "\n";
				_separator_desc = "\n";
			}

		} catch (Exception e) {
			CliUtils.print_usage_quit(System.err, Segmenter.class.getSimpleName(), opts, USAGE_HEADER, String.format("%s: %s%n", e.getClass().getSimpleName(), e.getMessage()), 1);
		}

	}

	int 	_level_normalize;
	int 	_level_filter;
	int 	_parallelism;
	String 	_filename_in;
	String 	_filename_out;
	String	_tokenizer_type;
	String	_sentence_splitter_type;
	String	_separator_sentence;
	String	_separator_token;
	String	_separator_desc;
	String _ruleset_sentence;
	String _ruleset_token;
	boolean _one_doc_per_line;
	boolean _merge_types;
	boolean _merge_tokens;
	boolean _boundary_as_part_of_sentence;

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		System.err.println("Setting parallelism to " + _parallelism);
		System.err.format("Using '%s' and '%s'.%n", _tokenizer_type, _sentence_splitter_type);
		try{
			if(_parallelism > 1 && _one_doc_per_line)
				run_parallel();
			else if(_parallelism <= 1 && _one_doc_per_line)
				run_sequential_line();
			else
				run_sequential_stream();
		}catch(Exception e){
			CliUtils.print_usage_quit(Segmenter.class.getSimpleName(), null, String.format("%s: %s%n", e.getClass().getSimpleName(), e.getMessage()), 1);
		}

	}

	private void run_sequential_stream() throws Exception{
		ISentenceSplitter sentenceSplitter = newSentenceSplitter();
		ITokenizer tokenizer = newTokenizer();

		InputStream in = System.in;
		if(!"-".equals(_filename_in))
			in = new FileInputStream(_filename_in);
		BufferedReader r = new BufferedReader(new InputStreamReader(in, Charset.defaultCharset()));

		OutputStream out = System.out;
		if(!"-".equals(_filename_out))
			out = new FileOutputStream(_filename_out);
		PrintWriter w = new PrintWriter(new OutputStreamWriter(out, Charset.defaultCharset()));

		split_and_tokenize(
				r,
				_filename_in,
				sentenceSplitter, 
				tokenizer, 
				_level_filter,
				_level_normalize,
				_merge_types,
				_merge_tokens,
				_separator_sentence,
				_separator_token,
				_separator_desc,
				w);

		r.close();
	}

	private void run_sequential_line() throws Exception{
		ISentenceSplitter sentenceSplitter = newSentenceSplitter();
		ITokenizer tokenizer = newTokenizer();

		InputStream in = System.in;
		if(!"-".equals(_filename_in))
			in = new FileInputStream(_filename_in);
		LineIterator liter = new LineIterator(new BufferedReader(new InputStreamReader(in, Charset.defaultCharset())));

		OutputStream out = System.out;
		if(!"-".equals(_filename_out))
			out = new FileOutputStream(_filename_out);
		PrintWriter w = new PrintWriter(new OutputStreamWriter(out, Charset.defaultCharset()));

		for(long lc = 0; liter.hasNext();){
			if(++lc % 1000 == 0)
				System.err.format("Processing line %d ('%s')%n", lc, _filename_in);
			String l = liter.next().replace("\\t", "\t").replace("\\n", "\n");
			split_and_tokenize(
					new StringReader(l),
					String.format("%s:%d",_filename_in,lc),
					sentenceSplitter, 
					tokenizer, 
					_level_filter,
					_level_normalize,
					_merge_types,
					_merge_tokens,
					_separator_sentence,
					_separator_token,
					_separator_desc,
					w);
		}
	}

	private void run_parallel() throws Exception{


		//		long start = System.currentTimeMillis();
		//		IntStream s = IntStream.range(0, 20);
		////System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "20");
		//		s.parallel().forEach(i -> {
		//			try { Thread.sleep(100); } catch (Exception ignore) {}
		//			System.out.print((System.currentTimeMillis() - start) + " ");
		//		});
		//		val forkJoinPool:ForkJoinPool = new ForkJoinPool(num_threads);
		//    forkJoinPool.submit(new Runnable() { 
		//      def run() = lines.parallel()
		//        .map[String](parsefun)
		//        .forEach(writefun)
		//    }).get


		// TODO: replace executorservice by the above stream provided parallelism stuff
		//		ExecutorService t = new ThreadPoolExecutor(_parallelism, _parallelism, 20L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

		InputStream in = System.in;
		if(!"-".equals(_filename_in))
			in = new FileInputStream(_filename_in);
		Stream<String> liter = new BufferedReader(new InputStreamReader(in, Charset.defaultCharset())).lines();

		OutputStream out = System.out;
		if(!"-".equals(_filename_out))
			out = new FileOutputStream(_filename_out);
		PrintWriter w = new PrintWriter(new OutputStreamWriter(out, Charset.defaultCharset()));

		ThreadLocal<ISentenceSplitter> sentenceSplitter = ThreadLocal.withInitial(() -> {
			try {
				return newSentenceSplitter();
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		});
		ThreadLocal<ITokenizer> tokenizer = ThreadLocal.withInitial(() -> {
			try {
				return newTokenizer();
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		});

		AtomicLong lc = new AtomicLong(0);
		ForkJoinPool forkJoinPool = new ForkJoinPool(_parallelism);
		forkJoinPool.submit(() -> 
			liter.parallel().forEach((line) -> {
				final long docid = lc.incrementAndGet();
				if(docid % 1000 == 0)
					System.err.format("Processing line %d ('%s')%n", docid, _filename_in);
	
				String l = line.replace("\\t", "\t").replace("\\n", "\n");
				split_and_tokenize(
						new StringReader(l),
						String.format("%s:%d", _filename_in, docid),
						sentenceSplitter.get(), 
						tokenizer.get(), 
						_level_filter,
						_level_normalize,
						_merge_types,
						_merge_tokens,
						_separator_sentence,
						_separator_token,
						_separator_desc,
						w);
		})).get();

	}

	public static void split_and_tokenize(Reader reader, String docid, ISentenceSplitter sentenceSplitter, ITokenizer tokenizer, int level_filter, int level_normalize, boolean merge_types, boolean merge_tokens, String separator_sentence, String separator_token, String separator_desc, PrintWriter writer){
		try{
			final StringBuffer buf = new StringBuffer(); // used for checking of stream is empty; take care when not running sequentially but in parallel!
			sentenceSplitter.init(reader).stream().sequential().forEach(sentence_segment -> {
				if(DEBUG){
					writer.format("%s%s", docid, separator_desc);
					writer.println(sentence_segment.toString());
					writer.print(separator_sentence);
				}
				if(sentence_segment.type != SegmentType.SENTENCE)
					return;
				tokenizer.init(sentence_segment.asString());
				Stream<String> tokens = null;
				if(DEBUG)
					tokens = tokenizer.stream().map(x -> x.toString() + separator_token);
				else
					tokens = StreamSupport.stream(tokenizer.filteredAndNormalizedTokens(level_filter, level_normalize, merge_types, merge_tokens).spliterator(), false).map(x -> x + separator_token);
				Spliterator<String> spliterator = tokens.spliterator();
				tokens = StreamSupport.stream(spliterator, false);
				buf.setLength(0);
				boolean empty = !spliterator.tryAdvance(x -> {buf.append(x);});
				if(empty)
					return;
				synchronized (writer) {
					// writer.write(Thread.currentThread().getId() + "\t");
					writer.format("%s%s", docid, separator_desc);
					writer.print(buf);
					tokens.forEach(writer::print);
					writer.print(separator_sentence);
					writer.flush();
				}
			});
		}catch(Exception e){
			Throwable t = e;
			while(t != null){
				System.err.format("%s: %s%n", e.getClass(), e.getMessage());
				t = e.getCause();
			}
		}
	}

	public ITokenizer newTokenizer() throws ClassNotFoundException, InstantiationException, IllegalAccessException{
		String packageName = ITokenizer.class.getPackage().getName();
		@SuppressWarnings("unchecked")
		Class<ITokenizer> clazz = (Class<ITokenizer>) Class.forName(String.format("%s.%s", packageName, _tokenizer_type));
		ITokenizer instance = clazz.newInstance();
		if(RuleTokenizer.class.getSimpleName().equals(_tokenizer_type)){
			de.tudarmstadt.lt.seg.token.rules.RuleSet rs = de.tudarmstadt.lt.seg.token.rules.RuleSet.get(_ruleset_sentence);
			((RuleTokenizer)instance).initParam(rs);
		}
		return instance;
	}

	public ISentenceSplitter newSentenceSplitter() throws ClassNotFoundException, InstantiationException, IllegalAccessException{
		String packageName = ISentenceSplitter.class.getPackage().getName();
		@SuppressWarnings("unchecked")
		Class<ISentenceSplitter> clazz = (Class<ISentenceSplitter>) Class.forName(String.format("%s.%s", packageName, _sentence_splitter_type));
		ISentenceSplitter instance = clazz.newInstance();
		if(RuleSplitter.class.getSimpleName().equals(_sentence_splitter_type)){
			de.tudarmstadt.lt.seg.sentence.rules.RuleSet rs = de.tudarmstadt.lt.seg.sentence.rules.RuleSet.get(_ruleset_sentence);
			((RuleSplitter)instance).initParam(rs, _boundary_as_part_of_sentence);
		}
		return instance;
	}

}
