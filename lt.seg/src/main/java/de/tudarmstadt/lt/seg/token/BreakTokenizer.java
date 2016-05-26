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

import java.io.IOException;
import java.io.Reader;
import java.text.BreakIterator;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import de.tudarmstadt.lt.seg.Segment;
import de.tudarmstadt.lt.seg.SegmentType;


/**
 * 
 * Uses default locale. Can be specified by user.language, user.country, user.script, user.variant
 * 
 * @author Steffen Remus
 *
 */
public class BreakTokenizer implements ITokenizer {

	Reader _reader = null;
	String _text = null;
	final Segment _segment = new Segment(){{ begin = 0; end = 0; type = SegmentType.UNKNOWN; text.setLength(0); }};
	final BreakIterator _token_bounds = BreakIterator.getWordInstance(Locale.getDefault());
	
	int _max_index = 0;
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
		_codepoints.clear();
		_chartypes.clear();
		_segment.type = SegmentType.UNKNOWN;
		_segment.begin = _segment.end;
		_segment.end = _token_bounds.next();
		if(_segment.end < 0)
			return false;
		_segment.text.append(_text.substring(_segment.begin, _segment.end));
		_segment.text.codePoints().forEach(cp -> {
			_codepoints.add(cp);
			_chartypes.add(Character.getType(cp));
		});
		
		_segment.type = SegmentType.infer(_chartypes);
		return _segment.end >= 0;
	}

	/* (non-Javadoc)
	 * @see de.tudarmstadt.lt.seg.token.ITokenizer#text()
	 */
	@Override
	public String text() {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see de.tudarmstadt.lt.seg.token.ITokenizer#init(java.io.Reader)
	 */
	@Override
	public ITokenizer init(Reader reader) {
		_reader = reader;
		_segment.begin = 0; _segment.end = 0; _segment.type = SegmentType.UNKNOWN; _segment.text.setLength(0);
		StringBuilder b = new StringBuilder();
		try {
			char[] buf = new char[8192];
			for(int n; (n = reader.read(buf, 0, buf.length)) > 0;)
				b.append(buf, 0, n);
		} catch (IOException e) {
			e.printStackTrace();
		}
		_text = b.toString();
		_token_bounds.setText(_text);
		_codepoints.clear();
		_chartypes.clear();
		return this;
	}
	
	

}


