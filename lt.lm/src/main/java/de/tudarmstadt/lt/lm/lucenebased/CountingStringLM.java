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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Bits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.AbstractLanguageModel;
import de.tudarmstadt.lt.lm.LanguageModel;
import de.tudarmstadt.lt.lm.PseudoSymbol;
import de.tudarmstadt.lt.lm.service.LMProviderUtils;

/**
 *
 * @author Steffen Remus
 */
public class CountingStringLM extends AbstractLanguageModel<String> implements LanguageModel<String> {

	private static final Logger LOG = LoggerFactory.getLogger(CountingStringLM.class);
	
	protected static Document _UNKOWN_NGRAM_LUCENE_DOCUMENT;
	protected static Document _UNKOWN_WORD_LUCENE_DOCUMENT;

	static{
		_UNKOWN_WORD_LUCENE_DOCUMENT = new Document();
		_UNKOWN_WORD_LUCENE_DOCUMENT.add(new StoredField("word", PseudoSymbol.UNKOWN_WORD.asString()));

		_UNKOWN_NGRAM_LUCENE_DOCUMENT = new Document();
		_UNKOWN_NGRAM_LUCENE_DOCUMENT.add(new StoredField("ngram", PseudoSymbol.UNKOWN_WORD.asString()));
		_UNKOWN_NGRAM_LUCENE_DOCUMENT.add(new StoredField("num", 0));
		_UNKOWN_NGRAM_LUCENE_DOCUMENT.add(new StoredField("cardinality", 0));
	}

	protected int _order;

	protected IndexReader _reader_ngram;
	protected IndexSearcher _searcher_ngram;

	protected IndexReader _reader_vocab;
	protected IndexSearcher _searcher_vocab;

	protected double[][] _num_ngrams;
	protected double[] _sum_ngrams;
	protected double[][] _N;
	protected boolean _fixed = false;


	public CountingStringLM(int order, File index_dir) {
		_order = order;

		try {
			LOG.info("Loading index from or creating index in '{}'.", index_dir.getAbsolutePath());

			File index_dir_vocab = new File(index_dir, "vocab");
			File index_dir_ngram = new File(index_dir, "ngram");

			_fixed = true;
			Directory directory = MMapDirectory.open(index_dir_ngram);
			//				directory = new RAMDirectory(directory, IOContext.DEFAULT);
			_reader_ngram = DirectoryReader.open(directory);
			_searcher_ngram = new IndexSearcher(_reader_ngram);

			directory = MMapDirectory.open(index_dir_vocab);
			//				directory = new RAMDirectory(directory, IOContext.DEFAULT);
			_reader_vocab = DirectoryReader.open(directory);
			_searcher_vocab = new IndexSearcher(_reader_vocab);

			LOG.info("Computing number of ngram occurrences.");
			File sumfile = new File(index_dir, "__sum_ngrams__");
			try {
				InputStream in = new FileInputStream(sumfile);
				Properties p = new Properties();
				p.load(in);
				in.close();
				int max_n = Math.max(_order, Integer.parseInt(p.getProperty("max_n")));
				if(max_n < order)
					LOG.error("max_n={} in {} is smaller than the order of the language model ({}).", max_n, sumfile, order);
				int max_c = Integer.parseInt(p.getProperty("max_c"));
				_N = new double[max_n+1][max_c];
				_sum_ngrams = new double[max_n+1];
				for(String name : p.stringPropertyNames()){
					if(name.startsWith("n")){
						int n = Integer.parseInt(name.substring(1, name.length()));
						String[] v = p.getProperty(name).split(",");
						for(int i = 0; i < v.length; i++){
							_N[n][i] = Double.parseDouble(v[i]);
						}
					}else if(name.startsWith("s")){
						int n = Integer.parseInt(name.substring(1, name.length()));
						_sum_ngrams[n] = Double.parseDouble(p.getProperty(name));
					}
				}
			} catch (Exception e) {
				LOG.error("Could not read ngram sum file '{}'.", sumfile, e);
				_N = new double[order+1][6];
				_sum_ngrams = new double[order+1];
			}

			_num_ngrams = new double[_N.length][4];
			long sum = 0;
			for(int n = 0; n < _N.length; n++){
				for(int i = 0; i < 3; i++)
					_num_ngrams[n][i] = _N[n][i];
				for(int i = 3; i < _N[n].length; i++)
					_num_ngrams[n][3] += _N[n][i];
				sum += _num_ngrams[n][0];
			}


			LOG.info("Number of Ngrams {}.", _searcher_ngram.collectionStatistics("ngram").docCount());
			LOG.info("Number of Ngrams {}.", sum);

			LOG.info("Vocabulary Size {}.", _searcher_vocab.collectionStatistics("word").docCount());

		} catch (IOException e) {
			LOG.error("Could not open lucene index: Dir={}; Dir exists={}; ", index_dir, index_dir.exists() && index_dir.isDirectory(), e);
		}
	}

