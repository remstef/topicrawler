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
package de.tudarmstadt.lt.ltbot.text;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.BeforeClass;
import org.junit.Test;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.DefaultExtractor;

/**
 * @author Steffen Remus
 *
 */
public class BoilerplateTests {
	
	static String _html;
	
	@BeforeClass
	public static void setupClass() throws IOException{
		String url = "http://www.spiegel.de"; //"http://www.dipf.de/de/dipf-aktuell/aktuelles"; // "http://www.jil.go.jp/index.htm";
		Document doc = Jsoup.connect(url).get();
//		System.out.println(doc.outputSettings().charset());
		_html = doc.toString();
//		System.out.println(_html);
		
	}

	
	@Test
	public void testBoilerpipe() throws IOException, BoilerpipeProcessingException{
		String plain = ArticleExtractor.getInstance().getText(_html);
		System.out.println("=== BEGIN BOILERPIPE OUTPUT 1 ===");
		System.out.println(plain);
		System.out.println("=== END BOILERPIPE OUTPUT 1 ===");
		
		plain = DefaultExtractor.getInstance().getText(_html);
		System.out.println("=== BEGIN BOILERPIPE OUTPUT 2 ===");
		System.out.println(plain);
		System.out.println("=== END BOILERPIPE OUTPUT 2 ===");
	}
	
	
	@Test
	public void testJSoup(){
		Document soup = Jsoup.parse(_html);
		String plain = soup.text();
		System.out.println("=== BEGIN JSOUP OUTPUT ===");
		System.out.println(plain);
		System.out.println("=== END JSOUP OUTPUT ===");
	}

}
