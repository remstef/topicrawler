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
package de.tudarmstadt.lt.seg.sentence.rules;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.tudarmstadt.lt.seg.SegmentationUtils;

/**
 * @author Steffen Remus
 *
 */
public class BoundaryList implements BoundaryProcessor {
	
	public Set<String> _sentence_endings = Collections.emptySet();
	
	static {
		try{
			DEFAULT = new BoundaryList();
		}catch(Exception e){
			throw new IllegalStateException();
		}
	}
	
	public static final BoundaryList DEFAULT;
	
	private BoundaryList() throws Exception {
		this(Thread.currentThread().getContextClassLoader().getResource("rulesets/sentence/default/boundaries.txt"), Charset.forName("UTF-8"));
	}
	
	public BoundaryList(URL boundary_file_location) throws Exception {
		this(boundary_file_location, Charset.defaultCharset());
	}
	
	
	public BoundaryList(URL boundary_file_location, Charset cs) throws Exception {
		this(new InputStreamReader(boundary_file_location.openStream(), cs));
	}
	
	public BoundaryList(InputStreamReader r) throws Exception {
		_sentence_endings = new HashSet<>();
		final BufferedReader br = new BufferedReader(r);
		for(String line = null; (line = br.readLine()) != null;)
			if(!line.isEmpty() && !line.startsWith("#"))
				_sentence_endings.add(SegmentationUtils.convert(line));
	}

	@Override
	public boolean isCompleteSentence(String text) {
		return getSuffixAsSentenceBoundary(text) != null;
	}
	
	public String getSuffixAsSentenceBoundary(String text){
		for(String sentence_ending : _sentence_endings)
			if(text.endsWith(sentence_ending))
				return sentence_ending;
		return null;
	}
	
	@Override
	public boolean isIncompleteSentence(String text) {
		return !isCompleteSentence(text);
	}

}
