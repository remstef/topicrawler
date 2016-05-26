package de.tudarmstadt.lt.lm.berkeleylm;

import edu.berkeley.nlp.lm.ConfigOptions;
import edu.berkeley.nlp.lm.WordIndexer;
import edu.berkeley.nlp.lm.io.KneserNeyLmReaderCallback;
import edu.berkeley.nlp.lm.map.NgramMap;
import edu.berkeley.nlp.lm.values.KneserNeyCountValueContainer.KneserNeyCounts;

public class KneserNeyLmReaderCallbackWrapper<W> extends KneserNeyLmReaderCallback<W> {

	public KneserNeyLmReaderCallbackWrapper(WordIndexer<W> wordIndexer, int maxOrder, ConfigOptions opts) {
		super(wordIndexer, maxOrder, opts);
	}

//	public KneserNeyLmReaderCallbackWrapper(WordIndexer<W> wordIndexer, int order) {
//		super(wordIndexer, order, new ConfigOptions());
//	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NgramMap<KneserNeyCounts> getNgramMap() {
		return ngrams;
	}

}