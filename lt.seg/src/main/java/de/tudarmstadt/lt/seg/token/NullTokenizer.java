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
import java.util.HashSet;
import java.util.Set;

import de.tudarmstadt.lt.seg.Segment;
import de.tudarmstadt.lt.seg.SegmentType;

/**
 * @author Steffen Remus
 *
 */
public class NullTokenizer implements ITokenizer{

	Reader _reader = null;
	final Segment _segment = new Segment(){{ begin = 0; end = 0; type = SegmentType.UNKNOWN; text.setLength(0); }};
	
	final Set<Integer> _codepoints = new HashSet<>();
	final Set<Integer> _chartypes = new HashSet<>();
	
	
	/* (non-Javadoc)
	 * @see java.util.Iterator#hasNext()
	 */
	@Override
	public boolean hasNext() {
		return _reader != null;
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#next()
	 */
	@Override
	public Segment next() {
		_reader = null;
		return _segment;
	}

	
	/* (non-Javadoc)
	 * @see de.tudarmstadt.lt.seg.sentence.ISentenceSplitter#text()
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
		_codepoints.clear();
		_chartypes.clear();
		_reader = reader;
		_segment.begin = 0; _segment.end = 0; _segment.type = SegmentType.UNKNOWN; _segment.text.setLength(0);
		try {
			char[] buf = new char[8192];
			for(int n; (n = reader.read(buf, 0, buf.length)) > 0;){
				_segment.text.append(buf, 0, n);
				for(int cp : buf){
					_codepoints.add(cp);
					_chartypes.add(Character.getType(cp));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		_segment.type = SegmentType.infer(_chartypes);
		return this;
	}

}
