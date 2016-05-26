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
package de.tudarmstadt.lt.lm.web.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.archive.crawler.framework.CrawlController;
import org.archive.modules.CrawlMetadata;
import org.archive.modules.CrawlURI;
import org.archive.modules.extractor.ExtractorHTML;
import org.archive.modules.fetcher.DefaultServerCache;
import org.archive.modules.fetcher.FetchHTTP;
import org.archive.modules.fetcher.SimpleCookieStorage;
import org.archive.modules.net.RobotsPolicy;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.util.Recorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.service.StringProviderMXBean;
import de.tudarmstadt.lt.ltbot.postprocessor.DecesiveValueProducerPerplexity;
import de.tudarmstadt.lt.ltbot.text.BoilerpipeTextExtractor;
import de.tudarmstadt.lt.ltbot.writer.SentenceMaker;



/**
 *
 * @author Steffen Remus
 **/
public class LanguageModelProviderServlet extends HttpServlet {

	private static final long serialVersionUID = -2072552736294683361L;

	private static Logger LOG = LoggerFactory.getLogger(LanguageModelProviderServlet.class);

	private static String _test_text = String.format("Hello brave new World.");

	private static String _html_header = String.format("<html>%n<head>%n<meta charset='UTF-8' />%n<meta http-equiv='Content-Type' content='text/html; charset=utf-8' />%n<style type='text/css'> p { margin:0 }</style>%n</head>%n<body>%n");

	private static String _html_footer = String.format("</body>%n</html>");

	private static String _host;
	private static int _port;

	private SortedMap<String, Integer> _lm_keys = new TreeMap<String, Integer>();
	private List<StringProviderMXBean> _lm_provider = new ArrayList<StringProviderMXBean>();
	private List<DecesiveValueProducerPerplexity> _perp_processors = new ArrayList<DecesiveValueProducerPerplexity>();

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		String port = config.getInitParameter("rmiport");
		_host = config.getInitParameter("rmihost");
		if (_host == null || port == null) {
			LOG.warn("rmihost or rmiport parameter not specified. Please specify correct parameters in web.xml:{}.", getClass().getSimpleName());
			if (_host == null)
				_host = "localhost";
			if (port == null)
				port = String.valueOf(Registry.REGISTRY_PORT);
		}
		try{
			_port = Integer.parseInt(port);
		} catch (NumberFormatException e) {
			LOG.warn("rmiport option value '{}' could not be parsed: {} {}. Please specify correct parameter in web.xml:{}.", port, e, getClass().getSimpleName(), e.getMessage(), getClass().getSimpleName());
			_port = Registry.REGISTRY_PORT;
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		resp.setContentType("text/html; charset=utf-8");
		PrintWriter w = new PrintWriter(resp.getWriter());
		String plaintext = req.getParameter("plaintext");
		String crawl_uri = req.getParameter("crawluri");
		String lm_key = req.getParameter("lm");
		String inputtype = req.getParameter("inputtype");
		boolean show_all_ngrams = req.getParameter("showall") != null;
		try {
			if (lm_key == null) // no parameter set
				show_available(w);
			else
				show(inputtype, lm_key, plaintext, crawl_uri, show_all_ngrams, w);
		} catch (Exception e) {
			w.format("Error %s: %s.", e.getClass().getSimpleName(), e.getMessage());
		}
		w.flush();
		w.close();
	}

	private void scanForLanguageModels() throws RemoteException, NotBoundException {
		Registry registry = LocateRegistry.getRegistry(_host, _port);
		Set<String> services = new HashSet<String>(Arrays.asList(registry.list()));
		if (services.size() == 0)
			System.out.println("no services available");
		for (String name : services) {
			//			if (_lm_keys.containsKey(name))
			//				continue;

			Remote r = registry.lookup(name);
			if (r instanceof StringProviderMXBean) {
				//				StringProviderMBean strprvdr = (StringProviderMBean) r;
				_lm_keys.put(name, _lm_keys.size());
				_lm_provider.add(null);
				_perp_processors.add(null);
			}
		}
	}

