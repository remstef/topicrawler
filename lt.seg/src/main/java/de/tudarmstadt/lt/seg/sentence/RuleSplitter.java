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
import de.tudarmstadt.lt.seg.sentence.rules.RuleSet;
import de.tudarmstadt.lt.seg.token.EmptySpaceTokenizer;
import de.tudarmstadt.lt.seg.token.ITokenizer;

/**
 * @author Steffen Remus
 *
 */
public class RuleSplitter implements ISentenceSplitter{

	
	ITokenizer _approximate_tokenizer = new EmptySpaceTokenizer();

	RuleSet _rules = RuleSet.DEFAULT_RULESET;
	Reader _reader = null;
	int _cp = 0;
	final Segment _segment_sentence = new Segment(){{ begin = 0; end = 0; type = SegmentType.UNKNOWN; text.setLength(0);}};
	final Segment _segment_sentence_boundary = new Segment(){{ begin = 0; end = 0; type = SegmentType.UNKNOWN; text.setLength(0); }};

	private boolean getNext(){
		_segment_sentence.text.setLength(0);
		_segment_sentence.type = SegmentType.UNKNOWN;
		_segment_sentence.begin = _segment_sentence.end;

		_segment_sentence_boundary.text.setLength(0);
		_segment_sentence_boundary.type = SegmentType.UNKNOWN;
		_segment_sentence_boundary.begin = _segment_sentence.begin;

		if(_cp < 0)
			return false;

		boolean first_is_newline =  SegmentationUtils.charIsLineSeparator(_cp);
		boolean is_empty = first_is_newline;

		while(_cp > 0){
			int cp_current = _cp;
			_segment_sentence.text.appendCodePoint(cp_current);
			++_segment_sentence.end;
			is_empty &= SegmentationUtils.charIsEmptySpace(cp_current);

			if(isSentenceBoundary() && checkSentenceBoundaryLookBack() && checkSentenceBoundaryLookAhead()){
				_cp = getNextCodePoint();
				break;
			}
			
			_cp = getNextCodePoint();
			int cp_next = _cp;

			if(is_empty && !SegmentationUtils.charIsLineSeparator(cp_next))
				break;
		}
		_segment_sentence.type = is_empty ? SegmentType.EMPTY_SPACE : SegmentType.SENTENCE; 

		return !_segment_sentence.hasZeroLength();
	}

	private boolean isSentenceBoundary(){
		String suffix = _rules._boundary_checker.getSuffixAsSentenceBoundary(_segment_sentence.text.substring((int)Math.max(_segment_sentence.text.length()-100,0)));
		if(suffix == null)
			return false;
		_segment_sentence_boundary.text.setLength(0);
		_segment_sentence_boundary.end = _segment_sentence.end;
		_segment_sentence_boundary.begin = _segment_sentence.end - suffix.length();
		_segment_sentence_boundary.text.append(_segment_sentence.text.substring(_segment_sentence.text.length() - suffix.length()));
		return true;
	}

	private boolean checkSentenceBoundaryLookAhead(){
		// get the next approximate token or sentence
		if(_cp < 0)
			return true;
		try {
			_reader.mark(1000);
			_approximate_tokenizer.init(_reader);
			if(!_approximate_tokenizer.hasNext())
				return true;
			String next_token = _approximate_tokenizer.next().asString();
			_reader.reset();
			if(!_rules._post_boundary_checker_list.isCompleteSentence(next_token))
				return false; // stop early 
			return _rules._post_boundary_checker_rules.isCompleteSentence(next_token);
		} catch (IOException e) {
			System.err.format("%s: %s%n", e.getClass().getName(), e.getMessage());
		}
		return false;
	}

	private boolean checkSentenceBoundaryLookBack(){
		if(!_rules._pre_boundary_checker_list.isCompleteSentence(_segment_sentence.text.toString()))
			return false; // stop early
		return _rules._pre_boundary_checker_rules.isCompleteSentence(_segment_sentence.text.substring(0, _segment_sentence_boundary.begin - _segment_sentence.begin));
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
		return _segment_sentence;
	}

	public int getNextCodePoint(){
		try {
			return _reader.read();
		} catch (IOException e) {
			System.err.format("%s: %s%n", e.getClass().getName(), e.getMessage());
			return -1;
		}
	}
	
	public ISentenceSplitter init(Reader reader, String languagecode) {
		_rules = RuleSet.get(languagecode);
		return init(reader);
	}
	
	public ISentenceSplitter init(Reader reader, RuleSet rules) {
		_rules = rules;
		return init(reader);
	}
	
	public ISentenceSplitter init(RuleSet rules) {
		_rules = rules;
		return this;
	}

	/* (non-Javadoc)
	 * @see de.tudarmstadt.lt.seg.sentence.ISentenceSplitter#init(java.io.Reader)
	 */
	@Override
	public ISentenceSplitter init(Reader reader) {
		// TODO: if not marksupported throw error
		_reader = reader;
		_segment_sentence.begin = 0; _segment_sentence.end = 0; _segment_sentence.type = SegmentType.UNKNOWN; _segment_sentence.text.setLength(0);
		_segment_sentence_boundary.begin = 0; _segment_sentence_boundary.end = 0; _segment_sentence_boundary.type = SegmentType.UNKNOWN; _segment_sentence_boundary.text.setLength(0);
		_cp = getNextCodePoint();
		return this;
	}

}
