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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tudarmstadt.lt.lm.LanguageModel;
import de.tudarmstadt.lt.lm.app.GenerateNgramIndex;
import de.tudarmstadt.lt.lm.perplexity.ModelPerplexity;
import de.tudarmstadt.lt.lm.service.AbstractStringProvider;
import de.tudarmstadt.lt.lm.service.LMProviderUtils;
import de.tudarmstadt.lt.lm.service.LtSegProvider;
import de.tudarmstadt.lt.lm.service.PreTokenizedStringProvider;
import de.tudarmstadt.lt.lm.util.Properties;
import de.tudarmstadt.lt.utilities.LogUtils;

/**
 *
 * @author Steffen Remus
 **/
public class KneserNeyTests {

	static {
		LogUtils.setLogLevel("de.tudarmstadt.lt.lm", "info");
	}
	
	static File _src_dir = new File(ClassLoader.getSystemClassLoader().getResource("cat").getPath()); // testlm cat texts
	
	static int _max_ngram_order = 5;
	
	static int reset_boundary_property;

	@BeforeClass
	public static void setupClass() throws IOException{
		reset_boundary_property = Properties.handleBoundaries();
		Properties.get().setProperty("lt.lm.handleBoundaries", "0");
		boolean recreate = true;
		GenerateNgramIndex.generate_index(_src_dir, new LtSegProvider(), 1, _max_ngram_order, 1, recreate/*re-create index*/);
	}
	
	@AfterClass
	public static void cleanUp() {
		Properties.get().setProperty("lt.lm.handleBoundaries", String.valueOf(reset_boundary_property));
	}
	
	@Test
	public void testKneserNeyProbabilities() throws Exception{
		KneserNeyLM lm = new KneserNeyLM(_max_ngram_order, new File(_src_dir, ".lmindex"), 0.7);
		runTests(lm);
	}
	
	@Test
	public void testStupidBackoffProbabilities() throws Exception{
		StupidBackoffLM lm = new StupidBackoffLM(_max_ngram_order, new File(_src_dir, ".lmindex"), 0.4);
		runTests(lm);
	}
	
	@Test
	public void testKneserNeyProbabilitiesAutoEstimatedDiscounts() throws Exception{
		KneserNeyLM lm = new KneserNeyLM(_max_ngram_order, new File(_src_dir, ".lmindex"), -1);
		runTests(lm);
	}

	@Test
	public void testRecursiveKneserNeyProbabilities() throws Exception{
		KneserNeyLMRecursive lm = new KneserNeyLMRecursive(_max_ngram_order, new File(_src_dir, ".lmindex"), 0.7);
		runTests(lm);
	}
	
	@Test
	public void testRecursiveKneserNeyProbabilitiesAutoEstimatedDiscounts() throws Exception{
		KneserNeyLMRecursive lm = new KneserNeyLMRecursive(_max_ngram_order, new File(_src_dir, ".lmindex"), -1);
		runTests(lm);
	}

	@Test
	public void testRecursiveModifiedKneserNeyProbabilities() throws Exception{
		ModifiedKneserNeyLMRecursive lm = new ModifiedKneserNeyLMRecursive(_max_ngram_order, new File(_src_dir, ".lmindex"), .7);
		runTests(lm);
	}

	@Test
	public void testRecursiveModifiedKneserNeyProbabilitiesAutoEstimatedDiscounts() throws Exception{
		ModifiedKneserNeyLMRecursive lm = new ModifiedKneserNeyLMRecursive(_max_ngram_order, new File(_src_dir, ".lmindex"), -1);
		runTests(lm);
	}
	
	@Test
	public void test_popt_probabilities() throws Exception{
		KneserNeyLMRecursive lm = new PoptKneserNeyLMRecursive(_max_ngram_order, new File(_src_dir, ".lmindex"), .3d);
		runTests(lm);
		lm = new PoptKneserNeyLMRecursive(_max_ngram_order, new File(_src_dir, ".lmindex"), -1);
		runTests(lm);
		
	}
	
	
	void runTests(LanguageModel<String> lm) throws Exception{
		testProbabilities(lm);
		testPredict(lm);
		testSequenceProb(lm);
		testPerplexity(lm);
	}

