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
package de.tudarmstadt.lt.seg.token;

import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.Test;

import de.tudarmstadt.lt.seg.SegmentationUtils;

/**
 * @author Steffen Remus
 *
 */
public class TokenizerTest {
	
	public static final String TEST_TEXT =
			"0815 4711, 007 done. "
			+ "\ufeffHello World... The answer is 42, but what is the question? "
			+ "I told this joke 3times.h3ll0 w0rld!!! "
			+ "4711Today is the 1st of Dec. 2014. "
			+ "Today is the aniversary of Prof. Dr. Stephen W. Hawking. "
			+ "Is this state-of-the-art? Could this b\ne state-\nof-\r\nthe-art ? "
			+ "Today is 2014/01/12 or 1.12.14. "
			+ "\t\n\t\r\n\f\n\t :-) ^^ α α \u00a0";
	
	@Test
	public void test() {		
		for(int cp : SegmentationUtils.boundaries){
			System.out.println(Character.toChars(cp));
			System.out.println(cp);
			System.out.println(Character.getName(cp));
			System.out.println(Character.getType(cp));
			System.out.println(Character.getName(Character.getType(cp)));
		}
	}
	
	@Test
	public void tokens(){
		ITokenizer t = new DiffTokenizer().init(TEST_TEXT);
		t.tokens().forEach( x -> System.out.print(x));		
	}
	
	@Test
	public void segmentsDiffTokenizer(){
		new DiffTokenizer().init(TEST_TEXT).forEach(System.out::println);
		System.out.println();
	}
	
	@Test
	public void segmentsBreakTokenizer(){
		new BreakTokenizer().init(TEST_TEXT).forEach(System.out::println);
		System.out.println();
	}
	
	@Test
	public void segmentsRuleTokenizer(){
		final AtomicInteger n = new AtomicInteger(0);
		new RuleTokenizer().init(TEST_TEXT).forEach(seg -> {n.incrementAndGet(); System.out.println(seg);});
		System.out.format("%d token segments %n%n", n.get());
	}
	
	@Test
	public void filteredTokensDiffTokenizer(){

		ITokenizer t = new DiffTokenizer();
		t.init(TEST_TEXT).filteredSegments(4, false).forEach(System.out::println);
		System.out.println();
		t.init(TEST_TEXT).filteredAndNormalizedTokens(6, 4, true, true).forEach( x -> System.out.print(x + " "));
		System.out.println();
	}

	@Test
	public void nonceTokenizerTest(){
		new NullTokenizer().init(TEST_TEXT).forEach(System.out::println);
	}
	
	@Test
	public void emptySpaceTokenizerTest(){
		new EmptySpaceTokenizer().init(TEST_TEXT).forEach(System.out::println);
	}
	
	@Test
	public void escapeNonAscii(){
		System.out.println(TEST_TEXT);
		System.out.println(SegmentationUtils.escapeNonAscii(TEST_TEXT));
	}
	
	@Test
	public void usecase1(){
		new DiffTokenizer().init(TEST_TEXT).filteredAndNormalizedTokens(5, 4, true, false).forEach(x -> {if(x.startsWith("a")) System.out.println(x);}); 
	}
	
	@Test
	public void testempty(){
		Stream<String> a = StreamSupport.stream(new DiffTokenizer().init(TEST_TEXT).filteredAndNormalizedTokens(5, 4, true, false).spliterator(), false).filter(x -> x.startsWith("a"));
		Stream<String> b = StreamSupport.stream(new DiffTokenizer().init(TEST_TEXT).filteredAndNormalizedTokens(5, 4, true, false).spliterator(), false).filter(x -> x.startsWith("a"));
		Stream<String> c = StreamSupport.stream(new DiffTokenizer().init(TEST_TEXT).filteredAndNormalizedTokens(5, 4, true, false).spliterator(), false).filter(x -> x.startsWith("a")).filter(x -> x.startsWith("b"));
		
		Spliterator<String> s = a.spliterator();
		a = StreamSupport.stream(s, false);
		final StringBuffer buf = new StringBuffer();
		System.out.format("-- %s --%n", s.tryAdvance(x -> {buf.append(x);}));
		System.out.println(buf);
		a.forEach(System.out::println);
		buf.setLength(0);
		System.out.format("-- %s --%n", s.tryAdvance(x -> {buf.append(x);}));
		System.out.println(buf);
		b.forEach(System.out::println);
		
		s = c.spliterator();
		c = StreamSupport.stream(s, false);
		buf.setLength(0);
		System.out.format("-- %s --%n", s.tryAdvance(x -> {buf.append(x);}));
		System.out.println(buf);
		c.forEach(System.out::println);
		System.out.format("-- %s --%n", s.tryAdvance(x -> {buf.append(x);}));
	}
	
	@Test
	public void mergeTest() {		
		ITokenizer t = new DiffTokenizer().init("a a 0 0 0 b 0");
		t.filteredSegments(3,true).forEach( x -> System.out.println(x));
		
		t = t.init("a a 0 0 0 b 0");
		t.filteredAndNormalizedTokens(3,3,true,true).forEach( x -> System.out.println(x));
	}
	

}
