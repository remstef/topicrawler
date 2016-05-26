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

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.LanguageModel;
import de.tudarmstadt.lt.lm.PseudoSymbol;
import de.tudarmstadt.lt.lm.perplexity.ModelPerplexity;
import de.tudarmstadt.lt.lm.util.Properties;

/**
 *
 * @author Steffen Remus
 **/
public abstract class AbstractStringProvider implements StringProviderMXBean {

	private static Logger LOG = LoggerFactory.getLogger(AbstractStringProvider.class);

	@SuppressWarnings("unchecked")
	protected final static List<String>[] EMPTY_NGRAM_LIST = new List[0];

	private LanguageModel<String> _language_model;
	private ModelPerplexity<String> _perplexity;
	protected String _default_language_code = Properties.defaultLanguageCode();

	public LanguageModel<String> getLanguageModel(){
		return _language_model;
	}

	public void setLanguageModel(LanguageModel<String> languageModel)  {
		_language_model = languageModel;
		_perplexity = new ModelPerplexity<String>(this);
	}

	@Override
	public boolean getModelReady() throws RemoteException {
		return _language_model != null;
	}

	@Override
	public int getLmOrder() throws Exception{
		return getLanguageModel().getOrder();
	}

	@Override
	public double getPerplexity() throws Exception {
		return _perplexity.get();
	}

	@Override
	public void resetPerplexity() throws Exception {
		_perplexity.reset();
	}

	@Override
	public double getPerplexity(String text, boolean skip_oov) throws Exception {
		return getPerplexity(text, _default_language_code, skip_oov);
	}

	@Override
	public double getPerplexity(String text, String language_code, boolean skip_oov) throws Exception {
		List<String>[] ngram_sequence = getNgrams(text, language_code);
		return ModelPerplexity.calculatePerplexity(_language_model, ngram_sequence, skip_oov);
	}

	@Override
	public void addToPerplexity(String text) throws Exception {
		addToPerplexity(text, _default_language_code);
	}

	@Override
	public void addToPerplexity(String text, String language_code) throws Exception {
		List<String>[] ngram_sequence = getNgrams(text, language_code);
		for (List<String> ngram : ngram_sequence)
			addToPerplexity(ngram);
	}

	@Override
	public double addToPerplexity(List<String> ngram) throws Exception{
		return _perplexity.addLog10Prob(ngram);
	}

	@Override
	public List<String>[] getNgrams(String text) throws Exception {
		return getNgrams(text, _default_language_code);
	}

	@Override
	public double getNgramLog10Probability(List<String> ngram) throws Exception{
		if(!getModelReady())
			throw new IllegalAccessError("Language model is not ready.");
		return _language_model.getNgramLogProbability(ngram);
	}

	@Override
	public double getNgramSequenceLog10Probability(List<String>[] ngram_sequence) throws Exception{
		if(!getModelReady())
			throw new IllegalAccessError("Language model is not ready.");
		return _language_model.getSequenceLogProbability(ngram_sequence);
	}

	@Override
	public double getSequenceLog10Probability(String sequence) throws Exception{
		if(!getModelReady())
			throw new IllegalAccessError("Language model is not ready.");
		return getNgramSequenceLog10Probability(getNgrams(sequence));
	}

	@Override
	public String predictNextWord(String sequence) throws Exception{
		return predictNextWord(tokenizeSentence(sequence));
	}

	@Override
	public String predictNextWord(List<String> ngram) throws Exception{
		if(!getModelReady())
			throw new IllegalAccessError("Language model is not ready.");
		return _language_model.predictNextWord(ngram);
	}

	@Override
	public int[] getNgramAsIds(List<String> ngram) throws Exception{
		if(!getModelReady())
			throw new IllegalAccessError("Language model is not ready.");
		return _language_model.getNgramAsIds(ngram);
	}

	@Override
	public List<String> getNgramAsWords(int[] ngram_ids) throws Exception{
		if(!getModelReady())
			throw new IllegalAccessError("Language model is not ready.");
		return _language_model.getNgramAsWords(ngram_ids);
	}

	@Override
	public List<String> splitSentences(String text) throws Exception {
		return splitSentences(text, Properties.defaultLanguageCode());
	}

	@Override
	public List<String> tokenizeSentence(String sentence) throws Exception{
		return tokenizeSentence(sentence, Properties.defaultLanguageCode());
	}
	
	/* (non-Javadoc)
	 * @see de.tudarmstadt.lt.lm.service.StringProviderMXBean#ngramContainsOOV(java.util.List)
	 */
	@Override
	public boolean ngramContainsOOV(List<String> ngram) throws Exception {
		return getLanguageModel().ngramContainsOOV(ngram);
	}
	
	/* (non-Javadoc)
	 * @see de.tudarmstadt.lt.lm.service.StringProviderMXBean#ngramEndsWithOOV(java.util.List)
	 */
	@Override
	public boolean ngramEndsWithOOV(List<String> ngram) throws Exception {
		return getLanguageModel().ngramEndsWithOOV(ngram);
	}

	@Override
	public List<String> tokenizeSentence(String sentence, String language_code) throws Exception{
		List<String> tokens = tokenizeSentence_intern(sentence, language_code);
		if(Properties.insertSentenceTags() <= 0 || tokens.isEmpty())
			return tokens;

		// TODO: this is surely not the best performing solution, improve
		List<String> sequence_with_sentence_tags = new ArrayList<String>(tokens.size() + 2); // worst case
		if(Properties.insertSentenceTags() % 2 == 1)
			sequence_with_sentence_tags.add(PseudoSymbol.SEQUENCE_START.asString());
		sequence_with_sentence_tags.addAll(tokens);
		if(Properties.insertSentenceTags() > 1)
			sequence_with_sentence_tags.add(PseudoSymbol.SEQUENCE_END.asString());
		return sequence_with_sentence_tags;
	}	

	@Override
	public List<String>[] getNgramSequenceFromSentence(List<String> sentence) throws Exception{
		return getNgramSequence(sentence);
	}

	@Override
	public List<String>[] getNgramSequence(List<String> sequence) throws Exception {
		return getNgramSequence(sequence, getLmOrder());
	}
	
	@Override
	public List<String>[] getNgramSequence(List<String> sequence, int order) throws Exception {
		return LMProviderUtils.getNgramSequence(sequence, order);
	}

	public Registry publish(Registry registry, String identifier, int port) throws RemoteException, AccessException, AlreadyBoundException {
		final StringProviderMXBean stub = (StringProviderMXBean) UnicastRemoteObject.exportObject(this, port);
		// RemoteServer.setLog(System.out);
		// Bind the remote object's stub in the registry
		registry.bind(identifier, stub);
		LOG.info("Stringprovider service '{}' published on port {}.", identifier, port);
		return registry;
	}

	public static StringProviderMXBean connectToServer(String host, int rmiPort, String identifier) {
		try {
			Registry registry = LocateRegistry.getRegistry(host, rmiPort);
			StringProviderMXBean provider = (StringProviderMXBean) registry.lookup(identifier);
			return provider;
		} catch (RemoteException e) {
			LOG.error("Unable to connect to rmi registry on '{}:{}'. {}: {}.", host, rmiPort, e.getClass().getSimpleName(), e.getMessage());
		} catch (NotBoundException e) {
			LOG.error("Unable to connect to service '{}'. {}: {}.", identifier, e.getClass().getSimpleName(), e.getMessage());
		}
		return null;
	}

}
