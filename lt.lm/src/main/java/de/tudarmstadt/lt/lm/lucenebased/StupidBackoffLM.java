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
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Steffen Remus
 */
public class StupidBackoffLM extends CountingStringLM {

	private static final Logger LOG = LoggerFactory.getLogger(StupidBackoffLM.class);

	protected long _V;
	protected double _D[]; // for each order a different discount D
	protected double _up;
	protected double _ud;
	
	public StupidBackoffLM(int order, File index_dir) {
		this(order, index_dir, .4);
	}

	public StupidBackoffLM(int order, File index_dir, double discount) {
		super(order, index_dir);
		_D = new double[getOrder()+1];
		Arrays.fill(_D, discount);
		 
		_up = 1d / (_num_ngrams[1][0]+1);
		_ud = _up * _D[0];
	}

	@Override
	public double getNgramLogProbability(List<String> ngram) {
		return Math.log10(getNgramProbability(ngram));
	}

	public double getNgramProbability(List<String> ngram) {
		// check length
		assert ngram.size() <= _order : "Length of Ngram must be lower or equal to the order of the language model.";

		double s = score(ngram);

		return s;
	}

	/**
	 * @param ngram
	 */
	public double score(List<String> ngram) {

		// check length
		int n = ngram.size(); 

		if(n < 1){
			LOG.warn("Ngram should not be zero length!");
			return _ud;
		}

		if(ngramEndsWithOOV(ngram)) 
			return _ud;

		List<String> ngram_, hist_;
		Document ngram_d, hist_d;
		double s, cs, ls, lw, nom, denom;

		// start with unigram probability
		ngram_ = ngram.subList(n-1, n);
		ngram_d = getNgramLuceneDoc(ngram_);
		nom = getQuantity(ngram_d);
		denom = _num_ngrams[1][0];
		
		if(nom==0 || denom == 0){
			LOG.trace(String.format("%50.50s s=%.3e c(%s)=%d / N=num_unigrams=%d lw=%.3e ls=%.3e", ngram_, _ud, ngram, (long)nom, (long)denom, _D[0], _up));
			return _ud;
		}
		
		s = nom / denom;
		lw = _D[1];
		if(ngram_.size() == n){
			LOG.trace(String.format("%50.50s s=%.3e, cs=%.3e, lw=%.3e, ls=%.3e", ngram_, s, s, 0d, 0d));
			return s;
		}
		
		// lower ngram probabilities
		for(int i = 2; i < n && i < _order; i++){
			ls = s;
			ngram_ = ngram.subList(n-i, n);
			hist_ = ngram.subList(n-i, n-1);
			ngram_d = getNgramLuceneDoc(ngram_);
			hist_d = getNgramLuceneDoc(hist_);
			nom = getQuantity(ngram_d);
			denom = getQuantity(hist_d);
			
			if(nom == 0 || denom == 0){
				lw = _D[i];
				if(ngram_.size() < _order)
					s = ls*lw;
				LOG.trace(String.format("%50.50s c(%s)=%d / c(%s)=%d", ngram, ngram_, (long)nom, hist_, (long)denom));
				LOG.trace(String.format("%50.50s s=%.3e, cs=%.3e, lw=%.3e, ls=%.3e", ngram_, s, 0d, lw, ls));
				return s;
			}

			cs = nom / denom;
			s = cs;
		}

		// end with probability of lm order if defined
		if(n < _order)
			return s;
		ls = s;
		ngram_ = ngram.subList(n-_order, n);
		hist_ = ngram.subList(n-_order, n-1);
		ngram_d = getNgramLuceneDoc(ngram_); // catch the case when the ngram might be longer than the lm order
		hist_d = getNgramLuceneDoc(hist_);
		nom = getQuantity(ngram_d);
		denom = getQuantity(hist_d);
		LOG.trace(String.format("%50.50s c(%s)=%d / c(%s)=%d", ngram, ngram_, (long)nom, hist_, (long)denom));
		
		if(nom==0 || denom == 0){
			lw = _D[1];
			if(ngram_.size() < _order)
				s = s*lw;
			LOG.trace(String.format("%50.50s s=%.3e, cs=%.3e, lw=%.3e, ls=%.3e", ngram_, s, 0d, lw, ls));
			return s;
		}

		cs = nom / denom;
		s = cs;
		LOG.trace(String.format("%50.50s s=%.3e, cs=%.3e, lw=%.3e, ls=%.3e", ngram_, s, cs, lw, ls));
		return s;	

	}

	static List<String> getNgramHistory(List<String> ngram){
		assert ngram.size() > 0 : "Ngram must be longer than 0!";
		return ngram.subList(0, ngram.size() - 1);
	}

	static List<String> getLowerOrderNgram(List<String> ngram){
		assert ngram.size() > 0 : "Ngram must be longer than 0!";
		return ngram.subList(1, ngram.size());
	}

	static boolean isDefined(double d){
		return !(Double.isInfinite(d) || Double.isNaN(d)) && d > 0;
	}

}
