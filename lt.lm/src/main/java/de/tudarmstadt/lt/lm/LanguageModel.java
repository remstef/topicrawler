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
package de.tudarmstadt.lt.lm;

import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Steffen Remus
 **/
public interface LanguageModel<W> {

	public int getOrder() throws Exception;

	public W predictNextWord(List<W> history) throws Exception;

	public W getWord(int wordId) throws Exception;

	public int getWordIndex(W word) throws Exception;

	public int[] getNgramAsIds(List<W> ngram) throws Exception;

	public List<W> getNgramAsWords(int[] ngram) throws Exception;

	public List<W> getNgramAsWords(List<Integer> ngram) throws Exception;

	public double getSequenceLogProbability(List<W>[] ngram_sequence) throws Exception;

	public double getNgramLogProbability(int[] ngram) throws Exception;

	/**
	 *
	 * @param ngram
	 * @return log10 probability of ngram, i.e. p(w_n|w_1...w_n-1)
	 */
	public double getNgramLogProbability(List<W> ngram) throws Exception;

	public Iterator<List<W>> getNgramIterator() throws Exception;

	public Iterator<List<Integer>> getNgramIdIterator() throws Exception;
	
	public boolean ngramContainsOOV(int[] ngram) throws Exception;
	
	public boolean ngramContainsOOV(List<W> ngram) throws Exception;
	
	public boolean ngramEndsWithOOV(List<W> ngram) throws Exception;
	
	public boolean ngramEndsWithOOV(int[] ngram) throws Exception;
	
	public boolean isUnkownWord(W word) throws Exception;
	
	public boolean isUnkownWord(int wordId) throws Exception;
	
}