	@Override
	public int getOrder() {
		return _order;
	}

	@Override
	public String predictNextWord(List<String> history_words) {
//
//		// check length
//		assert history_words.size() >= getOrder() - 1 : "Length of history must be at least of ngram order 1.";
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
//			double logprob = getNgramLogProbability(ngram);
//			LOG.trace("Word '{}' log10Prob: ", ngram.get(lastIndex) , logprob);
//			if (logprob > max_value) {
//				max_value = logprob;
//				max_word = ngram.get(lastIndex);
//			}
//		}
//		return max_word;
		// FIXME:
		return "";
	}

	@Override
	public String getWord(int wordId) {
		try {
			if(wordId < 0 || wordId > _searcher_vocab.getIndexReader().maxDoc())
				return PseudoSymbol.UNKOWN_WORD.asString();
			return _searcher_vocab.doc(wordId).get("word");
		} catch (IOException e) {
			LOG.error("Could not get word for id {}. Querying luceneindex failed. Max doc: {}.", wordId, _searcher_vocab.getIndexReader().maxDoc(), e);
			return null;
		}
	}

	@Override
	public int getWordIndex(String word) {
		if (word == null)
			return -1;
		Query query = new TermQuery(new Term("word", word));
		try {
			ScoreDoc[] hits = _searcher_vocab.search(query, null, 2).scoreDocs;
			if(hits.length < 1)
				return -1;
			if(hits.length > 1)
				LOG.warn("Found more than one entry for '{}', expected only one.", word);
			return hits[0].doc;
		} catch (IOException e) {
			LOG.error("Could not get id for word {}. Querying luceneindex failed: {}.", word, query);
			return -1;
		}
	}

	@Override
	public double getNgramLogProbability(int[] wordIds) {
		return getNgramLogProbabilityFromIds(Arrays.asList(ArrayUtils.toObject(wordIds)));
	}

	public double getNgramLogProbabilityFromIds(List<Integer> ngram) {
		return getNgramLogProbability(toWordList(ngram));
	}

	@Override
	public double getNgramLogProbability(List<String> ngram) {
		// check length
		assert ngram.size() <= _order : "Length of Ngram must be lower or equal to the order of the language model.";
		if(ngram.size() < 1)
			return Double.NEGATIVE_INFINITY;

		// c(w_1 ... w_n)
		Long nominator = getQuantity(ngram);
		if (nominator == 0)
			return Double.NEGATIVE_INFINITY;

		// c(w_1) / N
		if(ngram.size() == 1)
			return Math.log10((double)nominator) - Math.log10(_num_ngrams[1][0]);

		// c(w_1 ... w_n-1)
		Long denominator = getQuantity(ngram.subList(0, ngram.size() - 1));

		if(denominator == 0)
			return Double.NEGATIVE_INFINITY;

		double logprob = Math.log10((double)nominator) - Math.log10((double)denominator);
		return logprob;
	}


