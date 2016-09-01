/*
 *   Copyright 2015
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
package de.tudarmstadt.lt.seg;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.Test;

import de.tudarmstadt.lt.seg.sentence.ISentenceSplitter;
import de.tudarmstadt.lt.seg.sentence.RuleSplitter;
import de.tudarmstadt.lt.seg.token.DiffTokenizer;
import de.tudarmstadt.lt.seg.token.ITokenizer;
import de.tudarmstadt.lt.seg.token.TokenizerTest;

/**
 * @author Steffen Remus
 *
 */
public class TestParallelism {
	
	class DocSupplier implements Supplier<String>{

		/* (non-Javadoc)
		 * @see java.util.function.Supplier#get()
		 */
		@Override
		public String get() {
			return TokenizerTest.TEST_TEXT; 
		}
		
	}

	@Test
	public void testNewObject() {
		Stream.generate(new DocSupplier()).limit(100).parallel().flatMap(doc -> new RuleSplitter().init(doc).stream()).flatMap(s -> new DiffTokenizer().init(s.asString()).stream()).forEach(System.out::println);		
	}
	
	@Test
	public void testReuseObject() throws InterruptedException, ExecutionException {
		ThreadLocal<ISentenceSplitter> split = ThreadLocal.withInitial(() -> new RuleSplitter());
		ThreadLocal<ITokenizer> tok = ThreadLocal.withInitial(() -> new DiffTokenizer());
		Stream<String> lines = Stream.generate(new DocSupplier());
		ForkJoinPool forkJoinPool = new ForkJoinPool(10);
		forkJoinPool.submit(() -> {
			lines
			.limit(1000)
			.parallel()
			.flatMap(doc -> split.get().init(doc).stream()).flatMap(s -> tok.get().init(s.asString()).stream()).forEach(s -> System.out.println(String.format("%d\t%s",Thread.currentThread().getId(), s)));
		}).get();
		
	}

}
