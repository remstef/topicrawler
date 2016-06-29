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
package de.tudarmstadt.lt.ltbot.prefetch;

import static org.archive.modules.fetcher.FetchStatusCodes.S_OUT_OF_SCOPE;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.archive.modules.CrawlURI;
import org.archive.modules.ProcessResult;
import org.archive.modules.Processor;
import org.archive.modules.SchedulingConstants;
import org.archive.modules.extractor.Hop;

import de.tudarmstadt.lt.ltbot.postprocessor.SharedConstants;

/**
 *
 * @author Steffen Remus
 **/
public class DecesiveValuePrioritizer__old extends Processor {

	private final static Logger LOG = Logger.getLogger(DecesiveValuePrioritizer__old.class.getName());

	private AtomicLong _count_reject;
	private final AtomicLong[] _assignment_counts;
	protected double[] _assignmentBoundaries;

	public DecesiveValuePrioritizer__old() {
		setExtraInfoValueFieldName(SharedConstants.EXTRA_INFO_PERPLEXITY);
		setAssignmentBoundaries("5e2,5e3,Infinity"); // one for each priority (HIGH,MEDIUM,NORMAL) HIGHER is reserved for prerequisites
		setMaxValue(5e4);
		setMaxPrecedence(127);
		_assignment_counts = new AtomicLong[4]; // one for each priority
		for(int i = 0; i < _assignment_counts.length; _assignment_counts[i++] = new AtomicLong());
		_count_reject = new AtomicLong();
	}

	public String getExtraInfoValueFieldName() {
		return (String) getKeyedProperties().get("ExtraInfoFieldName");
	}

	public void setExtraInfoValueFieldName(String extraInfoFieldName) {
		getKeyedProperties().put("ExtraInfoFieldName", extraInfoFieldName);
	}

	public String getAssignmentBoundaries() {
		return (String) getKeyedProperties().get("assignmentBoundaries");
	}

	public void setAssignmentBoundaries(String assignmentBoundaries) {
		String[] bounds_as_strarr = assignmentBoundaries.split(",");
		double[] bounds = new double[bounds_as_strarr.length +1]; // first value is a dummy value, never used
		for (int i = 0; i < bounds_as_strarr.length; i++)
			bounds[i + 1] = Double.valueOf(bounds_as_strarr[i]);
		kp.put("assignmentBoundaries", assignmentBoundaries);
		_assignmentBoundaries = bounds;
	}

	protected double _maxvalue;
	public double getMaxvalue() {
		return _maxvalue;
	}

	public void setMaxValue(double value) {
		_maxvalue = value;
	}

	protected int _maxPrecedence;
	public int getMaxPrecedence() {
		return _maxPrecedence;
	}

	public void setMaxPrecedence(int value) {
		_maxPrecedence = value;
	}

	@Override
	protected boolean shouldProcess(CrawlURI uri) {
		return true;
	}

	@Override
	protected void innerProcess(CrawlURI uri) throws InterruptedException {
		assert false : "This method should never be called ";
		innerProcessResult(uri);
	}

	/* (non-Javadoc)
	 * @see org.archive.modules.Processor#innerProcessResult(org.archive.modules.CrawlURI)
	 */
	@Override
	protected ProcessResult innerProcessResult(CrawlURI uri) throws InterruptedException {

		uri.setSchedulingDirective(SchedulingConstants.NORMAL);
		uri.setHolderCost(_maxPrecedence);
		uri.setPrecedence(_maxPrecedence);
		addExtraInfo(uri, SharedConstants.EXTRA_INFO_ASSIGNED_SCHEDULING_DIRECTIVE, SchedulingConstants.NORMAL);
		addExtraInfo(uri, SharedConstants.EXTRA_INFO_ASSIGNED_COST_PRECEDENCE, _maxPrecedence);
		
		if(uri.isSeed())
			return assignSeedPriority(uri);

		CrawlURI via_uri = uri.getFullVia();
		
		return innerProcessResult(uri, via_uri);

		
	}
	
