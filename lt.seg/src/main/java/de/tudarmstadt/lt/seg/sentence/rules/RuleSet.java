/*
 *   Copyright 2015
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
package de.tudarmstadt.lt.seg.sentence.rules;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * @author Steffen Remus
 *
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
		_boundary_checker = BoundaryList.DEFAULT;
		_pre_boundary_checker_list = PreBoundaryListProcessor.DEFAULT;
		_pre_boundary_checker_rules = PreBoundaryRulesProcessor.DEFAULT;
		_post_boundary_checker_list = PostBoundaryListProcessor.DEFAULT;
		_post_boundary_checker_rules = PostBoundaryRulesProcessor.DEFAULT;
		_name = "default";
		RULE_SETS.put(_name, this);
	}
	
	private RuleSet(String name, URL basetokenizer_file_location, URL boundary_file_location, URL pre_boundary_file_location, URL pre_boundary_rule_file_location, URL post_boundary_file_location, URL post_boundary_rule_file_location){
		this(name, basetokenizer_file_location, boundary_file_location, pre_boundary_file_location, pre_boundary_rule_file_location, post_boundary_file_location, post_boundary_rule_file_location, Charset.defaultCharset());
	}
	
	private RuleSet(String name, URL basetokenizer_file_location, URL boundary_file_location, URL pre_boundary_file_location, URL pre_boundary_rule_file_location, URL post_boundary_file_location, URL post_boundary_rule_file_location,Charset cs){
		try{ _base_tokenizer = basetokenizer_file_location == null ? BaseTokenizer.DEFAULT : new BaseTokenizer(basetokenizer_file_location, cs) /*load tokenizer*/; }catch(Exception e){ throw new IllegalArgumentException(e); }
		try{ _boundary_checker = boundary_file_location == null ? BoundaryList.DEFAULT : new BoundaryList(boundary_file_location, cs); }catch(Exception e){ throw new IllegalArgumentException(e); }
		try{ _pre_boundary_checker_list = pre_boundary_file_location == null ? PreBoundaryListProcessor.DEFAULT : new PreBoundaryListProcessor(pre_boundary_file_location, cs); }catch(Exception e){ throw new IllegalArgumentException(e); }
		try{ _pre_boundary_checker_rules = pre_boundary_rule_file_location == null ? PreBoundaryRulesProcessor.DEFAULT : new PreBoundaryRulesProcessor(pre_boundary_rule_file_location, cs); }catch(Exception e){ throw new IllegalArgumentException(e); }
		try{ _post_boundary_checker_list = post_boundary_file_location == null ? PostBoundaryListProcessor.DEFAULT : new PostBoundaryListProcessor(post_boundary_file_location, cs); }catch(Exception e){ throw new IllegalArgumentException(e); }
		try{ _post_boundary_checker_rules = post_boundary_rule_file_location == null ? PostBoundaryRulesProcessor.DEFAULT : new PostBoundaryRulesProcessor(post_boundary_rule_file_location, cs); }catch(Exception e){ throw new IllegalArgumentException(e); }
		_name = name;
		RULE_SETS.put(_name, this);
	}
	
	private RuleSet(String name, BaseTokenizer base_tokenizer, BoundaryList boundary_checker, PreBoundaryListProcessor pre_boundary_checker_list, PreBoundaryRulesProcessor pre_boundary_checker_rules, PostBoundaryListProcessor post_boundary_checker_list, PostBoundaryRulesProcessor post_boundary_checker_rules){
		_base_tokenizer = base_tokenizer;
		_boundary_checker = boundary_checker;
		_pre_boundary_checker_list = pre_boundary_checker_list;
		_pre_boundary_checker_rules = pre_boundary_checker_rules;
		_post_boundary_checker_list = post_boundary_checker_list;
		_post_boundary_checker_rules = post_boundary_checker_rules;
		_name = name;
		RULE_SETS.put(_name, this);
	}
	
	public final String _name;
	public final BaseTokenizer _base_tokenizer;
	public final BoundaryList _boundary_checker;
	public final PreBoundaryListProcessor _pre_boundary_checker_list;
	public final PreBoundaryRulesProcessor _pre_boundary_checker_rules;
	public final PostBoundaryListProcessor _post_boundary_checker_list;
	public final PostBoundaryRulesProcessor _post_boundary_checker_rules;
	
	public static RuleSet get(String name){
		RuleSet r = RULE_SETS.get(name);
		if(r != null)
			return r;
		if(!getAvailable().contains(name))
			return DEFAULT_RULESET;
		String basedir = "rulesets/sentence/" + name + "/";
		r = newRuleSet(
				name, 
				Thread.currentThread().getContextClassLoader().getResource(basedir + "tokenizer.txt"),
				Thread.currentThread().getContextClassLoader().getResource(basedir + "boundaries.txt"), 
				Thread.currentThread().getContextClassLoader().getResource(basedir + "preBoundaryExceptions.txt"), 
				Thread.currentThread().getContextClassLoader().getResource(basedir + "preBoundaryRules.txt"), 
				Thread.currentThread().getContextClassLoader().getResource(basedir + "postBoundaryExceptions.txt"), 
				Thread.currentThread().getContextClassLoader().getResource(basedir + "postBoundaryRules.txt"));		
		return r;
	}

	public static Set<String> getAvailable(){
		return RULE_SETS.keySet();
	}
	
	private static Set<String> __getAvailable__(){
		Set<String> r = Collections.emptySet();
		try {
			URI uri = Thread.currentThread().getContextClassLoader().getResource("rulesets/sentence").toURI();
		    Path myPath;
			FileSystem jarFileSystem = null;
		    if (uri.getScheme().equals("jar")){
		        jarFileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
		        myPath = jarFileSystem.getPath("/rulesets/sentence");
		    } else {
		        myPath = Paths.get(uri);
		    }
		    r = Files.walk(myPath, 1).map(x -> x.getFileName().toString().replace("/", "")).filter(s -> !s.equals("sentence")).collect(Collectors.toSet());
			if(jarFileSystem != null)
				jarFileSystem.close();
		} catch (URISyntaxException | IOException e) {
			e.printStackTrace();
		}
		return r;
	}
	
	public static RuleSet newRuleSet(String name, URL basetokenizer_file_location, URL boundary_file_location, URL pre_boundary_file_location, URL pre_boundary_rule_file_location, URL post_boundary_file_location, URL post_boundary_rule_file_location){
		return newRuleSet(name, basetokenizer_file_location, boundary_file_location, pre_boundary_file_location, pre_boundary_rule_file_location, post_boundary_file_location, post_boundary_rule_file_location, Charset.defaultCharset());
	}
	
	public static RuleSet newRuleSet(String name, URL basetokenizer_file_location, URL boundary_file_location, URL pre_boundary_file_location, URL pre_boundary_rule_file_location, URL post_boundary_file_location, URL post_boundary_rule_file_location, Charset cs){
		RuleSet r = RULE_SETS.get(name);
		if(r != null)
			return r;
		r = new RuleSet(name, basetokenizer_file_location,  boundary_file_location, pre_boundary_file_location, pre_boundary_rule_file_location, post_boundary_file_location, post_boundary_rule_file_location, cs);
		return r;
	}

	

}
