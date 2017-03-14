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

package de.tudarmstadt.lt.seg.token.rules;

import de.tudarmstadt.lt.seg.Segment;
import de.tudarmstadt.lt.seg.SegmentType;
import de.tudarmstadt.lt.seg.SegmentationUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Steffen Remus.
 */
public class LookaheadList {

    Set<String> _tokens = Collections.emptySet();
    int _longest_token_length = 0;
    int _shortest_token_length = 0;

    static {
        try{
            DEFAULT = new LookaheadList();
        }catch(Exception e){
            throw new IllegalStateException(e);
        }
    }

    public static final LookaheadList DEFAULT;

    private LookaheadList() throws Exception {
        this(Thread.currentThread().getContextClassLoader().getResource("rulesets/token/default/lookahead-list.txt"), Charset.forName("UTF-8"));
    }

    public LookaheadList(URL lookahead_list_file, Charset cs) throws Exception {
        this(new InputStreamReader(lookahead_list_file.openStream(), cs));
    }

    public LookaheadList(InputStreamReader r) throws Exception {
        _tokens = new HashSet<>();
        _shortest_token_length = Integer.MAX_VALUE;
        final BufferedReader br = new BufferedReader(r);
        for(String line = null; (line = br.readLine()) != null;){
            if(!line.isEmpty() && !line.startsWith("#")){
                String conv = SegmentationUtils.convert(line);
                _longest_token_length = Math.max(_longest_token_length, conv.length());
                _shortest_token_length = Math.min(_shortest_token_length, conv.length());
                _tokens.add(conv);
            }
        }
        if(_tokens.isEmpty())
            _shortest_token_length = 0;
    }

    public boolean find_next_token(final Deque<Segment> lookahead_buffer) {

        String string_segments = lookahead_buffer.stream().map(Segment::asString).collect(Collectors.joining());

        Segment next_segment = lookahead_buffer.poll();
        Set<Integer> valid_token_endings = new HashSet<>(lookahead_buffer.stream().map(s -> Integer.valueOf(s.end - next_segment.begin)).collect(Collectors.toList()));

        // check for exact match
        String longest_match = null;
        // look for longest match
        for(int i = Math.min(_longest_token_length, string_segments.length()); i >= _shortest_token_length; --i){
            String test = string_segments.substring(0, i);
            if(_tokens.contains(test) && valid_token_endings.contains(i)){
                longest_match = test;
                break;
            }
        }

        if(longest_match != null) {
            next_segment.type = SegmentType.ABBRV;
            // fast forward
            while (!lookahead_buffer.isEmpty() && lookahead_buffer.peek().end <= next_segment.begin + longest_match.length())
                next_segment.end = lookahead_buffer.poll().end;
            next_segment.text.setLength(0);
            next_segment.text.append(string_segments.substring(0, next_segment.end - next_segment.begin));
        }

        lookahead_buffer.offerFirst(next_segment);

        return longest_match != null;
    }

}
