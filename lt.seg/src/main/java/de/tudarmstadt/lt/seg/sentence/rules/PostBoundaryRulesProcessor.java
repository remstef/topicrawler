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

public class PostBoundaryRulesProcessor implements BoundaryProcessor {
	
	Map<Pattern, Boolean> _patterns_and_decisions = Collections.emptyMap();
	
	static {
		try{
			DEFAULT = new PostBoundaryRulesProcessor();
		}catch(Exception e){
			throw new IllegalStateException();
		}
	}
	
	public static final PostBoundaryRulesProcessor DEFAULT;

	private PostBoundaryRulesProcessor() throws Exception {
		this(Thread.currentThread().getContextClassLoader().getResource("rulesets/sentence/default/postBoundaryRules.txt"), Charset.forName("UTF-8"));
	}

	public PostBoundaryRulesProcessor(URL boundary_file_location, Charset cs) throws Exception {
		this(new InputStreamReader(boundary_file_location.openStream(), cs));
	}

	public PostBoundaryRulesProcessor(InputStreamReader r) throws Exception {
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
		// try to find the first token, approximate heuristically by using emptyspaces TODO: use a sophisticated ITokenizer e.g. DiffTokenizer
		int first_token_end;
		for(first_token_end = 1; first_token_end < text.length(); first_token_end++)
			if(SegmentationUtils.charIsEmptySpace(text.codePointAt(first_token_end)))
				break;
		String first_token = text.substring(0,first_token_end); 

		boolean result = true;
		for (final Entry<Pattern, Boolean> entry : _patterns_and_decisions.entrySet()) {
			Matcher matcher = entry.getKey().matcher(first_token);
			if (matcher.matches()) 
				result &= entry.getValue();
		}
		return result;
	}
	
	@Override
	public boolean isIncompleteSentence(String text) {
		return isCompleteSentence(text);
	}
}

