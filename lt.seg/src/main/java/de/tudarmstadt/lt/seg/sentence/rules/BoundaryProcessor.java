package de.tudarmstadt.lt.seg.sentence.rules;


public interface BoundaryProcessor {
	
	/**
	 * 
	 * @param candidate
	 * @return
	 */
	boolean isCompleteSentence(String candidate);

	/**
	 * 
	 * @param candidate
	 * @return
	 */
	boolean isIncompleteSentence(String candidate);
	
}
