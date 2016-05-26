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

import static de.tudarmstadt.lt.ltbot.postprocessor.SharedConstants.EXTRA_INFO_PERPLEXITY;
import static de.tudarmstadt.lt.ltbot.postprocessor.SharedConstants.EXTRA_INFO_PLAINTEXT_ABBREVIATED;

import java.rmi.registry.Registry;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.archive.crawler.framework.CrawlController;
import org.archive.modules.CrawlURI;
import org.archive.modules.Processor;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.lt.lm.perplexity.ModelPerplexity;
import de.tudarmstadt.lt.lm.service.AbstractStringProvider;
import de.tudarmstadt.lt.lm.service.StringProviderMXBean;
import de.tudarmstadt.lt.ltbot.text.JSoupTextExtractor;
import de.tudarmstadt.lt.ltbot.text.TextExtractor;
import de.tudarmstadt.lt.ltbot.text.UTF8CleanerMin;
import de.tudarmstadt.lt.ltbot.writer.SentenceMaker;
import de.tudarmstadt.lt.utilities.collections.FixedSizeFifoLinkedList;


/**
 *
 * @author Steffen Remus
 *
 */
public class DecesiveValueProducerPerplexity extends Processor {

	protected final static Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("\\s+");
	private final static Logger LOG = Logger.getLogger(DecesiveValueProducerPerplexity.class.getName());

	protected TextExtractor _textExtractorInstance;
	protected SentenceMaker _sentenceMakerInstance;

	protected ThreadLocal<StringProviderMXBean> _lmprvdr = null;
	protected Object _lck = new Object();

	protected boolean _paused_due_to_error = false;

	protected double _last_assigned_perplexity = 0d;
	protected FixedSizeFifoLinkedList<Double> _last100PerplexityVaulues = new FixedSizeFifoLinkedList<Double>(100);
	protected CrawlURI _last_processed_uri = null;
	protected final Set<CrawlURI> _currently_processed_uris = Collections.newSetFromMap(new ConcurrentHashMap<CrawlURI, Boolean>());

	protected double _perplexity_min = Integer.MAX_VALUE;
	protected double _perplexity_max = Integer.MIN_VALUE;
	protected double _perplexity_avg = 0d;
	protected double _num_values = 0d;
	protected AtomicLong _num_inf_values = new AtomicLong();

	public DecesiveValueProducerPerplexity() {
		setServiceID("defaultlm");
		setRmihost("localhost");
		setRmiport(Registry.REGISTRY_PORT);

		TextExtractor extractor = new TextExtractor();
		extractor.setUtf8Cleaner(new UTF8CleanerMin());
		extractor.setHtmlTextExtractor(new JSoupTextExtractor());
		setTextExtractor(extractor);

		setExtraInfoValueFieldName(EXTRA_INFO_PERPLEXITY);
	}

	@Autowired
	public void setCrawlController(CrawlController crawlcontroller) {
		getKeyedProperties().put("crawlcontroller", crawlcontroller);
	}
	public CrawlController getCrawlController() {
		return (CrawlController) getKeyedProperties().get("crawlcontroller");
	}

	String test = "hello world";
	public void setTest(String test) {
		start();
		this.test = test;
	}
	public String getTest() {
		return test;
	}

	public String getServiceID() {
		return (String) getKeyedProperties().get("serviceId");
	}

	public void setServiceID(String language_model_id) {
		getKeyedProperties().put("serviceId", language_model_id);
	}

	public String getRmihost() {
		return (String) getKeyedProperties().get("rmihost");
	}

	public void setRmihost(String rmihost) {
		getKeyedProperties().put("rmihost", rmihost);
	}

	public int getRmiport() {
		return (Integer) getKeyedProperties().get("rmiport");
	}

	public void setRmiport(int rmiport) {
		getKeyedProperties().put("rmiport", rmiport);
	}

	public TextExtractor getTextExtractor() {
		return _textExtractorInstance;
	}

	public void setTextExtractor(TextExtractor text_extractor) {
		_textExtractorInstance = text_extractor;
	}

	public SentenceMaker getSentenceMaker() {
		return _sentenceMakerInstance;
	}

	public void setSentenceMaker(SentenceMaker sentenceMakerInstance) {
		_sentenceMakerInstance = sentenceMakerInstance;
	}

	public String getExtraInfoValueFieldName() {
		return (String) getKeyedProperties().get("ExtraInfoFieldName");
	}

	public void setExtraInfoValueFieldName(String extraInfoFieldName) {
		getKeyedProperties().put("ExtraInfoFieldName", extraInfoFieldName);
	}

	public double computePerplexity(String text) throws Exception{
		ModelPerplexity<String> perp = new ModelPerplexity<String>(_lmprvdr.get());
		for(String sentence : _sentenceMakerInstance.getSentences(text)){
			List<String>[] ngrams = _lmprvdr.get().getNgrams(sentence);
			if(ngrams.length <= 1) // at least 2 ngrams
				continue;
			if(ngrams[ngrams.length-1].size() < _lmprvdr.get().getLmOrder()) // at least one ngram with cardinality of lm
				continue;
			for(List<String> ngram : ngrams)
				perp.addLog10Prob(ngram);
		}
		return perp.get();
	}

