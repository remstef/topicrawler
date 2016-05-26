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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.util.Properties;

/**
 *
 * @author Steffen Remus
 */
public class KneserNeyLMRecursive extends KneserNeyLM {

	private static final Logger LOG = LoggerFactory.getLogger(KneserNeyLMRecursive.class);

	public KneserNeyLMRecursive(int order, File index_dir, double discount) {
		super(order, index_dir, discount);
		
		_num_recursions = Properties.knMaxbackoffrecursions();
		_num_recursions = _num_recursions < 0 ? _order : _num_recursions;
		LOG.info("Number of maximum backoff recursion steps: {}", _num_recursions);

	}

	@Override
	public double getNgramLogProbability(List<String> ngram) {
 		return Math.log10(getNgramProbability(ngram));
	}

	public double getNgramProbability(List<String> ngram_) {
		// check length
		assert ngram_.size() <= _order : "Length of Ngram must be lower or equal to the order of the language model.";
		assert ngram_.size() > 0 : "ngram must not be empty!";
		
		double prob = kn_recursive(ngram_, false, _num_recursions);
		
		if(Double.isNaN(prob)){
			LOG.error("Probability of ngram is NAN, that must not happen. Resetting p({})=0.", ngram_);
			return 0d;
		}
		
		if(prob > 1d){
			if(prob > 1.1d)
				LOG.error("Probability is greater than 1. This must not happen, something must be wrong with the ngram counts. (Resetting p({})={} => 1.0)", ngram_, prob);
			return 1d;
		}
		
		return prob;
	}

