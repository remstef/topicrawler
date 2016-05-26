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

import static de.tudarmstadt.lt.seg.SegmentType.WORD_WITH_NUMBER;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.tudarmstadt.lt.seg.Segment;
import de.tudarmstadt.lt.seg.SegmentType;
import de.tudarmstadt.lt.seg.SegmentationUtils;

/**
 * 
 * 
 * @author Steffen Remus
 *
 */
public class DiffTokenizer implements ITokenizer {

	Reader _reader = null;
	int _cp = 0;
	final Segment _segment = new Segment(){{ begin = 0; end = 0; type = SegmentType.UNKNOWN; text.setLength(0); }};

	final Set<Integer> _codepoints = Collections.synchronizedSet(new HashSet<>());
	final Set<Integer> _chartypes = Collections.synchronizedSet(new HashSet<>());

	@Override
	public boolean hasNext() {
		return getNext();
	}

	@Override
	public Segment next() {
		return _segment;
	}

	private boolean getNext(){
		_segment.text.setLength(0);
		_segment.type = SegmentType.UNKNOWN;
		_segment.begin = _segment.end;
		_codepoints.clear();
		_chartypes.clear();
		
		while(_cp > 0){
			int cp_current = _cp;
			_segment.text.appendCodePoint(cp_current);
			_codepoints.add(cp_current);
			int current_type = Character.getType(cp_current);
			_chartypes.add(current_type);
			
			try {
				_cp = _reader.read();
			} catch (IOException e) {
				System.err.format("%s: %s%n", e.getClass().getName(), e.getMessage());
				break;
			}
			++_segment.end;
				
				
			int cp_next = _cp;
			int next_type = Character.getType(cp_next);
			if(SegmentationUtils.charTypeIsEmptySpace(current_type) && !SegmentationUtils.charTypeIsEmptySpace(next_type))
				break;
			if(!SegmentationUtils.charTypeIsEmptySpace(current_type) && SegmentationUtils.charTypeIsEmptySpace(next_type))
				break;
			if(WORD_WITH_NUMBER.allowedCharacterTypes().contains(current_type) && !WORD_WITH_NUMBER.allowedCharacterTypes().contains(next_type))
				break;
			if(!WORD_WITH_NUMBER.allowedCharacterTypes().contains(current_type) && WORD_WITH_NUMBER.allowedCharacterTypes().contains(next_type))
				break;

		}
		
		_segment.type = SegmentType.infer(_chartypes);
		return !_segment.hasZeroLength();
	}

	/* (non-Javadoc)
	 * @see de.tudarmstadt.lt.seg.token.ITokenizer#init(java.io.Reader)
	 */
	@Override
	public ITokenizer init(Reader reader) {
		_reader = reader;
		_segment.begin = 0; _segment.end = 0; _segment.type = SegmentType.UNKNOWN; _segment.text.setLength(0);
		_codepoints.clear();
		_chartypes.clear();
		try {
			_cp = _reader.read();
		} catch (IOException e) {
			System.err.format("%s: %s%n", e.getClass().getName(), e.getMessage());
			return this;
		}
		return this;
	}

	
}


