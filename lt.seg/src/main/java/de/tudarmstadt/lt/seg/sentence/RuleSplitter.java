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

import org.apache.commons.lang.StringUtils;

import de.tudarmstadt.lt.seg.Segment;
import de.tudarmstadt.lt.seg.SegmentType;
import de.tudarmstadt.lt.seg.SegmentationUtils;
import de.tudarmstadt.lt.seg.sentence.rules.RuleSet;
import de.tudarmstadt.lt.seg.token.ITokenizer;

/**
 * @author Steffen Remus
 *
 */
public class RuleSplitter implements ISentenceSplitter{

	ITokenizer _tokenizer;
	boolean _boundary_as_part_of_sentence = true;
	RuleSet _rules = RuleSet.DEFAULT_RULESET;
	Reader _reader = null;
	int _cp = 0;
	final Segment _segment_sentence = new Segment(){{ begin = 0; end = 0; type = SegmentType.UNKNOWN; text.setLength(0);}};
	final Segment _segment_sentence_boundary = new Segment(){{ begin = 0; end = 0; type = SegmentType.UNKNOWN; text.setLength(0); }};

	private boolean getNext(){
		
		if(!_boundary_as_part_of_sentence) { // return sentence boundaries as individual segments only if it is wanted
			if(!_segment_sentence_boundary.hasZeroLength()){
				_segment_sentence.text.setLength(0);
				_segment_sentence.text.append(_segment_sentence_boundary.text);
				_segment_sentence.type = SegmentType.SENTENCE_BOUNDARY;
				_segment_sentence.begin = _segment_sentence_boundary.begin;
				_segment_sentence.end = _segment_sentence_boundary.end;
				
				_segment_sentence_boundary.text.setLength(0);
				_segment_sentence_boundary.type = SegmentType.UNKNOWN;
				_segment_sentence_boundary.begin = _segment_sentence.end;
				_segment_sentence_boundary.end = _segment_sentence.end;
				
				return true;
			}
		}
		
		_segment_sentence.text.setLength(0);
		_segment_sentence.type = SegmentType.UNKNOWN;
		_segment_sentence.begin = _segment_sentence.end;

		_segment_sentence_boundary.text.setLength(0);
		_segment_sentence_boundary.type = SegmentType.UNKNOWN;
		_segment_sentence_boundary.begin = _segment_sentence.begin;
		_segment_sentence_boundary.end = _segment_sentence.end;

		if(_cp < 0)
			return false;
		
		// collect empty spaces
		while(_cp > 0 && SegmentationUtils.charIsEmptySpace(_cp)){
			_segment_sentence.text.appendCodePoint(_cp);
			_segment_sentence.end++;
			_cp = getNextCodePoint();
		}
		if(!_segment_sentence.hasZeroLength()){ // we found empty spaces
			_segment_sentence.type = SegmentType.EMPTY_SPACE;
			return true;
		}
		
		while(_cp > 0){
			int cp_current = _cp;
			_segment_sentence.text.appendCodePoint(cp_current);
			_segment_sentence.end++;

			if(isSentenceBoundary() && checkSentenceBoundaryLookBack() && checkSentenceBoundaryLookAhead()){
				_cp = getNextCodePoint();
				if(!_boundary_as_part_of_sentence){ // currently boundary is part of sentence, thus, only if it is wished otherwise, remove it 
					_segment_sentence.text.delete(_segment_sentence.length() - _segment_sentence_boundary.length(), _segment_sentence.length());
					_segment_sentence.end = _segment_sentence_boundary.begin - 1;				
				}
				break;
			}
			// reset the boundary that was potentially found in isSentenceBoundary() because it was rejected by checkSentenceBoundaryLookBack or checkSentenceBoundaryLookAhead
			_segment_sentence_boundary.text.setLength(0);
			_segment_sentence_boundary.type = SegmentType.UNKNOWN;
			_segment_sentence_boundary.begin = _segment_sentence.end;
			_segment_sentence_boundary.end = _segment_sentence.end;
			
			_cp = getNextCodePoint();

		}
		
		_segment_sentence.type = SegmentType.SENTENCE; 
		return !_segment_sentence.hasZeroLength();
		
	}

	private boolean isSentenceBoundary(){
		String suffix = _rules._boundary_checker.getSuffixAsSentenceBoundary(/*just test 100 chars back*/_segment_sentence.text.substring((int)Math.max(_segment_sentence.text.length()-100,0)));
		if(suffix == null)
			return false;
		_segment_sentence_boundary.text.setLength(0);
		_segment_sentence_boundary.end = _segment_sentence.end;
		_segment_sentence_boundary.begin = _segment_sentence.end - suffix.length();
		_segment_sentence_boundary.text.append(suffix); // suffix ?= _segment_sentence.text.substring(_segment_sentence.text.length() - suffix.length()));
		return true;
	}

	private boolean checkSentenceBoundaryLookAhead(){
		// get the next approximate token or sentence
		if(_cp < 0)
			return true;
		try {
			_reader.mark(1000);
			_tokenizer.init(_reader);
			if(!_tokenizer.hasNext())
				return true;
			String next_token = _tokenizer.next().asString();
			_reader.reset();
			if(_rules._post_boundary_checker_list.isCompleteSentence(next_token))
				return _rules._post_boundary_checker_rules.isCompleteSentence(next_token);
			return false;
		} catch (IOException e) {
			System.err.format("%s: %s%n", e.getClass().getName(), e.getMessage());
		}
		return false;
	}

	private boolean checkSentenceBoundaryLookBack(){
		if(_rules._pre_boundary_checker_list.isCompleteSentence(_segment_sentence.text.toString()))
			return _rules._pre_boundary_checker_rules.isCompleteSentence(_segment_sentence.text.substring(0, _segment_sentence_boundary.begin - _segment_sentence.begin));
		return false;
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
	
	public ISentenceSplitter initParam(String languagecode, boolean boundary_as_part_of_sentence) {
		RuleSet rs = null;
		if(!StringUtils.isEmpty(languagecode))
			rs = RuleSet.get(languagecode);
		return initParam(rs, boundary_as_part_of_sentence);
	}
	
	
	
	public ISentenceSplitter initParam(RuleSet rules, boolean boundary_as_part_of_sentence) {
		_boundary_as_part_of_sentence = boundary_as_part_of_sentence;
		_rules = rules == null ? RuleSet.DEFAULT_RULESET : rules;
		_tokenizer = _rules._base_tokenizer.newTokenizer();
		return this;
	}

	/* (non-Javadoc)
	 * @see de.tudarmstadt.lt.seg.sentence.ISentenceSplitter#init(java.io.Reader)
	 */
	@Override
	public ISentenceSplitter init(Reader reader) {
		// TODO: if not marksupported throw error
		if(_tokenizer == null)
            _tokenizer = _rules._base_tokenizer.newTokenizer();
		_reader = reader;
		_segment_sentence.begin = 0; _segment_sentence.end = 0; _segment_sentence.type = SegmentType.UNKNOWN; _segment_sentence.text.setLength(0);
		_segment_sentence_boundary.begin = 0; _segment_sentence_boundary.end = 0; _segment_sentence_boundary.type = SegmentType.UNKNOWN; _segment_sentence_boundary.text.setLength(0);
		_cp = getNextCodePoint();
		return this;
	}

}
