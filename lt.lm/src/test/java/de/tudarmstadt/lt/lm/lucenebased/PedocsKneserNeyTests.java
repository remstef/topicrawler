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
package de.tudarmstadt.lt.lm.lucenebased;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tudarmstadt.lt.lm.LanguageModel;
import de.tudarmstadt.lt.lm.app.GenerateNgramIndex;
import de.tudarmstadt.lt.lm.service.PreTokenizedStringProvider;
import de.tudarmstadt.lt.utilities.LogUtils;

/**
 *
 * @author Steffen Remus
 **/
public class PedocsKneserNeyTests {

	static {
		LogUtils.setLogLevel("de.tudarmstadt.lt.lm", "trace");
	}

	static File _src_dir = new File("local_data/pedocs/pedocs-train");

	static int _max_ngram_order = 5;

	@BeforeClass
	public static void setupClass() throws IOException{
		if(!_src_dir.exists())
			return;
		GenerateNgramIndex.generate_index(_src_dir, new PreTokenizedStringProvider(), 1, _max_ngram_order, 1, false);
	}
	
	@Test
	public void testKneserNeyProbabilities() throws Exception{
		if(!_src_dir.exists())
			return;
		KneserNeyLM lm = new KneserNeyLM(_max_ngram_order, new File(_src_dir, ".lmindex"), 0.7);
		runTests(lm);
	}
	
	@Test
	public void testKneserNeyProbabilitiesAutoEstimatedDiscounts() throws Exception{
		if(!_src_dir.exists())
			return;
		KneserNeyLM lm = new KneserNeyLM(_max_ngram_order, new File(_src_dir, ".lmindex"), -1);
		runTests(lm);
	}

	@Test
	public void testRecursiveKneserNeyProbabilities() throws Exception{
		if(!_src_dir.exists())
			return;
		KneserNeyLMRecursive lm = new KneserNeyLMRecursive(_max_ngram_order, new File(_src_dir, ".lmindex"), 0.7);
		runTests(lm);
	}
	
	@Test
	public void testRecursiveKneserNeyProbabilitiesAutoEstimatedDiscounts() throws Exception{
		if(!_src_dir.exists())
			return;
		KneserNeyLMRecursive lm = new KneserNeyLMRecursive(_max_ngram_order, new File(_src_dir, ".lmindex"), -1);
		runTests(lm);
	}

	@Test
	public void testRecursiveModifiedKneserNeyProbabilities() throws Exception{
		if(!_src_dir.exists())
			return;
		ModifiedKneserNeyLMRecursive lm = new ModifiedKneserNeyLMRecursive(_max_ngram_order, new File(_src_dir, ".lmindex"), .7);
		runTests(lm);
	}

	@Test
	public void testRecursiveModifiedKneserNeyProbabilitiesAutoEstimatedDiscounts() throws Exception{
		if(!_src_dir.exists())
			return;
		ModifiedKneserNeyLMRecursive lm = new ModifiedKneserNeyLMRecursive(_max_ngram_order, new File(_src_dir, ".lmindex"), -1);
		runTests(lm);
	}
	
	@Test
	public void test_popt_probabilities() throws Exception{
		if(!_src_dir.exists())
			return;
		KneserNeyLMRecursive lm = new PoptKneserNeyLMRecursive(_max_ngram_order, new File(_src_dir, ".lmindex"), .3d);
		runTests(lm);
		lm = new PoptKneserNeyLMRecursive(_max_ngram_order, new File(_src_dir, ".lmindex"), -1);
		runTests(lm);
		
	}
	
//	@Test
//	public void testStandardKneserNeyProbabilities() throws Exception{
//		
//		KneserNeyLMRecursive lm = new KneserNeyLMRecursive(_max_ngram_order, new File(_src_dir, ".lmindex"), .7);
//		runTests(lm);
//	}
//
//	@Test
//	public void testStandradKneserNeyProbabilitiesAutoEstimatedDiscounts() throws Exception{
//		if(!_src_dir.exists())
//			return;
//		KneserNeyLMRecursive lm = new KneserNeyLMRecursive(_max_ngram_order, new File(_src_dir, ".lmindex"), -1);
//		runTests(lm);
//	}
//
//	@Test
//	public void testModifiedKneserNeyProbabilities() throws Exception{
//		if(!_src_dir.exists())
//			return;
//		ModifiedKneserNeyLMRecursive lm = new ModifiedKneserNeyLMRecursive(_max_ngram_order, new File(_src_dir, ".lmindex"), .7);
//		runTests(lm);
//	}
//
//	@Test
//	public void testModifiedKneserNeyProbabilitiesAutoEstimatedDiscounts() throws Exception{
//		if(!_src_dir.exists())
//			return;
//		ModifiedKneserNeyLMRecursive lm = new ModifiedKneserNeyLMRecursive(_max_ngram_order, new File(_src_dir, ".lmindex"), -1);
//		runTests(lm);
//	}

	void runTests(LanguageModel<String> lm) throws Exception{
		if(!_src_dir.exists())
			return;
		testProbabilities(lm);
		testSequenceProb(lm);
		testPerplexity(lm);
	}

	void testProbabilities(LanguageModel<String> lm) throws Exception {

		List<String> ngram;
		double logprob;
		double logprob_last;

		System.out.println("=====");

		ngram = Arrays.asList("Menü auf - / zuklappen".split(" "));
		logprob = lm.getNgramLogProbability(ngram);
		System.out.format("ngram=%s log10prob=%g prob=%g %n", ngram.toString(), logprob, Math.pow(10, logprob));
		logprob_last = logprob;

		ngram = Arrays.asList("- / zuklappen News c't".split(" "));
		logprob = lm.getNgramLogProbability(ngram);
		System.out.format("ngram=%s log10prob=%g prob=%g %n", ngram.toString(), logprob, Math.pow(10, logprob));
		Assert.assertTrue(logprob_last >= logprob);
		logprob_last = logprob;

	}

	void testSequenceProb(LanguageModel<String> lm) throws Exception{

	}

	void testPerplexity(LanguageModel<String> lm) throws Exception{
		
	}




}
