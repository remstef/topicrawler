package de.tudarmstadt.lt.lm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public abstract class AbstractLanguageModel<W> {

	public abstract int getOrder();

	public abstract W predictNextWord(List<W> history);

	public abstract W getWord(int wordId);

	public abstract int getWordIndex(W word);

	public int[] getNgramAsIds(List<W> ngram) {
		assert ngram.size() <= getOrder() && ngram.size() > 0: String.format("The size of the N-gram must be lower or equal to the order of the language model and greater than 0, but was %d.", ngram.size());
		int[] ids = new int[ngram.size()];
		for(int i = 0; i < ngram.size(); i++)
			ids[i] = getWordIndex(ngram.get(i));
		return ids;
	}

	public List<W> getNgramAsWords(List<Integer> ngram) {
		assert ngram.size() <= getOrder() && ngram.size() > 0: String.format("The size of the N-gram must be lower or equal to the order of the language model and greater than 0, but was %d.", ngram.size());
		List<W> words = new ArrayList<W>(ngram.size());
		for (int i = 0; i < ngram.size(); i++)
			words.add(getWord(ngram.get(i)));
		return words;
	}

	public List<W> getNgramAsWords(int[] ngram) {
		assert ngram.length <= getOrder() && ngram.length > 0: String.format("The size of the N-gram must be lower or equal to the order of the language model and greater than 0, but was %d.", ngram.length);
		List<W> words = new ArrayList<W>(ngram.length);
		for (int i = 0; i < ngram.length; i++)
			words.add(getWord(ngram[i]));
		return words;
	}

	public double getSequenceLogProbability(List<W>[] ngram_sequence) {
		double p = 0d;
		for (List<W> ngram : ngram_sequence)
			p += getNgramLogProbability(ngram);
		return p;
	}

	public abstract double getNgramLogProbability(int[] ngram);

	/**
	 * 
	 * @param ngram
	 * @return log10 probability of ngram, i.e. p(w_n|w_1...w_n-1)
	 */
	public abstract double getNgramLogProbability(List<W> ngram);

	public abstract Iterator<List<W>> getNgramIterator();

	public abstract Iterator<List<Integer>> getNgramIdIterator();

	

	public boolean ngramContainsOOV(List<W> ngram){
		return ngramContainsOOV(getNgramAsIds(ngram));
	}

	public boolean ngramContainsOOV(int[] ngram){
		for(int id : ngram)
			if(isUnkownWord(id))
				return true;
		return false;
	}
	
	public boolean ngramEndsWithOOV(List<W> ngram){
		return ngramEndsWithOOV(getNgramAsIds(ngram));
	}
	
	public boolean ngramEndsWithOOV(int[] ngram){
		return isUnkownWord(ngram[ngram.length-1]);
	}

	public boolean isUnkownWord(W word){
		return isUnkownWord(getWordIndex(word));
	}
	
	public boolean isUnkownWord(int wordId){
		return wordId < 0;
	}
		
}
