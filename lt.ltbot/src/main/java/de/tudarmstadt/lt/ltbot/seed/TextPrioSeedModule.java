package de.tudarmstadt.lt.ltbot.seed;

import org.apache.commons.httpclient.URIException;
import org.archive.modules.CrawlURI;
import org.archive.modules.SchedulingConstants;
import org.archive.modules.seeds.TextSeedModule;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;

import de.tudarmstadt.lt.ltbot.postprocessor.SharedConstants;

public class TextPrioSeedModule extends TextSeedModule {

	@Override
	protected void seedLine(String uri) {
		// copy from  super.seedLine(uri) except SchedulingDirective, Cost, and extraInfos

		if (!uri.matches("[a-zA-Z][\\w+\\-]+:.*")) { // Rfc2396 s3.1 scheme,
			// minus '.'
			// Does not begin with scheme, so try http://
			uri = "http://" + uri;
		}
		try {
			UURI uuri = UURIFactory.getInstance(uri);
			CrawlURI curi = new CrawlURI(uuri);
			curi.setSeed(true);
			curi.setSchedulingDirective(SchedulingConstants.HIGH);
			curi.setHolderCost(4);
			curi.setPrecedence(4);
			curi.getData().put(SharedConstants.EXTRA_INFO_PERPLEXITY, "2");
			curi.addExtraInfo(SharedConstants.EXTRA_INFO_PERPLEXITY, "2");
			curi.getData().put(SharedConstants.EXTRA_INFO_PERPLEXITY + "_via", "2");
			curi.addExtraInfo(SharedConstants.EXTRA_INFO_PERPLEXITY + "_via", "2");
			if (getSourceTagSeeds()) {
				curi.setSourceTag(curi.toString());
			}
			publishAddedSeed(curi);
		} catch (URIException e) {
			// try as nonseed line as fallback
			nonseedLine(uri);
		}
	}

}
