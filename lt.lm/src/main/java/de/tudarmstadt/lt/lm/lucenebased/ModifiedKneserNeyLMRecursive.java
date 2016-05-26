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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Steffen Remus
 */
public class ModifiedKneserNeyLMRecursive extends KneserNeyLMRecursive {

	private static final Logger LOG = LoggerFactory.getLogger(ModifiedKneserNeyLMRecursive.class);

	double[] _D1; // for each order a different discount D1
	double[] _D2; // for each order a different discount D2
	double[] _D3p; // for each order a different discount D3plus

	public ModifiedKneserNeyLMRecursive(int order, File index_dir, double discount) {
		super(order, index_dir, discount);

		_D1 = new double[getOrder()+1];
		_D2 = new double[getOrder()+1];
		_D3p = new double[getOrder()+1];

		// just set them evenly with a default value
		// if you do this you should use the standard KneserNeyLM
		// adjust by autoestimation below
		Arrays.fill(_D1, .7);
		Arrays.fill(_D2, .7);
		Arrays.fill(_D3p,.7);
		//		_D1[0] = 1/3d; _D2[0] = 1d; _D3p[0] = 1/3d; // lowest discount values (0-gram) just assume 1 count for each N1,N2,N3,N4; calculated according to default formula given by e.g. Chen & Goodman


		boolean auto_estimate_discounts = discount < 0 || discount > 1;
		if(!auto_estimate_discounts){
			LOG.info("Manually assigning discount D={}.", discount);
			Arrays.fill(_D1, discount);
			Arrays.fill(_D2, discount);
			Arrays.fill(_D3p, discount);
		}else{
			for(int n = 1; n <= order; n++){
				double[] N = Arrays.copyOf(_N[n], _N[n].length);

				LOG.info("Number of {}-grams occurring at least once:    {} (=={}).", n, _num_ngrams[n][0], N[0]);
				LOG.info("Number of {}-grams occurring exactly once:     {} (=={}).", n, _num_ngrams[n][1], N[1]);
				LOG.info("Number of {}-grams occurring exactly twice:    {} (=={}).", n, _num_ngrams[n][2], N[2]);
				LOG.info("Number of {}-grams occurring exactly 3 times:  {}.", n, N[3]);
				LOG.info("Number of {}-grams occurring exactly 4 times:  {}.", n, N[4]);
				LOG.info("Number of {}-grams occurring 5 times or more:  {}.", n, N[5]);
				LOG.info("Number of {}-grams occurring at least 3 times: {}.", n, _num_ngrams[n][3]);

				if(auto_estimate_discounts){
					//					Y  = N1 / (N1 + 2N2)
					//					D1 = 1−2Y(N2 / N1)
					//					D2 = 2−3Y(N3 / N2)
					//					D3+= 3−4Y(N4 / N3)
					// HACK: in order for this to work the following must hold: 4N[4] < 3N[3] < 2N[2] < N[1]
					// force this if necessary
					for(int i = N.length-1; i >= 2; --i){
						if(N[i] == 0){
							N[i] = 1;
							LOG.debug("Adjusted N({})[{}]={}", n, i, N[i]);
						}
						if(i * N[i] >= (i-1)*N[i-1]){
							N[i-1] = (N[i] * i) + 1;
							LOG.debug("Adjusted N({})[{}]={}", n, i-1, N[i-1]);
						}
					}
					double Y = Math.max(.05, Math.min(.5, N[1] / (N[1] + 2d*N[2])));  // discount for standard kneser-ney
					_D[n] = Y;
					_D1[n] = 1d-2d*Y*(N[2] / N[1]);
					_D2[n] = 2d-3d*Y*(N[3] / N[2]);
					_D3p[n] = 3d-4d*Y*(N[4] / N[3]);
					
					// normalize, so that it sums to Y
					double s = _D1[n] + _D2[n] + _D3p[n];
					_D1[n] = Math.max(.05, Math.min(.95, _D1[n] / s * Y));
					_D2[n] = Math.max(.05, Math.min(.95, _D2[n] / s * Y));
					_D3p[n] = Math.max(.05, Math.min(.95, _D3p[n] / s * Y));
					
//					_D1[n] = Double.isFinite(_D1[n]) ? _D1[n] : 0.7;
//					_D2[n] = Double.isFinite(_D2[n]) ? _D2[n] : 0.7;
//					_D3p[n] = Double.isFinite(_D3p[n]) ? _D3p[n] : 0.7;
//					
				}
			}
		}
				
		for(int n = 0; n <= _order; n++){ 
			LOG.info("Discount:D_n={}:      {}.", n, _D[n]);
			LOG.info("Discount D1_n={}:     {}.", n, _D1[n]);
			LOG.info("Discount D2_n={}:     {}.", n, _D2[n]);
			LOG.info("Discount D3plus_n={}: {}.", n, _D3p[n]);
		}

		double denom = _num_ngrams[2][0]; // num_bigrams()
		double uw = (_D[1] / denom);//			    lw = (d / denom) 
		double up = 1d / (_num_ngrams[1][0]+1); //			    lp = d / numunigrams() # uniform prob
		_ud = uw * up;
		
		LOG.info("Uniform weight:       {}", uw);
		LOG.info("Uniform probablity:   {} (log10={}, log2={})", up, Math.log10(up), Math.log10(up) / Math.log10(2));
		LOG.info("Uniform distribution: {} (log10={}, log2={})", _ud, Math.log10(_ud), Math.log10(_ud) / Math.log10(2));
		
	}

