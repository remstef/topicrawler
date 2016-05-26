package de.tudarmstadt.lt.lm.service;

import java.util.List;
import java.util.stream.Collectors;

public class FactoredLMStringProvider extends PreTokenizedStringProvider {
	
//	@SuppressWarnings("unchecked")
//	@Override
//	public List<String>[] getNgrams(String text) throws Exception {
//		List<String>[] ngrams = super.getNgrams(text);
//		return Arrays.stream(ngrams).map(ngram -> {
//			List<String> new_ngram = new ArrayList<>(ngram.size()+1);
//			String[] token_topic;
//			for(int i = 0; i < ngram.size()-1; i++){
//				token_topic = ngram.get(i).split(":", 2);
//				new_ngram.set(i, token_topic[0]);
//			}
//			token_topic = ngram.get(ngram.size()-1).split(":", 2);
//			new_ngram.set(ngram.size()-1, token_topic[0]);
//			
//			
//			
//			List<String> new_ngram = ngram.stream().map(t -> {
//				return t.split(":", 2)[0];
//			}).collect(Collectors.toList());
//			
//			return new_ngram;
//		}).toArray(List[]::new);
//	}
//	
//	@Override
//	public List<String>[] getNgramSequence(List<String> sequence) throws Exception {
//		// TODO Auto-generated method stub
//		return super.getNgramSequence(sequence);
//	}
	
	
	@Override
	public double getNgramLog10Probability(List<String> ngram) throws Exception {
		// ngram-format: 
		// topic:topic_log10_prob:token
		List<String> modified_ngram = ngram.stream().map(t -> {
			String[] splits = t.split(":", 3);
			return splits[2];
		}).collect(Collectors.toList());
		double log10prob = super.getNgramLog10Probability(modified_ngram);
		String last = ngram.get(ngram.size()-1);
		
		String[] splits = last.split(":", 3);
		double topic_log10_prob = Double.parseDouble(splits[1]);
		
		double result = log10prob + topic_log10_prob;
		
		return result;  
	}
	
	

}
