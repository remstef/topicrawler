package de.tudarmstadt.lt.seg.sentence.rules;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.tudarmstadt.lt.seg.SegmentationUtils;

public class PreBoundaryListProcessor implements BoundaryProcessor {

	Set<String> _reversed_exceptions = Collections.emptySet();
	int _longest_exception = 0;
	int _shortest_exception = 0;
	
	static {
		try{
			DEFAULT = new PreBoundaryListProcessor();
		}catch(Exception e){
			throw new IllegalStateException();
		}
	}
	
	public static final PreBoundaryListProcessor DEFAULT;
	
	private PreBoundaryListProcessor() throws Exception {
		this(Thread.currentThread().getContextClassLoader().getResource("rulesets/sentence/default/preBoundaryExceptions.txt"), Charset.forName("UTF-8"));
	}
	
	public PreBoundaryListProcessor(URL boundary_file_location, Charset cs) throws Exception {
		this(new InputStreamReader(boundary_file_location.openStream(), cs));
	}
	
	public PreBoundaryListProcessor(InputStreamReader r) throws Exception {
		_reversed_exceptions = new HashSet<>();
		_shortest_exception = Integer.MAX_VALUE;
		final BufferedReader br = new BufferedReader(r);
		for(String line = null; (line = br.readLine()) != null;){
			if(!line.isEmpty() && !line.startsWith("#")){
				String conv = SegmentationUtils.convert(line);
				_longest_exception = Math.max(_longest_exception, conv.length());
				_shortest_exception = Math.min(_shortest_exception, conv.length());
				_reversed_exceptions.add(SegmentationUtils.reversed(conv));
			}
		}
		if(_reversed_exceptions.isEmpty())
			_shortest_exception = 0;
	}

	public boolean isIncompleteSentence(String text) {
		// rtrim
		int end_suffix = text.length(); 
		while(end_suffix > 0 && SegmentationUtils.charIsEmptySpace(text.codePointAt(--end_suffix)));
		
		// try to find the last token in the text before the boundary was found, approximate heuristically by using empty spaces
		int begin_suffix;
		for(begin_suffix = end_suffix; begin_suffix >= 0; --begin_suffix)
			if(SegmentationUtils.charIsEmptySpace(text.codePointAt(begin_suffix)))
				break;
		begin_suffix += 1;
		end_suffix += 1;
		String last_token = text.substring(begin_suffix, end_suffix); 
		
		String rev_suffix = SegmentationUtils.reversed(last_token);
		return _reversed_exceptions.contains(rev_suffix);
	}

	@Override
	public boolean isCompleteSentence(String candidate) {
		return !isIncompleteSentence(candidate);
	}
}
