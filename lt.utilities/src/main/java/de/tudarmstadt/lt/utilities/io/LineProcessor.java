/*
 *   Copyright 2015
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
package de.tudarmstadt.lt.utilities.io;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Steffen Remus
 *
 */
public abstract class LineProcessor implements Runnable {
	
	private final static Logger LOG = LoggerFactory.getLogger(LineProcessor.class);
	
	boolean _parallel;
	String _file;
	String _out;
	PrintStream _pout;
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
		_pout = System.out;
		if(!"-".equals(_out)){
			try {
				_pout = new PrintStream(new FileOutputStream(new File(_out), true));
			} catch (FileNotFoundException e) {
				LOG.error("Could not open ouput file '{}' for writing.", _out, e);
				System.exit(1);
			}
		}

		if("-".equals(_file.trim())){
			LOG.info("Processing text from stdin ('{}').", _file);
			try{run(new InputStreamReader(System.in, "UTF-8"));}catch(Exception e){LOG.error("Could not process file '{}'.", _file, e);}
		}else{

			File f_or_d = new File(_file);
			if(!f_or_d.exists())
				throw new Error(String.format("File or directory '%s' not found.", _file));

			if(f_or_d.isFile()){
				LOG.info("Processing file '{}'.", f_or_d.getAbsolutePath());
				try{run(new InputStreamReader(new FileInputStream(f_or_d), "UTF-8"));}catch(Exception e){LOG.error("Could not process file '{}'.", f_or_d.getAbsolutePath(), e);}
			}

			if(f_or_d.isDirectory()){
				File[] txt_files = f_or_d.listFiles(new FileFilter(){
					@Override
					public boolean accept(File f) {
						return f.isFile() && f.getName().endsWith(".txt");
					}});

				for(int i = 0; i < txt_files.length; i++){
					File f = txt_files[i];
					LOG.info("Processing file '{}' ({}/{}).", f.getAbsolutePath(), i + 1, txt_files.length);

					try{ run(new InputStreamReader(new FileInputStream(f), "UTF-8")); }catch(Exception e){LOG.error("Could not process file '{}'.", f.getAbsolutePath(), e);}

				}
			}
		}
	}
	
	void run(Reader r) {
		if(_parallel)
			runParallel(r);
		else
			runSequential(r);
	}
	
	void runParallel(Reader r) {
		throw new NotImplementedException("This functionality is not implemented here");
		// TODO: once java8 is activated uncomment the following lines 
//		BufferedReader br = new BufferedReader(r);
//		br.lines().parallel().forEach(line -> processLine(line));
	}
	
	void runSequential(Reader r) {
		
		long l = 0;
		for(LineIterator liter = new LineIterator(r); liter.hasNext(); ){
			if(++l % 5000 == 0)
				LOG.info("processing line {}.", l);
			String line = liter.next();
			processLine(line);
		}
		
	}

	abstract void processLine(String line);
	
	public synchronized void println(String toPrint){
		_pout.println(toPrint);
	}

}
