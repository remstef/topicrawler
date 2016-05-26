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

import static de.tudarmstadt.lt.utilities.ProcessUtils.run_process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.service.AbstractStringProvider;
import de.tudarmstadt.lt.lm.service.LtSegProvider;
import de.tudarmstadt.lt.lm.util.Properties;
import de.tudarmstadt.lt.utilities.ArrayUtils;
import de.tudarmstadt.lt.utilities.cli.CliUtils;
import de.tudarmstadt.lt.utilities.cli.ExtendedGnuParser;

/**
 *
 * @author Steffen Remus
 */
public class GenerateNgramIndex implements Runnable {

	private static Logger LOG = LoggerFactory.getLogger(GenerateNgramIndex.class);
	private final static String USAGE_HEADER = "Options:";

	private static File py_src = new File(Properties.pythonBinariesDirectory());
	
	public static void main(String[] args) throws IOException {
		new GenerateNgramIndex(args).run();
	}
	
	/**
	 * 
	 */
	public GenerateNgramIndex() { /* default constructor, pass options via new Ngrams(){{ param=value; ... }} */ }
	
	@SuppressWarnings("static-access")
	public GenerateNgramIndex(String[] args){

		Options opts = new Options();
		opts.addOption(OptionBuilder.withLongOpt("help").withDescription("Display help message.").create("?"));
		opts.addOption(OptionBuilder.withLongOpt("mincount").withArgName("int").hasArg().withDescription("specify the number of times an ngram must occur to be considered in further calculations. Ngrams with counts below mincount are filtered. (default: 1).").create("m"));
		opts.addOption(OptionBuilder.withLongOpt("overwrite").hasOptionalArg().withArgName("{true|false}").withDescription("Overwrite existing index (default: false).").create("w"));
		
		// options parsed and passed to Ngrams app
		opts.addOption(OptionBuilder.withLongOpt("ptype").withArgName("class").hasArg().withDescription("specify the instance of the language model provider that you want to use: {LtSegProvider, BreakIteratorStringProvider, UimaStringProvider, PreTokenizedStringProvider} (default: LtSegProvider)").create("p"));
		opts.addOption(OptionBuilder.withLongOpt("cardinality").withArgName("ngram-order").hasArg().withDescription("Specify the cardinality of the ngrams (min. 1). Specify a range using 'from-to'. (Examples: 5 = extract 5grams; 1-5 = extract 1grams, 2grams, ..., 5grams; default: 1-5).").create("n"));
		opts.addOption(OptionBuilder.withLongOpt("file").withArgName("filename").hasArg().withDescription("Specify the file or directory that contains '.txt' file to read from. Specify '-' to read from stdin. (Note: when no output directory is provided this parameter must refer to a directory!)").isRequired().create("f"));
		opts.addOption(OptionBuilder.withLongOpt("dest").withArgName("directory").hasArg().withDescription("Specify the index directory for the related language model files. (default: <srcdir>/.lmindex)").create("d"));
		opts.addOption(OptionBuilder.withLongOpt("accross_sentences").hasOptionalArg().withArgName("{true|false}").withDescription("Generate Ngrams across sentence boundaries.").create("a"));
		
		try {
			CommandLine cmd = new ExtendedGnuParser(true).parse(opts, args);
			if (cmd.hasOption("help")) 
				CliUtils.print_usage_quit(System.err, Ngrams.class.getSimpleName(), opts, USAGE_HEADER, null, 0);

			_provider_type_ =		 	cmd.getOptionValue("ptype", LtSegProvider.class.getSimpleName());
			_file_ =		 			cmd.getOptionValue("file", "-");
			_out_ = 					cmd.getOptionValue("dest");
			
			if(_out_ != null)
				_index_dir = new File(_out_);
			else{
				if("-".equals(_file_))
					throw new IllegalArgumentException("Output directory must be provided when generating ngrams from stdin. Please provide a destination directory or generate ngrams from directory.");
				File f_or_dir = new File(_file_);
				if(!f_or_dir.exists())
					throw new IllegalArgumentException(String.format("Input file or directory '%s' does not exist.", f_or_dir.getAbsolutePath()));
				if(f_or_dir.isFile())
					throw new IllegalArgumentException("Output directory must be provided when generating ngrams from file. Please provide a destination directory or generate ngrams from directory.");
				if(f_or_dir.isDirectory())
					_index_dir = new File(f_or_dir, ".lmindex");
			}
			
			String order =				cmd.getOptionValue("cardinality","1-5");
			_accross_sentences_ = 		cmd.hasOption("accross_sentences");
			if(_accross_sentences_ && cmd.getOptionValue("accross_sentences") != null)
				_accross_sentences_ = Boolean.parseBoolean(cmd.getOptionValue("accross_sentences"));

			int dash_index = order.indexOf('-');
			_order_to_ = Integer.parseInt(order.substring(dash_index + 1, order.length()).trim());
			_order_from_ = _order_to_;
			if(dash_index == 0)
				_order_from_ = 1;
			if(dash_index > 0)
				_order_from_ = Math.max(1, Integer.parseInt(order.substring(0,dash_index).trim()));
			
			_overwrite =cmd.hasOption("overwrite");
			if(_overwrite && cmd.getOptionValue("overwrite") != null)
				_overwrite = Boolean.parseBoolean(cmd.getOptionValue("overwrite"));
			
			_mincount = Integer.parseInt(cmd.getOptionValue("mincount", "1"));

		} catch (Exception e) {
			CliUtils.print_usage_quit(System.err, Ngrams.class.getSimpleName(), opts, USAGE_HEADER, String.format("%s: %s%n", e.getClass().getSimpleName(), e.getMessage()), 1);
		}
		
	}
	
