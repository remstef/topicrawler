package de.tudarmstadt.lt.seg.sentence.rules;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tudarmstadt.lt.seg.SegmentationUtils;


public class PreBoundaryRulesProcessor implements BoundaryProcessor {

	Map<Pattern, Boolean> _patterns_and_decisions = Collections.emptyMap();
	
	static {
		try{
			DEFAULT = new PreBoundaryRulesProcessor();
		}catch(Exception e){
			throw new IllegalStateException();
		}
	}
	
	public static final PreBoundaryRulesProcessor DEFAULT;

	private PreBoundaryRulesProcessor() throws Exception {
		this(Thread.currentThread().getContextClassLoader().getResource("rulesets/sentence/default/preBoundaryRules.txt"), Charset.forName("UTF-8"));
	}

	public PreBoundaryRulesProcessor(URL boundary_file_location, Charset cs) throws Exception {
		this(new InputStreamReader(boundary_file_location.openStream(), cs));
	}

	public PreBoundaryRulesProcessor(InputStreamReader r) throws Exception {
		_patterns_and_decisions = new HashMap<>();
		final BufferedReader br = new BufferedReader(r);
		for(String line = null; (line = br.readLine()) != null;){
			if(line.isEmpty())
				continue;

			if (line.startsWith("#"))
				continue;

			String decision = line.substring(0,2); // '+ ' or '- '
			if( !(decision.equals("- ") || decision.equals("+ ")) ){
				System.err.format("Unable to parse line '%s' in postBoundaryRules-file. Please specify a decision using '+ '/'- ' in at the beginning of a line in front of the pattern.");
				continue;
			}

			try{
				Pattern pattern = Pattern.compile(line.substring(2));
				_patterns_and_decisions.put(pattern, decision.equals("+ "));
			}catch(Exception e){
				e.printStackTrace();
				continue;
			}
		}
	}

	@Override
	public boolean isCompleteSentence(String text) {
		if(text.length() < 1)
			return true;
			
		// rtrim
		int end_suffix = text.length(); 
		while(end_suffix > 0 && SegmentationUtils.charIsEmptySpace(text.codePointAt(--end_suffix)));
		
		// try to find the last token, approximate heuristically by using empty spaces
		int begin_suffix;
		for(begin_suffix = end_suffix; begin_suffix >= 0; --begin_suffix)
			if(SegmentationUtils.charIsEmptySpace(text.codePointAt(begin_suffix)))
				break;
		begin_suffix += 1;
		end_suffix += 1;
		String last_token = text.substring(begin_suffix, end_suffix); 
		
		boolean result = true;
		for (final Entry<Pattern, Boolean> entry : _patterns_and_decisions.entrySet()) {
			Matcher matcher = entry.getKey().matcher(last_token);
			if (matcher.matches()) 
				result &= entry.getValue();			
		}
		return result;
	}

	@Override
	public boolean isIncompleteSentence(String text) {
		return !isCompleteSentence(text);
	}

}