	public double kn_recursive(List<String> ngram_, boolean isLower, int num_recursions) {
		// check length
		List<String> ngram = ngram_;
		int n = ngram.size(); 		//			    n = len(ngram)
		
//		if(isUnkownWord(ngram_.get(n-1))) 
//			return _ud; //			    p = 1 / numunigrams()+1 # uniform prob
		
		if(n > 1){
			List<String> hist = ngram.subList(0, ngram.size()-1);		//			    hist  = ngram[:-1]
			List<String> lower = ngram.subList(1, ngram.size());		//			    lower = ngram[1:]

			if(!isLower){ 		//			    if not islower:
				//			        # highest order prob and weight
				double c = getQuantity(ngram); 			//			        c      = num(ngram)
				double c_hist = getQuantity(hist);		//			        c_hist = num(hist)
				LOG.trace(String.format("%50.50s c=%d / c_hist=%d", ngram, (long)c, (long)c_hist));
				if(num_recursions == 0){//	        if num_recursions == 0: # recursion stop?
					double p = c_hist == 0 ? 0 : c / c_hist;//	                p = 0 if c_hist == 0 else c / c_hist
					double pkn = p, lw =0, lp =0;//	                pkn = p; lw = 0; lp = 0
					LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram, pkn, p, lw, lp));//			            _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
					return pkn;//	                return pkn
				}
				double lp = kn_recursive(lower, true, num_recursions-1); 			//			        lp = kn(lower, d, True)
				if(c_hist == 0){			//			        if c_hist == 0:
					double p = 0d, lw = _D[n], pkn = lw * lp;	//			            p = 0; lw = d; pkn = lw * lp
					LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram, pkn, p, lw, lp));//			            _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
					return pkn; 			//			            return pkn
				}
				double p = Math.max(c - _D[n], 0) / c_hist;//			        p  = max(c - d, 0) / c_hist
				double num_follow_hist = getNumFollow(hist)[0];
				LOG.trace(String.format("%50.50s num_follow_hist=%d", ngram, (long)num_follow_hist));
				double lw = (_D[n] / c_hist) * num_follow_hist;//			        lw = (d / c_hist) * numfollow(hist)
				double pkn = p + lw * lp;	//			        pkn = p + lw * lp
				LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram, pkn, p, lw, lp));//			        _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
				return pkn;//			        return pkn
			}else{ 		//			    elif n > 1:
				double nom = getNumPrecede(ngram)[0];
				double denom = getNumFollowerPrecede(hist)[0];	//			        denom = numfollowerprecede(hist)
				LOG.trace(String.format("%50.50s num_precede=%d / num_follower_precede_hist=%d", ngram, (long)nom, (long)denom));
				if(num_recursions == 0){//			if num_recursions == 0: # recursion stop?
					double p = denom == 0 ? 0 : nom / denom;//		            p = 0 if denom == 0 else numprecede(ngram) / denom
					double pkn = p, lw =0, lp =0;//	                pkn = p; lw = 0; lp = 0
					LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram, pkn, p, lw, lp));//			            _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
					return pkn;
				}
				//			        # continuation prob and weight for bigrams++
				double lp = kn_recursive(lower, true, num_recursions-1);	//			        lp = kn(lower, d, True)
				if(denom == 0){//			        if denom == 0:
					if(nom != 0)
						LOG.error("Oh nooo, something is terribly wrong with the calculations. The denominator in p_cont must never ever be zero when the nominator is != zero. ({}, nom=numprecede(ngram)={}, denom=numfollowerprecede(hist)={})", ngram, nom, denom);
					double p = 0d, lw = _D[n], pkn = lw * lp;	//			            p = 0; lw = d; pkn = lw * lp
					LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram, pkn, p, lw, lp));//			        _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
					return pkn;
				}
				double p = Math.max(nom - _D[n], 0) / denom;//			        p  = max(numprecede(ngram)-d,0) / denom
				double num_follow_hist = getNumFollow(hist)[0];
				LOG.trace(String.format("%50.50s num_follow_hist=%d", ngram, (long)num_follow_hist));
				double lw = (_D[n] / denom) * num_follow_hist;//			        lw = (d / denom) * numfollow(hist)
				double pkn = p + lw * lp;	//			        pkn = p + lw * lp
				LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram, pkn, p, lw, lp));//			        _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
				return pkn; //			        return pkn
			}
		}
		//			    # continuation prob and weight for unigrams
		double nom, denom;
		if(!isLower){
			nom = getQuantity(ngram);
			denom = _sum_ngrams[2];
			LOG.trace(String.format("%50.50s num_ngram=%d / sum_bigrams=%d", ngram, (long)nom, (long)denom));
		}else{
			nom = getNumPrecede(ngram)[0];
			denom = _num_ngrams[2][0];//			    denom = numbigrams() # divide by number of bigrams
			LOG.trace(String.format("%50.50s num_precede=%d / num_bigrams=%d", ngram, (long)nom, (long)denom));
		}		
		if(num_recursions == 0){//	    	    if num_recursions == 0: # recursion stop?
			double p = nom / denom;//	    	        p = 0 if denom == 0 else numprecede(ngram) / denom
			double pkn = p, lw =0, lp =0;//	                pkn = p; lw = 0; lp = 0
			LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram, pkn, p, lw, lp));//			            _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
			return pkn;//	    	        return pkn
		}
		double p = Math.max(nom - _D[1], 0) / denom;//			    p  = max(numprecede(ngram)-d,0) / denom
		double lw = (_D[1] / denom);//			    lw = (d / denom) 
		double lp = 1d / (_num_ngrams[1][0]+1); //			    lp = d / numunigrams() # uniform prob
		double pkn = p + lw * lp;//			    pkn = p + lw * lp
		LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram, pkn, p, lw, lp));//			    _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
		return pkn;//			    return pkn
	}

	@Override
	public String predictNextWord(List<String> history_words) {
//		// check length
//		assert history_words.size() >= 1 : "Length of history must be at least of ngram order 1.";
//		List<String> pruned_history_words = history_words.subList(Math.max(1, (history_words.size() - getOrder()) + 1), history_words.size());
//
//		LOG.debug("History: {}; pruned: {}.", history_words.toString(), pruned_history_words.toString());
//
//		//		List<Integer> wordIds = toIntegerList(pruned_history_words);
//		//		wordIds.add(-1);
//		List<String> ngram = new ArrayList<String>(pruned_history_words);
//		ngram.add("<unk>");
//		int lastIndex = ngram.size() - 1;
//		double max_value = -Double.MAX_VALUE;
//		String max_word = ngram.get(lastIndex);
//
//		for(Iterator<String> iter = getVocabularyIterator(); iter.hasNext();){
//			ngram.set(lastIndex, iter.next());
//			double logprob = Math.log10(kn_recursive(ngram, false, 1));
//			LOG.trace("Word '{}' log10Prob: ", ngram.get(lastIndex) , logprob);
//			if (logprob > max_value) {
//				max_value = logprob;
//				max_word = ngram.get(lastIndex);
//			}
//		}
//		return max_word;
		// FIXME
		return "";
	}

}