	public long getQuantity(List<String> ngram) {
		if (ngram == null)
			throw new IllegalAccessError("Ngram is null.");
		if(ngram.isEmpty())
			return 0L;
		String ngram_str = StringUtils.join(ngram, ' ');
		Term query_term = new Term("ngram", ngram_str);
		Query query = new TermQuery(query_term);
		try {
			Document doc = null;
			ScoreDoc[] hits = _searcher_ngram.search(query, 2).scoreDocs;
			if(hits.length >= 1){
				if(hits.length > 1)
					LOG.warn("Found more than one entry for '{}', expected only one.", ngram_str);
				doc = _searcher_ngram.doc(hits[0].doc);
				return getQuantity(doc);
			}
		} catch (IOException e) {
			LOG.error("Could not get ngram {}. Luceneindex failed.", ngram_str, e);
		}
		return 0L;
	}
	
	long getQuantity(Document ngram){
		return getNgramCountFromDoc(ngram);
	}

	long getNgramCountFromDoc(Document ngram){
		IndexableField field = ngram.getField("num");
		return field.numericValue().longValue();
	}

	int getCardinality(Document ngram){
		IndexableField field = ngram.getField("cardinality");
		return field.numericValue().intValue();
	}
	
	String getNgramString(Document ngram){
		IndexableField field = ngram.getField("ngram");
		if(field == null)
			return "<unk>";
		return field.stringValue();
	}
	
	String getWordString(Document word){
		IndexableField field = word.getField("word");
		if(field == null)
			return "<unk>";
		return field.stringValue();
	}
	
	public Document getNgramLuceneDoc(List<String> ngram) {
		if (ngram == null)
			throw new IllegalAccessError("Ngram is null.");
		if(ngram.isEmpty())
			return _UNKOWN_NGRAM_LUCENE_DOCUMENT;
		String ngram_str = StringUtils.join(ngram, ' ');
		return getNgramLuceneDoc(ngram_str);
	}

	public Document getNgramLuceneDoc(String ngram_str) {
		if (ngram_str == null)
			throw new IllegalAccessError("Ngram is null.");
		if(ngram_str.isEmpty())
			return _UNKOWN_NGRAM_LUCENE_DOCUMENT;
		Query query = new TermQuery(new Term("ngram", ngram_str));
		try {
			Document doc = null;
			ScoreDoc[] hits = _searcher_ngram.search(query, 2).scoreDocs;
			if(hits.length >= 1){
				if(hits.length > 1)
					LOG.warn("Found more than one entry for '{}', expected only one.", ngram_str);
				doc = _searcher_ngram.doc(hits[0].doc);
				return doc;
			}
		} catch (IOException e) {
			LOG.error("Could not get ngram {}. Luceneindex failed.", ngram_str, e);
		}
		return _UNKOWN_NGRAM_LUCENE_DOCUMENT;
	}

