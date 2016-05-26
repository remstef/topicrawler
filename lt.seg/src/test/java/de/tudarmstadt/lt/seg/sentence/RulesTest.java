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
package de.tudarmstadt.lt.seg.sentence;

import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import de.tudarmstadt.lt.seg.sentence.rules.BoundaryList;
import de.tudarmstadt.lt.seg.sentence.rules.BoundaryProcessor;
import de.tudarmstadt.lt.seg.sentence.rules.PostBoundaryListProcessor;
import de.tudarmstadt.lt.seg.sentence.rules.PostBoundaryRulesProcessor;
import de.tudarmstadt.lt.seg.sentence.rules.PreBoundaryListProcessor;
import de.tudarmstadt.lt.seg.sentence.rules.PreBoundaryRulesProcessor;
import de.tudarmstadt.lt.seg.sentence.rules.RuleSet;
import de.tudarmstadt.lt.seg.token.TokenizerTest;

/**
 * @author Steffen Remus
 *
 */
public class RulesTest {
	
	
	@Test
	public void ruleSplitterTest(){
		ISentenceSplitter s = new RuleSplitter().init(SentenceSplitterTest.TEST_TEXT);
		s.forEach(System.out::println);
		System.out.println("+++");
		s.init(TokenizerTest.TEST_TEXT);
		s.forEach(System.out::println);
		s.init("Hallo, dies ist ein Testsatz\\n Dies hier auch.");
		
		s.forEach(System.out::println);
	}
	
	@Test
	public void testBoundaryList() throws Exception{
//		Arrays.stream(SegmentationUtils.JAVA_EMPTY_SPACE_ESCAPE_SEQUENCES).forEach(x -> System.out.format("\\u%04x%n", x));
		URL u = Thread.currentThread().getContextClassLoader().getResource("rulesets/sentence/default/boundaries.txt");
		System.out.println(u); 
		Assert.assertTrue(u != null); // test existence

		BoundaryProcessor b = BoundaryList.DEFAULT;
		Assert.assertTrue(
				!b.isCompleteSentence("On the "));
		Assert.assertTrue(
				b.isCompleteSentence("On the 1. dot it's: \"Not a sentence.\", but on the second it is. "));
		Assert.assertTrue(
				b.isCompleteSentence("On the 1. dot it's: \"Not a sentence.\".\n"));
		Assert.assertTrue(
				b.isCompleteSentence("On the 1. dot it's: \"Not a sentence.\", but on the second it is.\n\n"));
		Assert.assertTrue(
				b.isCompleteSentence("Abb. "));
		Assert.assertTrue(
				b.isCompleteSentence("A. "));
		Assert.assertTrue(
				b.isCompleteSentence("\t"));
		
	}
	
	
	@Test
	public void testPreBoundaryExceptions() throws Exception{
		URL u = Runtime.class.getResource("/rulesets/sentence/default/preBoundaryExceptions.txt");
		System.out.println(u); 
		Assert.assertTrue(u != null); // test existence

		BoundaryProcessor b = PreBoundaryListProcessor.DEFAULT;
		Assert.assertTrue(
				b.isCompleteSentence("On the "));
		
		Assert.assertTrue(
				!b.isCompleteSentence("Abb. "));
		
		Assert.assertTrue(
				b.isCompleteSentence("\t"));
	}
	
	@Test
	public void testPostBoundaryExceptions() throws Exception{
		URL u = Runtime.class.getResource("/rulesets/sentence/default/postBoundaryExceptions.txt");
		System.out.println(u); 
		Assert.assertTrue(u != null); // test existence

		BoundaryProcessor b = PostBoundaryListProcessor.DEFAULT;
		Assert.assertTrue(
				b.isCompleteSentence("On the "));
		
		Assert.assertTrue(
				!b.isCompleteSentence("Januar"));
		
		Assert.assertTrue(
				b.isCompleteSentence("\n"));
	}
	
	@Test
	public void testPreBoundaryRules() throws Exception{
		URL u = Runtime.class.getResource("/rulesets/sentence/default/preBoundaryRules.txt");
		System.out.println(u); 
		Assert.assertTrue(u != null); // test existence

		BoundaryProcessor b = PreBoundaryRulesProcessor.DEFAULT;
		Assert.assertTrue(
				b.isCompleteSentence("On the "));
		
		Assert.assertTrue(
				!b.isCompleteSentence("A"));
		
		Assert.assertTrue(
				b.isCompleteSentence("Aasdkb"));
		
		Assert.assertTrue(
				b.isCompleteSentence("A."));
		
		Assert.assertTrue(
				b.isCompleteSentence("\t"));
	}
	
	@Test
	public void testPostBoundaryRules() throws Exception{
		URL u = Runtime.class.getResource("/rulesets/sentence/default/postBoundaryRules.txt");
		System.out.println(u); 
		Assert.assertTrue(u != null); // test existence

		BoundaryProcessor b = PostBoundaryRulesProcessor.DEFAULT;
		Assert.assertTrue(
				b.isCompleteSentence("On the "));
		
		Assert.assertTrue(
				b.isCompleteSentence("A"));
		
		Assert.assertTrue(
				!b.isCompleteSentence("asfh"));
		
		Assert.assertTrue(
				!b.isCompleteSentence("a."));
		
		Assert.assertTrue(
				b.isCompleteSentence("\n"));
	}
	
	@Test
	public void testAvailableLanguageCodes(){
		System.out.println(RuleSet.getAvailable());
		Assert.assertSame(RuleSet.DEFAULT_RULESET._post_boundary_checker_list, RuleSet.get("am")._post_boundary_checker_list);
		Assert.assertNotSame(RuleSet.DEFAULT_RULESET._boundary_checker, RuleSet.get("am")._boundary_checker);
	}

}
