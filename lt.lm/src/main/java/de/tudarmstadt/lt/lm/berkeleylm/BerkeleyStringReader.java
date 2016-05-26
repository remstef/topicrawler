package de.tudarmstadt.lt.lm.berkeleylm;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.berkeley.nlp.lm.WordIndexer;
import edu.berkeley.nlp.lm.io.LmReader;
import edu.berkeley.nlp.lm.io.LmReaderCallback;
import edu.berkeley.nlp.lm.util.LongRef;

/**
 * Class for reading raw string arrays.
 * 
 * @author Steffen Remus
 * 
 * @param <W>
 */
public class BerkeleyStringReader implements LmReader<LongRef, LmReaderCallback<LongRef>> {

	private final static Logger LOG = LoggerFactory.getLogger(BerkeleyStringReader.class);

	private static final String _unused_words_as_string_dummy = "i am a dummy sequence";
	private final WordIndexer<String> _wordIndexer;
	private final List<String> _words;

	public BerkeleyStringReader(List<String> words, final WordIndexer<String> wordIndexer) {
		_wordIndexer = wordIndexer;
		_words = words;
	}

	/**
	 * Reads the Strings from an array.
	 * 
	 * @param LmReaderCallback
	 */
	@Override
	public void parse(final LmReaderCallback<LongRef> callback) {
		countNgrams(callback);
	}

	/**
	 * @param callback
	 * @return
	 */
	private void countNgrams(final LmReaderCallback<LongRef> callback) {
		LOG.trace("Adding sentence '{}' to BerkeleyLM.", StringUtils.join(_words, ' '));
		final int[] sent = new int[_words.size()];
		for (int i = 0; i < _words.size(); i++)
			sent[i] = _wordIndexer.getOrAddIndexFromString(_words.get(i));
		callback.call(sent, 0, sent.length, new LongRef(1L), _unused_words_as_string_dummy);
	}

}
