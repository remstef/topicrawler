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
package de.tudarmstadt.lt.lm.lucenebased;

import java.io.File;
import java.util.List;

import org.apache.lucene.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Steffen Remus
 */
public class OneBackoffKneserNeyLM extends KneserNeyLM {

	private static final Logger LOG = LoggerFactory.getLogger(OneBackoffKneserNeyLM.class);

	public OneBackoffKneserNeyLM(int order, File index_dir, double discount) {
		super(order, index_dir, discount);
	}

	@Override
	public double kn(List<String> ngram_, double punk) {
		// check length

		int n = ngram_.size(); 		//			    n = len(ngram)
		
		if(n < 1){
			LOG.warn("Ngram should not be zero length!");
			return punk;
		}
		
		if(ngramEndsWithOOV(ngram_)) 
			return punk;
		

		Document ngram, hist;
		double pkn, p, lw, lp, nom, denom;

		// start with lower order probability
		ngram = getNgramLuceneDoc(ngram_.subList(n-_order+1, n)); // catch the case when the ngram might be longer than the lm order
		nom = getNumPrecede(ngram)[0];
		
		if(nom == 0)
			return punk;
		
		if(n == 2)
			denom = _num_ngrams[2][0];
		else{
			hist = getNgramLuceneDoc(ngram_.subList(n-_order+1, n-1)); // catch the case when the ngram might be longer than the lm order
			denom = getNumFollowerPrecede(hist)[0];
		}
		pkn = nom / denom;
		LOG.trace(String.format("%50.50s num_precede=%d / num_bigrams=%d", ngram, (long)nom, (long)denom));
		
		
		// end with probability of lm order if defined
		if(n < _order)
			return pkn;
		
		ngram = getNgramLuceneDoc(ngram_.subList(n-_order, n)); // catch the case when the ngram might be longer than the lm order
		hist = getNgramLuceneDoc(ngram_.subList(n-_order, n-1));
		nom = getQuantity(ngram);
		denom = getQuantity(hist);

		if(denom == 0)
			return pkn;
		
		lp = pkn;
		p = Math.max(nom - _D[n], 0) / denom;
		lw = getNumFollow(hist)[0] * _D[n] / denom;
		pkn = p + lw * lp;
		
		return pkn;		
	}

}