	private boolean connectToLanguageModel(String key) throws Exception {
		try {
			int index = _lm_keys.get(key);
			Registry registry = LocateRegistry.getRegistry(_host, _port);
			StringProviderMXBean lmprvdr = (StringProviderMXBean) registry.lookup(key);
			if (lmprvdr.getModelReady()) {
				_lm_provider.set(index, lmprvdr);
				return true;
			}

		} catch (RemoteException e) {
			LOG.error("Unable to connect to rmi registry on {}:{}. {}: {}.", _host, _port, e.getClass().getSimpleName(), e.getMessage());
		} catch (NotBoundException e) {
			LOG.error("Unable to connect to service {}. {}: {}.", key, e.getClass().getSimpleName(), e.getMessage());
		}
		return false;
	}

	private void show_available(PrintWriter w) throws RemoteException, NotBoundException, UnsupportedEncodingException {
		scanForLanguageModels();
		w.write(_html_header);
		w.format("<h3>%s</h3>%n", "Available Language Model Provider:");
		for (Entry<String, Integer> lmkey2index : _lm_keys.entrySet())
			w.format("<p><strong><a href='?lm=%s'>%s</a></strong></p>%n", URLEncoder.encode(lmkey2index.getKey(), "UTF-8"), lmkey2index.getKey());
		w.write(_html_footer);
	}

	private void show(String inputtype, String lm_key, String plaintext, String crawluri, boolean show_all_ngrams, PrintWriter w) throws Exception {
		if("uri".equals(inputtype))
			plaintext = null;
		if("text".equals(inputtype))
			crawluri = null;

		if (plaintext == null && crawluri == null) // no parameter set
			plaintext = _test_text;

		w.write(_html_header);
		final Integer lm_index = _lm_keys.get(lm_key);
		if (lm_index == null) {
			LOG.error("Language model '{}' unknown.", lm_key);
			w.format("<p>Language model '%s' unknown. Please go to <a href='?'>main page</a>.</p>", lm_key);
			return;
		}

		StringProviderMXBean s = _lm_provider.get(lm_index);
		if (s == null) {
			if (!connectToLanguageModel(lm_key)) {
				w.format("<p>Language Model is loading. Please try again later.</p>", lm_key);
				w.write(_html_footer);
				w.flush();
				return;
			}
			s = _lm_provider.get(lm_index);
		}

		w.format("<h3>%s</h3>%n", lm_key);

		double crawl_perp = -1d;

		if (crawluri != null) {
			Object[] triple = crawl_and_extract_plaintext(crawluri, lm_key, lm_index);
			plaintext = (String)triple[0];
			crawl_perp = (Double)triple[1];
		}
		else
			crawluri = "";

		w.format("<p>Crawl URI:</p>%n<p><form action='' method='get'><input type='text' name='crawluri' value='%s' size='100' />&nbsp;&nbsp;<input type='submit' value='Submit'><input type='hidden' name='inputtype' value='uri'><input type='hidden' name='lm' value='%s'><input type='hidden' name='action' value='show'></form></p>%n", crawluri, lm_key);
		w.format("<p>Crawler Perplexity = %012g</p>%n", crawl_perp);
		w.format("<p>Bytes Plaintext = %d</p>%n", plaintext.getBytes().length);
		w.format("<br />%n");
		w.format("<p>Plaintext:</p>%n<p><form action='' method='get'><textarea rows='20' cols='150' name='plaintext'>%s</textarea><p><input type='submit' value='Submit'></p><input type='hidden' name='inputtype' value='text'><input type='hidden' name='lm' value='%s'><input type='hidden' name='action' value='show'></form></p>%n", plaintext, lm_key);
		w.format("<p>Perplexity = %012g</p>%n", s.getPerplexity(plaintext, false));
		w.format("<br />%n");

		w.format("<tt>%n");
		List<String>[] ngram_sequence = s.getNgrams(plaintext);
		w.format("+++ #ngrams= %d +++ <br /><br />%n", ngram_sequence.length);

		// TODO: provide ngrams
		for (int i = 0; i < ngram_sequence.length && (i <= 500 || show_all_ngrams); i++) {
			List<String> ngram = ngram_sequence[i];
			double log10_prob = s.getNgramLog10Probability(ngram);
			double prob10 = Math.pow(10, log10_prob);
			double log2_prob = log10_prob / Math.log10(2);
			int[] ngram_ids = s.getNgramAsIds(ngram);
			List<String> ngram_lm = s.getNgramAsWords(ngram_ids);
			w.format("%s <br />%n &nbsp;=> %s <br />%n &nbsp;= %012g (log_2 = %012g)<br /><br />%n",
					StringEscapeUtils.escapeHtml(ngram.toString()),
					StringEscapeUtils.escapeHtml(ngram_lm.toString()),
					prob10,
					log2_prob);
		}
		if (ngram_sequence.length > 500 && !show_all_ngrams)
			w.format("...%n");
		w.format("<br />%n");
		w.format("</tt>%n");

		w.write(_html_footer);

	}


