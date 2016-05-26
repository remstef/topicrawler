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

import java.io.File;
import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.archive.modules.CrawlMetadata;
import org.archive.modules.CrawlURI;
import org.archive.modules.extractor.ExtractorHTML;
import org.archive.modules.extractor.Link;
import org.archive.modules.fetcher.DefaultServerCache;
import org.archive.modules.fetcher.FetchHTTP;
import org.archive.modules.fetcher.SimpleCookieStorage;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.Recorder;
import org.junit.After;

/**
 *
 * @author Steffen Remus
 **/
public class ProcessorTestbase {

	static FetchHTTP _fetchHttp;
	static ExtractorHTML _extractor_html;
	static Recorder _httpRecorder;

	static Registry _registry;

	static String _serviceId = "testservice";
	static String _host = "localhost";
	static int _rmiport = 1099;
	static int _serviceport = 0; // automatic

	public void setup() throws Exception {
		setupHeritrix();
		// setup rmi registry

		_registry = LocateRegistry.createRegistry(_rmiport);
	}

	@After
	public void tearDown() throws AccessException, RemoteException, NotBoundException {
		_fetchHttp.stop();
		_extractor_html.stop();
		_registry.unbind(_serviceId);
		UnicastRemoteObject.unexportObject(_registry, true);
	}

	private static void setupHeritrix() throws IOException {
		File tmpfile = File.createTempFile("ltbot.", ".tmpdir");
		if (tmpfile.exists())
			tmpfile.delete();
		File tmpdir = tmpfile;
		tmpdir.mkdir();
		System.out.format("Path to temporary directory: '%s'.", tmpdir);

		_httpRecorder = new Recorder(tmpdir, ProcessorTestbase.class.getSimpleName(), 16 * 1024, 512 * 1024);
		Recorder.setHttpRecorder(_httpRecorder);

		_fetchHttp = new FetchHTTP();
		_fetchHttp.setCookieStorage(new SimpleCookieStorage());
		_fetchHttp.setServerCache(new DefaultServerCache());
		CrawlMetadata uap = new CrawlMetadata();
		uap.setUserAgentTemplate(ProcessorTestbase.class.getSimpleName());
		_fetchHttp.setUserAgentProvider(uap);
		_fetchHttp.start();

		_extractor_html = new ExtractorHTML();
		_extractor_html.setExtractJavascript(false);
		_extractor_html.afterPropertiesSet();
		_extractor_html.start();

	}

	public CrawlURI crawlURI(String uri) throws InterruptedException, IOException {

		UURI uuri = UURIFactory.getInstance(uri);

		CrawlURI curi = new CrawlURI(uuri);
		curi.setSeed(true);
		curi.setRecorder(_httpRecorder);

		_fetchHttp.process(curi);
		_extractor_html.process(curi);

		for (Link out : curi.getOutLinks()) {
			CrawlURI candidate = curi.createCrawlURI(curi.getBaseURI(), out);
			curi.getOutCandidates().add(candidate);
		}

		return curi;
	}
}
