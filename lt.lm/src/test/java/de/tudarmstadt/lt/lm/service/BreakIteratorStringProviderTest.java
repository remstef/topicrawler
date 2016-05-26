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
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tudarmstadt.lt.lm.LanguageModel;
import de.tudarmstadt.lt.lm.LanguageModelHelper;
import de.tudarmstadt.lt.lm.PseudoSymbol;
import de.tudarmstadt.lt.lm.mapbased.CountingLM;
import de.tudarmstadt.lt.lm.mapbased.LaplaceSmoothedLM;
import de.tudarmstadt.lt.lm.perplexity.ProbDistPerplexity;
import de.tudarmstadt.lt.utilities.LogUtils;
import de.tudarmstadt.lt.utilities.properties.Properties;

/**
 *
 * @author Steffen Remus
 **/
public class BreakIteratorStringProviderTest {
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


	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// setup rmi registry
		_registry = LocateRegistry.createRegistry(_rmiport);
		// setup test languagemodelserver

		final AbstractStringProvider stub = new BreakIteratorStringProvider();
		stub.publish(_registry, _lm_id, _app_port);

		// load lm
		LanguageModel<String> lm = new LaplaceSmoothedLM<String>((CountingLM<String>)LanguageModelHelper.createCountingLmTxtFilesInDirectory(new BreakIteratorStringProvider(), _src_dir, _lm_order));
		stub.setLanguageModel(lm);

		System.out.println(ProbDistPerplexity.calculatePerplexity(lm, stub, Arrays.asList("<s> the lazy brown dog </s>".split(" "))));
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		_registry.unbind(_lm_id);
		UnicastRemoteObject.unexportObject(_registry, true);
	}

	@Test
	public void test() throws Exception {
		StringProviderMXBean strprovider = AbstractStringProvider.connectToServer(_host, _rmiport, _lm_id);
		System.out.println(strprovider.getPerplexity("Schöne neue Wörld.", false));
		System.out.println(Arrays.toString(strprovider.getNgrams("Schöne neue Wörld.")));
		System.out.println(Arrays.toString(strprovider.getNgrams("M")));
	}

	@Test
	public void testFiltering() throws Exception {
		StringProviderMXBean strprovider = AbstractStringProvider.connectToServer(_host, _rmiport, _lm_id);
		List<String>[] ngrams = strprovider.getNgrams("The qu1ck br0wn fox jumps over the lazy dog.");
		System.out.println(Arrays.asList(ngrams));
	}

	@Test
	public void testSentenceTagInsertion() throws Exception {
		String seq = "The qu1ck br0wn fox jumps over the lazy dog";
		StringProviderMXBean provider = AbstractStringProvider.connectToServer(_host, _rmiport, _lm_id);
		String orig = Properties.get().getProperty("lt.lm.insertSentenceTags");

		Properties.get().setProperty("lt.lm.insertSentenceTags", "0");
		List<String>[] ngrams = provider.getNgrams(seq);
		System.out.println(Arrays.asList(ngrams));
		Assert.assertEquals("The", ngrams[0].get(0));
		Assert.assertEquals("dog", ngrams[ngrams.length-1].get(provider.getLmOrder()-1));

		Properties.get().setProperty("lt.lm.insertSentenceTags", "1");
		ngrams = provider.getNgrams(seq);
		System.out.println(Arrays.asList(ngrams));
		Assert.assertEquals(PseudoSymbol.SEQUENCE_START.asString(), ngrams[0].get(0));
		Assert.assertEquals("dog", ngrams[ngrams.length-1].get(provider.getLmOrder()-1));

		Properties.get().setProperty("lt.lm.insertSentenceTags", "2");
		ngrams = provider.getNgrams(seq);
		System.out.println(Arrays.asList(ngrams));
		Assert.assertEquals("The", ngrams[0].get(0));
		Assert.assertEquals(PseudoSymbol.SEQUENCE_END.asString(), ngrams[ngrams.length-1].get(provider.getLmOrder()-1));

		Properties.get().setProperty("lt.lm.insertSentenceTags", "3");
		ngrams = provider.getNgrams(seq);
		System.out.println(Arrays.asList(ngrams));
		Assert.assertEquals(PseudoSymbol.SEQUENCE_START.asString(), ngrams[0].get(0));
		Assert.assertEquals(PseudoSymbol.SEQUENCE_END.asString(), ngrams[ngrams.length-1].get(provider.getLmOrder()-1));

		Properties.get().setProperty("lt.lm.insertSentenceTags", orig);
	}

	@Test
	public void testSentenceBoundaryHandling() throws Exception {
		String seq = "The qu1ck br0wn fox jumps over the lazy dog";
		StringProviderMXBean provider = AbstractStringProvider.connectToServer(_host, _rmiport, _lm_id);
		String orig1 = Properties.get().getProperty("lt.lm.insertSentenceTags");
		String orig = Properties.get().getProperty("lt.lm.handleBoundaries");

		Properties.get().setProperty("lt.lm.insertSentenceTags", "0");

		Properties.get().setProperty("lt.lm.handleBoundaries", "-1");
		List<String>[] ngrams = provider.getNgrams("The");
		System.out.println(Arrays.asList(ngrams));
		Assert.assertTrue(ngrams.length == 0);

		Properties.get().setProperty("lt.lm.handleBoundaries", "0");
		ngrams = provider.getNgrams("The");
		System.out.println(Arrays.asList(ngrams));
		Assert.assertTrue(ngrams.length == 1);
		Assert.assertEquals("The", ngrams[0].get(0));

		Properties.get().setProperty("lt.lm.handleBoundaries", "1");
		ngrams = provider.getNgrams(seq);
		System.out.println(Arrays.asList(ngrams));
		Assert.assertEquals("The", ngrams[0].get(0));
		Assert.assertEquals("The", ngrams[0].get(1));

		Properties.get().setProperty("lt.lm.handleBoundaries", "2");
		ngrams = provider.getNgrams(seq);
		System.out.println(Arrays.asList(ngrams));
		Assert.assertEquals("The", ngrams[0].get(0));
		Assert.assertEquals("qu1ck", ngrams[0].get(1));
		Assert.assertTrue(ngrams[0].size() == 2);
		Assert.assertTrue(ngrams[1].size() == 3);
		
		ngrams = provider.getNgrams("^ hello world");
		System.out.println(Arrays.asList(ngrams));
		Assert.assertEquals("^", ngrams[0].get(0));
		Assert.assertEquals("hello", ngrams[0].get(1));
		Assert.assertTrue(ngrams[0].size() == 2);
		Assert.assertTrue(ngrams[1].size() == 3);

		Properties.get().setProperty("lt.lm.handleBoundaries", orig);
		Properties.get().setProperty("lt.lm.insertSentenceTags", orig1);
	}


	//	Hello world.
	//	Hello
	//
	//	world
	//	.
	//
	//	This is a very simple test.
	//	This
	//
	//	is
	//
	//	a
	//
	//	very
	//
	//	simple
	//
	//	test
	//	.


}
