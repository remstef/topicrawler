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

import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import de.tudarmstadt.lt.seg.Segment;

/**
 * @author Steffen Remus
 *
 */
public interface ISentenceSplitter extends Iterator<Segment>, Iterable<Segment> {
	
	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	default Iterator<Segment> iterator(){
		return this;
	}
	
	/**
	 * @return
	 */
	default Stream<Segment> stream(){
		Iterable<Segment> iterable = () -> ISentenceSplitter.this;
		return StreamSupport.stream(iterable.spliterator(), false);
	}
	
	/**
	 * @return
	 */
	default Iterable<String> sentences(){
		return new Iterable<String>() {
			@Override
			public Iterator<String> iterator() {
				return stream().map(s -> s.text.toString()).iterator();
			}
		};
	}
	
	/**
	 * 
	 * @return
	 */
	@Deprecated
	default String text(){
		throw new UnsupportedOperationException();
	}

	/**
	 * 
	 * @param text
	 * @return
	 */
	default ISentenceSplitter init(String text){
		return init(new StringReader(text));
	}
	
	/**
	 * 
	 * @param reader
	 * @return
	 */
	ISentenceSplitter init(Reader reader);
}