	void testProbabilities(LanguageModel<String> lm) throws Exception {

		List<String> ngram;
		double logprob;
		double logprob_last;

		Iterator<List<String>> iter = lm.getNgramIterator();
		while (iter.hasNext()) {
			ngram = iter.next();
			logprob = lm.getNgramLogProbability(ngram);

//			System.out.format("ngram=%s log10prob=%g prob=%g %n",
//					ngram.toString(),
//					logprob,
//					Math.pow(10, logprob)
//					);

			Assert.assertTrue(logprob <= Math.log(1.001));
			Assert.assertTrue(logprob > Double.NEGATIVE_INFINITY);
			Assert.assertTrue(!Double.isInfinite(logprob));
			Assert.assertTrue(!Double.isNaN(logprob));
		}

		if(_src_dir.getAbsolutePath().endsWith("testlm")){
			if(_max_ngram_order > 1){
				System.out.println("=====");
				ngram = Arrays.asList("black bear".split(" "));
				logprob = lm.getNgramLogProbability(ngram);
				System.out.format("ngram=%s log10prob=%g prob=%g %n", ngram.toString(), logprob, Math.pow(10, logprob));
				logprob_last = logprob;

				ngram = Arrays.asList("brown bear".split(" "));
				logprob = lm.getNgramLogProbability(ngram);
				System.out.format("ngram=%s log10prob=%g prob=%g %n", ngram.toString(), logprob, Math.pow(10, logprob));
				Assert.assertTrue(logprob_last <= logprob);
				logprob_last = logprob;

				ngram = Arrays.asList("black cat".split(" "));
				logprob = lm.getNgramLogProbability(ngram);
				System.out.format("ngram=%s log10prob=%g prob=%g %n", ngram.toString(), logprob, Math.pow(10, logprob));
				Assert.assertTrue(logprob_last < logprob);
				logprob_last = logprob;

				ngram = Arrays.asList("brown cat".split(" "));
				logprob = lm.getNgramLogProbability(ngram);
				System.out.format("ngram=%s log10prob=%g prob=%g %n", ngram.toString(), logprob, Math.pow(10, logprob));
				Assert.assertTrue(logprob_last <= logprob);
				System.out.println("=====");
			}

			if(_max_ngram_order > 2){
				System.out.println("=====");

				ngram = Arrays.asList("quick brown fox".split(" "));
				logprob = lm.getNgramLogProbability(ngram);
				System.out.format("ngram=%s log10prob=%g prob=%g %n", ngram.toString(), logprob, Math.pow(10, logprob));
				logprob_last = logprob;

				ngram = Arrays.asList("slow brown fox".split(" "));
				logprob = lm.getNgramLogProbability(ngram);
				System.out.format("ngram=%s log10prob=%g prob=%g %n", ngram.toString(), logprob, Math.pow(10, logprob));
				Assert.assertTrue(logprob_last >= logprob);
				logprob_last = logprob;

				ngram = Arrays.asList("quick black fox".split(" "));
				logprob = lm.getNgramLogProbability(ngram);
				System.out.format("ngram=%s log10prob=%g prob=%g %n", ngram.toString(), logprob, Math.pow(10, logprob));
				Assert.assertTrue(logprob_last >= logprob);
				logprob_last = logprob;

				ngram = Arrays.asList("slow black fox".split(" "));
				logprob = lm.getNgramLogProbability(ngram);
				System.out.format("ngram=%s log10prob=%g prob=%g %n", ngram.toString(), logprob, Math.pow(10, logprob));
				Assert.assertTrue(logprob_last >= logprob);
				logprob_last = logprob;

				ngram = Arrays.asList("quick brown dog".split(" "));
				logprob = lm.getNgramLogProbability(ngram);
				System.out.format("ngram=%s log10prob=%g prob=%g %n", ngram.toString(), logprob, Math.pow(10, logprob));
				Assert.assertTrue(logprob_last >= logprob);
				logprob_last = logprob;

				ngram = Arrays.asList("slow brown dog".split(" "));
				logprob = lm.getNgramLogProbability(ngram);
				System.out.format("ngram=%s log10prob=%g prob=%g %n", ngram.toString(), logprob, Math.pow(10, logprob));
				Assert.assertTrue(logprob_last >= logprob);
				logprob_last = logprob;

				ngram = Arrays.asList("quick black dog".split(" "));
				logprob = lm.getNgramLogProbability(ngram);
				System.out.format("ngram=%s log10prob=%g prob=%g %n", ngram.toString(), logprob, Math.pow(10, logprob));
				Assert.assertTrue(logprob_last >= logprob);
				logprob_last = logprob;

				ngram = Arrays.asList("slow black dog".split(" "));
				logprob = lm.getNgramLogProbability(ngram);
				System.out.format("ngram=%s log10prob=%g prob=%g %n", ngram.toString(), logprob, Math.pow(10, logprob));
				Assert.assertTrue(logprob_last >= logprob);
				System.out.println("=====");
			}
		}
		
//		ngram = Arrays.asList("quick black cat".split(" "));
//		logprob = lm.getNgramLogProbability(ngram);
//		System.out.format("ngram=%s log10prob=%g prob=%g %n", ngram.toString(), logprob, Math.pow(10, logprob));
//		
//		ngram = Arrays.asList("word with cognates jnkjg many".split(" "));
//		logprob = lm.getNgramLogProbability(ngram);
//		System.out.format("ngram=%s log10prob=%g prob=%g %n", ngram.toString(), logprob, Math.pow(10, logprob));
		
	}