	protected ProcessResult innerProcessResult(CrawlURI uri, CrawlURI via_uri) throws InterruptedException {
		if (via_uri == null) // actually, that should never happen 
			return forget(uri);
		
		if(via_uri.isPrerequisite() || via_uri.getLastHop().equals(Hop.REFER.getHopString()) || (via_uri.getFetchStatus() / 100) == 3 /* redirect status code */ || via_uri.getLastHop().equals(Hop.EMBED.getHopString()))
			return innerProcessResult(uri, via_uri.getFullVia());
			
		if (via_uri.isSeed() && uri.getLastHop().equals(Hop.REFER.getHopString()))
			return assignSeedPriority(uri);
		
		if (via_uri.isSeed() && (uri.getFetchStatus() / 100) == 3 /* redirect status code */)
			return assignSeedPriority(uri);

		if (via_uri.isSeed() && uri.isPrerequisite())
			return assignSeedPriority(uri);
		
		if (via_uri.isSeed() && uri.getLastHop().equals(Hop.EMBED.getHopString()))
			return assignSeedPriority(uri);

		Object obj = getExtraInfo(via_uri, getExtraInfoValueFieldName());
		if(obj == null)
			// uri is probably a prerequisite or a redirect URL
//			if(uri.isPrerequisite() || uri.getLastHop().equals(Hop.REFER.getHopString()) || (via_uri.getFetchStatus() / 100) == 3 /* redirect status code */)
//				return handleRedirectsAndPrerequisites(uri, via_uri);
//			else // that should never ever happen
				return forget(uri);
			
		double value = Double.valueOf((String)obj);
		
		int schedulingConstants_priority = getPriorityAsSchedulingDirective(value);
		if (schedulingConstants_priority < 0)
			return forget(uri);
		// lower values have higher precedence, i.e. higher priority
		int cost = getPrecedenceCost(value, schedulingConstants_priority);
		if(uri.isPrerequisite() || uri.getLastHop().equals(Hop.REFER.getHopString()) || (via_uri.getFetchStatus() / 100) == 3 /* redirect status code */ || uri.getLastHop().equals(Hop.EMBED.getHopString())){
			cost = Math.max(2, (int)(cost / 2));
//			schedulingConstants_priority = Math.max(SchedulingConstants.HIGHEST, schedulingConstants_priority-1);
		}
		
		uri.setSchedulingDirective(schedulingConstants_priority);
		_assignment_counts[schedulingConstants_priority].incrementAndGet();
		LOG.finest(String.format("Assigned scheduling directive %d to %s.", schedulingConstants_priority, uri.toString()));
		
		uri.setHolderCost(cost);
		uri.setPrecedence(cost);
		LOG.finest(String.format("Assigned precedence cost %d to %s.", cost, uri.toString()));

		addExtraInfo(uri, SharedConstants.EXTRA_INFO_ASSIGNED_SCHEDULING_DIRECTIVE, schedulingConstants_priority);
		addExtraInfo(uri, SharedConstants.EXTRA_INFO_ASSIGNED_COST_PRECEDENCE, cost);
		addExtraInfo(uri, SharedConstants.EXTRA_INFO_PERPLEXITY + "_via", String.format("%012g", value));

		return ProcessResult.PROCEED;
	}

