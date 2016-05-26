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

import java.util.List;
import java.util.stream.Stream;

/**
 * @author Steffen Remus
 *
 */
public class SentenceMaker {
	
	SentenceMakerJava8 _s = new SentenceMakerJava8();
	
	
	public synchronized int getMinLength(){
		return _s.getMinLength();
	}
	public synchronized void setMinLength(int min_lengt){
		_s.setMinLength(min_lengt);
	}
	
	public synchronized String getTargetLanguageCode(){
		return _s.getTargetLanguageCode();
	}
	public synchronized void setTargetLanguageCode(String target_language_code){
		_s.setTargetLanguageCode(target_language_code);
	}
	
	public synchronized Stream<String> getSentencesStream(String text){
		return _s.getSentencesStream(text);
	}
	
	public synchronized Stream<String> getSentencesStream(String text, String languagecode){
		return _s.getSentencesStream(text, languagecode);
	}
	
	public synchronized List<String> getSentences(String text){
		return _s.getSentences(text);
	}
	public synchronized List<String> getSentences(String text, String languagecode){
		return _s.getSentences(text, languagecode);
	}
	
}
