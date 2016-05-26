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

import org.apache.commons.lang.StringUtils;

import de.l3s.boilerpipe.extractors.ArticleExtractor;

/**
 *
 * @author Steffen Remus
 **/
public class BoilerpipeTextExtractor implements HtmlTextExtractor {

	private final static Logger LOG = Logger.getLogger(BoilerpipeTextExtractor.class.getName());

	@Override
	public String getPlaintext(final String htmltext) {
		try {
			String plaintext = ArticleExtractor.getInstance().getText(htmltext);
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
