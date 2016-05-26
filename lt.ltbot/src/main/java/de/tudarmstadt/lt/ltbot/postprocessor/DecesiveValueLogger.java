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

import java.util.logging.Logger;

import org.archive.crawler.reporting.CrawlerLoggerModule;
import org.archive.modules.CrawlURI;
import org.archive.modules.Processor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.lt.utilities.TimeUtils;

/**
 *
 * @author Steffen Remus
 **/
public class DecesiveValueLogger extends Processor {

	private final static Logger LOG = Logger.getLogger(DecesiveValueLogger.class.getName());
	
	protected Logger _fileLogger = null;
	
	
	/**
     * If enabled, log decisions to file named logs/{spring-bean-id}.log. 
     */
    { setLogToFile(true); }
    public boolean getLogToFile() {
        return (Boolean) kp.get("logToFile");
    }
    public void setLogToFile(boolean enabled) {
        kp.put("logToFile",enabled);
    }
	
    { setExtraInfoValueFieldName(SharedConstants.EXTRA_INFO_PERPLEXITY); }
	public String getExtraInfoValueFieldName() {
		return (String) kp.get("ExtraInfoFieldName");
	}
	public void setExtraInfoValueFieldName(String extraInfoFieldName) {
		kp.put("ExtraInfoFieldName", extraInfoFieldName);
	}

    protected CrawlerLoggerModule _loggerModule;
    public CrawlerLoggerModule getLoggerModule() {
        return _loggerModule;
    }
    @Autowired
    public void setLoggerModule(CrawlerLoggerModule loggerModule) {
        _loggerModule = loggerModule;
    }
    
	public DecesiveValueLogger() {
		super();
	}
	protected boolean isRunning = false; 
    public void start() {
        if(isRunning) {
            return; 
        }
        if (getLogToFile() && _fileLogger == null) {
            _fileLogger = _loggerModule.setupSimpleLog(getBeanName());
        }
        isRunning = true; 
    }
    public boolean isRunning() {
        return this.isRunning;
    }
    public void stop() {
        isRunning = false; 
    }
    
	@Override
	protected boolean shouldProcess(CrawlURI uri) {
		return uri.getData().containsKey(getExtraInfoValueFieldName());
	}
	
	
	@Override
	protected void innerProcess(CrawlURI uri) throws InterruptedException {
		if(!getLogToFile())
			return;
		LOG.finest(String.format("[%s (%s): %s via %s]: %s; %s;", getBeanName(), getExtraInfoValueFieldName(), uri, uri.getVia(), uri.getExtraInfo(), uri.getFullVia() != null ? uri.getFullVia().getExtraInfo() : "{}"));
		String toLog = getLogString(uri);
		if(toLog == null || toLog.isEmpty())
			return;
		_fileLogger.info(toLog); 
	}
	
	private String getLogString(CrawlURI curi){
		String timestamp = TimeUtils.get_ISO_8601_UTC();
		String value_as_str = curi.getData().containsKey(getExtraInfoValueFieldName()) ? curi.getData().get(getExtraInfoValueFieldName()).toString() : "null";
		String current_scheduling_directive = String.valueOf(curi.getSchedulingDirective());
		String current_precedence = String.valueOf(curi.getPrecedence());
		String assigned_scheduling_directive = "_";
		String assigned_precedence = "_";
		String abbr_text = "_";
		try{
			JSONObject info = curi.getExtraInfo();
			assigned_scheduling_directive = info.has(EXTRA_INFO_ASSIGNED_SCHEDULING_DIRECTIVE) ? info.get(EXTRA_INFO_ASSIGNED_SCHEDULING_DIRECTIVE).toString() : "_";
			assigned_precedence = info.has(EXTRA_INFO_ASSIGNED_COST_PRECEDENCE) ? info.get(EXTRA_INFO_ASSIGNED_COST_PRECEDENCE).toString() : "_";
			abbr_text = info.has(EXTRA_INFO_PLAINTEXT_ABBREVIATED) ? info.getString(EXTRA_INFO_PLAINTEXT_ABBREVIATED) : "_";
		}catch(Exception e){
			/* I don't care */
		}
		return String.format("%s\t%s\t%s(%s)\t%s(%s)\t%s\t[%-50s]", timestamp, value_as_str, current_scheduling_directive, assigned_scheduling_directive, current_precedence, assigned_precedence, curi.toString(), abbr_text);
	}



}
