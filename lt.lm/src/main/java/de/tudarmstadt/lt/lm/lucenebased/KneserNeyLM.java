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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.PseudoSymbol;
import de.tudarmstadt.lt.lm.util.Properties;
import static java.lang.Math.*;

/**
 *
 * @author Steffen Remus
 */
public class KneserNeyLM extends CountingStringLM {

	private static final Logger LOG = LoggerFactory.getLogger(KneserNeyLM.class);

	protected static Document _UNKOWN_NGRAM_LUCENE_DOCUMENT;
	protected static Document _UNKOWN_WORD_LUCENE_DOCUMENT;

	static{
		_UNKOWN_WORD_LUCENE_DOCUMENT = new Document();
		_UNKOWN_WORD_LUCENE_DOCUMENT.add(new StoredField("word", PseudoSymbol.UNKOWN_WORD.asString()));

		_UNKOWN_NGRAM_LUCENE_DOCUMENT = new Document();
		_UNKOWN_NGRAM_LUCENE_DOCUMENT.add(new StoredField("ngram", PseudoSymbol.UNKOWN_WORD.asString()));
		_UNKOWN_NGRAM_LUCENE_DOCUMENT.add(new StoredField("num", 0));
		_UNKOWN_NGRAM_LUCENE_DOCUMENT.add(new StoredField("cardinality", 0));
		_UNKOWN_NGRAM_LUCENE_DOCUMENT.add(new StoredField("nf_s", 0));
		_UNKOWN_NGRAM_LUCENE_DOCUMENT.add(new StoredField("nf_N1", 0));
		_UNKOWN_NGRAM_LUCENE_DOCUMENT.add(new StoredField("nf_N2", 0));
		_UNKOWN_NGRAM_LUCENE_DOCUMENT.add(new StoredField("nf_N3", 0));
		_UNKOWN_NGRAM_LUCENE_DOCUMENT.add(new StoredField("np_s", 0));
		_UNKOWN_NGRAM_LUCENE_DOCUMENT.add(new StoredField("np_N1", 0));
		_UNKOWN_NGRAM_LUCENE_DOCUMENT.add(new StoredField("np_N2", 0));
		_UNKOWN_NGRAM_LUCENE_DOCUMENT.add(new StoredField("np_N3", 0));
		_UNKOWN_NGRAM_LUCENE_DOCUMENT.add(new StoredField("nfp_s", 0));
		_UNKOWN_NGRAM_LUCENE_DOCUMENT.add(new StoredField("nfp_N1", 0));
		_UNKOWN_NGRAM_LUCENE_DOCUMENT.add(new StoredField("nfp_N2", 0));
		_UNKOWN_NGRAM_LUCENE_DOCUMENT.add(new StoredField("nfp_N3", 0));
	}

	protected long _V;
	protected double[] _D; // for each order a different discount D1
	protected int _num_recursions;
	protected boolean _uniformbackoff;
	protected double _up_log;
	protected double _uw_log;
	protected double _ud;
	protected boolean _use_static_punk;
	
