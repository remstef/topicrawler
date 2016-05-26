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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.modules.CrawlURI;
import org.archive.util.Recorder;

/**
 *
 * @author Steffen Remus
 **/
public class TextExtractor {

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final Logger LOG = Logger.getLogger(TextExtractor.class.getName());

	private HtmlTextExtractor _html;
	private UTF8Cleaner _clean;

	public String getUtf8HtmlText(CrawlURI curi) {
		// get the html text of this page and remove the boilerplate code
				Recorder recorder = curi.getRecorder();
				if (recorder == null) {
					LOG.severe(String.format("Something is very wrong here, I cannot get the content of that URI: '%s'.", curi.toString()));
					return "ERROR while getting content.";
				}
				try {
					InputStream in = recorder.getContentReplayInputStream();
					ByteArrayOutputStream b = new ByteArrayOutputStream(8192);
					byte[] buf = new byte[8192];
					for (int r = 0; (r = in.read(buf)) != -1;)
						b.write(buf, 0, r);
					ByteBuffer bb = ByteBuffer.wrap(b.toByteArray());
					String html_utf8 = new String(UTF8.encode(recorder.getCharset().decode(bb)).array(), UTF8);
					return html_utf8;
				} catch (Throwable t) {
					StringBuilder b = new StringBuilder();
					for (int i = 1; t != null && i < 10; i++) {
						String message = String.format("Failed to get content of URI: '%s'. (%d %s:%s)", curi.toString(), i, t.getClass().getSimpleName(), t.getMessage());
						b.append(message).append("\n");
						LOG.log(Level.SEVERE, message, t);
						t = t.getCause();
					}
					return b.toString();
				}
	}

	public String getCleanedUtf8HtmlText(CrawlURI curi) {
		String html_utf8 = getUtf8HtmlText(curi);
		String cleaned_html_utf8 = _clean.clean(html_utf8);
		return cleaned_html_utf8;
	}
	
	public String getUtf8PlainText(CrawlURI curi) {
		String html_utf8 = getUtf8HtmlText(curi);
		String cleaned_html_utf8 = _clean.clean(html_utf8);
		return cleaned_html_utf8;
	}

	public String getCleanedUtf8PlainText(CrawlURI curi) {
		String clean_html_utf8 = getCleanedUtf8HtmlText(curi);
		String plain_utf8 = _html.getPlaintext(clean_html_utf8);
		String cleaned_plain_text = _clean.clean(plain_utf8);
		return cleaned_plain_text;
	}

	public HtmlTextExtractor getHtmlTextExtractor() {
		return _html;
	}

	public void setHtmlTextExtractor(HtmlTextExtractor html_text_extractor) {
		_html = html_text_extractor;
		LOG.info(String.format("%s set to %s.", HtmlTextExtractor.class.getSimpleName(), html_text_extractor.getClass().getName()));
	}

	public UTF8Cleaner getUtf8Cleaner() {
		return _clean;
	}

	public void setUtf8Cleaner(UTF8Cleaner utf8_cleaner) {
		_clean = utf8_cleaner;
		LOG.info(String.format("%s set to %s.", UTF8Cleaner.class.getSimpleName(), utf8_cleaner.getClass().getName()));
	}

}
