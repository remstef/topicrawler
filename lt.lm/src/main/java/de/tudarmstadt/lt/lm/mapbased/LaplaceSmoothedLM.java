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

import java.util.List;

/**
 * 
 * @author Steffen Remus
 */
public class LaplaceSmoothedLM<W> extends CountingLM<W> {

	public LaplaceSmoothedLM(CountingLM<W> decoratedLm) {
		super();
		_order = decoratedLm._order;
		_index = decoratedLm._index;
		_inv_index = decoratedLm._inv_index;
		_fixed = decoratedLm._fixed;
		_sum_one_grams = 0d;
		_ngrams_of_order = decoratedLm._ngrams_of_order;
		_ngrams_of_lower_order = decoratedLm._ngrams_of_lower_order;
	}

	@Override
	public double getNgramLogProbabilityFromIds(List<Integer> ngram) {
		// check length
		assert ngram.size() <= getOrder() : "Length of ngram must be equal to the order of the language model.";
		if (ngram.size() < 1)
			return Double.NEGATIVE_INFINITY;

		// c(w_1 ... w_n) + 1
		Integer nominator = _ngrams_of_order.get(ngram) + 1;

		// c(w_1)+1 / N+|V|
		if (ngram.size() == 1)
			return Math.log10(nominator) - Math.log10(_sum_one_grams + _index.size());

		// c(w_1 ... w_n-1) + |V|
		Integer denominator = _ngrams_of_lower_order.get(ngram.subList(0, ngram.size() - 1)) + _index.size();

		// c(w_1 ... w_n) / c(w_1 ... w_n-1)+|V|
		double logprob = Math.log10(nominator) - Math.log10(denominator);
		return logprob;
	}
}
