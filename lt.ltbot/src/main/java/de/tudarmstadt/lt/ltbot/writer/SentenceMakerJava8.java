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
package de.tudarmstadt.lt.ltbot.writer;

import java.io.StringReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.tudarmstadt.lt.seg.Segment;
import de.tudarmstadt.lt.seg.SegmentType;
import de.tudarmstadt.lt.seg.sentence.ISentenceSplitter;
import de.tudarmstadt.lt.seg.sentence.LineSplitter;
import de.tudarmstadt.lt.seg.sentence.RuleSplitter;
import de.tudarmstadt.lt.seg.token.DiffTokenizer;
import de.tudarmstadt.lt.seg.token.ITokenizer;

/**
 * @author Steffen Remus
 * 
 * stupid jetty6 cannot handle java8, thus I need a wrapper around it. See <code>SentenceMaker</code>
 *  
 */
class SentenceMakerJava8 {

	/**
	 * use ThreadLocal because DiffTokenizer and RuleSplitter are not thread safe
	 */
	ThreadLocal<ITokenizer> _tokenizer = ThreadLocal.withInitial(DiffTokenizer::new);
	ThreadLocal<RuleSplitter> _rule_splitter = ThreadLocal.withInitial(RuleSplitter::new);
	ThreadLocal<ISentenceSplitter> _line_splitter = ThreadLocal.withInitial(LineSplitter::new);


	protected int _min_length = 1;
	public int getMinLength(){
		return _min_length;
	}
	public void setMinLength(int min_length){
		_min_length = min_length;
	}

	protected String _target_language_code = "default";
	public String getTargetLanguageCode(){
		return _target_language_code;
	}
	public void setTargetLanguageCode(String target_language_code){
		_target_language_code = target_language_code;
	}

	public Stream<String> getSentencesStream(String text){
		return getSentencesStream(text, _target_language_code);
	}

	public Stream<String> getSentencesStream(String text, String languagecode){
		return _line_splitter.get().init(new StringReader(text)).stream().filter(s -> s.type == SegmentType.SENTENCE).map(Segment::asString).flatMap(line -> {
			return _rule_splitter.get().init(new StringReader(line), languagecode).stream().filter(s -> s.type == SegmentType.SENTENCE).sequential().map(s -> {
				final AtomicInteger c = new AtomicInteger();
				String r = _tokenizer.get().init(s.asString()).stream().sequential().map(t -> {
					if(t.isWord())
						c.incrementAndGet();
					if(t.type == SegmentType.EMPTY_SPACE)
						return " ";
					if(t.isReadable())
						return t.asString();
					return "";
				}).collect(Collectors.joining()).trim();
				if(c.get() < _min_length)
					return "";
				return r;
			}).filter(s -> !s.isEmpty());
		});
	}

	public List<String> getSentences(String text){
		return getSentences(text, _target_language_code);
	}
	public List<String> getSentences(String text, String languagecode){
		return getSentencesStream(text, languagecode).collect(Collectors.toList());
	}

}
