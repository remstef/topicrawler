package de.tudarmstadt.lt.ltbot.deciderules;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.URIException;
import org.archive.modules.CrawlURI;
import org.archive.modules.deciderules.PredicatedDecideRule;


public class TooManyDomainsDecideRule extends PredicatedDecideRule {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5431194700037839879L;

	private final static Logger LOG = Logger.getLogger(TooManyDomainsDecideRule.class.getName());

	private final Set<String> _domains_observed = new HashSet<String>();
	private int _max_domains = 20;

	/**
	 * Evaluate whether given CrawlURI is over the threshold number of domains.
	 * 
	 * @param curi
	 * @return true if the max-domains is exceeded
	 */
	@Override
	protected boolean evaluate(CrawlURI curi) {
		boolean ret_val = true; // reject by default
		try {
			String host = curi.getUURI().getHost();
			String domain = getDomain(host);
			ret_val = _domains_observed.size() > _max_domains;
			_domains_observed.add(domain);
		} catch (URIException e) {
			LOG.log(Level.SEVERE, String.format("Could not process URL: '%s'.", curi.toString()), e);
			return true; // reject on errors
		}
		return ret_val;
	}

	public int getMaxDomains() {
		return _max_domains;
	}

	public void setMaxDomains(int max_domains) {
		_max_domains = max_domains;
	}

	private static final String getDomain(String host) {
		int index_of_last_dot = host.lastIndexOf('.');
		int index_of_second_last_dot = host.lastIndexOf('.', index_of_last_dot);
		String domain = host.substring(index_of_second_last_dot);
		return domain;
	}

}
