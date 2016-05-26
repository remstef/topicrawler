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
package de.tudarmstadt.lt.ltbot.postprocessor;

import static de.tudarmstadt.lt.ltbot.postprocessor.SharedConstants.EXTRA_INFO_ASSIGNED_COST_PRECEDENCE;
import static de.tudarmstadt.lt.ltbot.postprocessor.SharedConstants.EXTRA_INFO_ASSIGNED_SCHEDULING_DIRECTIVE;
import static de.tudarmstadt.lt.ltbot.postprocessor.SharedConstants.EXTRA_INFO_PLAINTEXT_ABBREVIATED;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.archive.crawler.framework.Engine;
import org.archive.modules.CrawlURI;
import org.archive.modules.Processor;
import org.archive.spring.ConfigPath;
import org.json.JSONObject;

/**
 *
 * @author Steffen Remus
 **/
public class DecesiveValueLogger2 extends Processor {

	private final static Logger LOG = Logger.getLogger(DecesiveValueLogger2.class.getName());

	protected Writer _w;
	protected ConfigPath path = new ConfigPath(Engine.LOGS_DIR_NAME, "${launchId}/logs");

	public DecesiveValueLogger2() {
		setExtraInfoValueFieldName(SharedConstants.EXTRA_INFO_PERPLEXITY);
		setLogfile("post-document-priorities.log");
	}

	public ConfigPath getPath() {
		return path;
	}

	public void setPath(ConfigPath cp) {
		this.path.merge(cp);
	}

	public String getLogfile() {
		return (String) kp.get("priorities-logfile");
	}

	public void setLogfile(String filename) {
		kp.put("priorities-logfile", filename);
	}

	public String getExtraInfoValueFieldName() {
		return (String) kp.get("ExtraInfoFieldName");
	}

	public void setExtraInfoValueFieldName(String extraInfoFieldName) {
		kp.put("ExtraInfoFieldName", extraInfoFieldName);
	}

	@Override
	public void start() {
		initWriter();
		super.start();
	}

	@Override
	public void stop() {
		try{
			_w.close();
		} catch (IOException e) {
			LOG.warning("Failed to gracefully close FileWriter.");
		}
		super.stop();
	}

	public void initWriter() {
		synchronized (this) {
			// close old writer if one exists
			if (_w != null) try {
				_w.close();
			} catch (IOException e) {
				LOG.warning("Failed to gracefully close FileWriter.");
			}
			// init new writer
			String filename = getLogfile();
			File f = new File(path.getFile(), filename);
			for (int i = 0; (f.exists() && f.length() > 0); f = new File(path.getFile(), ++i + "." + filename));
			kp.put("priorities-logfile", f.getName());
			try {
				_w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
			} catch (IOException e) {
				LOG.severe("Start cancelled due to errors. See log for more information.");
				throw new RuntimeException(String.format("[%s] Could not initialize FileWriter for file '%s'.", getClass().getName(), f.getAbsolutePath()));
			}
		}
	}

	@Override
	protected boolean shouldProcess(CrawlURI uri) {
		return true;
	}

	@Override
	protected void innerProcess(CrawlURI uri) throws InterruptedException {
		LOG.finest(String.format("[%s (%s): %s via %s]: %s; %s;", getBeanName(), getExtraInfoValueFieldName(), uri, uri.getVia(), uri.getExtraInfo(), uri.getFullVia() != null ? uri.getFullVia().getExtraInfo() : "{}"));
		JSONObject info = uri.getExtraInfo();
		try {
			synchronized (_w) {
				if (!(info.has(EXTRA_INFO_PLAINTEXT_ABBREVIATED) || uri.getVia() != null))
					return;/* DO NOT WRITE ANYTHING */
				if (info.has(EXTRA_INFO_PLAINTEXT_ABBREVIATED))
					/* decision value was computed on the current document */
					writeInformationFromCurrentURI(uri, info);
				else
					/* decision value was computed on the preceding document */
					writeInformationFromPrecedingURI(uri);
				_w.flush();
			}
		} catch (Throwable t) {
			for (int i = 1; t != null && i < 10; i++) {
				LOG.log(Level.WARNING, String.format("Failed to write decesive value from extra info to file (%d-%s:%s).", i, t.getClass().getName(), t.getMessage()), t);
				t = t.getCause();
			}
		}
	}

	private void writeInformationFromPrecedingURI(CrawlURI uri) throws Throwable {
		CrawlURI via_uri = uri.getFullVia();
		if(via_uri == null)
			return; // skip this uri
		JSONObject uri_info = uri.getExtraInfo();
		JSONObject via_info = via_uri.getExtraInfo();
		if (via_info.length() == 0)
			return; // skip this uri

		if (!via_info.has(getExtraInfoValueFieldName()))
			return;/* DO NOT WRITE ANYTHING */

		long timestamp = System.currentTimeMillis();
		String value_as_str = via_info.getString(getExtraInfoValueFieldName());
		String via_url_description = String.format("via %s", via_uri);
		int actual_scheduling_directive = uri.getSchedulingDirective();
		int actual_precedence = uri.getPrecedence();
		String assigned_scheduling_directive = uri_info.has(EXTRA_INFO_ASSIGNED_SCHEDULING_DIRECTIVE) ? uri_info.get(EXTRA_INFO_ASSIGNED_SCHEDULING_DIRECTIVE).toString() : "_";
		String assigned_precedence = uri_info.has(EXTRA_INFO_ASSIGNED_COST_PRECEDENCE) ? uri_info.get(EXTRA_INFO_ASSIGNED_COST_PRECEDENCE).toString() : "_";

		_w.write(String.format("%015d\t%s\t%d(%s)\t%d(%s)\t%s\t[%-50s]%n", timestamp, value_as_str, actual_scheduling_directive, assigned_scheduling_directive, actual_precedence, assigned_precedence, uri.toString(), via_url_description));
	}

	private void writeInformationFromCurrentURI(CrawlURI uri, JSONObject info) throws Throwable {
		if (!info.has(getExtraInfoValueFieldName()))
			return;/* NOTHING TO WRITE */

		String value_as_str = info.getString(getExtraInfoValueFieldName());
		long timestamp = System.currentTimeMillis();
		String abbr_text = null;
		abbr_text = info.getString(EXTRA_INFO_PLAINTEXT_ABBREVIATED);
		String current_scheduling_directive = String.valueOf(uri.getSchedulingDirective());
		String current_precedence = String.valueOf(uri.getPrecedence());

		_w.write(String.format("%015d\t%s\t%s\t%s\t%s\t[%-50s]%n", timestamp, value_as_str, current_scheduling_directive, current_precedence, uri.toString(), abbr_text));

	}

}
