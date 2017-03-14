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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.LineIterator;
import org.junit.Test;

import de.tudarmstadt.lt.seg.SegmentType;
import de.tudarmstadt.lt.seg.app.Segmenter;
import de.tudarmstadt.lt.seg.token.DiffTokenizer;
import de.tudarmstadt.lt.seg.token.EmptySpaceTokenizer;
import de.tudarmstadt.lt.seg.token.ITokenizer;
import de.tudarmstadt.lt.seg.token.TokenizerTest;
import de.tudarmstadt.lt.utilities.IOUtils;

/**
 * @author Steffen Remus
 *
 */
public class SentenceSplitterTest {

	public final static String TEST_TEXT = "\r\n\t\tthis is a sentence!\n\n \n\t\n\n\tThis is another sentence. \t\t\n   This is yet another sentence. On the 1. dot it's: \"Not a sentence.\", but on the second it is.\n\nRight? "
			+ "Das 19. Jahrhundert legte hier Grundsteine für "
			+ "die Entwicklungen, die im 20. neue Ausprägungen und globale Dimensionen "
			+ " gewinnen sollten. Der Faschismus und der Nationalsozialismus des 20."
			+ " Jahrhunderts werden sich als national-völkische Bewegungen"
			+ " manifestieren. Hochtechnisierte und hochgerüstete Staaten werden sich"
			+ " hier in romantischen Rückbesinnungen auf völkische Ursprünge definieren"
			+ " und Konflikte globaler Dimensionen austragen, die die Welt neu ordnen"
			+ " werden. Es geht aus der Sicht der Haushalte des 17. und 18. Jahrhunderts darum, den Abfluss von Edelmetall ins Ausland zu verhindern.\n"
			+ "In the 1920s he was also a contributor to \"Vanity Fair\" and British \"Vogue\" magazines.\nBloomsbury Set.\\nDuring World War I, Huxley spent much of his time at Garsington Manor near Oxford, home of Lady Ottoline Morrell, working as a farm labourer.";

	@Test
	public void breakSplitterTest() {
		ISentenceSplitter s = new BreakSplitter().init(TEST_TEXT);
		System.out.format("+++ %s +++ %n", s.getClass().getName());
		s.forEach(System.out::println);
		System.out.println("+++");
		s.init(TokenizerTest.TEST_TEXT);
		s.forEach(System.out::println);
	}

	@Test
	public void nonceSplitterTest(){
		ISentenceSplitter s = new NullSplitter().init(TEST_TEXT);
		System.out.format("+++ %s +++ %n", s.getClass().getName());
		s.forEach(System.out::println);
		System.out.println("+++");
		s.init(TokenizerTest.TEST_TEXT);
		s.forEach(System.out::println);
	}

	@Test
	public void lineSplitterTest(){
		ISentenceSplitter s = new LineSplitter().init(TEST_TEXT);
		System.out.format("+++ %s +++ %n", s.getClass().getName());
		s.forEach(System.out::println);
		System.out.println("+++");
		s.init(TokenizerTest.TEST_TEXT);
		s.forEach(System.out::println);
	}

	@Test
	public void ruleSplitterTest(){
		final AtomicInteger n = new AtomicInteger(0);
		ISentenceSplitter s = new RuleSplitter().initParam("default", false).init(TEST_TEXT);
		System.out.format("+++ %s +++ %n", s.getClass().getName());
		s.forEach(seg -> {if(seg.type == SegmentType.SENTENCE) n.incrementAndGet(); System.out.println(seg);});
		System.out.println("+++");
		s.init(TokenizerTest.TEST_TEXT);
		s.forEach(seg -> {if(seg.type == SegmentType.SENTENCE) n.incrementAndGet(); System.out.println(seg);});
		System.out.format("%d sentences.%n", n.get());
	}

	@Test
	public void ruleSplitterLineTest(){
		ISentenceSplitter sentenceSplitter = new RuleSplitter();
		ITokenizer tokenizer = new EmptySpaceTokenizer();
		StringWriter s = new StringWriter();
		PrintWriter w = new PrintWriter(s);

		LineIterator liter = new LineIterator(new BufferedReader(new StringReader(TEST_TEXT)));
		for(long lc = 0; liter.hasNext();){
			if(++lc % 1000 == 0)
				System.err.format("Processing line %d %n", lc);

			Segmenter.split_and_tokenize(
					new StringReader(liter.next()),
					String.format("%s:%d", "TEST_TEXT", lc),
					sentenceSplitter, 
					tokenizer, 
					2,
					0,
					false,
					false,
					"\n",
					"\n",
					"\n",
					w);
		}
		System.out.println(s.toString());
	}

	@Test
	public void fileTest() throws FileNotFoundException, IOException{
		ISentenceSplitter sentenceSplitter = new RuleSplitter();
		ITokenizer tokenizer = new DiffTokenizer();

		StringWriter s = new StringWriter();
		InputStream is = Runtime.class.getResourceAsStream("/test.txt");
		String test = IOUtils.read(is, StandardCharsets.UTF_8);

		Segmenter.split_and_tokenize(
				new StringReader(test),
				"test",
				sentenceSplitter, 
				tokenizer, 
				3,
				2,
				false,
				false,
				"\n",
				" ",
				"\t",
				new PrintWriter(s));

		System.out.println(s.toString());
	}

}
