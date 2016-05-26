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

import java.io.File;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tudarmstadt.lt.lm.LanguageModel;
import de.tudarmstadt.lt.lm.mapbased.CountingLM;
import de.tudarmstadt.lt.lm.perplexity.ModelPerplexity;
import de.tudarmstadt.lt.lm.util.Properties;
import de.tudarmstadt.lt.utilities.LogUtils;

/**
 *
 * @author Steffen Remus
 **/
public class LanguageModelServerTest {
	static {
		LogUtils.setLogLevel("de.tudarmstadt.lt.lm", "trace");
	}

	static Registry _registry;
	static String _lm_id = "testlm";
	static File _src_dir = new File(ClassLoader.getSystemClassLoader().getResource("testlm").getPath());
	static String _host = "localhost";
	static int _rmiport = 1099;
	static int _app_port = 0;
	static int _lm_order = 3;
	static CountingLM<String> _lm_served; // only needed for ngrams because the used iterator is not serializable and for other testing purposes

	static int reset_boundary_property;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		reset_boundary_property = Properties.handleBoundaries();
		Properties.get().setProperty("lt.lm.handleBoundaries", "0");
		// setup rmi registry
		_registry = LocateRegistry.createRegistry(_rmiport);
		// setup test languagemodelserver
		final LanguageModelServer<String> stub = new LanguageModelServer<String>();
		stub.publish(_registry, _lm_id, _app_port);
		// load lm
		_lm_served = new CountingLM<String>(3);
		for(List<String> ngram : LMProviderUtils.getNgramSequence(Arrays.asList("The quick brown fox".split(" ")), _lm_served.getOrder()))
			_lm_served.addNgram(ngram);
		for(List<String> ngram : LMProviderUtils.getNgramSequence(Arrays.asList("The quick brown cat".split(" ")), _lm_served.getOrder()))
			_lm_served.addNgram(ngram);
		
		// precheck
		precheck(_lm_served, _lm_served.getNgramIdIterator());

		stub.setSourceModel(_lm_served);

		// postcheck
		precheck(stub, stub.getNgramIdIterator());

	}

	@Test
	public void precheck() throws Exception {
		precheck(_lm_served, _lm_served.getNgramIdIterator());

		LanguageModel<String> lm = LanguageModelServer.connectToServer(_host, _rmiport, _lm_id);

		precheck(lm, _lm_served.getNgramIdIterator());
	}

	static void precheck(LanguageModel<String> lm, Iterator<List<Integer>> iter) throws Exception {
		AbstractStringProvider lmp = new PreTokenizedStringProvider();
		lmp.setLanguageModel(lm);
		// precheck
		ModelPerplexity<String> perp = new ModelPerplexity<String>(lmp);
		while (iter.hasNext()) {
			List<Integer> ngram_as_ids = iter.next();
			List<String> ngram = lm.getNgramAsWords(ngram_as_ids);
			double logprob = lm.getNgramLogProbability(ngram);
			double prob = Math.pow(10, logprob);
			System.out.format("ngram=%s ids=%s log10prob=%g prob=%g %n",
					ngram.toString(),
					ngram_as_ids.toString(),
					logprob,
					prob
					);
			Assert.assertTrue(logprob <= 0);
			Assert.assertTrue(logprob > Double.NEGATIVE_INFINITY);
			Assert.assertTrue(!Double.isInfinite(logprob));
			Assert.assertTrue(!Double.isNaN(logprob));
			perp.addLog10Prob(ngram);
		}
		System.out.format("ModelPerplexity: %g %n", perp.get());
	}

	static void check(LanguageModel<String> lm) throws Exception {
		// check
		List<String> test1 = Arrays.asList("The quick brown fox".split(" "));
		List<String> test2 = Arrays.asList("The quick brown cat".split(" "));

		AbstractStringProvider lmp =  new PreTokenizedStringProvider();
		lmp.setLanguageModel(lm);
		double perp = ModelPerplexity.calculatePerplexity(lm, lmp, test1, false);
		System.out.format("%s ModelPerplexity: %g %n", test1.toString(), perp);

		// 2 ngrams: [[The, quick, brown], [quick, brown, fox]]
		// 2^(1/2) == 1.1892 == (1/2)^(-1/4)
		Assert.assertEquals(1.41421, perp, 1e-4);
		System.out.println("---");

		perp = ModelPerplexity.calculatePerplexity(lm, lmp, test2, false);
		System.out.format("%s ModelPerplexity: %g %n", test2.toString(), perp);
		Assert.assertEquals(1.41421, perp, 1e-4);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		_registry.unbind(_lm_id);
		UnicastRemoteObject.unexportObject(_registry, true);
		Properties.get().setProperty("lt.lm.handleBoundaries", String.valueOf(reset_boundary_property));
	}

	@Test
	public void checkLocal() throws Exception {

		check(_lm_served);

	}

	@Test
	public void checkRemote() throws Exception {
		LanguageModel<String> lm = LanguageModelServer.connectToServer(_host, _rmiport, _lm_id);
		check(lm);
	}

}