	boolean _overwrite;
	int _mincount;
	
	File _ngram_file;
	
	String _provider_type_;
	AbstractStringProvider _provider;
	String _file_;
	String _out_; 
	File _index_dir;
	int _order_to_;
	int _order_from_;
	boolean _accross_sentences_;
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		if(!_index_dir.exists())
			_index_dir.mkdir();
		_ngram_file = new File(_index_dir, "ngram.raw.txt.gz");
		if(_overwrite && _ngram_file.exists())
			_ngram_file.delete();
		
		if(!_ngram_file.exists()){
			new Ngrams(){{

				_provider_type = _provider_type_;
				_prvdr = _provider;
				_file = _file_;
				_out = _ngram_file.getAbsolutePath();
				_order_from = _order_from_;
				_order_to = _order_to_;
				_accross_sentences = _accross_sentences_;

			}}.run();
		}
		
		try {
			generate_index();
		} catch (IOException e) {
			LOG.error("Could not generate Ngram index on file '{}'. {}: {}", _file_, e.getClass().getName(), e.getMessage());
		}

	}
	
	public static File generate_index(File src_dir, AbstractStringProvider prvdr, int order_from, int order_to, int mincount, boolean overwrite_existing_index) throws IOException{
		if(!src_dir.exists())
			throw new IOException(String.format("Input dir '%s' does not exist.", src_dir.getAbsolutePath()));
		GenerateNgramIndex g = new GenerateNgramIndex(){{
			_file_ = src_dir.getAbsolutePath();
			_index_dir = new File(src_dir, ".lmindex");
			_provider = prvdr;
			_order_from_ = order_from;
			_order_to_ = order_to;
			_mincount = mincount;
			_overwrite = overwrite_existing_index;
		}};
		g.run();
		return g._index_dir;
	}

	public File generate_index() throws IOException{
		if(!_index_dir.exists())
			_index_dir.mkdirs();

		LOG.info(String.format("Creating count files ..."));
		count_ngrams();

		LOG.info(String.format("Creating ngram index from count files ..."));
		LOG.info(String.format("Adding ngrams to index ..."));
		File ngram_joined_counts_file = new File(_index_dir, "ngram.counts.joined.txt.gz");
		create_ngram_index(ngram_joined_counts_file);

		LOG.info(String.format("Adding vocabulary to index ..."));
		File vocab_file = new File(_index_dir,"ngram.vocabulary.txt.gz");
		create_vocabulary_index(vocab_file);
		LOG.info(String.format("Finished."));

		return _index_dir;
	}

	public void count_ngrams(){
		String sort_extra_param = "";
		if(Properties.additionalSortParam() != null && !Properties.additionalSortParam().isEmpty()){
			sort_extra_param = Properties.additionalSortParam();
			sort_extra_param = sort_extra_param.replace("${indexdir}", _index_dir.getAbsolutePath());
			LOG.info("Sort extra parameter = '{}'", sort_extra_param);
		}
		LOG.info("Current directory: '{}'", new File("").getAbsolutePath());
		
		String command = null;
		LOG.info(String.format("Counting ngrams."));
		File ngram_count_file = new File(_index_dir,"ngram.counts.txt.gz");
		if(ngram_count_file.exists() && !_overwrite)
			LOG.info("File '{}' already exists.", ngram_count_file);
		else{
			command = String.format("(export LC_ALL=C; cat %s | gzip -c -d | python mr_ngram_count.py -m | sort %s | python mr_ngram_count.py -r %d | gzip -c > %s)",
					_ngram_file.getAbsolutePath(),
					sort_extra_param,
					_mincount,
					ngram_count_file.getAbsolutePath());
			run_process(command, py_src, true);
		}

		LOG.info(String.format("Extracting vocabulary."));
		File ngram_vocabulary = new File(_index_dir,"ngram.vocabulary.txt.gz");
		if(ngram_vocabulary.exists() && !_overwrite)
			LOG.info("File '{}' already exists.", ngram_vocabulary.getAbsolutePath());
		else{
			command = String.format("(export LC_ALL=C; cat %s | gzip -c -d | python mr_ngram_vocab.py -m | sort %s | python mr_ngram_vocab.py -r | gzip -c > %s)",
					ngram_count_file.getAbsolutePath(),
					sort_extra_param,
					ngram_vocabulary.getAbsolutePath());
			run_process(command, py_src, true);
		}

		LOG.info(String.format("Counting N_follow = N(w,x)."));
		File words_following_ngram_count_file = new File(_index_dir,"ngram.counts.nfollow.txt.gz");
		if(words_following_ngram_count_file.exists() && !_overwrite)
			LOG.info("File '{}' already exists.", words_following_ngram_count_file.getAbsolutePath());
		else{
			command = String.format("(export LC_ALL=C; cat %s | gzip -c -d | python mr_ngram_nfollow.py -m | sort -k1,1 -t$'\t' %s | python mr_ngram_nfollow.py -r | gzip -c > %s)", // just sort is ok too
					ngram_count_file.getAbsolutePath(),
					sort_extra_param,
					words_following_ngram_count_file.getAbsolutePath());
			run_process(command, py_src, true);
		}

		LOG.info(String.format("Counting N_precede = N(x,w)."));
		File words_preceding_ngram_count_file = new File(_index_dir, "ngram.counts.nprecede.txt.gz");
		if(words_preceding_ngram_count_file.exists() && !_overwrite)
			LOG.info("File '{}' already exists.", words_preceding_ngram_count_file.getAbsolutePath());
		else{
			command = String.format("(export LC_ALL=C; cat %s | gzip -c -d | python mr_ngram_nprecede.py -m | sort -k1,1 -t$'\t' %s | python mr_ngram_nprecede.py -r | gzip -c > %s)", //-k1,1 -k2,2 -t $'\t'
					ngram_count_file.getAbsolutePath(),
					sort_extra_param,
					words_preceding_ngram_count_file.getAbsolutePath());
			run_process(command, py_src, true);
		}
		
		LOG.info(String.format("Counting N_follower_precede = N(x,ngram,x)."));
		File follower_preceding_count_file = new File(_index_dir, "ngram.counts.nfollowerprecede.txt.gz");
		if(follower_preceding_count_file.exists() && !_overwrite)
			LOG.info("File '{}' already exists.", follower_preceding_count_file.getAbsolutePath());
		else{
			command = String.format("(export LC_ALL=C; cat %s | gzip -c -d | python mr_ngram_nfollowerprecede.py -m | sort -k1,1 -t$'\t' %s | python mr_ngram_nfollowerprecede.py -r | gzip -c > %s)", //-k1,1 -k2,2 -t $'\t'
					ngram_count_file.getAbsolutePath(),
					sort_extra_param,
					follower_preceding_count_file.getAbsolutePath());
			run_process(command, py_src, true);
		}

		LOG.info(String.format("Joining ngram counts."));
		File ngram_joined_counts_file = new File(_index_dir, "ngram.counts.joined.txt.gz");
		if(ngram_joined_counts_file.exists() && !_overwrite)
			LOG.info("File '{}' already exists.", ngram_joined_counts_file.getAbsolutePath());
		else{
			command = String.format("(export LC_ALL=C; join -a1 -e '' -1 1 -2 1 -t $'\t' <(cat %s | gzip -c -d) <(cat %s | gzip -c -d) | join -a1 -e '' -1 1 -2 1 -t $'\t' - <(cat %s | gzip -c -d) | join -a1 -e '' -1 1 -2 1 -t $'\t' - <(cat %s | gzip -c -d) | gzip -c > %s)",
					ngram_count_file.getAbsolutePath(),
					words_preceding_ngram_count_file.getAbsolutePath(),
					words_following_ngram_count_file.getAbsolutePath(),
					follower_preceding_count_file.getAbsolutePath(),
					ngram_joined_counts_file.getAbsolutePath());
			run_process(command, new File("."), true);
		}

	}

	public void create_ngram_index(File ngram_joined_counts_file) throws IOException{
		File index_dir = new File(_index_dir, "ngram");
		if(index_dir.exists()){
			LOG.info("Ngram index already exists in directory '{}'.", index_dir.getAbsolutePath());
			if(_overwrite){
				LOG.info("Overwriting index '{}',", index_dir);
				index_dir.delete();
			}
			else
				return;
		}
		index_dir.mkdirs();

		Analyzer analyzer = new KeywordAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_4_9, analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		// use 80 percent of the available total memory
		double total_mem_mb = (double)Runtime.getRuntime().maxMemory() / 1e6;
		double percentage_ram_buffer = Properties.ramBufferPercentage();
		if(percentage_ram_buffer > 0){
			double percentage_ram_buffer_mb = total_mem_mb * percentage_ram_buffer;
			LOG.info(String.format("Setting ram buffer size to %.2f MB (%.2f%% from %.2f MB)", percentage_ram_buffer_mb, percentage_ram_buffer * 100, total_mem_mb));
			iwc.setRAMBufferSizeMB(percentage_ram_buffer_mb);
		}
		
		Directory directory = new MMapDirectory(index_dir);
		IndexWriter writer_ngram = new IndexWriter(directory, iwc);

		InputStream in = new FileInputStream(ngram_joined_counts_file);
		if(ngram_joined_counts_file.getName().endsWith(".gz"))
			in = new GZIPInputStream(in);
		LineIterator iter = new LineIterator(new BufferedReader(new InputStreamReader(in,"UTF-8")));
		
		Document doc = new Document();
		Field f_ngram = new StringField("ngram", "", Store.YES); doc.add(f_ngram);
		Field f_n = new IntField("cardinality", 0, Store.YES); doc.add(f_n);
		Field f_word = new StringField("word", "", Store.YES); doc.add(f_word);
		Field f_hist = new StringField("history", "", Store.YES); doc.add(f_hist);
		Field f_lower = new StringField("lower", "", Store.YES); doc.add(f_lower);
		Field f_count = new StoredField("num", 0L); doc.add(f_count);
		
		Field[] f_follow = new Field[4];
		f_follow[0] = new StoredField("nf_s", 0L); doc.add(f_follow[0]);
		f_follow[1] = new StoredField("nf_N1", 0L); doc.add(f_follow[1]);
		f_follow[2] = new StoredField("nf_N2", 0L); doc.add(f_follow[2]);
		f_follow[3] = new StoredField("nf_N3", 0L); doc.add(f_follow[3]);
		Field[] f_precede = new Field[4];
		f_precede[0] = new StoredField("np_s", 0L); doc.add(f_precede[0]);
		f_precede[1] = new StoredField("np_N1", 0L); doc.add(f_precede[1]);
		f_precede[2] = new StoredField("np_N2", 0L); doc.add(f_precede[2]);
		f_precede[3] = new StoredField("np_N3", 0L); doc.add(f_precede[3]);
		Field[] f_followerprecede = new Field[4];
		f_followerprecede[0] = new StoredField("nfp_s", 0L); doc.add(f_followerprecede[0]);
		f_followerprecede[1] = new StoredField("nfp_N1", 0L); doc.add(f_followerprecede[1]);
		f_followerprecede[2] = new StoredField("nfp_N2", 0L); doc.add(f_followerprecede[2]);
		f_followerprecede[3] = new StoredField("nfp_N3", 0L); doc.add(f_followerprecede[3]);
		
		Long[][] N = new Long[][]{{0L,0L,0L,0L,0L,0L}};
		Long[] S = new Long[] { 0L };
		long c = 0;
		while(iter.hasNext()){
			if(++c % 100000 == 0)
				LOG.info("Adding {}'th ngram.", c);
			String line = iter.next();
			try{
				String[] splits = de.tudarmstadt.lt.utilities.StringUtils.rtrim(line).split("\t");
				String ngram_str = splits[0];
				if(de.tudarmstadt.lt.utilities.StringUtils.trim(ngram_str).isEmpty()){
					LOG.warn("Ngram is empty, skipping line {}: '{}' (file '{}').", c, line, ngram_joined_counts_file);
					continue;
				}
					
				List<String> ngram = Arrays.asList(ngram_str.split(" "));
				long num = Long.parseLong(splits[1]);
				int n = ngram.size();

				f_ngram.setStringValue(ngram_str);
				f_n.setIntValue(n);
				f_word.setStringValue(ngram.get(ngram.size()-1));
				f_hist.setStringValue(StringUtils.join(ngram.subList(0, ngram.size()-1), " "));
				f_lower.setStringValue(StringUtils.join(ngram.subList(1, ngram.size()), " "));
				f_count.setLongValue(num);
				
				for(int j = 0; j < f_follow.length; j++){
					f_follow[j].setLongValue(0L);
					f_precede[j].setLongValue(0L);
					f_followerprecede[j].setLongValue(0L);
				}
				
				if(splits.length > 2 && !splits[2].isEmpty()){
					// precede or follow or followerprecede
					String[] splits_ = splits[2].split(":");
					String type = splits_[0];
					String[] count_values = splits_[1].split(",");
					if(count_values.length > 0){
						if("n_f".equals(type))
							f_follow[0].setLongValue(Long.parseLong(count_values[0]));
						else if("n_p".equals(type))
							f_precede[0].setLongValue(Long.parseLong(count_values[0]));
						else if("n_fp".equals(type))
							f_followerprecede[0].setLongValue(Long.parseLong(count_values[0]));
					}
					for(int i = 1; i < count_values.length; i++){
						if("n_f".equals(type))
							f_follow[i].setLongValue(Long.parseLong(count_values[i]));
						else if("n_p".equals(type))
							f_precede[i].setLongValue(Long.parseLong(count_values[i]));
						else if("n_fp".equals(type))
							f_followerprecede[i].setLongValue(Long.parseLong(count_values[i]));
					}
				}
				if(splits.length > 3 && !splits[3].isEmpty()){
					// should be follow or followerprecede
					String[] splits_ = splits[3].split(":");
					String type = splits_[0];
					String[] count_values = splits_[1].split(",");
					if(count_values.length > 0){
						if("n_f".equals(type))
							f_follow[0].setLongValue(Long.parseLong(count_values[0]));
						else if("n_p".equals(type))
							f_precede[0].setLongValue(Long.parseLong(count_values[0]));
						else if("n_fp".equals(type))
							f_followerprecede[0].setLongValue(Long.parseLong(count_values[0]));
					}
					for(int i = 1; i < count_values.length; i++){
						if("n_f".equals(type))
							f_follow[i].setLongValue(Long.parseLong(count_values[i]));
						else if("n_p".equals(type))
							f_precede[i].setLongValue(Long.parseLong(count_values[i]));
						else if("n_fp".equals(type))
							f_followerprecede[i].setLongValue(Long.parseLong(count_values[i]));
					}
				}
				if(splits.length > 4 && !splits[4].isEmpty()){
					// should be followerprecede
					String[] splits_ = splits[4].split(":");
					String type = splits_[0];
					String[] count_values = splits_[1].split(",");
					if(count_values.length > 0){
						if("n_f".equals(type))
							f_follow[0].setLongValue(Long.parseLong(count_values[0]));
						else if("n_p".equals(type))
							f_precede[0].setLongValue(Long.parseLong(count_values[0]));
						else if("n_fp".equals(type))
							f_followerprecede[0].setLongValue(Long.parseLong(count_values[0]));
					}
					for(int i = 1; i < count_values.length; i++){
						if("n_f".equals(type))
							f_follow[i].setLongValue(Long.parseLong(count_values[i]));
						else if("n_p".equals(type))
							f_precede[i].setLongValue(Long.parseLong(count_values[i]));
						else if("n_fp".equals(type))
							f_followerprecede[i].setLongValue(Long.parseLong(count_values[i]));
					}
				}

				writer_ngram.addDocument(doc);
				
				while(N.length <= n){
					N = ArrayUtils.getConcatinatedArray(N, new Long[][]{{0L,0L,0L,0L,0L,0L}});
					S = ArrayUtils.getConcatinatedArray(S, new Long[]{ 0L });
				}
				
				if(num == 1L) 		N[n][1]++;
				else if(num == 2L) 	N[n][2]++;
				else if(num == 3L)	N[n][3]++;
				else if(num == 4L)	N[n][4]++;
				else 				N[n][5]++;
				N[n][0]++;
				S[n] += num;

			}catch(Exception e){
				LOG.error("Could not process line '{}' in file '{}:{}', malformed line.", line, ngram_joined_counts_file, c, e);
			}
		}

		writer_ngram.forceMergeDeletes();
		writer_ngram.commit();
		writer_ngram.close();
		
		StringBuilder b = new StringBuilder(String.format("#%n# Number of times where an ngram occurred: %n#  at_least_once, exactly_once, exactly_twice, exactly_three_times, exactly_four_times, five_times_or_more.%n#%nmax_n=%d%nmax_c=6%n", N.length-1));
		for(int n = 1; n < N.length; n++)
			b.append(String.format("n%d=%s%n", n, StringUtils.join(N[n],',')));
		for(int n = 1; n < S.length; n++)
			b.append(String.format("s%d=%d%n", n, S[n]));
		FileUtils.writeStringToFile(new File(_index_dir, "__sum_ngrams__"), b.toString());
	
	}

	public void create_vocabulary_index(File vocabulary_file) throws IOException{
		File index_dir = new File(_index_dir, "vocab");
		if(index_dir.exists()){
			LOG.info("Vocabulary index already exists in directory '{}'.", index_dir.getAbsolutePath());
			if(_overwrite){
				LOG.info("Overwriting index '{}',", index_dir);
				index_dir.delete();
			}
			else
				return;
		}
		index_dir.mkdirs();
		Analyzer analyzer = new KeywordAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_4_9, analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		iwc.setRAMBufferSizeMB(1024.0);
		Directory directory = new MMapDirectory(index_dir);
		IndexWriter writer_vocab = new IndexWriter(directory, iwc);

		InputStream in = new FileInputStream(vocabulary_file);
		if(vocabulary_file.getName().endsWith(".gz"))
			in = new GZIPInputStream(in);
		LineIterator iter = new LineIterator(new BufferedReader(new InputStreamReader(in,"UTF-8")));
		Document doc = new Document();
		Field f_word = new StringField("word", "", Field.Store.YES); doc.add(f_word);
		long c = 0;
		while(iter.hasNext()){
			if(++c % 10000 == 0)
				LOG.info("Adding {}'th word.", c);
			String line = iter.next();
			try{
				String word = line.trim();
				f_word.setStringValue(word);
				writer_vocab.addDocument(doc);
			}catch(Exception e){
				LOG.warn("Could not process line '{}' in file '{}', malformed line.", line, vocabulary_file, e);
			}
		}

		writer_vocab.forceMergeDeletes();
		writer_vocab.commit();
		writer_vocab.close();
	}

}
