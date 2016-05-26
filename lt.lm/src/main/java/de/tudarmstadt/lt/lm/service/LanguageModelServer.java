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

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.LanguageModel;

/**
 *
 * @author Steffen Remus
 **/
public class LanguageModelServer<W> implements LanguageModelServerMBean<W> {

	private final static Logger LOG = LoggerFactory.getLogger(LanguageModelServer.class);

	private LanguageModel<W> _lm;

	public LanguageModelServer() {
		this(null);
	}

	public LanguageModelServer(LanguageModel<W> language_model) {
		_lm = language_model;
	}

	@Override
	public int getOrder() throws Exception {
		if (_lm == null)
			throw new IllegalAccessError("No language model set.");
		return _lm.getOrder();
	}

	@Override
	public W predictNextWord(List<W> history) throws Exception {
		if (_lm == null)
			throw new IllegalAccessError("No language model set.");
		return _lm.predictNextWord(history);
	}

	@Override
	public W getWord(int wordId) throws Exception {
		if (_lm == null)
			throw new IllegalAccessError("No language model set.");
		return _lm.getWord(wordId);
	}

	@Override
	public int getWordIndex(W word) throws Exception {
		if (_lm == null)
			throw new IllegalAccessError("No language model set.");
		return _lm.getWordIndex(word);
	}

	@Override
	public int[] getNgramAsIds(List<W> ngram) throws Exception {
		if (_lm == null)
			throw new IllegalAccessError("No language model set.");
		return _lm.getNgramAsIds(ngram);
	}

	@Override
	public List<W> getNgramAsWords(int[] ngram) throws Exception {
		if (_lm == null)
			throw new IllegalAccessError("No language model set.");
		return _lm.getNgramAsWords(ngram);
	}

	@Override
	public List<W> getNgramAsWords(List<Integer> ngram) throws Exception {
		if (_lm == null)
			throw new IllegalAccessError("No language model set.");
		return _lm.getNgramAsWords(ngram);
	}

	@Override
	public double getSequenceLogProbability(List<W>[] ngram_sequence) throws Exception {
		if (_lm == null)
			throw new IllegalAccessError("No language model set.");
		return _lm.getSequenceLogProbability(ngram_sequence);
	}

	@Override
	public double getNgramLogProbability(int[] ngram) throws Exception {
		if (_lm == null)
			throw new IllegalAccessError("No language model set.");
		return _lm.getNgramLogProbability(ngram);
	}

	@Override
	public double getNgramLogProbability(List<W> ngram) throws Exception {
		if (_lm == null)
			throw new IllegalAccessError("No language model set.");
		return _lm.getNgramLogProbability(ngram);
	}

	@Override
	public Iterator<List<W>> getNgramIterator() throws Exception {
		if (_lm == null)
			throw new IllegalAccessError("No language model set.");
		return _lm.getNgramIterator();
	}

	@Override
	public Iterator<List<Integer>> getNgramIdIterator() throws Exception {
		if (_lm == null)
			throw new IllegalAccessError("No language model set.");
		return _lm.getNgramIdIterator();
	}
	

	@Override
	public boolean ngramContainsOOV(List<W> ngram) throws Exception {
		if (_lm == null)
			throw new IllegalAccessError("No language model set.");
		return _lm.ngramContainsOOV(ngram);
	}
	

	@Override
	public boolean ngramEndsWithOOV(List<W> ngram) throws Exception {
		if (_lm == null)
			throw new IllegalAccessError("No language model set.");
		return _lm.ngramEndsWithOOV(ngram);
	}
	
	@Override
	public boolean isUnkownWord(W word) throws Exception {
		if(_lm == null)
			throw new IllegalAccessError("No language model set.");
		return _lm.isUnkownWord(word);
	}
	
	@Override
	public boolean isUnkownWord(int wordId) throws Exception {
		if(_lm == null)
			throw new IllegalAccessError("No language model set.");
		return _lm.isUnkownWord(wordId);
	}
	
	@Override
	public boolean ngramContainsOOV(int[] ngram) throws Exception {
		if(_lm == null)
			throw new IllegalAccessError("No language model set.");
		return _lm.ngramContainsOOV(ngram);

	}

	@Override
	public boolean ngramEndsWithOOV(int[] ngram) throws Exception {
		if(_lm == null)
			throw new IllegalAccessError("No language model set.");
		return _lm.ngramEndsWithOOV(ngram);
	}

	@Override
	public boolean getSourceModelReady() {
		return _lm != null;
	}

	@Override
	public void setSourceModel(LanguageModel<W> model) {
		_lm = model;
	}

	@Override
	public Class<?> getSourceModelClass() {
		return _lm.getClass();
	}

	public void publish(Registry registry, String identifier, int port) throws RemoteException, AlreadyBoundException {
		LOG.info("Trying to publish Language model service '{}' on port {}.", identifier, port);
		@SuppressWarnings("unchecked")
		final LanguageModelServerMBean<String> stub = (LanguageModelServerMBean<String>) UnicastRemoteObject.exportObject(this, port);
		// RemoteServer.setLog(System.out);
		// Bind the remote object's stub in the registry
		registry.bind(identifier, stub);
		LOG.info("Language model service '{}' published on port {}.", identifier, port);
	}

	public static LanguageModel<String> connectToServer(String host, int rmiPort, String identifier) {
		try {
			LOG.info("Trying to connect to Language model service '{}' on {}:{}.", identifier, host, rmiPort);
			Registry registry = LocateRegistry.getRegistry(host, rmiPort);
			@SuppressWarnings("unchecked")
			LanguageModel<String> lm = (LanguageModel<String>) registry.lookup(identifier);
			return lm;
		} catch (RemoteException e) {
			LOG.error("Unable to connect to rmi registry on {}:{}. {}: {}.", host, rmiPort, e.getClass().getSimpleName(), e.getMessage());
		} catch (NotBoundException e) {
			LOG.error("Unable to connect to service {}. {}: {}.", identifier, e.getClass().getSimpleName(), e.getMessage());
		}
		return null;
	}
}