	double getDiscount(int n, double count){
		if(count <= 0)
			return 0;
		if(count == 1)
			return _D1[n];
		if(count == 2)
			return _D2[n];
		return _D3p[n];
	}

	@Override
	public double kn_recursive(List<String> ngram_, boolean isLower, int num_recursions) {
		// check length
		List<String> ngram = ngram_;
		int n = ngram.size(); 		//			    n = len(ngram)
		
		if(isUnkownWord(ngram_.get(n-1))) 
			return _ud; //			    p = 1 / numunigrams()+1 # uniform prob
		
		if(n > 1){
			List<String> hist = ngram.subList(0, ngram.size()-1);		//			    hist  = ngram[:-1]
			List<String> lower = ngram.subList(1, ngram.size());		//			    lower = ngram[1:]

			if(!isLower){ 		//			    if not islower:
				//			        # highest order prob and weight
				double c = getQuantity(ngram); 			//			        c      = num(ngram)
				double d = getDiscount(n, c);
				double c_hist = getQuantity(hist);		//			        c_hist = num(hist)
				LOG.trace(String.format("%50.50s c=%d / c_hist=%d", ngram, (long)c, (long)c_hist));
				if(num_recursions == 0){//	        if num_recursions == 0: # recursion stop?
					double p = c_hist == 0 ? 0 : c / c_hist;//	                p = 0 if c_hist == 0 else c / c_hist
					double pkn = p, lw = 0, lp =0;//	                pkn = p; lw = 0; lp = 0
					LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram, pkn, p, lw, lp));//			            _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
					return pkn;//	                return pkn
				}
				double lp = kn_recursive(lower, true, num_recursions-1); 			//			        lp = kn(lower, d, True)
				if(c_hist == 0){			//			        if c_hist == 0:
					double p = 0d, lw = _D[n], pkn = lw * lp;	//			            p = 0; lw = d; pkn = lw * lp
					LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram, pkn, p, lw, lp));//			            _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
					return pkn; 			//			            return pkn
				}
				double p = Math.max(c - d, 0) / c_hist;//			        p  = max(c - d, 0) / c_hist
				double[] N = getNumFollow(hist);
				LOG.trace(String.format("%50.50s num_follow_hist=%s", ngram, Arrays.toString(N)));
				double lw = (_D1[n]*N[1] + _D2[n]*N[2] + _D3p[n]*N[3]) / c_hist;//			        lw = (d / c_hist) * numfollow(hist)
				double pkn = p + lw * lp;	//			        pkn = p + lw * lp
				LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram, pkn, p, lw, lp));//			        _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
				return pkn;//			        return pkn
			}else if(n > 1){ 		//			    elif n > 1:
				double nom = getNumPrecede(ngram)[0];
				double denom = getNumFollowerPrecede(hist)[0];	//			        denom = numfollowerprecede(hist)
				LOG.trace(String.format("%50.50s num_precede=%d / num_follower_precede_hist=%d", ngram, (long)nom, (long)denom));
				if(num_recursions == 0){//			if num_recursions == 0: # recursion stop?
					double p = denom == 0 ? 0 : nom / denom;//		            p = 0 if denom == 0 else numprecede(ngram) / denom
					double pkn = p, lw = 0, lp = 0;//	                pkn = p; lw = 0; lp = 0
					LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram, pkn, p, lw, lp));//			            _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
					return pkn;
				}
				//			        # continuation prob and weight for bigrams++
				double lp = kn_recursive(lower, true, num_recursions-1);	//			        lp = kn(lower, d, True)
				double d = getDiscount(n, (long)nom);
				if(denom == 0){//			        if denom == 0:
					if(nom != 0)
						LOG.error("Oh nooo, something is terribly wrong with the calculations. The denominator in p_cont must never ever be zero when the nominator is != zero. ({}, nom=numprecede(ngram)={}, denom=numfollowerprecede(hist)={})", ngram, nom, denom);
					double p = 0d, lw = _D[n], pkn = lw * lp;	//			            p = 0; lw = d; pkn = lw * lp
					LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram, pkn, p, lw, lp));//			            _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
					return pkn;
				}
				double p = Math.max(nom - d, 0) / denom;//			        p  = max(numprecede(ngram)-d,0) / denom
				double[] N = getNumFollow(hist);
				LOG.trace(String.format("%50.50s num_follow_hist=%s", ngram, Arrays.toString(N)));
				double lw = (_D1[n]*N[1] + _D2[n]*N[2] + _D3p[n]*N[3]) / denom;//			        lw = (d / denom) * numfollow(hist)
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
		
		double d = getDiscount(n, (long)nom);
		double p = Math.max(nom - d, 0) / denom;//			    p  = max(numprecede(ngram)-d,0) / denom
		double lw = (_D[1] / denom); //			    lw = (d / denom) 
		double lp = 1d / (_num_ngrams[1][0]+1); //			    lp = 1 / numunigrams() # uniform prob
		double pkn = p + lw * lp;//			    pkn = p + lw * lp
		LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram, pkn, p, lw, lp));//			    _logger.log(9,('{:<50} :{pkn:.3f} = {p:.3f} + {lw:.3f} * {lp:.3f}').format(('{!s:<10}'*n).format(*ngram), pkn=pkn, p=p, lw=lw, lp=lp))
		return pkn;//			    return pkn
	}
}