	private Object[] crawl_and_extract_plaintext(String crawluri, String lm_key, int lm_index) throws Exception {
		FetchHTTP _fetchHttp;
		ExtractorHTML _extractor_html;
		Recorder _httpRecorder;
		CrawlController _controller;

		// set up heritrix
		File tmpfile = File.createTempFile("ltbot.", ".tmpdir");
		if (tmpfile.exists())
			tmpfile.delete();
		File tmpdir = tmpfile;
		tmpdir.mkdir();
		System.out.format("Path to temporary directory: '%s'.", tmpdir);

		_httpRecorder = new Recorder(tmpdir, LanguageModelProviderServlet.class.getSimpleName(), 16 * 1024, 512 * 1024);
		Recorder.setHttpRecorder(_httpRecorder);

		_fetchHttp = new FetchHTTP();
		_fetchHttp.setCookieStorage(new SimpleCookieStorage());
		_fetchHttp.setServerCache(new DefaultServerCache());
		CrawlMetadata uap = new CrawlMetadata();
		uap.setUserAgentTemplate(LanguageModelProviderServlet.class.getSimpleName());
		uap.setAvailableRobotsPolicies(RobotsPolicy.STANDARD_POLICIES);
		uap.setRobotsPolicyName("obey");
		_fetchHttp.setUserAgentProvider(uap);
		_fetchHttp.start();

		_extractor_html = new ExtractorHTML();
		_extractor_html.setMetadata(uap);
		_extractor_html.setExtractJavascript(false);
		_extractor_html.afterPropertiesSet();
		_extractor_html.start();

		_controller = new CrawlController();

		if (_perp_processors.get(lm_index) == null) {
			// setup priority post processor
			DecesiveValueProducerPerplexity pm = new DecesiveValueProducerPerplexity();
			pm.setRmihost(_host);
			pm.setRmiport(_port);
			pm.setServiceID(lm_key);
			//pm.getTextExtractor().setUtf8Cleaner(new UTF8CleanerExt());
			pm.setSentenceMaker(new SentenceMaker());
			pm.getTextExtractor().setHtmlTextExtractor(new BoilerpipeTextExtractor());
			pm.setCrawlController(_controller);
			pm.start();
			_perp_processors.set(lm_index, pm);
		}

		UURI uuri = UURIFactory.getInstance(crawluri);
		CrawlURI curi = new CrawlURI(uuri);
		curi.setSeed(true);
		curi.setRecorder(_httpRecorder);

		_fetchHttp.process(curi);
		_extractor_html.process(curi);

		DecesiveValueProducerPerplexity p = _perp_processors.get(lm_index);

		p.process(curi);
		System.out.println(curi);
		System.out.println("Heritrix FetchStatus: " + curi.getFetchStatus());
		System.out.println(p.report());

		Double perp = Double.valueOf(curi.getExtraInfo().getString(p.getExtraInfoValueFieldName()));
		String plaintext = p.getTextExtractor().getCleanedUtf8PlainText(curi);

		// p.stop();
		_fetchHttp.stop();
		_extractor_html.stop();

		return new Object[] { plaintext, perp };
	}

}
