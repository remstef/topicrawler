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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Steffen Remus.
 */
public class RuleSet {

    private final static Map<String, RuleSet> RULE_SETS = new HashMap<String, RuleSet>();
    public final static RuleSet DEFAULT_RULESET = new RuleSet();

    static{
        for(String lang : __getAvailable__())
            RULE_SETS.put(lang, null);
    }

    private RuleSet(){
        _base_tokenizer = BaseTokenizer.DEFAULT;
        _lookahead_list = LookaheadList.DEFAULT;
        _lookahead_rules = NamedLookaheadRules.DEFAULT;
        _name = "default";
        RULE_SETS.put(_name, this);
    }

    private RuleSet(String name, URL basetokenizer_file_location, URL lookahead_list_file_location, URL lookahead_rules_file_location){
        this(name, basetokenizer_file_location, lookahead_list_file_location, lookahead_rules_file_location, Charset.defaultCharset());
    }

    private RuleSet(String name, URL basetokenizer_file_location, URL lookahead_list_file_location, URL lookahead_rules_file_location, Charset cs){
        try{ _base_tokenizer = basetokenizer_file_location == null ? BaseTokenizer.DEFAULT : new BaseTokenizer(basetokenizer_file_location, cs) /*load tokenizer*/; }catch(Exception e){ throw new IllegalArgumentException(e); }
        try{ _lookahead_list = lookahead_list_file_location == null ? LookaheadList.DEFAULT : new LookaheadList(lookahead_list_file_location, cs); }catch(Exception e){ throw new IllegalArgumentException(e); }
        try{ _lookahead_rules = lookahead_rules_file_location == null ? NamedLookaheadRules.DEFAULT : new NamedLookaheadRules(lookahead_rules_file_location, cs); }catch(Exception e){ throw new IllegalArgumentException(e); }
        _name = name;
        RULE_SETS.put(_name, this);
    }

    private RuleSet(String name, BaseTokenizer base_tokenizer, LookaheadList lookahead_list, NamedLookaheadRules lookahead_rules){
        _name = name;
        _base_tokenizer = base_tokenizer;
        _lookahead_list = lookahead_list;
        _lookahead_rules = lookahead_rules;
        RULE_SETS.put(_name, this);
    }

    public final String _name;
    public final BaseTokenizer _base_tokenizer;
    public final LookaheadList _lookahead_list;
    public final NamedLookaheadRules _lookahead_rules;

    public static RuleSet get(String name){
        RuleSet r = RULE_SETS.get(name);
        if(r != null)
            return r;
        if(!getAvailable().contains(name))
            return DEFAULT_RULESET;
        String basedir = "rulesets/token/" + name + "/";
        r = newRuleSet(
                name,
                Thread.currentThread().getContextClassLoader().getResource(basedir + "tokenizer.txt"),
                Thread.currentThread().getContextClassLoader().getResource(basedir + "lookahead-list.txt"),
                Thread.currentThread().getContextClassLoader().getResource(basedir + "lookahead-rules.txt"));
        return r;
    }

    public static Set<String> getAvailable(){
        return RULE_SETS.keySet();
    }

    private static Set<String> __getAvailable__(){
        Set<String> r = Collections.emptySet();
        try {
            URI uri = Thread.currentThread().getContextClassLoader().getResource("rulesets/token").toURI();
            Path myPath;
            FileSystem jarFileSystem = null;
            if (uri.getScheme().equals("jar")){
                jarFileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                myPath = jarFileSystem.getPath("/rulesets/token");
            } else {
                myPath = Paths.get(uri);
            }
            r = Files.walk(myPath, 1).map(x -> x.getFileName().toString().replace("/", "")).filter(s -> !(s.equals("token"))).collect(Collectors.toSet());
            if(jarFileSystem != null)
                jarFileSystem.close();
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        return r;
    }

    public static RuleSet newRuleSet(String name, URL base_tokenizer_file_location, URL lookahead_list_file_location, URL lookahead_rules_file_location){
        return newRuleSet(name, base_tokenizer_file_location, lookahead_list_file_location, lookahead_rules_file_location, Charset.defaultCharset());
    }

    public static RuleSet newRuleSet(String name, URL base_tokenizer_file_location, URL lookahead_list_file_location, URL lookahead_rules_file_location, Charset cs){
        RuleSet r = RULE_SETS.get(name);
        if(r != null)
            return r;
        r = new RuleSet(name, base_tokenizer_file_location, lookahead_list_file_location, lookahead_rules_file_location, cs);
        return r;
    }

}
