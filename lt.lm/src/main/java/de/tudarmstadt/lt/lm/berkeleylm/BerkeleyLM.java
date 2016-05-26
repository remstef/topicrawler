package de.tudarmstadt.lt.lm.berkeleylm;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.AbstractLanguageModel;
import de.tudarmstadt.lt.lm.LanguageModel;
import de.tudarmstadt.lt.lm.util.Properties;
import edu.berkeley.nlp.lm.ArrayEncodedNgramLanguageModel;
import edu.berkeley.nlp.lm.ArrayEncodedProbBackoffLm;
import edu.berkeley.nlp.lm.ConfigOptions;
import edu.berkeley.nlp.lm.StringWordIndexer;
import edu.berkeley.nlp.lm.WordIndexer;
import edu.berkeley.nlp.lm.WordIndexer.StaticMethods;
import edu.berkeley.nlp.lm.cache.ArrayEncodedCachingLmWrapper;
import edu.berkeley.nlp.lm.collections.Iterators;
import edu.berkeley.nlp.lm.io.ArpaLmReader;
import edu.berkeley.nlp.lm.io.LmReaders;
import edu.berkeley.nlp.lm.map.NgramMap;
import edu.berkeley.nlp.lm.map.NgramMapWrapper;
import edu.berkeley.nlp.lm.values.KneserNeyCountValueContainer.KneserNeyCounts;
import edu.berkeley.nlp.lm.values.ProbBackoffPair;

public class BerkeleyLM<W> extends AbstractLanguageModel<W> implements LanguageModel<W> {

	private final static Logger LOG = LoggerFactory.getLogger(BerkeleyLM.class);

	private final ArrayEncodedNgramLanguageModel<W> _blm;
	private ArrayEncodedCachingLmWrapper<W> _blm_cache = null;
	private NgramMapWrapper<W, ?> _ngram_map_wrapper = null;

	private int _index_unk;
	private int _index_begin_s;
	private int _index_end_s;

	public BerkeleyLM(ArrayEncodedNgramLanguageModel<W> berkeley_language_model) {
		_blm = berkeley_language_model;
		_index_unk = _blm.getWordIndexer().getIndexPossiblyUnk(_blm.getWordIndexer().getUnkSymbol());
		_index_begin_s = _blm.getWordIndexer().getIndexPossiblyUnk(_blm.getWordIndexer().getStartSymbol());
		_index_begin_s = _blm.getWordIndexer().getIndexPossiblyUnk(_blm.getWordIndexer().getEndSymbol());
	}