	@Override
	public void start() {
		LOG.info("Starting.");
		// connect lm
		boolean connected = connectStringProviderService();
		boolean test_success = testStringProviderService();
		if (!connected || !test_success) {
			LOG.severe("Start cancelled due to errors.");
			throw new RuntimeException(String.format("%s cancelled start due to errors. See log for more details.", getClass().getSimpleName()));
		}

		// test preconditions
		boolean fail = false;
		if (fail |= _textExtractorInstance == null)
			LOG.severe("No TextExtractor specified.");
		if (fail |= getCrawlController() == null)
			LOG.severe("No CrawlController specified.");
		if (fail |= _lmprvdr == null)
			LOG.severe("No StringProvider connected.");
		if (fail)
			throw new RuntimeException(String.format("%s cancelled start due to errors. See log for more details.", getClass().getSimpleName()));

		super.start();
		LOG.info("Started.");
	}


	private boolean testStringProviderService() {
		try {
			computePerplexity("Hello Test Run run run run.");
		} catch (Throwable t) {
			for (int i = 1; t != null && i < 10; i++) {
				LOG.log(Level.SEVERE, String.format("Initialization test failed. (%d %s:%s)", i, t.getClass().getSimpleName(), t.getMessage()), t);
				t = t.getCause();
			}
			return false;
		}
		LOG.info("Initialization test succeeded.");
		return true;
	}

	@Override
	public void stop() {
		disconnectService();
		super.stop();
	}

	private void disconnectService() {
		/* is there something to do? */
	}

	private boolean connectStringProviderService() {
		LOG.info(String.format("Connecting to service rmi://%s:%d/%s.", getRmihost(), getRmiport(), getServiceID()));
		if(_lmprvdr == null){
			_lmprvdr = new ThreadLocal<StringProviderMXBean>(){
				@Override
				protected StringProviderMXBean initialValue() {
					return AbstractStringProvider.connectToServer(getRmihost(), getRmiport(), getServiceID());
				}
			};
		}
		else{
			_lmprvdr.set(AbstractStringProvider.connectToServer(getRmihost(), getRmiport(), getServiceID()));
		}
		if (_lmprvdr.get() == null) {
			LOG.severe(String.format("Could not connect to service rmi://%s:%d/%s.", getRmihost(), getRmiport(), getServiceID()));
			return false;
		}
		LOG.info(String.format("Connected to service rmi://%s:%d/%s.", getRmihost(), getRmiport(), getServiceID()));
		return true;
	}

	@Override
	protected boolean shouldProcess(CrawlURI uri) {
		return uri.hasBeenLinkExtracted() && uri.getContentLength() > 0 && uri.getFetchStatus() >= 200 && uri.getFetchStatus() <= 207;
	}

	@Override
	protected void innerProcess(CrawlURI uri) throws InterruptedException {
		_currently_processed_uris.add(uri);

		if (_paused_due_to_error) {
			// reconnect
			if (!connectStringProviderService() || !testStringProviderService()) {
				LOG.severe("No service connected.");
				getCrawlController().requestCrawlPause(); // do not crawl any further
				_paused_due_to_error = true;
				_lmprvdr = null;
				_currently_processed_uris.remove(uri);
				return;
			}
		}

		double perplexity = getPerplexity(uri);
		String perplexity_as_str = String.format("%012g", perplexity);
		addExtraInfo(uri, getExtraInfoValueFieldName(), perplexity_as_str);


		synchronized (_lck) {
			if(Double.isInfinite(perplexity)){
				_num_inf_values.incrementAndGet();
			}else{
				double temp = (_perplexity_avg * _num_values) + perplexity;
				_perplexity_avg = temp / ++_num_values;
				if(_perplexity_min > perplexity)
					_perplexity_min = perplexity;
				if(_perplexity_max < perplexity)
					_perplexity_max = perplexity;
				_last100PerplexityVaulues.add(perplexity);
			}
			_currently_processed_uris.remove(uri);
			_last_processed_uri = uri;
			_last_assigned_perplexity = perplexity;
		}

	}


	static void addExtraInfo(CrawlURI uri, String key, Object value) {
		try {
			uri.getExtraInfo().put(key, value);
			uri.getData().put(key, value);
		} catch (Throwable t) {
			for (int i = 1; t != null && i < 10; i++) {
				LOG.log(Level.WARNING, String.format("Failed to add perplexity value to extra info for uri: '%s' (%d-%s:%s).", uri.toString(), i, t.getClass().getName(), t.getMessage()));
				t = t.getCause();
			}
		}
	}

