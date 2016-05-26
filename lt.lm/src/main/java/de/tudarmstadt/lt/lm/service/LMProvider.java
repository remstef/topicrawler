package de.tudarmstadt.lt.lm.service;

import java.rmi.Remote;
import java.util.List;

public interface LMProvider<W> extends Remote {
	
	public List<W>[] getNgramSequence(List<W> sequence) throws Exception;
		
	public List<W>[] getNgramSequence(List<W> sequence, int order) throws Exception;

	
}
