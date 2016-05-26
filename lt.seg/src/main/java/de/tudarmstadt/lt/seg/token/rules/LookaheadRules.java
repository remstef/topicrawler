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
public class LookaheadRules {

    Map<Pattern, Boolean> _patterns = Collections.emptyMap();
    List<Pattern> _global_reject_patterns = Collections.emptyList();

    static {
        try{
            DEFAULT = new LookaheadRules();
        }catch(Exception e){
            throw new IllegalStateException();
        }
    }

    public static final LookaheadRules DEFAULT;

    private LookaheadRules() throws Exception {
        this(Thread.currentThread().getContextClassLoader().getResource("rulesets/token/default/lookahead-rules.txt"), Charset.forName("UTF-8"));
    }

    public LookaheadRules(URL lookahead_list_file, Charset cs) throws Exception {
        this(new InputStreamReader(lookahead_list_file.openStream(), cs));
    }

    public LookaheadRules(InputStreamReader r) throws Exception {
        _patterns = new LinkedHashMap<>(); // insertion order is important
        _global_reject_patterns = new LinkedList<>();
        final BufferedReader br = new BufferedReader(r);
        for(String line; (line = br.readLine()) != null;){
            if (line.trim().isEmpty() || line.startsWith("#"))
                continue;

            String decision = line.substring(0,2); // '+ ' or '- '
            if( !(decision.equals("- ") || decision.equals("+ ")) ){
                System.err.format("Unable to parse line '%s' in lookahead-rules-file. Please specify a decision using '+ '/'- ' in at the beginning of a line in front of the pattern.", line);
                continue;
            }

            try{
                Pattern pattern = Pattern.compile("^" + line.substring(2), Pattern.CANON_EQ);
                _patterns.put(pattern, decision.equals("+ "));
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        // put the first negative patterns that occur before any positive pattern occurs into a special global reject list and remove it from the pattern map
        for(Iterator<Map.Entry<Pattern, Boolean>> pattern_iterator = _patterns.entrySet().iterator(); pattern_iterator.hasNext();) {
            Map.Entry<Pattern, Boolean> pattern = pattern_iterator.next();
            if(pattern.getValue())
                break;
            _global_reject_patterns.add(pattern.getKey());
            pattern_iterator.remove();
        }
    }

    public boolean find_next_token(Deque<Segment> lookahead_buffer) {

        String string_segments = lookahead_buffer.stream().map(Segment::asString).collect(Collectors.joining());

        Segment next_segment = lookahead_buffer.poll();

        String match = null;
        positive_loop: for(Iterator<Map.Entry<Pattern, Boolean>> pattern_iterator = _patterns.entrySet().iterator(); pattern_iterator.hasNext();) {
            Map.Entry<Pattern, Boolean> next_pattern = pattern_iterator.next();
            if(!next_pattern.getValue()) // skip negative patterns
                continue;
            assert next_pattern.getValue() : "this loop should only go over positive patterns";
            Matcher pos_m = next_pattern.getKey().matcher(string_segments);
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

                while(pattern_iterator.hasNext()) { // check if there are negative patterns following
                    next_pattern = pattern_iterator.next();
                    // if next pattern is a positive rule break the outer loop, because we found a match with no matching negative rule
                    if(next_pattern.getValue())
                        break positive_loop;
                    // else check first if negative pattern matches
                    Matcher neg_m = next_pattern.getKey().matcher(match);
                    // if so, we continue the positive rule loop, and skip any following negative rules
                    if(neg_m.matches()) {
                        match = null;
                        continue positive_loop;
                    }
                    // if not continue the negative loop, maybe there is still a rule to come that matches
                }

                break;

            }

        }

        if(match != null) {
            next_segment.type = SegmentType.WORD_WITH_NUMBER;
            // fast forward
            while (!lookahead_buffer.isEmpty() && lookahead_buffer.peek().end <= next_segment.begin + match.length())
                next_segment.end = lookahead_buffer.poll().end;
            next_segment.text.setLength(0);
            next_segment.text.append(string_segments.substring(0, next_segment.end - next_segment.begin));
        }

        lookahead_buffer.offerFirst(next_segment);

        return match != null;

    }

}