	private ProcessResult handleRedirectsAndPrerequisites(CrawlURI uri, CrawlURI via_uri) {		
		if(via_uri.getFetchStatus() == S_OUT_OF_SCOPE)
			return forget(uri);
		Object obj = getExtraInfo(via_uri, SharedConstants.EXTRA_INFO_PERPLEXITY + "_via");
		if(obj == null)
			return ProcessResult.PROCEED;  // -> status -50 (i guess via urls don't have perplexity value because they are not captured by html rule?? -> investigate)
//			// no perplexity value and no perplexity via, that should never ever happen
//			return forget(uri); // -> status -63
		//CandidatesProcessor
		// FIXME: status code of rejected prerequisites is -63 (that doesn't look nice, not sure though if its really considered to be an error)
		// this if condition removes this, but then a lot of prereqisites are fetched that are not needed
		// if(!uri.isPrerequisite() && !uri.toString().endsWith("robots.txt")){
		
		int schedulingConstants_priority = (Integer)getExtraInfo(via_uri,SharedConstants.EXTRA_INFO_ASSIGNED_SCHEDULING_DIRECTIVE);
		int cost = (Integer)getExtraInfo(via_uri,SharedConstants.EXTRA_INFO_ASSIGNED_COST_PRECEDENCE);
		
		addExtraInfo(uri, SharedConstants.EXTRA_INFO_PERPLEXITY, obj);
		addExtraInfo(uri, SharedConstants.EXTRA_INFO_PERPLEXITY + "_via", obj);
		addExtraInfo(uri, SharedConstants.EXTRA_INFO_ASSIGNED_SCHEDULING_DIRECTIVE, schedulingConstants_priority);
		addExtraInfo(uri, SharedConstants.EXTRA_INFO_ASSIGNED_COST_PRECEDENCE, cost);
		
		uri.setSchedulingDirective(schedulingConstants_priority);
		_assignment_counts[schedulingConstants_priority].incrementAndGet();
		LOG.finest(String.format("Assigned scheduling directive %d to %s.", schedulingConstants_priority, uri.toString()));
		uri.setHolderCost(cost);
		uri.setPrecedence(cost);
		LOG.finest(String.format("Assigned precedence cost %d to %s.", cost, uri.toString()));

		return ProcessResult.FINISH;
	}
	
	Object getExtraInfo(CrawlURI uri, String key) throws IllegalStateException {
		// if present in data map return value
		if(uri.getData().containsKey(key))
			return uri.getData().get(key);
		// else check if its available in extra info json object
		try {
			if(uri.getExtraInfo().has(key))
				return uri.getExtraInfo().get(key);
		} catch (Throwable t) {
			/* NOTHING SHOULD HAPPEN HERE. AND IN THE UNLIKELY CASE THAT SOMETHING HAPPENS I DO NOT CARE ABOUT IT. */
		}
		return null;
	}

	int getPrecedenceCost(double val, int schedulingConstants_priority) {
		// cost should be in [0,_maxPrecedence], lower values are better, try to squash values into this range
		int cost = _maxPrecedence;
		switch(schedulingConstants_priority){
		case SchedulingConstants.HIGHEST: return 1; // 2^0
		case SchedulingConstants.HIGH: return 2; // 2^1
		case SchedulingConstants.MEDIUM: return 8; // 2^3
		case SchedulingConstants.NORMAL: cost = 64; // 2^6
		}

		// --> squeeze [_assignmentBoundaries[SchedulingConstants.NORMAL], _maxvalue] into [64, _maxPrecedence]
		//				[A, B] --> [a, b]
		//				newval = (val - A)*(b-a)/(B-A) + a

		double B = Math.min(Integer.MAX_VALUE, _maxvalue); // use maxvalue as maximum if specified and below Integer.maxvalue
		if (val >= B)
			return _maxPrecedence;
		// take log of A, B and val to make computation numerically more stable
		B = Math.log1p(B);
		double A = Math.log1p(_assignmentBoundaries[SchedulingConstants.MEDIUM]);
		val = Math.log1p(val);
		assert val >= A : "Value is smaller than lower boundary. That should not happen.";
		double a = cost;
		double b = _maxPrecedence;
		cost = (int)Math.ceil((val-A)*(b-a)/(B-A) + a);
		return cost;
	}


	int getPriorityAsSchedulingDirective(double perplexity) {
		if (perplexity < 1d || !Double.isFinite(perplexity))
			return -1; // best remove from frontier
		// HIGHEST = 0, HIGH = 1, ... but reserve HIGHEST for prerequistes
		if (perplexity <= _assignmentBoundaries[SchedulingConstants.HIGH]) return SchedulingConstants.HIGH; // higher than medium
		if (perplexity <= _assignmentBoundaries[SchedulingConstants.MEDIUM]) return SchedulingConstants.MEDIUM; // higher than normal
		if (perplexity <= _assignmentBoundaries[SchedulingConstants.NORMAL]) return SchedulingConstants.NORMAL; // default
		// else best remove from frontier
		return -1;
	}
	