	double getPerplexity(CrawlURI uri) {
		assert _lmprvdr != null : "String provider service must not be null here. This should have been checked before.";

		String cleaned_plaintext = _textExtractorInstance.getCleanedUtf8PlainText(uri).trim();
		String cleaned_plaintext_abbr = MULTIPLE_SPACES_PATTERN.matcher(StringUtils.abbreviate(cleaned_plaintext, 50)).replaceAll(" ");
		addExtraInfo(uri, EXTRA_INFO_PLAINTEXT_ABBREVIATED, cleaned_plaintext_abbr);
		if (cleaned_plaintext.isEmpty())
			return Double.POSITIVE_INFINITY;
		double perplexity = Double.POSITIVE_INFINITY;
		try {
			String docid = "#" + Integer.toHexString(cleaned_plaintext.hashCode());
			LOG.fine(String.format("Sending text with id '%s' to StringProvider: '%s' (length %d).", docid, cleaned_plaintext_abbr, cleaned_plaintext.length()));
			perplexity = computePerplexity(cleaned_plaintext);
			//			if (Double.isNaN(perplexity)) {
			//				double perplexity_new = -1d;
			//				LOG.log(Level.WARNING, String.format("[%s '%s'] failed to get meaningful perplexity: %g. Setting perplexity to %g.", uri.toString(), cleaned_plaintext_abbr, perplexity, perplexity_new));
			//				perplexity = perplexity_new;
			//			}
			LOG.fine(String.format("[%s, '%s'] perplexity: %g.", uri.toString(), cleaned_plaintext_abbr, perplexity));
		} catch (Throwable t) {
			for (int i = 1; t != null && i < 10; i++) {
				LOG.log(Level.SEVERE,
						String.format("Could not compute perplexity for URI '%s' and text: '%s'. (%d %s:%s)",
								uri.toString(),
								cleaned_plaintext_abbr,
								i,
								t.getClass().getSimpleName(),
								t.getMessage()),
						t);
				t = t.getCause();
				LOG.log(Level.SEVERE, "Requesting to pause crawl.");
				getCrawlController().requestCrawlPause();
				_paused_due_to_error = true;
			}
		}
		if(Double.isInfinite(perplexity)){
			LOG.log(Level.FINE, String.format("[%s '%s'] resetting infinite perplexity to predefined maximum perplexity value (-1).", uri.toString(), cleaned_plaintext_abbr));
			perplexity = -1;
		}
		return perplexity;
	}

	@Override
	public String report() {
		/*
		 * TODO: create average counter: per last x second(s)
		 */
		synchronized (_lck) {
			StringBuilder sb = new StringBuilder();
			sb.append(String.format("Processor: %s %n", getClass().getName()));
			sb.append(String.format("  Number of processed URIs: %d %n", (long)( _num_values + _num_inf_values.get())));
			sb.append(String.format("  Number of URIs with infinite perplexity: %d %n", (long) _num_inf_values.get()));
			sb.append(String.format("  Currently processing: %s %n", _currently_processed_uris.toString()));
			sb.append(String.format("  Last processed URI: %s (Perplexity: %g) %n", _last_processed_uri == null ? "no-uri" : _last_processed_uri.toString(), _last_assigned_perplexity));
			sb.append(String.format("  Perplexity values total: %n"));
			sb.append(String.format("    %-30s %g %n", "min: ", _perplexity_min));
			sb.append(String.format("    %-30s %g %n", "max: ", _perplexity_max));
			sb.append(String.format("    %-30s %g %n", "total average: ", _perplexity_avg));
			sb.append(String.format("  Perplexity values last 10: %n"));
			double[] min_max_avg = min_max_average(_last100PerplexityVaulues.subList(0, Math.min(_last100PerplexityVaulues.size(), 10)));
			sb.append(String.format("    %-30s %g %n", "min: ", min_max_avg[0]));
			sb.append(String.format("    %-30s %g %n", "max: ", min_max_avg[1]));
			sb.append(String.format("    %-30s %g %n", "total average: ", min_max_avg[2]));
			sb.append(String.format("  Perplexity values last 100: %n"));
			min_max_avg = min_max_average(_last100PerplexityVaulues);
			sb.append(String.format("    %-30s %g %n", "min: ", min_max_avg[0]));
			sb.append(String.format("    %-30s %g %n", "max: ", min_max_avg[1]));
			sb.append(String.format("    %-30s %g %n", "total average: ", min_max_avg[2]));
			// sb.append(String.format("    %-30s %g %n", "Average Last 10 Sec. ", 0d));
			return sb.toString();
		}


	}

	private static double[] min_max_average(List<Double> values) {
		double min = Integer.MAX_VALUE;
		double max = Integer.MIN_VALUE;
		double avg = 0d;
		for(int i = 0; i < values.size(); i++){
			double value = values.get(i);
			if (value < min)
				min = value;
			if (value > max)
				max = value;
			avg += (value / values.size());
		}
		return new double[] { min, max, avg };
	}

}
