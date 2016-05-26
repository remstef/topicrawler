/*
 *   Copyright 2014
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
package de.tudarmstadt.lt.ltbot.writer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang.StringUtils;
import org.archive.io.RecordingInputStream;
import org.archive.modules.CrawlURI;
import org.archive.modules.Processor;
import org.archive.spring.ConfigPath;
import org.archive.util.FileUtils;

import de.tudarmstadt.lt.ltbot.postprocessor.SharedConstants;
import de.tudarmstadt.lt.ltbot.text.TextExtractor;
import de.tudarmstadt.lt.utilities.TimeUtils;


/**
 * @author Steffen Remus
 *
 */
public class PlainTextDocumentWriter extends Processor{
	
	private static final Logger LOG = Logger.getLogger(PlainTextDocumentWriter.class.getName());

	private final static String TEXT_EXTRACT_KEY = "text";
	
	private final static String FILE_NUMBER_REPLACEMENT = "{num}";
	
	private final static String TIME_REPLACEMENT = "{time}";
	
	private Object _lck = new Object();

	/**
	 * 
	 */
	public PlainTextDocumentWriter() {}
	
	/**
	 * plain text extractor
	 */
	protected TextExtractor _textExtractorInstance;
	public TextExtractor getTextExtractor() {
		return _textExtractorInstance;
	}
	public void setTextExtractor(TextExtractor text_extractor) {
		_textExtractorInstance = text_extractor;
	}
	
	/**
	 * Top-level directory for plaintext files.
	 */
	protected ConfigPath _path = new ConfigPath("PlainTextDocumentWriter directory", "${launchId}/plaintext");
	public ConfigPath getPath() {
		return _path;
	}
	public void setPath(ConfigPath newpath) {
		_path = newpath; 
	}
		