	private ProcessResult assignSeedPriority(CrawlURI uri){
		// for seed urls or urls that are a prerequisite for seed urls
		_assignment_counts[SchedulingConstants.HIGH].incrementAndGet();
		uri.setSchedulingDirective(SchedulingConstants.HIGH);
		uri.setHolderCost(2);
		uri.setPrecedence(2);
		addExtraInfo(uri, SharedConstants.EXTRA_INFO_PERPLEXITY + "_via", "2");
		addExtraInfo(uri, SharedConstants.EXTRA_INFO_ASSIGNED_SCHEDULING_DIRECTIVE, SchedulingConstants.HIGH);
		addExtraInfo(uri, SharedConstants.EXTRA_INFO_ASSIGNED_COST_PRECEDENCE, 2);
		return ProcessResult.PROCEED;
	}

	private ProcessResult privilege(CrawlURI uri){
		// for seed urls or urls that are a prerequisite for seed urls
		_assignment_counts[SchedulingConstants.HIGH].incrementAndGet();
		uri.setSchedulingDirective(SchedulingConstants.HIGH);
		uri.setHolderCost(2);
		uri.setPrecedence(2);
		addExtraInfo(uri, SharedConstants.EXTRA_INFO_PERPLEXITY + "_via", "2");
		addExtraInfo(uri, SharedConstants.EXTRA_INFO_ASSIGNED_SCHEDULING_DIRECTIVE, SchedulingConstants.HIGH);
		addExtraInfo(uri, SharedConstants.EXTRA_INFO_ASSIGNED_COST_PRECEDENCE, 2);
		return ProcessResult.PROCEED;
	}

	private ProcessResult forget(CrawlURI uri){
		_count_reject.incrementAndGet();			
		uri.clearPrerequisiteUri();
		uri.setFetchStatus(S_OUT_OF_SCOPE); // this will not consider the url for further processing // TODO: there must be a better solution, maybe extend org.archive.crawler.prefetch.FrontierPreparer or org.archive.crawler.prefetch.CandidateScoper
		uri.setSchedulingDirective(SchedulingConstants.NORMAL);
		uri.setHolderCost(_maxPrecedence);
		uri.setPrecedence(_maxPrecedence);
		return ProcessResult.FINISH;
	}

	static void addExtraInfo(CrawlURI uri, String key, Object value) {
		uri.getData().put(key, value);
		try {
			uri.getExtraInfo().put(key, value);
		} catch (Throwable t) {
			/* NOTHING SHOULD HAPPEN HERE. AND IN THE UNLIKELY CASE THAT SOMETHING HAPPENS I DO NOT CARE ABOUT IT. */
		}
	}

	@Override
	public String report() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("Processor: %s %n", getClass().getName()));
		sb.append(String.format("  SchedulingConstants assignment counts: [%n", getClass().getName()));
		sb.append(String.format("    %d '%s' (<= %g); %n", _assignment_counts[SchedulingConstants.HIGHEST].get(), "HIGHEST", _assignmentBoundaries[SchedulingConstants.HIGHEST]));
		sb.append(String.format("    %d '%s' (<= %g); %n", _assignment_counts[SchedulingConstants.HIGH].get(), "HIGH", _assignmentBoundaries[SchedulingConstants.HIGH]));
		sb.append(String.format("    %d '%s' (<= %g); %n", _assignment_counts[SchedulingConstants.MEDIUM].get(), "MEDIUM", _assignmentBoundaries[SchedulingConstants.MEDIUM]));
		sb.append(String.format("    %d '%s' (<= %g); %n", _assignment_counts[SchedulingConstants.NORMAL].get(), "NORMAL", _assignmentBoundaries[SchedulingConstants.NORMAL]));
		sb.append(String.format("    %d '%s'%n", _count_reject.get(), "REJECTED"));
		sb.append(String.format("  ]%n"));
		return sb.toString();
	}

}
