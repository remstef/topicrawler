package de.tudarmstadt.lt.seg.sentence.rules;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.tudarmstadt.lt.seg.SegmentationUtils;

public class PostBoundaryListProcessor implements BoundaryProcessor {
	
	Set<String> _exception = Collections.emptySet();
	int _longest_exception = 0;
	int _shortest_exception = 0;

	static {
		try{
			DEFAULT = new PostBoundaryListProcessor();
		}catch(Exception e){
			throw new IllegalStateException();
		}
	}
	
	public static final PostBoundaryListProcessor DEFAULT;
	
	private PostBoundaryListProcessor() throws Exception {
		this(Thread.currentThread().getContextClassLoader().getResource("rulesets/sentence/default/postBoundaryExceptions.txt"), Charset.forName("UTF-8"));
	}
	
	public PostBoundaryListProcessor(URL boundary_file_location, Charset cs) throws Exception {
		this(new InputStreamReader(boundary_file_location.openStream(), cs));
	}
	
	public PostBoundaryListProcessor(InputStreamReader r) throws Exception {
		_exception = new HashSet<>();
		_shortest_exception = Integer.MAX_VALUE;
		final BufferedReader br = new BufferedReader(r);
		for(String line = null; (line = br.readLine()) != null;){
			if(!line.isEmpty() && !line.startsWith("#")){
				String conv = SegmentationUtils.convert(line);
				_longest_exception = Math.max(_longest_exception, conv.length());
				_shortest_exception = Math.min(_shortest_exception, conv.length());
				_exception.add(conv);
			}
		}
		if(_exception.isEmpty())
			_shortest_exception = 0;
	}

	public boolean isIncompleteSentence(String candidate_after_boundary) { 
		return _exception.contains(candidate_after_boundary);
	}

	@Override
	public boolean isCompleteSentence(String candidate_after_boundary) {
		return !isIncompleteSentence(candidate_after_boundary);
	}

}