	public KneserNeyLM(int order, File index_dir, double discount) {
		super(order, index_dir);
		
		_uniformbackoff = Properties.knUniformBackoff();
		LOG.info("Backoff with uniform disribution regardless of maximum backoff recursion steps: {}", _uniformbackoff);
		
		_D = new double[getOrder()+1];
		Arrays.fill(_D, .7);
		
		boolean auto_estimate_discounts = discount > 1 || discount < 0;
		if(!auto_estimate_discounts){
			Arrays.fill(_D, discount);	
			LOG.info("Discount D={}.", discount);
		}else{
			Arrays.fill(_D, .7);// initially set discounts evenly with a default value, default value comes as a suggestion from jurafsky and martin
			LOG.info("Computing number of ngram occurrences.");
			for(int n = 1; n <= order; n++){
				double[] N = Arrays.copyOf(_N[n], _N[n].length);

				LOG.info("Number of {}-grams occurring at least once:    {}", n, N[0]);
				LOG.info("Number of {}-grams occurring exactly once:     {}", n, N[1]);
				LOG.info("Number of {}-grams occurring exactly twice:    {}", n, N[2]);

				//					D = N1 / (N1 + 2N2)
				// in order for this to work the following must hold: 2N[2] < N[1]
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
				_D[n] = N[1] / (N[1] + 2d*N[2]);  // discount for standard kneser-ney
				LOG.info("Discount D_n={}:     {}.", n, _D[n]);
			}
		}

		try {
			_V = _searcher_vocab.collectionStatistics("word").docCount();
		} catch (IOException e) {
			LOG.error("Vocabulary luceneindex failed. Could not get number of documents (words).", e);
		}
		
		if(Float.isFinite(Properties.knUnkLog10Prob())){
			_ud = pow(10, Properties.knUnkLog10Prob());
			_use_static_punk = true;
			_uw_log = Double.NaN;
			_up_log = Double.NaN;
		}else{
			_use_static_punk = false;
			// static unkprob
			_up_log = -log(_num_ngrams[2][0]); // 1/num_bigrams
			_uw_log = log(_D[1]) - log(_num_ngrams[1][0]); // D/num_unigrams
			_ud = exp(_up_log + _uw_log);
		}
		
		LOG.info("Uniform weight:       {}", exp(_uw_log));
		LOG.info("Uniform probablity:   {} (log10={}, log2={})", exp(_up_log), _up_log / log(10), _up_log / log(2));
		LOG.info("Uniform distribution: {} (log10={}, log2={})", _ud, log10(_ud), log10(_ud) / log(2));
	}
	
	public double punk(List<String> ngram){
		if(_use_static_punk)
			return _ud;
		if(ngram.size() < 2)
			return _ud;
		List<String> bigram_hist = ngram.subList(ngram.size()-2, ngram.size()-1);
		double nom = getNumFollow(bigram_hist)[0];
		if(nom == 0)
			return _ud;
		double up = log(nom) - log(_num_ngrams[2][0]);
		double ud = exp(up + _uw_log);
		return ud;
	}

	@Override
	public double getNgramLogProbability(List<String> ngram) {
 		return log10(getNgramProbability(ngram));
	}

	public double getNgramProbability(List<String> ngram) {
		// check length
		assert ngram.size() <= _order : "Length of Ngram must be lower or equal to the order of the language model.";
		assert ngram.size() > 0 : "ngram must not be empty!";
		
		double punk = punk(ngram);
		double prob = kn(ngram, punk);
		
		if(Double.isNaN(prob)){
			LOG.warn("Probability of ngram is NAN, that must not happen. Resetting p({})=p([])={}.", ngram, _ud);
			return _ud;
		}
		
		if(prob > 1d){
			if(prob > 1.1d)
				LOG.warn("Probability is greater than 1. This must not happen, something must be wrong with the ngram counts. (Resetting p({})={} => 1.0)", ngram, prob);
			return 1d;
		}
		
		if(prob < punk){
			if(!_use_static_punk && (prob - punk) > 1e-5)
				LOG.warn("Probability of ngram is lower than probability of unknowns, that should not happen. Resetting p({})={} -> p([])={}.", ngram, prob,  punk);
			return punk;
		}
		return prob;
	}

