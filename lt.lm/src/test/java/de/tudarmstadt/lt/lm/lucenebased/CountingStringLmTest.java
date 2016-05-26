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
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import de.tudarmstadt.lt.lm.app.GenerateNgramIndex;
import de.tudarmstadt.lt.lm.mapbased.CountingLM;
import de.tudarmstadt.lt.lm.service.BreakIteratorStringProvider;
import de.tudarmstadt.lt.lm.service.LMProviderUtils;
import de.tudarmstadt.lt.lm.service.LtSegProvider;
import de.tudarmstadt.lt.lm.util.Properties;
import de.tudarmstadt.lt.utilities.LogUtils;

/**
 *
 * @author Steffen Remus
 */
public class CountingStringLmTest {
	static {
		LogUtils.setLogLevel("de.tudarmstadt.lt.lm", "trace");
	}

	static File _src_dir = new File(ClassLoader.getSystemClassLoader().getResource("testlm").getPath());

	static int reset_boundary_property; 
	
	@BeforeClass
	public static void setupClass() throws IOException{
		// set handleboundaries property (currently still buggy for 1)
		reset_boundary_property = Properties.handleBoundaries();
		Properties.get().setProperty("lt.lm.handleBoundaries", "0");
		GenerateNgramIndex.generate_index(_src_dir, new LtSegProvider(), 1, 5, 1, true);
	}
	
	@AfterClass
	public static void cleanUp() {
		Properties.get().setProperty("lt.lm.handleBoundaries", String.valueOf(reset_boundary_property));
	}


	@Test
	public void testCreate() throws IOException, IllegalAccessException {

		CountingLM<String> lm_ = new CountingLM<String>(3);
		for(List<String> ngram : LMProviderUtils.getNgramSequence(Arrays.asList("<s> the quick brown fox </s>".split(" ")), lm_.getOrder()))
			lm_.addNgram(ngram);
		for(List<String> ngram : LMProviderUtils.getNgramSequence(Arrays.asList("<s> the quick brown cat </s>".split(" ")), lm_.getOrder()))
			lm_.addNgram(ngram);
		lm_.fixItNow();

		CountingStringLM lm = new CountingStringLM(3, new File(_src_dir, ".lmindex"));

		Iterator<List<String>> iter = lm.getNgramIterator();
		while (iter.hasNext()) {
			List<String> ngram = iter.next();
			double logprob = lm.getNgramLogProbability(ngram);
			double prob = Math.pow(10, logprob);

			System.out.format("ngram=%s log10prob=%g prob=%g %n",
					ngram.toString(),
					logprob,
					prob
					);

			Assert.assertTrue(logprob <= 0);
			Assert.assertTrue(logprob > Double.NEGATIVE_INFINITY);
			Assert.assertTrue(!Double.isInfinite(logprob));
			Assert.assertTrue(!Double.isNaN(logprob));

		}

		System.out.println("=====");

		Iterator<List<String>> iter_ = lm_.getNgramIterator();
		while (iter_.hasNext()) {
			List<String> ngram = iter_.next();
			double logprob = lm_.getNgramLogProbability(ngram);
			double prob = Math.pow(10, logprob);

			System.out.format("ngram=%s log10prob=%g prob=%g %n",
					ngram.toString(),
					logprob,
					prob
					);
			
			Assert.assertTrue(logprob <= 0);
			Assert.assertTrue(logprob > Double.NEGATIVE_INFINITY);
			Assert.assertTrue(!Double.isInfinite(logprob));
			Assert.assertTrue(!Double.isNaN(logprob));

		}
		
	}

	@Test
	public void testVocabularyIterator() throws IllegalAccessException{

		CountingStringLM lm = new CountingStringLM(3, new File(_src_dir, ".lmindex"));

		
		Iterator<String> iter = lm.getVocabularyIterator();
		while (iter.hasNext()) {
			String word = iter.next();
			System.out.format("%s %d%n", word, lm.getWordIndex(word));
		}

		System.out.println("=====");
	
	}

	@Test
	@Ignore
	public void testPredict() throws IOException, IllegalAccessException {

		CountingLM<String> lm_ = new CountingLM<String>(3);
		for(List<String> ngram : LMProviderUtils.getNgramSequence(Arrays.asList("<s> the quick brown fox </s>".split(" ")), lm_.getOrder()))
			lm_.addNgram(ngram);
		for(List<String> ngram : LMProviderUtils.getNgramSequence(Arrays.asList("<s> the quick brown cat </s>".split(" ")), lm_.getOrder()))
			lm_.addNgram(ngram);
		lm_.fixItNow();

		CountingStringLM lm = new CountingStringLM(3, new File(CountingStringLmTest.class.getClassLoader().getResource(".").getPath(), getClass().getSimpleName() + ".index"));

		System.out.println(lm.predictNextWord(Arrays.asList("the quick".split(" "))));
		System.out.println(lm.predictNextWord(Arrays.asList("quick brown".split(" "))));

		System.out.println("=====");

		System.out.println(lm_.predictNextWord(Arrays.asList("the quick".split(" "))));
		System.out.println(lm_.predictNextWord(Arrays.asList("quick brown".split(" "))));
	}






}
