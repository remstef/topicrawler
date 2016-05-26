package de.tudarmstadt.lt.seg.sentence.rules;


public interface BoundaryProcessor {
	
	/**
	 * 
	 * @param candidate
	 * @return
	 */
	default boolean isCompleteSentence(String candidate){
		return !isIncompleteSentence(candidate);
	}

	/**
	 * 
	 * @param candidate
	 * @return
	 */
	boolean isIncompleteSentence(String candidate);
	
}
