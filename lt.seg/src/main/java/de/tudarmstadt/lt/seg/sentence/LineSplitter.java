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

import java.io.IOException;
import java.io.Reader;

import de.tudarmstadt.lt.seg.Segment;
import de.tudarmstadt.lt.seg.SegmentType;
import de.tudarmstadt.lt.seg.SegmentationUtils;

/**
 * @author Steffen Remus
 *
 */
public class LineSplitter implements ISentenceSplitter{

	Reader _reader = null;
	int cp = 0;
	final Segment _segment = new Segment(){{ begin = 0; end = 0; type = SegmentType.UNKNOWN; text.setLength(0); }};
	
	private boolean getNext(){
		_segment.text.setLength(0);
		_segment.type = SegmentType.UNKNOWN;
		_segment.begin = _segment.end;
		
		if(cp < 0)
			return false;
		
		boolean first_is_newline =  SegmentationUtils.charIsLineSeparator(cp);
		boolean is_empty = first_is_newline;

		while(true){
			_segment.text.appendCodePoint(cp);
			int cp_current = cp;
			is_empty &= SegmentationUtils.charIsLineSeparator(cp_current);
			
			try {
				cp = _reader.read();
			} catch (IOException e) {
				System.err.format("%s: %s%n", e.getClass().getName(), e.getMessage());
				break;
			}
			_segment.end++;
			
			if(cp < 0)
				break;
				
			int cp_next = cp;
			
			if(is_empty && !SegmentationUtils.charIsLineSeparator(cp_next))
				break;

			if(!is_empty && SegmentationUtils.charIsLineSeparator(cp_next))
				break;
		}
		_segment.type = is_empty ? SegmentType.EMPTY_SPACE : SegmentType.SENTENCE; 
		
		return !_segment.hasZeroLength();
	}
		
	/* (non-Javadoc)
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return getNext();
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#next()
	 */
	@Override
	public Segment next() {
		return _segment;
	}

	/* (non-Javadoc)
	 * @see de.tudarmstadt.lt.seg.sentence.ISentenceSplitter#init(java.io.Reader)
	 */
	@Override
	public ISentenceSplitter init(Reader reader) {
		_reader = reader;
		_segment.begin = 0; _segment.end = 0; _segment.type = SegmentType.UNKNOWN; _segment.text.setLength(0);
		try {
			cp = _reader.read();
		} catch (IOException e) {
			System.err.format("%s: %s%n", e.getClass().getName(), e.getMessage());
			return this;
		}
		return this;
	}

}
