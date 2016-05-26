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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Steffen Remus
 */
public class PoptKneserNeyLMRecursive extends KneserNeyLMRecursive {

	private static final Logger LOG = LoggerFactory.getLogger(PoptKneserNeyLMRecursive.class);

	public PoptKneserNeyLMRecursive(int order, File index_dir, double discount) {
		super(order, index_dir, discount);
	}

	@Override
	public double kn_recursive(List<String> ngram_, boolean isLower, int num_recursions) {
		// check length
		List<String> ngram = ngram_;
		int n = ngram.size(); 		//			    n = len(ngram)
		
		if(ngramEndsWithOOV(ngram_)) 
			return  _ud;
		
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
				
				if(c ==0 || c_hist == 0){			//			        if c_hist == 0:
					double lp = kn_recursive(lower, false, num_recursions-1); 			//			        lp = kn(lower, d, True)
					double p = 0d, lw = 1d, pkn = lw * lp;	//			            p = 0; lw = d; pkn = lw * lp
					LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram, pkn, p, lw, lp));//			            _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
					return pkn; 			//			            return pkn
				}
				double lp = kn_recursive(lower, true, num_recursions-1); 			//			        lp = kn(lower, d, True)
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
					double p = 0d, lw = 1d, pkn = lw * lp;	//			            p = 0; lw = d; pkn = lw * lp
					LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram, pkn, p, lw, lp));//			            _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
					return pkn; 			//			            return pkn
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

		nom = getNumPrecede(ngram)[0];
		denom = _num_ngrams[2][0];//			    denom = numbigrams() # divide by number of bigrams
		LOG.trace(String.format("%50.50s num_precede=%d / num_bigrams=%d", ngram, (long)nom, (long)denom));
		if(nom == 0){
			LOG.debug("Warn: Word '{}' seems to be known but has no precedence count. This often happens when using mincount > 1.", ngram.get(n-1));
			return _ud;
		}
		if(denom == 0)
			return _ud;
		return nom / denom;
		
//		double p = nom / denom;//
//		double p  = Math.max(nom-_D[1],0) / denom;
//		double pkn = p + _ud;
//		LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram, pkn, p, _uw, _up));//			            _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
//		return pkn;
	}

}
