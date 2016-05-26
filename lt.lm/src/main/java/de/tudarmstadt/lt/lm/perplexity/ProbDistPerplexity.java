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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.LanguageModel;
import de.tudarmstadt.lt.lm.service.LMProvider;
import de.tudarmstadt.lt.lm.service.LMProviderUtils;

/**
 * 
 * Perplexity of a probability distribution
 * 
 * 2^H(P) = 2^[-\sum\limits_x p(x)log_2(p(x))]
 * 
 * You probably want to use {@link ModelPerplexity}
 * 
 * @author Steffen Remus
 * 
 */
@Deprecated()
public class ProbDistPerplexity<W> implements Serializable{

	private static final long serialVersionUID = 7512778907145068035L;

	private final static Logger LOG = LoggerFactory.getLogger(ProbDistPerplexity.class);

	private LanguageModel<W> _lm;
	private double _sum_probs = 0d;
	private long _sum_ngrams = 0L;

	public ProbDistPerplexity(LanguageModel<W> languageModel) {
		_lm = languageModel;
	}

	public void reset() {
		_sum_probs = 0d;
		_sum_ngrams = 0L;
	}

	public double get() throws Exception {
		if(Double.isInfinite(_sum_probs))
			return Double.POSITIVE_INFINITY;
		double e = -(_sum_probs);
		double perp = Math.pow(2, e);
		LOG.trace("{}", String.format("%s sum(p(x)log(p(x)))=%g; H(p)=%g; Perp=%6.3e;", "[...]", _sum_probs, e, perp));
		return perp;
	}

	public long getN() {
		return _sum_ngrams;
	}

	public double getLog2Probs() {
		return _sum_probs;
	}

	public double addLog2Prob(List<W> ngram) throws Exception {
		_sum_ngrams++;
		double prod = calcProbProduct(_lm, ngram);
		_sum_probs += prod;
		return prod;
	}

	public static <W> double calculatePerplexity(LanguageModel<W> lm, LMProvider<W> lmp, List<W> wordSequence) throws Exception {
		List<W>[] ngram_sequence = lmp.getNgramSequence(wordSequence);
		return calculatePerplexity(lm, ngram_sequence);
	}

	public static <W> double calculatePerplexity(LanguageModel<W> lm, List<W>[] ngramSequence) throws Exception {
		double sum_probs = 0d;
		for (List<W> ngram : ngramSequence){
			double prod = calcProbProduct(lm, ngram);
			if(Double.isInfinite(prod))
				return Double.POSITIVE_INFINITY;
			sum_probs += prod;
		}

		double e = -(sum_probs);
		double perp = Math.pow(2, e);
		LOG.trace("{}", String.format("%s sum(p(x)log(p(x)))=%g; H(p)=%g; Perp=%6.3e;", "[...]", sum_probs, e, perp));
		return perp;
	}


	public static <W> double calcProbProduct(LanguageModel<W> lm, List<W> ngram) throws Exception {
		assert ngram.size() <= lm.getOrder() : "The size of the N-gram must be equal to the order of the language model.";
		double log10probability = lm.getNgramLogProbability(ngram);
		if(Double.isInfinite(log10probability)){
			LOG.trace("p({})={}, log_2={}; log_10={}; p(x)log_2(p(x))={})", ngram.toString(), String.format("%g", 0d), String.format("%g", log10probability), String.format("%g", log10probability), String.format("%g", 0d));
			return Double.NEGATIVE_INFINITY;
		}
		double log2probability = log10probability / Math.log10(2);
		double prob = Math.pow(2, log2probability);
		double product = prob * log2probability;
		LOG.trace("p({})={}, log_2={}; log_10={}; p(x)log_2(p(x))={})", ngram.toString(), String.format("%g", prob), String.format("%g", log2probability), String.format("%g", log10probability), String.format("%g", product));
		return product;
	}

}