	public Document getWordLuceneDoc(String word) {
		if (word == null)
			throw new IllegalAccessError("Word is null.");
		if(de.tudarmstadt.lt.utilities.StringUtils.trim(word).isEmpty())
			return _UNKOWN_WORD_LUCENE_DOCUMENT;
		Query query = new TermQuery(new Term("word", word));
		try {
			Document doc = null;
			ScoreDoc[] hits = _searcher_vocab.search(query, 2).scoreDocs;
			if(hits.length >= 1){
				if(hits.length > 1)
					LOG.warn("Found more than one entry for '{}', expected only one.", word);
				doc = _searcher_vocab.doc(hits[0].doc);
				return doc;
			}
		} catch (IOException e) {
			LOG.error("Could not get word {}. Luceneindex failed.", word, e);
		}
		return _UNKOWN_WORD_LUCENE_DOCUMENT;
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

	@Override
	public Iterator<List<String>> getNgramIterator() {
		return new NgramIterator();
	}
	
	public Iterator<String> getVocabularyIterator() {
		return new VocabularyIterator();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<List<Integer>> getNgramIdIterator() {
		return IteratorUtils.transformedIterator(getNgramIterator(), new Transformer() {
			@Override
			public Object transform(Object ngram) {
				return getNgramAsIds((List<String>)ngram);
			}
		});
	}

//	public int addSequence(List<String> sequence) throws IllegalAccessException {
//		if(sequence.size() < 1)
//			return 0;
//		int c = 0;
//		for(int n = 1; n <= getOrder(); n++){
//			List<String>[] ngram_sequence = LMProviderUtils.getNgramSequence(sequence, n);
//			for (List<String> ngram : ngram_sequence)
//				addNgram(ngram);
//			c += ngram_sequence.length;
//		}
//		return c;
//	}

	public int addNgramSequence(List<String>[] ngram_sequence) throws IllegalAccessException {
		for (List<String> ngram : ngram_sequence)
			addNgram(ngram);
		return ngram_sequence.length;
	}

	public int addNgram(List<String> ngram) throws IllegalAccessException {
		throw new UnsupportedOperationException();
	}

	public int addNgramAsIds(List<Integer> ngram) throws IllegalAccessException {
		List<String> ngram_s = Arrays.asList(new String[ngram.size()]);
		for(int i = 0; i < ngram.size(); i++){
			String w = getWord(ngram.get(i));
			ngram_s.set(i, w == null ? String.format("_%d_", ngram.get(i)) : w);
		}
		return addNgram(ngram_s);
	}

	public Integer getOrAddWord(String word) throws IllegalAccessException {
		throw new UnsupportedOperationException();
	}

	private List<String> toWordList(List<Integer> ngramIds) {
		List<String> ngram = new ArrayList<String>(ngramIds.size());
		for (Integer ngram_i : ngramIds)
			ngram.add(getWord(ngram_i));
		return ngram;
	}

	public boolean isFix(){
		return _fixed;
	}

	public void fixItNow() {
		_fixed = true;
	}
	
	public class NgramIterator implements Iterator<List<String>>{

		Bits _liveDocs = MultiFields.getLiveDocs(CountingStringLM.this._reader_ngram);
		int current_docid = -1;
		List<String> current_ngram = null;
		
		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			if(++current_docid >= CountingStringLM.this._reader_ngram.maxDoc())
				return false;
			
			current_ngram = null;
			for (; current_docid < CountingStringLM.this._reader_ngram.maxDoc(); current_docid++) {
			    if (_liveDocs != null && !_liveDocs.get(current_docid))
			        continue;
			    try {
					Document doc = CountingStringLM.this._reader_ngram.document(current_docid);
					if(getCardinality(doc) != _order)
						continue;
					current_ngram = Arrays.asList(getNgramString(doc).split(" "));
					break;
				} catch (IOException e) {
					LOG.error("Could not get ngram lucene doc with id {}.", current_docid, e);
					return false;
				}
			}
			
			return current_ngram != null;
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		@Override
		public List<String> next() {
			return current_ngram;
		}
		
	}
	
	public class VocabularyIterator implements Iterator<String>{

		Bits _liveDocs = MultiFields.getLiveDocs(CountingStringLM.this._reader_vocab);
		int current_docid = -1;
		String current_word = null;
		
		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			if(++current_docid >= CountingStringLM.this._reader_vocab.maxDoc())
				return false;
			
			current_word = null;
			for (; current_docid < CountingStringLM.this._reader_vocab.maxDoc(); current_docid++) {
			    if (_liveDocs != null && !_liveDocs.get(current_docid))
			        continue;
			    try {
					Document doc = CountingStringLM.this._reader_vocab.document(current_docid);
					current_word = getWordString(doc);
					break;
				} catch (IOException e) {
					LOG.error("Could not get ngram lucene doc with id {}.", current_docid, e);
					return false;
				}
			}
			
			return current_word != null;
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		@Override
		public String next() {
			return current_word;
		}
		
	}

}
