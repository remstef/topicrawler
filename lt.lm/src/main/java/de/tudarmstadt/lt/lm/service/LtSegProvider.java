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
package de.tudarmstadt.lt.lm.service;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.util.Properties;
import de.tudarmstadt.lt.seg.Segment;
import de.tudarmstadt.lt.seg.SegmentType;
import de.tudarmstadt.lt.seg.sentence.ISentenceSplitter;
import de.tudarmstadt.lt.seg.token.ITokenizer;
import de.tudarmstadt.lt.utilities.ArrayUtils;

/**
 *
 * @author Steffen Remus
 **/
public class LtSegProvider extends AbstractStringProvider implements StringProviderMXBean{

	private static Logger LOG = LoggerFactory.getLogger(LtSegProvider.class);
	
	private ThreadLocal<ITokenizer> _tokenizer;
	private ThreadLocal<ISentenceSplitter> _sentenceSplitter;
	
	/**
	 * 
	 */
	public LtSegProvider() {
		_sentenceSplitter = ThreadLocal.withInitial(() -> {
			try {
				return Properties.sentenceSplitter().newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			}
		});
		_tokenizer = ThreadLocal.withInitial(() -> {
			try {
				return Properties.tokenizer().newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			}
		});

	}

	@Override
	public List<String>[] getNgrams(String text, String language_code) throws Exception {
		LOG.trace(String.format("Computing ngrams from text: %s", StringUtils.abbreviate(text, 200)));
		List<String>[] ngrams = null;

		for(String sentence : splitSentences(text, language_code)){
			List<String> tokens = tokenizeSentence(sentence, language_code);
			if(tokens.isEmpty())
				continue;
			LOG.trace(String.format("Current sentence: %s", StringUtils.abbreviate(tokens.toString(), 200)));
			List<String>[] current_ngrams = getNgramSequenceFromSentence(tokens);

			LOG.trace(String.format("Current ngrams: %s", StringUtils.abbreviate(Arrays.toString(current_ngrams), 200)));
			if (ngrams == null)
				ngrams = current_ngrams;
			else
				ngrams = ArrayUtils.getConcatinatedArray(ngrams, current_ngrams);
		}

		if (ngrams == null)
			ngrams = EMPTY_NGRAM_LIST;
		LOG.trace(String.format("Ngrams for text: '%s': %s ", StringUtils.abbreviate(text, 200), StringUtils.abbreviate(Arrays.toString(ngrams), 200)));
		return ngrams;
	}

	@Override
	public List<String> splitSentences(String text, String language_code) throws Exception {
		LOG.trace(String.format("Splitting sentences from text: %s", StringUtils.abbreviate(text, 200)));
		List<String> sentences = new ArrayList<String>();
		
		if(Properties.onedocperline()){
			LineIterator liter = new LineIterator(new StringReader(text));
			for(String line; (line = liter.hasNext() ? liter.next() : null) != null;)
				split_and_add_sentences(line, sentences);
		}else{
			split_and_add_sentences(text, sentences);
		}
		
		LOG.trace(String.format("Split text '%s' into '%d' sentences.", StringUtils.abbreviate(text, 200), sentences.size()));
		return sentences;
	}
	
	private void split_and_add_sentences(String text, List<String> sentences){
		text = de.tudarmstadt.lt.utilities.StringUtils.trim_and_replace_emptyspace(text, " ");
		ISentenceSplitter sentenceSplitter = _sentenceSplitter.get();
		sentenceSplitter.init(text);
		for(Segment s : sentenceSplitter){
			if(s.type == SegmentType.SENTENCE){
				String sentence = de.tudarmstadt.lt.utilities.StringUtils.trim_and_replace_emptyspace(s.text.toString(), " ");
				if(sentence.isEmpty())
					continue;
				LOG.trace(String.format("Current sentence: %s", StringUtils.abbreviate(sentence, 200)));
				sentences.add(sentence);
			}
		}
	}

	@Override
	public List<String> tokenizeSentence_intern(String sentence, String language_code){		
		ITokenizer tokenizer = _tokenizer.get();
		List<String> tokens = new ArrayList<String>();
		
		tokenizer.init(sentence).filteredAndNormalizedTokens(Properties.tokenfilter(), Properties.tokennormalize(), Properties.merge() >= 1, Properties.merge() >= 2).forEach(tokens::add);

		return tokens;
		
	}

}
