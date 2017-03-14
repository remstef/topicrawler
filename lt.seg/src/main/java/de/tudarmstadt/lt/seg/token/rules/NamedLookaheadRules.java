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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Steffen Remus.
 */
public class NamedLookaheadRules {

    Pattern[] _patterns = new Pattern[0];
    boolean[] _decisions = new boolean[0];
    SegmentType[] _names = new SegmentType[0];
    Pattern[] _global_reject_patterns = new Pattern[0];

    static {
        try{
            DEFAULT = new NamedLookaheadRules();
        }catch(Exception e){
            throw new IllegalStateException();
        }
    }

    public static final NamedLookaheadRules DEFAULT;

    private NamedLookaheadRules() throws Exception {
        this(Thread.currentThread().getContextClassLoader().getResource("rulesets/token/default/lookahead-rules.txt"), Charset.forName("UTF-8"));
    }

    public NamedLookaheadRules(URL lookahead_list_file, Charset cs) throws Exception {
        this(new InputStreamReader(lookahead_list_file.openStream(), cs));
    }
    
    public void extendPatternArrays(){
    	_patterns = Arrays.copyOf(_patterns, _patterns.length+1);
    	_decisions = Arrays.copyOf(_decisions, _patterns.length);
    	_names = Arrays.copyOf(_names, _patterns.length);
    }

    public NamedLookaheadRules(InputStreamReader r) throws Exception {
        if(_patterns.length > 0)
        	throw new IllegalStateException("Rules are already defined. Rules should be read only once!");
        
        final BufferedReader br = new BufferedReader(r);
        for(String line; (line = br.readLine()) != null;){
            if (line.trim().isEmpty() || line.startsWith("#"))
                continue;

            char decision = line.charAt(0); // '+' or '-'
            if( !('-' == decision || '+' == decision) ){
                System.err.format("Unable to parse line '%s' in lookahead-rules-file. Please specify a decision using '+ '/'- ' in at the beginning of a line in front of the pattern.", line);
                continue;
            }

            String name = SegmentType.UNSPECIFIED.toString();
            int pattern_begin_index = line.indexOf(' ', 1)+1;
            if(':'  == line.charAt(1) && pattern_begin_index > 2)
            	name = line.substring(2, pattern_begin_index).trim();
            
            try{
                Pattern pattern = Pattern.compile("^" + line.substring(pattern_begin_index), Pattern.CANON_EQ);
                int index = _patterns.length;
                extendPatternArrays();
                _patterns[index] = pattern;
                _decisions[index] = ('+' == decision);
                _names[index] = SegmentType.valueOf(name); // throws error if not defined
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        // put the first negative patterns that occur before any positive pattern occurs into a special global reject list and remove it from the pattern map
        int l = 0; for(; _decisions[l] && l < _patterns.length; l++);
        if(l > 0){
	        _global_reject_patterns = Arrays.copyOfRange(_patterns, 0, l);
	        _patterns = Arrays.copyOfRange(_patterns, l, _patterns.length);
	        _decisions = Arrays.copyOfRange(_decisions, l, _decisions.length);
	        _names = Arrays.copyOfRange(_names, l, _names.length);
        }
        
    }

    public boolean find_next_token(Deque<Segment> lookahead_buffer) {

        String string_segments = lookahead_buffer.stream().map(Segment::asString).collect(Collectors.joining());

        Segment next_segment = lookahead_buffer.poll();

        String match = null;
        SegmentType match_type = SegmentType.UNSPECIFIED;
        positive_loop: for(int i = 0; i < _patterns.length; i++) {
        	Pattern next_pattern = _patterns[i];
        	boolean decision = _decisions[i];
        	match_type = _names[i];
            if(!decision) // skip negative patterns
                continue;
            assert decision : "this loop should only go over positive patterns";
            Matcher pos_m = next_pattern.matcher(string_segments);
            if(pos_m.find() && pos_m.start() == 0){

                match = pos_m.group();

                // check if pattern ends at a segment boundary
                int e = match.length() + next_segment.begin;

                boolean ends_with_seg_bound = next_segment.end == e;
                for(Segment s : lookahead_buffer) {
                    if (ends_with_seg_bound)
                        break;
                    if(s.end > e)
                        break;
                    ends_with_seg_bound = s.end == e;
                }
                if(!ends_with_seg_bound)
                    continue  positive_loop;

                for(Pattern global_reject : _global_reject_patterns) { // check for global negative rules
                    Matcher neg_m = global_reject.matcher(match);
                    // if so, we continue the positive rule loop, and skip any following negative rules
                    if(neg_m.matches()) {
                        match = null;
                        continue positive_loop;
                    }
                }

                for(int j = i+1; j < _patterns.length; j++){
                    next_pattern = _patterns[j];
                    // if next pattern is a positive rule break the outer loop, because we found a match with no matching negative rule
                    if(_decisions[j])
                        break positive_loop;
                    // else check first if negative pattern matches
                    Matcher neg_m = next_pattern.matcher(match);
                    // if so, we continue the positive rule loop, and skip any following negative rules
                    if(neg_m.matches()) {
                        match = null;
                        continue positive_loop;
                    }
                    // if not continue the negative loop, maybe there is still a rule to come that matches
                }
                match = null;
                break;

            }

        }

        if(match != null) {
            next_segment.type = match_type;
            // fast forward
            while (!lookahead_buffer.isEmpty() && lookahead_buffer.peek().end <= next_segment.begin + match.length())
                next_segment.end = lookahead_buffer.poll().end;
            next_segment.text.setLength(0);
            try{
            	next_segment.text.append(string_segments.substring(0, next_segment.end - next_segment.begin));
            }catch(Exception e){
            	e.printStackTrace();
            }
        }

        lookahead_buffer.offerFirst(next_segment);

        return match != null;

    }

}
