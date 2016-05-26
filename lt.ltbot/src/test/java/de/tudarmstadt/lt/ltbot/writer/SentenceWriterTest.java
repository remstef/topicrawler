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

import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * @author Steffen Remus
 *
 */
public class SentenceWriterTest {

	@Test
	public void test() {
		SentenceWriter w = new SentenceWriter(){{_sentence_maker = new SentenceMaker();}};
		w._current_stream = System.out;
		w.writeSentences(null, "This is a test. This is also a test.");
	}
	
	@Test
	public void testbean(){
		Resource xmlResource = new FileSystemResource("jobs/profile-ltbot-default-seedfile/profile-crawler-beans-ltbot.cxml");
		BeanFactory factory = new XmlBeanFactory(xmlResource);
		SentenceWriter w = (SentenceWriter)factory.getBean("sentenceWriterHtml");
		SentenceMaker m = (SentenceMaker)factory.getBean("sentenceMaker");

	}
	

}
