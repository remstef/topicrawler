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
package de.tudarmstadt.lt.lm.service;

import java.io.StringReader;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.utilities.ArrayUtils;

/**
 *
 * @author Steffen Remus
 **/
public class BreakIteratorStringProvider extends AbstractStringProvider implements StringProviderMXBean{

	private static Logger LOG = LoggerFactory.getLogger(BreakIteratorStringProvider.class);

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

		text = de.tudarmstadt.lt.utilities.StringUtils.trim_and_replace_emptyspace(text, " ");

		for(LineIterator iter = new LineIterator(new StringReader(text)); iter.hasNext();){
			String line = iter.nextLine();
			BreakIterator sentence_bounds = BreakIterator.getSentenceInstance(LocaleUtils.toLocale(language_code));
			sentence_bounds.setText(line);
			int begin_s = sentence_bounds.first();
			for (int end_s = sentence_bounds.next(); end_s != BreakIterator.DONE; begin_s = end_s, end_s = sentence_bounds.next()) {

				String sentence = de.tudarmstadt.lt.utilities.StringUtils.trim(line.substring(begin_s, end_s));
				if(sentence.isEmpty())
					continue;
				sentences.add(sentence);
				LOG.trace(String.format("Current sentence: %s", StringUtils.abbreviate(sentence, 200)));
			}
		}
		LOG.trace(String.format("Split text '%s' into '%d' sentences.", StringUtils.abbreviate(text, 200), sentences.size()));
		return sentences;
	}

	@Override
	public List<String> tokenizeSentence_intern(String sentence, String language_code){
		ArrayList<String> tokens = new ArrayList<String>();
		BreakIterator token_bounds = BreakIterator.getWordInstance(LocaleUtils.toLocale(language_code));
		token_bounds.setText(sentence.trim());
		int begin_t = token_bounds.first();
		for (int end_t = token_bounds.next(); end_t != BreakIterator.DONE; begin_t = end_t, end_t = token_bounds.next()) {
			String token = de.tudarmstadt.lt.utilities.StringUtils.trim_and_replace_emptyspace(sentence.substring(begin_t, end_t), "_");
			if(!token.isEmpty()){ // add token iff token is not empty
				tokens.add(token);
			}
		}
		return tokens;
	}

}