	@SuppressWarnings("unchecked")
	void testPredict(LanguageModel<String> lm) throws Exception {
		LogUtils.setLogLevel("de.tudarmstadt.lt.lm", "debug");
		StringBuilder b = new StringBuilder();
		for(int i = 0; i < 1; i++){
			List<String> q = new LinkedList<String>();
			q.add("<s>");
			if(_src_dir.getName().endsWith("testlm")){
				q.add("the");
				b.append("<s> the");
			}else{
				q.add("The");
				b.append("<s> The");	
			}

			String word = null;

			int c = 0;
			while(!"</s>".equals(word) && c++ < 20){
				System.out.println(q);
				word = lm.predictNextWord(q);
				if(q.size() >= _max_ngram_order)
					((Queue<String>)q).poll();
				q.add(word);
				b.append(" ").append(word);
			}
			System.out.println(q);
			System.out.println(b);
			b.setLength(0); // clear
			System.out.println("===");

		}
		LogUtils.setLogLevel("de.tudarmstadt.lt.lm", "info");
	}
	
	void testSequenceProb(LanguageModel<String> lm) throws Exception{
		double v = lm.getSequenceLogProbability(LMProviderUtils.getNgramSequence(Arrays.asList("<s> the quick brown fox </s>".split(" ")), lm.getOrder()));
		System.out.println(v);
		double w = lm.getSequenceLogProbability(LMProviderUtils.getNgramSequence(Arrays.asList("<s> the quick brown bear </s>".split(" ")), lm.getOrder()));
		System.out.println(w);
		Assert.assertTrue(v > w);
	}
	
	void testPerplexity(LanguageModel<String> lm) throws Exception{
		AbstractStringProvider lmp =  new PreTokenizedStringProvider();
		lmp.setLanguageModel(lm);
		double v = ModelPerplexity.calculatePerplexity(lm,lmp, Arrays.asList("<s> the quick brown fox </s>".split(" ")), false);
		System.out.println(v);
		double w = ModelPerplexity.calculatePerplexity(lm, lmp, Arrays.asList("<s> the quick brown bear </s>".split(" ")), false);
		System.out.println(w);
		Assert.assertTrue(v < w);
		
	}




}
