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

import java.io.Reader;
import java.io.StringReader;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import de.tudarmstadt.lt.seg.Segment;
import de.tudarmstadt.lt.seg.SegmentType;
import de.tudarmstadt.lt.seg.SegmentationUtils;

/**
 * @author Steffen Remus
 *
 */
public interface ITokenizer extends Iterator<Segment>, Iterable<Segment>{

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	default Iterator<Segment> iterator() {
		return this;
	}

	/**
	 * 
	 * @return
	 */
	default Iterable<String> tokens(){
		return new Iterable<String>() {
			@Override
			public Iterator<String> iterator() {
				return stream().map(s -> s.text.toString()).iterator();
			}
		};
	}

	/**
	 * 
	 * @param normalize
	 * @return
	 */
	@Deprecated
	default Iterable<String> filteredTokens(boolean normalize){
		return new Iterable<String>() {
			@Override
			public Iterator<String> iterator() {
				Stream<Segment> stream = stream().filter(s -> s.isReadable());
				if(!normalize)
					return stream.map(s -> s.asString()).iterator();
				else
					return stream.map(s -> s.asNormalizedString(normalize ? 3 : 0)).iterator();
			}
		};
	}

	/**
	 * 
	 * @param level 
	 * @return
	 */
	default Iterable<Segment> filteredSegments(int level, boolean mergetypes){
		return new Iterable<Segment>() {
			@Override
			public Iterator<Segment> iterator() {
				Stream<Segment> segments = stream();
				if(level >= 1)
					segments = segments.filter(x -> x.type != SegmentType.CONTROL);   // remove control characters
				if(level >= 2)
					segments = segments.filter(x -> x.type != SegmentType.EMPTY_SPACE);   // remove empty spaces
				if(level >= 3)
					segments = segments.filter(Segment::isReadable);					  // remove non-readable and unclassified segments
				if(level >= 4)
					segments = segments.filter(x -> x.type != SegmentType.PUNCT);	// remove punctuation
				if(level >= 5)
					segments = segments.filter(x -> !EnumSet.of(SegmentType.META, SegmentType.URI, SegmentType.EMAIL, SegmentType.EMO).contains(x.type));	// remove metadata stuff
				if(level >= 6)
					segments = segments.filter(x -> !EnumSet.of(SegmentType.WORD_WITH_NUMBER, SegmentType.NUMBER, SegmentType.DATE, SegmentType.PHONE, SegmentType.TIME).contains(x.type));	// remove numbers
				if(mergetypes){
					final Iterator<Segment> segments_iter = segments.iterator();
					segments = StreamSupport.stream(SegmentationUtils.mergeConsectutiveTypes(new Iterable<Segment>() {
							@Override
							public Iterator<Segment> iterator() {
								return segments_iter;
							}
						}).spliterator(), false) ;
				}
				
				return segments.iterator();
			}
		};
	}

	/**
	 * 
	 * @param filtration_level
	 * @return
	 */
	default Iterable<String> filteredAndNormalizedTokens(int level_filter, int level_normalize, boolean mergetypes, boolean mergetokens){
		return new Iterable<String>() {
			@Override
			public Iterator<String> iterator() {
				Stream<Segment> segments = StreamSupport.stream(filteredSegments(level_filter, mergetypes).spliterator(), false);
				Stream<String> tokens = segments.map( x -> x.asNormalizedString(level_normalize) );
				if(mergetokens){
					final Iterator<String> token_iter = tokens.iterator();
					tokens = StreamSupport.stream(SegmentationUtils.mergeConsecutiveTokens(new Iterable<String>() {
						@Override
						public Iterator<String> iterator() {
							return token_iter;
						}
					}).spliterator(), false) ;
				}
				return tokens.iterator();
			}
		};
	}

	/**
	 * 
	 * @return
	 */
	default Stream<Segment> stream(){
		Iterable<Segment> iterable = () -> ITokenizer.this;
		return StreamSupport.stream(iterable.spliterator(), false);
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
	default ITokenizer init(final String text){
		return init(new StringReader(text));
	}

	/**
	 * 
	 * @param reader
	 * @return
	 */
	ITokenizer init(Reader reader);

}
