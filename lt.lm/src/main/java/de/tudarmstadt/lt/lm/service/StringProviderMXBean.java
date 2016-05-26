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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 *
 * @author Steffen Remus
 **/
public interface StringProviderMXBean extends Remote, LMProvider<String> {

	public boolean getModelReady() throws RemoteException;

	public int getLmOrder() throws Exception;

	public double getPerplexity() throws Exception;

	public void resetPerplexity() throws Exception;

	public double getPerplexity(String text, boolean skip_oov) throws Exception;

	public double getPerplexity(String text, String language_code, boolean skip_oov) throws Exception;

	public void addToPerplexity(String text) throws Exception;

	public void addToPerplexity(String text, String language_code) throws Exception;

	public double addToPerplexity(List<String> ngram) throws Exception;

	public List<String>[] getNgrams(String text) throws Exception;

	public List<String>[] getNgrams(String text, String language_code) throws Exception;

	public double getNgramLog10Probability(List<String> ngram) throws Exception;

	public double getNgramSequenceLog10Probability(List<String>[] ngram_sequence) throws Exception;

	public double getSequenceLog10Probability(String ngram_sequence) throws Exception;

	public String predictNextWord(String ngram_sequence) throws Exception;

	public String predictNextWord(List<String> ngram) throws Exception;

	public int[] getNgramAsIds(List<String> ngram) throws Exception;

	public List<String>[] getNgramSequenceFromSentence(List<String> sentence) throws Exception;

	public List<String> getNgramAsWords(int[] ngram_ids) throws Exception;

	public List<String> splitSentences(String text, String language_code) throws Exception;

	public List<String> splitSentences(String text) throws Exception;

	public List<String> tokenizeSentence(String sentence) throws Exception;

	public List<String> tokenizeSentence(String sentence, String language_code) throws Exception;

	public List<String> tokenizeSentence_intern(String sentence, String language_code) throws Exception;

	public boolean ngramContainsOOV(List<String> ngram) throws Exception;
	
	public boolean ngramEndsWithOOV(List<String> ngram) throws Exception;
	

}