    /**
     * Max size of each file.
     */
    protected long _maxFileSizeBytes = 100 * 1000000; // 100MB
    public long getMaxFileSizeBytes() {
        return _maxFileSizeBytes;
    }
    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        _maxFileSizeBytes = maxFileSizeBytes;
    }
    
    protected String _prefix = "";
    public String getPrefix(){
    	return _prefix;
    }
    public void setPrefix(String prefix){
    	_prefix = prefix;
    }
    
    /**
     * Filename format
     * 
     * [{prefix}-]{time}-{num}.txt.gz
     * 
     */
    protected String _filename_format = String.format("%s-%s.txt", TIME_REPLACEMENT, FILE_NUMBER_REPLACEMENT);
    public String getFilenameFormat() {
        return _filename_format;
    }
    public void setFilenameFormat(String filename_format) {
    	_filename_format = filename_format;
    }
	
	
	File _current_file = null;
	PrintStream _current_stream = null;
	int _num_current_file = -1;
	AtomicLong _num_uris = new AtomicLong();
	AtomicLong _num_uris_written = new AtomicLong();
	
	AtomicLong _num_bytes_written = new AtomicLong();
	AtomicLong _num_bytes_docs_written = new AtomicLong();
	
	/* (non-Javadoc)
	 * @see org.archive.modules.Processor#start()
	 */
	@Override
	public void start() {
		super.start();
	}
	
	/* (non-Javadoc)
	 * @see org.archive.modules.Processor#stop()
	 */
	@Override
	public void stop() {
		if(_current_stream != null)
			_current_stream.close();
		_current_stream = null;
		super.stop();
	}

	/* (non-Javadoc)
	 * @see org.archive.modules.Processor#shouldProcess(org.archive.modules.CrawlURI)
	 */
	@Override
	protected boolean shouldProcess(CrawlURI curi) {
		return isSuccess(curi) &&  curi.getContentLength() > 0 && curi.getFetchStatus() >= 200 && curi.getFetchStatus() <= 207 ;
	}
	
	/* (non-Javadoc)
	 * @see org.archive.modules.Processor#innerProcess(org.archive.modules.CrawlURI)
	 */
	@Override
	protected void innerProcess(CrawlURI curi) throws InterruptedException {
		try {
			File basedir = getPath().getFile();
			FileUtils.ensureWriteableDirectory(basedir);
		} catch (IOException e) {
			throw new RuntimeException(String.format("Could not ensure writeable base directory '%s'. %s: %s.", getPath(), e.getClass().getName(), e.getMessage()), e);
		}
		_num_uris.getAndIncrement();
		RecordingInputStream recis = curi.getRecorder().getRecordedInput();
		if (0L == recis.getResponseContentLength()) {
			return;
		}

		// content already written for this URI.
		boolean is_revisited = curi.getData().containsKey(TEXT_EXTRACT_KEY);
		if(is_revisited)
			return;
			
		try {
			String cleaned_plaintext = _textExtractorInstance.getCleanedUtf8PlainText(curi);
			updateOuputFile();
			writeplaintext(curi, cleaned_plaintext);
			String cleaned_plaintext_abbr = StringUtils.abbreviate(cleaned_plaintext, 50);
			curi.getData().put(TEXT_EXTRACT_KEY, cleaned_plaintext_abbr);
		} catch (IOException e) {
			curi.getNonFatalFailures().add(e);
		}
	}
	
	/**
	 * @param cleaned_plaintext
	 */
	protected void writeplaintext(CrawlURI curi, String cleaned_plaintext) {
		String perplexity_value_as_string = "null";
		if(curi != null && curi.getData() != null){
			Object obj = curi.getData().get(SharedConstants.EXTRA_INFO_PERPLEXITY);
			if(obj != null)
				perplexity_value_as_string = (String)obj;
		}

		String time = TimeUtils.get_ISO_8601_UTC();
		synchronized (_lck) {
			String s = String.format("%s\t%s\t%s\t%s%n", time, cleaned_plaintext.replaceAll("\t", "\\t").replaceAll("\n", "\\n"), curi, perplexity_value_as_string);
			_current_stream.println(s);
			_num_bytes_written.getAndAdd(s.getBytes().length);
			_num_bytes_docs_written.getAndAdd(cleaned_plaintext.replaceAll("\t", "\\t").replaceAll("\n", "\\n").getBytes().length);
			_current_stream.flush();
		}
		_num_uris_written.getAndIncrement();
	}
	
	protected File updateOuputFile() throws IOException{
		File basedir = getPath().getFile();
		File out = _current_file;
		if(out == null || out.length() > _maxFileSizeBytes){
			synchronized (_lck) {
				if(_current_stream != null)
					_current_stream.close();
				int not_ok_count = 0;
				while(not_ok_count > -1 && not_ok_count < 10){
					for(++_num_current_file; out == null || out.exists(); ++_num_current_file){
						out = new File(basedir, getFilename());
					}
					_current_file = out;
					try {
						_current_stream = openPrintToFileStream(_current_file);
					} catch (Throwable t) {
						for (int i = 1; t != null && i < 10; i++) {
							String message = String.format("Failed to open file for writing: '%s'. (%d %s:%s)", _current_file.getAbsolutePath(), i, t.getClass().getSimpleName(), t.getMessage());
							LOG.log(Level.SEVERE, message, t);
							t = t.getCause();
						}
						not_ok_count++;
						if(not_ok_count >= 10)
							throw new IOException(String.format("Failed to open file for writing: '%s'. I tried %d times but I give up now.", _current_file.getAbsolutePath(), not_ok_count));
						continue;
					}
					// break this loop, we're ok now
					not_ok_count = -1;
					break; 
				}
			}
		}
		return out;
	}

	/**
	 * @param num_current_file
	 * @return
	 */
	protected String getFilename() {
		String n = String.format("%05d", _num_current_file);
		String filename = getFilenameFormat();
		if(_prefix != null && !_prefix.isEmpty())
			filename = _prefix + "-" + filename;
		if(filename.contains(TIME_REPLACEMENT))
			filename = filename.replace(TIME_REPLACEMENT, TimeUtils.getSimple17());
		if(filename.contains(FILE_NUMBER_REPLACEMENT))
			filename = filename.replace(FILE_NUMBER_REPLACEMENT, n);
		else
			filename += "-" + n;
		return filename;
	}
	
	protected PrintStream openPrintToFileStream(File outputfile) throws IOException {
		OutputStream os = new FileOutputStream(outputfile, true);
		if (getFilenameFormat().endsWith(".gz")){
			os = new GZIPOutputStream(os); //{{ def.setLevel(Deflater.BEST_COMPRESSION); }};
		}
		PrintStream p = new PrintStream(os);
		p.flush();
		return p;
	}
	
	/* (non-Javadoc)
	 * @see org.archive.modules.Processor#report()
	 */
	@Override
	public String report() {
		StringBuilder b = new StringBuilder();
		b.append(super.report());
		b.append(String.format("  %-30.30s %d %n", "Number of processed URIs: ", _num_uris.get()));
		b.append(String.format("  %-30.30s %d %n", "Number of written URIs: ", _num_uris_written.get()));
		b.append(String.format("  %-30.30s %d %n", "Number of Bytes written: ", _num_bytes_written.get()));
		b.append(String.format("  %-30.30s %.3f %n", "Number of MBytes written: ", _num_bytes_written.get() / (1000d * 1000d)));
		b.append(String.format("  %-30.30s %.3f %n", "Number of GBytes written: ", _num_bytes_written.get() / (1000d * 1000d * 1000d)));
		b.append(String.format("  %-30.30s %d %n", "Number of Document Bytes written: ", _num_bytes_docs_written.get()));
		b.append(String.format("  %-30.30s %.3f %n", "Number of Document MBytes written: ", _num_bytes_docs_written.get() / (1000d * 1000d)));
		b.append(String.format("  %-30.30s %.3f %n", "Number of Document GBytes written: ", _num_bytes_docs_written.get() / (1000d * 1000d * 1000d)));
		return b.toString();
	}

}
