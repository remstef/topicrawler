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

import org.apache.commons.lang.StringUtils;
import org.archive.crawler.framework.Scoper;
import org.archive.modules.CrawlURI;
import org.archive.modules.ProcessResult;
import org.archive.modules.SchedulingConstants;
import org.archive.modules.extractor.Hop;

import de.tudarmstadt.lt.ltbot.postprocessor.SharedConstants;

/**
 *
 * @author Steffen Remus
 **/
public class DecesiveValuePrioritizer extends Scoper {

	private final static Logger LOG = Logger.getLogger(DecesiveValuePrioritizer.class.getName());

	private AtomicLong _count_reject;
	private final AtomicLong[] _assignment_counts;
	protected double[] _assignmentBoundaries;

	public DecesiveValuePrioritizer() {
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
		__innerProcessResult(uri);
	}

	/* (non-Javadoc)
	 * @see org.archive.modules.Processor#innerProcessResult(org.archive.modules.CrawlURI)
	 */
//	@Override
	protected ProcessResult __innerProcessResult(CrawlURI uri) throws InterruptedException {

		CrawlURI via_uri = uri.getFullVia();

		// uri is seed
		if(via_uri == null || uri.isSeed()){
			if(uri.isSeed()){
				return schedule(uri, via_uri, 2d, "");
			}else{
				if(StringUtils.isNotEmpty(uri.getPathFromSeed())) {
					char lastHop = uri.getPathFromSeed().charAt(uri.getPathFromSeed().length()-1);
					if(lastHop == 'R') {
						// refer
						return schedule(uri, via_uri, 2d, "");
					}
				}
				LOG.warning(String.format("'%s' has no via URL %s / %s.", uri, uri.flattenVia(), via_uri));
				//				assert false : "Why is your via null, that should not happen!";
				return ProcessResult.FINISH;
			}
		}
		
		boolean uri_is_redirect = uri.getLastHop().equals(Hop.REFER.getHopString());//s || uri.getLastHop().equals(Hop.EMBED.getHopString());
		boolean via_is_redirect = via_uri.getLastHop().equals(Hop.REFER.getHopString()) || via_uri.getLastHop().equals(Hop.EMBED.getHopString()); //|| (via_uri.getFetchStatus() / 100) == 3 /* redirect status code */
		
		String debug_uri = String.format("%s:%n%s\ndirective / cost %s/%s %n---> via %s%n%s\ndirective / cost %s/%s", 
				uri.toString() + (uri.isPrerequisite() ? " is preq" : "") +  (uri_is_redirect ? " is redirect" : "") + (uri.isSeed() ? " is seed" : ""), 
				uri.getData(), 
				uri.getSchedulingDirective(),
				uri.getPrecedence(),
				via_uri.toString() + (via_uri.isPrerequisite() ? " is preq" : "") + (via_is_redirect ? " is redirect" : "") + (via_uri.isSeed() ? " is seed" : ""), 
				via_uri.getData(),
				via_uri.getSchedulingDirective(),
				via_uri.getPrecedence());
		LOG.finest(debug_uri);

		double perplexity = _maxvalue;
		
		if(uri_is_redirect || uri.isPrerequisite() || via_uri.isPrerequisite()) // uri is embed/refer/prereq/...
			return scheduleSame(uri, via_uri);
		else
			perplexity = getPerplexity(uri, via_uri, debug_uri);

		return schedule(uri, via_uri, perplexity, debug_uri);
	}

	double getPerplexity(CrawlURI uri, CrawlURI via_uri, String debug_uri){
		Object obj = getExtraInfo(via_uri, getExtraInfoValueFieldName());
		if(obj == null){
			LOG.info(String.format("%s - (%s)\tno priority value found at field %s.%n+++BEGIN+++%n%s%n+++END+++%n", via_uri, uri.flattenVia(), getExtraInfoValueFieldName(), debug_uri));
			// FIXME: schedule same in such a case? 			//			perplexity = getPerplexity(uri, via_uri, getExtraInfoValueFieldName() + "_via");
//			LOG.warning(String.format("unable to schedule %s.", uri));
//			obj = getExtraInfo(via_uri, getExtraInfoValueFieldName() + "_via");
//			if(obj == null){
//				LOG.warning(String.format("%s - (%s)\tno priority value found at field %s.", via_uri, uri.flattenVia(), getExtraInfoValueFieldName()+"_via"));
				return _maxvalue;
//			}
		}
		double value = Double.valueOf((String)obj);
		return value;
	}

