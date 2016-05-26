/*
 *   Copyright 2012
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
package de.tudarmstadt.lt.lm.mapbased;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.AbstractLanguageModel;
import de.tudarmstadt.lt.lm.LanguageModel;
import de.tudarmstadt.lt.lm.service.LMProviderUtils;
import de.tudarmstadt.lt.utilities.collections.Bag;
import de.tudarmstadt.lt.utilities.collections.HashBag;

/**
 * 
 * @author Steffen Remus
 */
public class CountingLM<W> extends AbstractLanguageModel<W> implements LanguageModel<W> {

	private static final Logger LOG = LoggerFactory.getLogger(CountingLM.class);

	protected int _order;
	protected List<W> _index;
	protected Map<W, Integer> _inv_index;
	protected double _sum_one_grams;
	protected Bag<List<Integer>> _ngrams_of_order;
	protected Bag<List<Integer>> _ngrams_of_lower_order;
	protected boolean _fixed = false;

	public CountingLM() {	}

	public CountingLM(int order) {
		_order = order;
		_index = new ArrayList<W>();
		_inv_index = new HashMap<W, Integer>();
		_sum_one_grams = 0d;
		_ngrams_of_order = new HashBag<List<Integer>>();
		_ngrams_of_lower_order = new HashBag<List<Integer>>();
	}

	@Override
	public int getOrder() {
		return _order;
	}

	@Override
	public W predictNextWord(List<W> history_words) {
//		// check length
//		assert history_words.size() >= getOrder() - 1 : "Length of history must be at least of ngram order - 1.";
//		List<W> pruned_history_words = history_words.subList((history_words.size() - getOrder()) + 1, history_words.size());
//
//		LOG.debug("History: {}; pruned: {}.", history_words.toString(), pruned_history_words.toString());
//
//		List<Integer> wordIds = toIntegerList(pruned_history_words);
//		wordIds.add(-1);
//		int lastIndex = wordIds.size() - 1;
//		double max_value = -Double.MAX_VALUE;
//		Integer max_wordId = 0;
//		for (int i = 0; i < _index.size(); i++) {
//			wordIds.set(lastIndex, i);
//			double logprob = getNgramLogProbabilityFromIds(wordIds);
//			LOG.trace("Word '{}'({}) log10Prob: ", getWord(i) ,i, logprob);
//			if (logprob > max_value) {
//				max_value = logprob;
//				max_wordId = i;
//			}
//		}
//		return _index.get(max_wordId);
		// FIXME:
		return null;
	}

	@Override
	public W getWord(int wordId) {
		return wordId < _index.size() && wordId >= 0 ? _index.get(wordId) : null;
	}

	@Override
	public int getWordIndex(W word) {
		if (word == null)
			return -1;
		Integer index = _inv_index.get(word);
		return index != null ? index : -1;
	}

	@Override
	public double getNgramLogProbability(int[] wordIds) {
		return getNgramLogProbabilityFromIds(Arrays.asList(ArrayUtils.toObject(wordIds)));
	}

	public double getNgramLogProbabilityFromIds(List<Integer> ngram) {
		// check length
		assert ngram.size() <= _order : "Length of Ngram must be lower or equal to the order of the language model.";
		if(ngram.size() < 1)
			return Double.NEGATIVE_INFINITY;

		// c(w_1 ... w_n)
		Integer nominator = _ngrams_of_order.getQuantity(ngram);
		if (nominator == 0)
			return Double.NEGATIVE_INFINITY;

		// c(w_1) / N
		if(ngram.size() == 1)
			return Math.log10(nominator) - Math.log10(_sum_one_grams);

		// c(w_1 ... w_n-1)
		Integer denominator = _ngrams_of_lower_order.getQuantity(ngram.subList(0, ngram.size() - 1));

		if(denominator == 0)
			return Double.NEGATIVE_INFINITY;

		double logprob = Math.log10(nominator) - Math.log10(denominator);
		return logprob;
	}

	@Override
	public double getNgramLogProbability(List<W> ngram) {
		return getNgramLogProbabilityFromIds(toIntegerList(ngram));
	}


	@Override
	public Iterator<List<W>> getNgramIterator() {
		@SuppressWarnings("unchecked")
		Iterator<List<W>> iter = IteratorUtils.transformedIterator(getNgramIdIterator(), new Transformer() {
			@Override
			public Object transform(final Object o) {
				List<Integer> ngram = (List<Integer>) o;
				return toWordList(ngram);
			}
		});
		return iter;
	}

	@Override
	public Iterator<List<Integer>> getNgramIdIterator() {
		@SuppressWarnings("unchecked")
		Iterator<List<Integer>> iter = IteratorUtils.transformedIterator(_ngrams_of_order.entrySet().iterator(), new Transformer() {
			@Override
			public Object transform(final Object o) {
				Entry<List<Integer>, Integer> ngram_counts = (Entry<List<Integer>, Integer>) o;
				return ngram_counts.getKey();
			}
		});
		return iter;
	}

//	public int addSequence(List<W> sequence) throws IllegalAccessException {
//		if(sequence.size() < 1)
//			return 0;
//		List<W>[] ngram_sequence = LMProviderUtils.getNgramSequence(sequence, _order);
//		for (List<W> ngram : ngram_sequence)
//			addNgram(ngram);
//		return ngram_sequence.length;
//	}

	public int addNgramSequence(List<W>[] ngram_sequence) throws IllegalAccessException {
		for (List<W> ngram : ngram_sequence)
			addNgram(ngram);
		return ngram_sequence.length;
	}

	public int addNgram(List<W> ngram) throws IllegalAccessException {
		for (W ngram_i : ngram)
			getOrAddWord(ngram_i);
		List<Integer> ngram_ = toIntegerList(ngram);
		return addNgramAsIds(ngram_);
	}

	public int addNgramAsIds(List<Integer> ngram) throws IllegalAccessException {
		// check if it is fixed
		if (_fixed)
			throw new IllegalAccessException("LanguageModel is already fixed, which means no more values can be added.");
		// check length
		assert ngram.size() <= _order : "Length of ngram must be lower or equal to the order of the language model.";
		assert ngram.size() > 0 : "Length of ngram must be larger than 0.";
		// add to bag of lm order
		int count = _ngrams_of_order.add(ngram);
		// add to bag of lower order
		if(ngram.size() == 1){
			_sum_one_grams++;
			return count;
		}
		_ngrams_of_lower_order.add(ngram.subList(0, ngram.size() - 1));
		if(ngram.size() == 2)
			_sum_one_grams++;
		return count;
	}

	public Integer getOrAddWord(W word) throws IllegalAccessException {
		Integer index = _inv_index.get(word);
		if(index != null)
			return index;
		if (_fixed)
			throw new IllegalAccessException("LanguageModel is already fixed, which means no more values can be added.");
		index = _index.size();
		_index.add(word);
		_inv_index.put(word, index);
		return index;
	}

	private List<Integer> toIntegerList(List<W> ngram) {
		List<Integer> ngram_ = new ArrayList<Integer>(ngram.size());
		for (W ngram_i : ngram)
			ngram_.add(getWordIndex(ngram_i));
		return ngram_;
	}

	private List<W> toWordList(List<Integer> ngramIds) {
		List<W> ngram = new ArrayList<W>(ngramIds.size());
		for (Integer ngram_i : ngramIds)
			ngram.add(getWord(ngram_i));
		return ngram;
	}

	public void fixItNow() {
		_fixed = true;
	}

}
