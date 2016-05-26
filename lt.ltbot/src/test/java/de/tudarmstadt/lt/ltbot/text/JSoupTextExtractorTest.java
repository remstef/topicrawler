/*
 *   Copyright 2013
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
import org.junit.Test;

import de.tudarmstadt.lt.ltbot.text.HtmlTextExtractor;
import de.tudarmstadt.lt.ltbot.text.JSoupTextExtractor;

/**
 *
 * @author Steffen Remus
 */
public class JSoupTextExtractorTest {

	@Test
	public void test() throws IOException {
		// Document doc = Jsoup.connect("http://bildungsserver.de/").get();
		// Document doc = Jsoup.connect("http://schoolcomputing.wikia.com/wiki/WindERP,_School_ERP_Software,_School_Management_System_,School_Management_Software?action=edit&section=4").get();
		Document doc = Jsoup.connect("http://www.jil.go.jp/index.htm").get();
		System.out.println(doc.outputSettings().charset());

		String html = doc.toString();
		System.out.println(html);

		HtmlTextExtractor extr = new JSoupTextExtractor();
		String plain = extr.getPlaintext(html);

		System.out.println(plain);
		System.out.println(plain.length());

		System.out.println(doc.text());


	}





}
