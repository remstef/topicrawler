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
package de.tudarmstadt.lt.lm.mapbased;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.tudarmstadt.lt.lm.LanguageModelHelper;
import de.tudarmstadt.lt.lm.service.BreakIteratorStringProvider;
import de.tudarmstadt.lt.lm.service.LMProviderUtils;
import de.tudarmstadt.lt.utilities.LogUtils;

/**
 * 
 * @author Steffen Remus
 */
public class CountingLmTest {
	static {
		LogUtils.setLogLevel("de.tudarmstadt.lt.lm", "trace");
	}

	static File _src_dir = new File(ClassLoader.getSystemClassLoader().getResource("testlm").getPath());

	@Test
	public void testCreate() throws IOException, IllegalAccessException {

		CountingLM<String> lm_reference = new CountingLM<String>(3);
		for(List<String> ngram : LMProviderUtils.getNgramSequence(Arrays.asList("<s> <s> the quick brown fox </s>".split(" ")), lm_reference.getOrder()))
			lm_reference.addNgram(ngram);
		for(List<String> ngram : LMProviderUtils.getNgramSequence(Arrays.asList("<s> <s> the quick brown cat </s>".split(" ")), lm_reference.getOrder()))
			lm_reference.addNgram(ngram);
		
		CountingLM<String> lm = (CountingLM<String>)LanguageModelHelper.createCountingLmTxtFilesInDirectory(new BreakIteratorStringProvider(), _src_dir, 3);
		Iterator<List<Integer>> iter = lm.getNgramIdIterator();
		while (iter.hasNext()) {
			List<Integer> ngram_as_ids = iter.next();
			List<String> ngram = lm.getNgramAsWords(ngram_as_ids);
			double logprob = lm.getNgramLogProbabilityFromIds(ngram_as_ids);
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

		}
	}





}