	public double kn(List<String> ngram, double punk) {
		int n = ngram.size();
		if(n < 1){
			LOG.warn("Ngram should not be zero length!");
			return punk;
		}
		
		if(ngramEndsWithOOV(ngram)) 
			return punk;

		List<String> ngram_, hist_, hist__ = null;
		Document ngram_d, hist_d, hist_d_ = null;
		double pkn, p, lw, lp = 0, nom, denom, nfh, nfh_ = 0, nph, nfph, ch = 0d;

		// start with unigram probability
		ngram_ = ngram.subList(n-1, n);
		ngram_d = getNgramLuceneDoc(ngram_);
		nom = getNumPrecede(ngram_d)[0];
		denom = _num_ngrams[2][0];
		LOG.trace(String.format("%50.50s np(%s)=%d / nfp([])=num_bigrams=%d", ngram_, ngram_, (long)nom, (long)denom));		
		if(nom == 0){
			nom = getQuantity(ngram_d);
			if(nom == 0)
				return exp(-log(_num_ngrams[1][0])); // 1 / num_unigrams	
			return exp(log(nom)-log(_sum_ngrams[1])); // c(w) / sum_unigrams 
//			return punk;
		}
		
		pkn = exp(log(nom) - log(denom));
		LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram_, pkn, pkn, 0d, 0d));
		
		// lower ngram probabilities
		for(int i = 2; i < n && i < _order; i++){
			hist_ = ngram.subList(n-i, n-1);
			hist_d = getNgramLuceneDoc(hist_);
			
			nfph = getNumFollowerPrecede(hist_d)[0];
			nfh = getNumFollow(hist_d)[0];
			nph = getNumPrecede(hist_d)[0];
			
			// heuristically correct nfph count. It must be larger or equal to the maximum of nph or nfh.
			// counts may go wrong when pruning ngrams by mincount 
			nfph = max(nfh, max(nph, nfph));
			denom = nfph;
			if(denom == 0){
				LOG.trace(String.format("%50.50s reset_pkn denominator nfp(%s)=0", ngram, hist_));
				if(ch != 0 && lp != 0 && nfh_ != 0 && hist_d_ != null){
					// use higher order probability of lower order ngram here
					nom = getQuantity(ngram_d);
					LOG.trace(String.format("%50.50s reset_pkn c(%s)=%d / c(%s)=%d", ngram, ngram_, (long)nom, hist_, (long)ch));
					denom = log(ch);
					nom = max(nom - _D[i], 0);
					p = 0;
					if(nom != 0)
						p = exp(log(nom) - denom);
					lw = (log(_D[i]) - denom) + log(nfh_);
					pkn = p + exp(lw + log(lp));
					LOG.trace(String.format("%50.50s reset_pkn pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram_, pkn, p, exp(lw), lp));
				}
				LOG.trace(String.format("%50.50s reset_pkn denominator c(%s)=0 -> pkn=%.3e", ngram, hist__, pkn));
				return pkn;
			}
			
			denom = log(denom);
			ngram_ = ngram.subList(n-i, n);
			ngram_d = getNgramLuceneDoc(ngram_);
			nom = getNumPrecede(ngram_d)[0];
			ch = getQuantity(hist_d);
			
			LOG.trace(String.format("%50.50s np(%s)=%d / nfp(%s)=%d", ngram_, getNgramString(ngram_d), (long)nom, getNgramString(hist_d), (long)nfph));
			LOG.trace(String.format("%50.50s nf(%s)=%d", ngram_, getNgramString(hist_d), (long)nfh));
			
			if(nfh == 0){
				// HACK: Usually, this never happens since the nfp count was larger than 0,
				// but when using mincounts, sequences are disrupted and nfh counts may go wrong
				// Since this happens usually only for infrequent ngrams, we reset the nfh count to 1
				nfh = 1;
				LOG.debug(String.format("%50.50s WARN reset nf(%s)=0->%d", ngram_, getNgramString(hist_d), (long)nfh));
			}
			
			lp = pkn;
			lw = (log(_D[i]) - denom) + log(nfh);
			p = 0;
			nom = max(nom - _D[i], 0);
			if(nom != 0)
				p = exp(log(nom) - denom);
			pkn = p + exp(lw + log(lp));
			LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram_, pkn, p, exp(lw), lp));
			
			if(nom == 0) // higher ngram probabilities will be zero, let's not waste probability mass for that
				return pkn;
			
			nfh_ = nfh;
			hist_d_ = hist_d;
			hist__ = hist_;
		}
		
		
		hist_ = ngram.subList(max(0, n-_order), n-1);
		hist_d = getNgramLuceneDoc(hist_);
		ch = getQuantity(hist_d);
		
		if(ch == 0){
			LOG.trace(String.format("%50.50s reset_pkn denominator c(%s)=0", ngram, hist_));
			if(hist_d_ != null && (ch = getQuantity(hist_d_)) != 0 && lp != 0 && nfh_ != 0 && hist_d_ != null){
				// use higher order probability of lower order ngram here
				nom = getQuantity(ngram_d);
				LOG.trace(String.format("%50.50s reset_pkn c(%s)=%d / c(%s)=%d", ngram_, getNgramString(ngram_d), (long)nom, getNgramString(hist_d_), (long)ch));
				denom = log(ch);
				nom = max(nom - _D[n], 0);
				p = 0;
				if(nom != 0)
					p = exp(log(nom) - denom);
				lw = (log(_D[n]) - denom) + log(nfh_);
				pkn = p + exp(lw + log(lp));
				LOG.trace(String.format("%50.50s reset_pkn pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram_, pkn, p, exp(lw), lp));
			}
			LOG.trace(String.format("%50.50s reset_pkn denominator c(%s)=0 -> pkn=%.3e", ngram, hist__, pkn));
			return pkn;
		}
		
		ngram_ = ngram.subList(max(0, n-_order), n);
		ngram_d = getNgramLuceneDoc(ngram_); // catch the case when the ngram might be longer than the lm order
		nom = getQuantity(ngram_d);
		LOG.trace(String.format("%50.50s c(%s)=%d / c(%s)=%d", ngram_, getNgramString(ngram_d), (long)nom, getNgramString(hist_d), (long)ch));
		denom = log(ch);
		
		nfh = getNumFollow(hist_d)[0];
		if(nfh == 0){
			// HACK: usually, this never happens since the history count is greater than 0, 
			// but when using mincount, ngrams with min counts are removed, 
			// thus leading to a history count but not necessarily to a follower count. 
			// Since this happens usually only for infrequent ngrams, we reset the nfh count to 1
			nfh = 1;
			LOG.trace(String.format("%50.50s nf(%s)=0->%d", ngram_, getNgramString(hist_d), (long)nfh));
		}else
			LOG.trace(String.format("%50.50s nf(%s)=%d", ngram_, getNgramString(hist_d), (long)nfh));
			
		lp = pkn;
		nom = max(nom - _D[n], 0);
		p = 0;
		if(nom != 0)
			p = exp(log(nom) - denom);
		lw = (log(_D[n]) - denom) + log(nfh);
		pkn = p + exp(lw + log(lp));
		LOG.trace(String.format("%50.50s pkn=%.3e, p=%.3e, lw=%.3e, lp=%.3e", ngram_, pkn, p, exp(lw), lp));
		
		return pkn;		
	}

	double[] getNumFollow(List<String> ngram){
		Document d_ngram = getNgramLuceneDoc(ngram);
		return getNumFollow(d_ngram);
	}

	double[] getNumFollow(Document ngram){
		double[] v = new double[4];
		IndexableField field = ngram.getField("nf_N1");
		if(field != null)
			v[1] = field.numericValue().longValue();
		field = ngram.getField("nf_N2");
		if(field != null)
			v[2] = field.numericValue().longValue();
		field = ngram.getField("nf_N3");
		if(field != null)
			v[3] = field.numericValue().longValue();
		v[0] = v[1] + v[2] +  v[3];
		return v;
	}

	double[] getNumPrecede(List<String> ngram){
		Document d_ngram = getNgramLuceneDoc(ngram);
		return getNumPrecede(d_ngram);
	}

	double[] getNumPrecede(Document ngram){
		double[] v = new double[4];
		IndexableField field = ngram.getField("np_N1");
		if(field != null)
			v[1] = field.numericValue().longValue();
		field = ngram.getField("np_N2");
		if(field != null)
			v[2] = field.numericValue().longValue();
		field = ngram.getField("np_N3");
		if(field != null)
			v[3] = field.numericValue().longValue();
		v[0] = v[1] + v[2] +  v[3];
		return v;
	}

	double[] getNumFollowerPrecede(List<String> ngram){
		Document d_ngram = getNgramLuceneDoc(ngram);
		return getNumFollowerPrecede(d_ngram);
	}

	double[] getNumFollowerPrecede(Document d_ngram){
		double[] v = new double[4];
		IndexableField field = d_ngram.getField("nfp_N1");
		if(field != null)
			v[1] = field.numericValue().longValue();
		field = d_ngram.getField("nfp_N2");
		if(field != null)
			v[2] = field.numericValue().longValue();
		field = d_ngram.getField("nfp_N3");
		if(field != null)
			v[3] = field.numericValue().longValue();
		v[0] = v[1] + v[2] +  v[3];
		return v;
	}
	
	double[] getNumFollowerPrecede2(Document d_ngram){
		String ngram = getNgramString(d_ngram);
		return getNumFollowerPrecede2(ngram);
	}
	
	double[] getNumFollowerPrecede2(String ngram_str) {
		if (ngram_str == null)
			throw new IllegalAccessError("Ngram is null.");
		if(ngram_str.isEmpty())
			return getNumFollowerPrecede(_UNKOWN_NGRAM_LUCENE_DOCUMENT);
		
		double[] v = new double[4]; 
		Query query = new ConstantScoreQuery(new TermQuery(new Term("history", ngram_str)));
		try {
			Document doc = null;
			ScoreDoc[] hits = _searcher_ngram.search(query, Integer.MAX_VALUE).scoreDocs;
			if(hits.length < 1)
				return getNumFollowerPrecede(_UNKOWN_NGRAM_LUCENE_DOCUMENT);
			
			for(ScoreDoc hit : hits){
				doc = _searcher_ngram.doc(hit.doc);
				double[] v_ = getNumFollow(doc);
				for(int i = 0; i < v_.length; i++)
					v[i] += v_[i];
			}
		} catch (IOException e) {
			LOG.error("Could not get ngram {}. Luceneindex failed.", ngram_str, e);
		}
		return v;
	}

	@Override
	public String predictNextWord(List<String> history_words) {
//		// check length
//		assert history_words.size() >= 1 : "Length of history must be at least of ngram order 1.";
//		List<String> pruned_history_words = history_words.subList(max(0, (history_words.size() - getOrder()) + 1), history_words.size());
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
//		double r = random();
//		double x = 0d;
//		List<String> words = new ArrayList<String>();
//		List<Double> vals = new ArrayList<Double>();
//		List<Integer> ids = new ArrayList<Integer>();
//		
//		for(Iterator<String> iter = getVocabularyIterator(); iter.hasNext();){
//			ngram.set(lastIndex, iter.next());
//			double logprob = log10(kn(ngram, 0d));
//			if (logprob > max_value) {
//				LOG.debug("Word '{}' log10Prob: {}", ngram.get(lastIndex) , logprob);
//				max_value = logprob;
//				max_word = ngram.get(lastIndex);
//			}
//			double p = pow(10, logprob);
//			if((x+=p) > r){
//				return ngram.get(lastIndex);
//			}
//			vals.add(p);
//			words.add(ngram.get(lastIndex));
//			ids.add(ids.size());
//		}
//		
////		Collections.sort(ids, new Comparator<Integer>(){
////			@Override
////			public int compare(Integer o1, Integer o2) {
////				return vals.get(o1).compareTo(vals.get(o2));
////			}
////		});
////		
////		for(Integer i : ids){
////			if((x+=vals.get(i)) > r){
////				return words.get(i);
////			}
////		}
//
//		return max_word;
		// FIXME:
		return "";
	}

}
