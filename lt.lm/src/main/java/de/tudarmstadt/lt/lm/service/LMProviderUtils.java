package de.tudarmstadt.lt.lm.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.tudarmstadt.lt.lm.util.Properties;

public class LMProviderUtils {
	
	private LMProviderUtils() { /* DO NOT INSTANTIATE */ }
	
	@SuppressWarnings("unchecked")
	public static <T> List<T>[] getNgramSequence(List<T> sequence, int ngram_length) {
		assert sequence.size() >= 1 : "The length of the sequence must not be empty.";
		if(sequence.isEmpty() )
			return new List[0];
		int boundary_handling = Properties.handleBoundaries();
		// TODO: very inefficient
		if(boundary_handling == 1){
			List<T> new_sequence = new ArrayList<T>(sequence.size() + ngram_length - 2);
			for(int i = 0; i < ngram_length - 2; i++)
				new_sequence.add(sequence.get(0));
			new_sequence.addAll(sequence);
			sequence = new_sequence;
		}

		if(boundary_handling == -1 && sequence.size() < ngram_length)
			return new List[0];

		if(sequence.size() == 1 || (sequence.size() <= ngram_length && boundary_handling < 2))
			return new List[]{ sequence };
		
		int l = sequence.size();
		int o = Math.min(l, ngram_length); // should be also order of the language model

		int n = l - o + 1;
		int i  = 0;
		if(Properties.handleBoundaries() == 2)
			n = n + o - 2;

		List<T>[] ngram_sequence = new List[n];

		// insert ngrams
		if(Properties.handleBoundaries() == 2){
			for(; i < o - 2; i++)
				ngram_sequence[i] = (List<T>) Arrays.asList(sequence.subList(0, i+2).toArray());
		}

		// handle Ngrams
		for (int j = 0; i < n; j++, i++)
			ngram_sequence[i] = (List<T>) Arrays.asList(sequence.subList(j, j + o).toArray());

		return ngram_sequence;

	}

}