	ProcessResult schedule(CrawlURI uri, CrawlURI via_uri, double perplexity, String debug_uri){
		LOG.fine(String.format("Perplexity %s = %f", uri.toString(), perplexity));
		int schedulingdirective = getPriorityAsSchedulingDirective(perplexity);
		if (schedulingdirective < 0){ // "forget" url 
			_count_reject.incrementAndGet();			
			uri.clearPrerequisiteUri();
			uri.setFetchStatus(S_OUT_OF_SCOPE); // this will not consider the url for further processing // TODO: there must be a better solution, maybe extend org.archive.crawler.prefetch.FrontierPreparer or org.archive.crawler.prefetch.CandidateScoper
			uri.setSchedulingDirective(SchedulingConstants.NORMAL); // just in case, put it into the least important bucket
			uri.setHolderCost(_maxPrecedence);
			uri.setPrecedence(_maxPrecedence);
			uri.addExtraInfo(getExtraInfoValueFieldName() + "_via", String.format("%012g", perplexity));
			LOG.fine(String.format("Assigned scheduling directive %d to %s.", uri.getSchedulingDirective(), uri.toString()));
			LOG.fine(String.format("Assigned precedence cost %d to %s.", uri.getHolderCost(), uri.toString()));
			return ProcessResult.FINISH;
		}

		int cost = getPrecedenceCost(perplexity, schedulingdirective);
		
		if(uri.isPrerequisite())
			uri.setSchedulingDirective(Math.max(SchedulingConstants.HIGHEST, schedulingdirective-1));
		else
			uri.setSchedulingDirective(schedulingdirective);
		
		uri.setHolderCost(cost);
		uri.setPrecedence(cost);

		_assignment_counts[schedulingdirective].incrementAndGet();

		LOG.fine(String.format("Assigned scheduling directive %d to %s.", schedulingdirective, uri.toString()));
		LOG.fine(String.format("Assigned precedence cost %d to %s.", cost, uri.toString()));

		addExtraInfo(uri, SharedConstants.EXTRA_INFO_ASSIGNED_SCHEDULING_DIRECTIVE, schedulingdirective);
		addExtraInfo(uri, SharedConstants.EXTRA_INFO_ASSIGNED_COST_PRECEDENCE, cost);
		addExtraInfo(uri, getExtraInfoValueFieldName() + "_via", String.format("%012g", perplexity));

		return ProcessResult.PROCEED;
	}

	ProcessResult scheduleSame(CrawlURI uri, CrawlURI via_uri){
		if(uri.isPrerequisite()){
			if (via_uri.isSeed() && !uri.getUURI().getScheme().equals("dns"))
				uri.setSchedulingDirective(Math.max(SchedulingConstants.HIGHEST, via_uri.getSchedulingDirective()-1));
			else
				uri.setSchedulingDirective(via_uri.getSchedulingDirective());
			if(!via_uri.isPrerequisite())
				uri.setSchedulingDirective(Math.max(SchedulingConstants.HIGH, via_uri.getSchedulingDirective()-1));
		}
		else
			uri.setSchedulingDirective(via_uri.getSchedulingDirective());
		
		int cost = Math.max(0, via_uri.getHolderCost()-1);
		uri.setHolderCost(cost);
		uri.setPrecedence(cost);

		_assignment_counts[uri.getSchedulingDirective()].incrementAndGet();

		LOG.finest(String.format("Assigned scheduling directive %d to %s.", uri.getSchedulingDirective(), uri.toString()));
		LOG.finest(String.format("Assigned precedence cost %d to %s.", uri.getHolderCost(), uri.toString()));

		addExtraInfo(uri, SharedConstants.EXTRA_INFO_ASSIGNED_SCHEDULING_DIRECTIVE, uri.getSchedulingDirective());
		addExtraInfo(uri, SharedConstants.EXTRA_INFO_ASSIGNED_COST_PRECEDENCE, uri.getHolderCost());

		Object val = via_uri.getData().get(getExtraInfoValueFieldName());
		if(val != null){
			uri.getData().put(getExtraInfoValueFieldName(), val);
			uri.addExtraInfo(getExtraInfoValueFieldName(), val);
		}
		val = via_uri.getData().get(getExtraInfoValueFieldName()+"_via");
		if(val != null){
			uri.getData().put(getExtraInfoValueFieldName()+"_via", val);
			uri.addExtraInfo(getExtraInfoValueFieldName()+"_via", val);
		}

		return ProcessResult.PROCEED;
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
		case SchedulingConstants.HIGH: return 4; // 2^2
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
//		assert val >= A : "Value is smaller than lower boundary. That should not happen.";
		double a = cost;
		double b = _maxPrecedence;
		cost = (int)Math.ceil((val-A)*(b-a)/(B-A) + a);
		return cost;
	}

	int getPriorityAsSchedulingDirective(double perplexity) {
		if (perplexity <= 1d)
			return -1; // remove from frontier
		if(!Double.isFinite(perplexity))
			if(!Double.isFinite(_assignmentBoundaries[SchedulingConstants.NORMAL]))
				return SchedulingConstants.NORMAL; // default
			else 
				return -1; // remove

		// HIGHEST = 0, HIGH = 1, ... but reserve HIGHEST for prerequistes
		if (perplexity <= _assignmentBoundaries[SchedulingConstants.HIGH]) return SchedulingConstants.HIGH; // higher than medium
		if (perplexity <= _assignmentBoundaries[SchedulingConstants.MEDIUM]) return SchedulingConstants.MEDIUM; // higher than normal
		if (perplexity <= _assignmentBoundaries[SchedulingConstants.NORMAL]) return SchedulingConstants.NORMAL; // default

		// else best remove from frontier // should not happen
//		assert false : "You should not be here";
		return -1;
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
		sb.append(String.format("    %d '%s' (%s); %n", _assignment_counts[SchedulingConstants.HIGHEST].get(), "HIGHEST", "Prerequisites"));
		sb.append(String.format("    %d '%s' (<= %g); %n", _assignment_counts[SchedulingConstants.HIGH].get(), "HIGH", _assignmentBoundaries[SchedulingConstants.HIGH]));
		sb.append(String.format("    %d '%s' (<= %g); %n", _assignment_counts[SchedulingConstants.MEDIUM].get(), "MEDIUM", _assignmentBoundaries[SchedulingConstants.MEDIUM]));
		sb.append(String.format("    %d '%s' (<= %g); %n", _assignment_counts[SchedulingConstants.NORMAL].get(), "NORMAL", _assignmentBoundaries[SchedulingConstants.NORMAL]));
		sb.append(String.format("    %d '%s'%n", _count_reject.get(), "REJECTED"));
		sb.append(String.format("  ]%n"));
		return sb.toString();
	}

}
