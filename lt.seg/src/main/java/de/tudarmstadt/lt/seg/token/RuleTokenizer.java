/*
 *  Copyright (c) 2016
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.tudarmstadt.lt.seg.token;

import de.tudarmstadt.lt.seg.Segment;
import de.tudarmstadt.lt.seg.SegmentType;
import de.tudarmstadt.lt.seg.token.rules.RuleSet;

import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by Steffen Remus.
 */
public class RuleTokenizer implements ITokenizer {

    RuleSet _ruleset = RuleSet.DEFAULT_RULESET;
    ITokenizer _base_tokenizer;

    final Deque<Segment> _lookahead_buffer = new ArrayDeque<>(100);

    final Segment _segment = new Segment(){{ begin = 0; end = 0; type = SegmentType.UNKNOWN; text.setLength(0); }};

    public boolean getNext(){
        // clear
        _segment.text.setLength(0);
        _segment.type = SegmentType.UNKNOWN;
        _segment.begin = _segment.end;

        // fill lookahead buffer
        while(_lookahead_buffer.size() < 100 && _base_tokenizer.hasNext()) {
            Segment next = _base_tokenizer.next();
            Segment copy = new Segment();
            copy.begin = next.begin;
            copy.end = next.end;
            copy.text.append(next.text);
            copy.type = next.type;
            _lookahead_buffer.offer(copy);
        }

        if(_lookahead_buffer.isEmpty())
            return false;

        boolean changed_buffer = false;
        changed_buffer = _ruleset._lookahead_list.find_next_token(_lookahead_buffer);
        changed_buffer &= _ruleset._lookahead_rules.find_next_token(_lookahead_buffer);

        Segment next_segment = _lookahead_buffer.poll();
        _segment.type = next_segment.type;
        _segment.text.append(next_segment.text);
        _segment.begin = next_segment.begin;
        _segment.end = next_segment.end;

        return true;
    }

    public ITokenizer init(Reader reader, String languagecode) {
        _ruleset = RuleSet.get(languagecode);
        _base_tokenizer = _ruleset._base_tokenizer.newTokenizer();
        return init(reader);
    }

    public ITokenizer init(Reader reader, RuleSet rules) {
        _ruleset = rules;
        _base_tokenizer = _ruleset._base_tokenizer.newTokenizer();
        return init(reader);
    }

    public ITokenizer init(RuleSet rules) {
        _ruleset = rules;
        _base_tokenizer = _ruleset._base_tokenizer.newTokenizer();
        return this;
    }

    @Override
    public ITokenizer init(Reader reader) {
        if(_base_tokenizer == null)
            _base_tokenizer = _ruleset._base_tokenizer.newTokenizer();
        _base_tokenizer.init(reader);
        return this;
    }

    @Override
    public boolean hasNext() {
        return getNext();
    }

    @Override
    public Segment next() {
        return _segment;
    }
}
