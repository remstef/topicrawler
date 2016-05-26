/*
 *   Copyright 2012
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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 *
 * @author Steffen Remus
 **/
public class JSoupTextExtractor implements HtmlTextExtractor {

	private final static Logger LOG = Logger.getLogger(JSoupTextExtractor.class.getName());

	private final static Pattern _end_prgrph_ptrn = Pattern.compile("(?i)</p>");
	private final static Pattern _nwln_ptrn = Pattern.compile("\r?\n");
	private final static Pattern _tmp_nwln_ptrn = Pattern.compile("br2nl");
	private final static Pattern _emptln_ptrn = Pattern.compile("(?m)\\s+$");


	@Override
	public String getPlaintext(final String htmltext) {
		try {
			// preserve newlines
			// html = html.replaceAll("(?i)<br[^>]*>", "br2nl"); // <br>s are often just inserted for style
			String hhtmltext = _end_prgrph_ptrn.matcher(htmltext).replaceAll("</p>br2nl");
			hhtmltext = _nwln_ptrn.matcher(hhtmltext).replaceAll("br2nl");

			Document soup = Jsoup.parse(hhtmltext);
			String plaintext = soup.text();

			plaintext = _tmp_nwln_ptrn.matcher(plaintext).replaceAll("\n");
			plaintext = _emptln_ptrn.matcher(plaintext.trim()).replaceAll("");

			return plaintext;
		} catch (Throwable t) {
			for (int i = 1; t != null && i < 10; i++) {
				LOG.log(Level.SEVERE, String.format("Failed to get plaintext from while '%s' (%d %s:%s).", StringUtils.abbreviate(htmltext, 100), i, t.getClass().getName(), t.getMessage()), t);
				t = t.getCause();
			}
			return "Failed to get plaintext content \n" + htmltext;
		}
	}

}