	/**
	 * 
	 * read ngram model from pretokenized sentences.
	 * Expects one sentence per line, tokens separated by space.
	 * 
	 * @see see edu.berkeley.nlp.lm.io.MakeKneserNeyArpaFromText for reference
	 * @param src_dir
	 * @param accept_file_regex_pattern
	 * @param order
	 * @param arpa_filename (possibly null)
	 * @return
	 */
	public static BerkeleyLM<String> createFromFiles(final String src_dir, final String accept_file_regex_pattern, final int order, String arpa_filename, double discount, int mincount) {
		File src_dir_ = new File(src_dir);
		if(!src_dir_.isDirectory())
			throw new IllegalArgumentException(String.format("Expected directory but got %s", src_dir_.getAbsolutePath()));
		String src_dir_name = src_dir_.getName();
		List<String> files = Arrays.asList(src_dir_.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return new File(dir,name).isFile() && name.matches(accept_file_regex_pattern);
			}
		}));
		File arpa_file;
		if(arpa_filename == null)
			arpa_file = new File(src_dir, src_dir_name + ".arpa.gz");
		else
			arpa_file = new File(arpa_filename);

		final StringWordIndexer wordIndexer = new StringWordIndexer(); //indexer with default symbols
		wordIndexer.setStartSymbol(ArpaLmReader.START_SYMBOL);
		wordIndexer.setEndSymbol(ArpaLmReader.END_SYMBOL);
		wordIndexer.setUnkSymbol(ArpaLmReader.UNK_SYMBOL);

		ConfigOptions opts = new ConfigOptions();		
		opts.kneserNeyMinCounts = new double[order];
		Arrays.fill(opts.kneserNeyMinCounts, mincount);
		if(discount < 0){
			opts.kneserNeyDiscounts = null;
		}else{
			opts.kneserNeyDiscounts = new double[order];
			Arrays.fill(opts.kneserNeyDiscounts, discount);
		}
		opts.unknownWordLogProb = Properties.knUnkLog10Prob();

		LmReaders.createKneserNeyLmFromTextFiles(files, wordIndexer, order, arpa_file, new ConfigOptions());
		return loadFromArpaFile(arpa_file.getAbsolutePath());
	}

	public static BerkeleyLM<String> loadFromArpaFile(String arpa_file) {
		WordIndexer<String> indexer = new StringWordIndexer();
		indexer = new StringWordIndexer(); // indexer with default symbols
		ConfigOptions opts = new ConfigOptions();
		opts.unknownWordLogProb = Properties.knUnkLog10Prob();
		ArrayEncodedProbBackoffLm<String> lm = LmReaders.readArrayEncodedLmFromArpa(new ArpaLmReader<String>(arpa_file, indexer, Integer.MAX_VALUE), Properties.useCompressedBerkelyLM(), indexer, opts);
		return new BerkeleyLM<String>(lm);
	}

	@Override
	public int getOrder() {
		return _blm.getLmOrder();
	}

	@Override
	public W getWord(int wordId) {
		return _blm.getWordIndexer().getWord(wordId);
	}

	@Override
	public int getWordIndex(W word) {
		return _blm.getWordIndexer().getIndexPossiblyUnk(word);
	}

	@Override
	public double getNgramLogProbability(int[] wordIds) {
		if(_blm_cache != null)
			return _blm_cache.getLogProb(wordIds);
		return _blm.getLogProb(wordIds);
	}

	@Override
	public double getNgramLogProbability(List<W> words) {
		// System.out.println(Arrays.toString(words));
		for(int i = 0; i < words.size(); i++)
			if(words.get(i) == null)
				words.set(i, _blm.getWordIndexer().getUnkSymbol());
		int[] wordIds = StaticMethods.toArray(_blm.getWordIndexer(), words);
		return getNgramLogProbability(wordIds);
	}

	public double getNativeSequenceLogProbability(List<W> sequence) {
		return _blm.scoreSentence(sequence);
	}

	@Override
	public W predictNextWord(List<W> history_words) {
//		// check length
//		assert history_words.size() >= getOrder() - 1 : "Length of history must be at least of ngram order - 1.";
//		List<W> pruned_history_words = history_words.subList((history_words.size() - getOrder()) + 1, history_words.size());
//
//		LOG.debug("History: {}; pruned: {}.", history_words.toString(), pruned_history_words.toString());
//
//		int[] wordIds = new int[pruned_history_words.size() + 1];
//		for (int i = 0; i < pruned_history_words.size(); i++)
//			wordIds[i] = _blm.getWordIndexer().getIndexPossiblyUnk(pruned_history_words.get(i));
//
//		int lastIndex = wordIds.length-1;
//		double max_value = -Double.MAX_VALUE;
//		int max_wordId = _blm.getWordIndexer().getIndexPossiblyUnk(_blm.getWordIndexer().getUnkSymbol());
//		for (int i = 0; i < _blm.getWordIndexer().numWords(); i++) {
//			if (i == _index_unk || i == _index_begin_s || i == _index_end_s)
//				continue;
//			wordIds[lastIndex] = i;
//			double logprob = getNgramLogProbability(wordIds);
//			if (logprob > max_value) {
//				max_value = logprob;
//				max_wordId = i;
//			}
//		}
//		return _blm.getWordIndexer().getWord(max_wordId);
		// FIXME:
		return null;
	}

	public ArrayEncodedNgramLanguageModel<W> get() {
		return _blm;
	}

	public void activateCache(boolean yes_or_no) {
		if (yes_or_no) _blm_cache = ArrayEncodedCachingLmWrapper.wrapWithCacheNotThreadSafe(_blm);
		else _blm_cache = null;

	}

	@Override
	public Iterator<List<W>> getNgramIterator() {
		return getNgramIterator(getOrder());
	}

	public Iterator<List<W>> getNgramIterator(int order) {
		if (_ngram_map_wrapper == null) initNgramMapAvailability();
		return _ngram_map_wrapper.getMapForOrder(order - 1).keySet().iterator();
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Iterator<List<Integer>> getNgramIdIterator() {
		if (_ngram_map_wrapper == null) initNgramMapAvailability();
		Iterator<NgramMap.Entry> iter = ((NgramMap) _ngram_map_wrapper.getNgramMap()).getNgramsForOrder(getOrder() - 1).iterator();
		Iterators.Transform<NgramMap.Entry, List<Integer>> transform = new Iterators.Transform<NgramMap.Entry, List<Integer>>(iter) {
			@Override
			protected List<Integer> transform(NgramMap.Entry next) {
				return Arrays.asList(ArrayUtils.toObject(next.key));
			}
		};
		return transform;
	}

	private void initNgramMapAvailability() {
		if (_blm instanceof ArrayEncodedProbBackoffLm) {
			NgramMap<ProbBackoffPair> map = ((ArrayEncodedProbBackoffLm<W>) _blm).getNgramMap();
			_ngram_map_wrapper = new NgramMapWrapper<W, ProbBackoffPair>(map, _blm.getWordIndexer());
		}
		else if (_blm instanceof KneserNeyLmReaderCallbackWrapper) {
			NgramMap<KneserNeyCounts> map = ((KneserNeyLmReaderCallbackWrapper<W>) _blm).getNgramMap();
			_ngram_map_wrapper = new NgramMapWrapper<W, KneserNeyCounts>(map, _blm.getWordIndexer());
		}
		else throw new Error(String.format(
				"Language model must be either an ArrayEncodedProbBackoffLm or a KneserNeyLmReaderCallbackWrapper instance, but is an instance of %s.",
				_blm.getClass().getSimpleName()));
	}
	
	@Override
	public boolean isUnkownWord(W word) {
		return isUnkownWord(_blm.getWordIndexer().getIndexPossiblyUnk(word));
	}
	
	@Override
	public boolean isUnkownWord(int wordId) {
		return wordId == _index_unk;
	}

}
