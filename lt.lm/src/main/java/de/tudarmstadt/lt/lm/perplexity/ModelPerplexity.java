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
package de.tudarmstadt.lt.lm.perplexity;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.LanguageModel;
import de.tudarmstadt.lt.lm.PseudoSymbol;
import de.tudarmstadt.lt.lm.service.LMProvider;
import de.tudarmstadt.lt.lm.service.StringProviderMXBean;

/**
 * 
 * Perplexity of a probability model
 * 
 *  2^H(P) =  2^[ - \sum\limits_{i=1}^n 1/n log_2(p(x_i))]
 *         = 10^[ - \sum\limits_{i=1}^n 1/n log_10(p(x_i))]
 * n = number of words / number of added ngrams
 * 
 * @author Steffen Remus
 * 
 */

public class ModelPerplexity<W> implements Serializable{

	private static final long serialVersionUID = 7512778907145068035L;

	private final static Logger LOG = LoggerFactory.getLogger(ModelPerplexity.class);

	private StringProviderMXBean _lm_prvdr;
	private long _sum_ngrams = 0L;
	private double _sum_log10probs = 0d;
	private double _base_perplexity;
	private int _lm_order;
	
	@SuppressWarnings("unchecked")
	public ModelPerplexity(StringProviderMXBean lm_prvdr) {
		_lm_prvdr = lm_prvdr;
		try{
			_lm_order = lm_prvdr.getLmOrder();
			Object[] unk_seqence = new Object[_lm_order];
			Arrays.fill(unk_seqence, PseudoSymbol.UNKOWN_WORD.asString());
			addLog10Prob(Arrays.asList((W[])unk_seqence)); // initialize perplexity with lowest possible ngram probability
			_base_perplexity = get();
			reset();
		}catch(Exception e){
			LOG.error("Could not estimate base perplexity. ", e);
			_base_perplexity = (double)Integer.MAX_VALUE;
		}
		
	}

	public void reset() {
		_sum_ngrams = 0L;
		_sum_log10probs = 0d;
	}

	public double get() {
		long N = _sum_ngrams;
		if(N == 0 || !Double.isFinite(_sum_log10probs))
			return _base_perplexity;
		double e = -(_sum_log10probs / N);
		double perp = Math.pow(10, e);
		LOG.trace("{}", String.format("%s n=%d; log10Probs=%6.3e; H(X)~=%6.3e; Perp=%6.3e;", "[...]", _sum_ngrams, _sum_log10probs, e, perp));
		return perp;
	}

	public long getN() {
		return _sum_ngrams;
	}

	public double getLog10Probs() {
		return _sum_log10probs;
	}

	@SuppressWarnings("unchecked")
	public double addLog10Prob(List<W> ngram) throws Exception {
		double log10prob = calcLog10Prob(_lm_prvdr, (List<String>)ngram);
		_sum_ngrams++;
		_sum_log10probs += log10prob;
		return log10prob;
	}

	public static <W> double calculatePerplexity(LanguageModel<W> lm, LMProvider<W> lmp, List<W> wordSequence, boolean skip_oov) throws Exception {
		List<W>[] ngram_sequence = lmp.getNgramSequence(wordSequence);
		return calculatePerplexity(lm, ngram_sequence, skip_oov);
	}

	public static <W> double calculatePerplexity(LanguageModel<W> lm, List<W>[] ngramSequence, boolean skip_oov) throws Exception {
		double sum_log10probs = 0d;
		int n_oov = 0;
		for (List<W> ngram : ngramSequence){
			if(skip_oov && lm.ngramEndsWithOOV(ngram)){
				n_oov++;
				continue;
			}
			sum_log10probs += calcLog10Prob(lm, ngram);
		}

		double N = ngramSequence.length - n_oov; // + lm.getOrder() - 1;
		if(N == 0 || !Double.isFinite(sum_log10probs))
			return Integer.MAX_VALUE;
		double e = -(sum_log10probs / N);
		double perp = Math.pow(10, e);
		LOG.trace("{}", String.format("%s n=%f; log10Probs=%6.3e; H(X)~=%6.3e; Perp=%6.3e;", Arrays.asList(ngramSequence), N, sum_log10probs, e, perp));
		return perp;
	}


	private static <W> double calcLog10Prob(LanguageModel<W> lm, List<W> ngram) throws Exception {
		assert ngram.size() <= lm.getOrder() : "The size of the N-gram must be equal to the order of the language model.";
		double log10probability = lm.getNgramLogProbability(ngram);
		if(LOG.isTraceEnabled()){
			double log2probability = log10probability / Math.log10(2);
			LOG.trace("p({})={}, log_2={}; log_10={})",
					ngram.toString(),
					String.format("%g", Math.pow(2, log2probability)),
					String.format("%g", log2probability),
					String.format("%g", log10probability));
		}
		return log10probability;
	}
	
	public static double calcLog10Prob(StringProviderMXBean lmprvdr, List<String> ngram) throws Exception {
		assert ngram.size() <= lmprvdr.getLmOrder() : "The size of the N-gram must be equal to the order of the language model.";
		double log10probability = lmprvdr.getNgramLog10Probability(ngram);
		if(LOG.isTraceEnabled()){
			double log2probability = log10probability / Math.log10(2);
			LOG.trace("p({})={}, log_2={}; log_10={})",
					ngram.toString(),
					String.format("%g", Math.pow(2, log2probability)),
					String.format("%g", log2probability),
					String.format("%g", log10probability));
		}
		return log10probability;
	}

}
