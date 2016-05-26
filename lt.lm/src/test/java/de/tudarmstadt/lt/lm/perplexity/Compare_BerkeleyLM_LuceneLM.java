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
package de.tudarmstadt.lt.lm.perplexity;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.LanguageModel;
import de.tudarmstadt.lt.lm.LanguageModelHelper;
import de.tudarmstadt.lt.lm.app.GenerateNgramIndex;
import de.tudarmstadt.lt.lm.lucenebased.KneserNeyLMRecursive;
import de.tudarmstadt.lt.lm.lucenebased.OneBackoffKneserNeyLM;
import de.tudarmstadt.lt.lm.lucenebased.PoptKneserNeyLMRecursive;
import de.tudarmstadt.lt.lm.lucenebased.StupidBackoffLM;
import de.tudarmstadt.lt.lm.service.AbstractStringProvider;
import de.tudarmstadt.lt.lm.service.LtSegProvider;
import de.tudarmstadt.lt.lm.service.PreTokenizedStringProvider;
import de.tudarmstadt.lt.lm.util.Properties;
import de.tudarmstadt.lt.utilities.IOUtils;
import de.tudarmstadt.lt.utilities.ProcessUtils;
import de.tudarmstadt.lt.utilities.LogUtils;

/**
 * @author Steffen Remus
 *
 */
public class Compare_BerkeleyLM_LuceneLM {
	
	private final static Logger LOG = LoggerFactory.getLogger(Compare_BerkeleyLM_LuceneLM.class);

	static File _base_dir = new File(ClassLoader.getSystemClassLoader().getResource("ptest").getPath());
	static File _train_dir = new File(_base_dir,"train");
	static File _test_dir = new File(_base_dir,"test");
	static File _tiny_test_dir = new File(_base_dir,"test_tiny");

	static String _tiny_test;
	static String _test;

	static int _n = 5;
	static int _mincount = 0;
	static double _discount = -1;
	
	static AbstractStringProvider _provider = new PreTokenizedStringProvider();
	
	static PrintWriter _pw;
	
	static int reset_boundary_property;
	
	@BeforeClass
	public static void setup(){
		reset_boundary_property = Properties.handleBoundaries();
		Properties.get().setProperty("lt.lm.handleBoundaries", "0");
		LogUtils.setLogLevel("de.tudarmstadt.lt.lm", "info");
		try {
			_tiny_test = IOUtils.readFile(new File(_tiny_test_dir, "test_perplex_tiny.txt").getAbsolutePath());
			_test = IOUtils.readFile(new File(_test_dir, "test_perplex.txt").getAbsolutePath());		
			
			GenerateNgramIndex.generate_index(_train_dir, new PreTokenizedStringProvider(), 1, _n, _mincount, true/*re-create index*/);
			
			_pw = new PrintWriter(new File(_base_dir, "out.txt"));
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Properties.get().setProperty("lt.lm.insertSentenceTags", "1");
		Properties.get().setProperty("lt.lm.knMaxbackoffrecursions", "-1");
//		LogUtils.setLogLevel("de.tudarmstadt.lt.lm", "trace");
	}
	

	
	
	
	@AfterClass
	public static void cleanup(){
		Properties.get().setProperty("lt.lm.handleBoundaries", String.valueOf(reset_boundary_property));
		_pw.flush();
		_pw.close();
		
		ProcessUtils.run_process("/usr/local/bin/python3 -c \""
				+"from pandas import *" + "\n"
				+"import os; print(os.getcwd())"  + "\n"
				+"import matplotlib.pyplot as plt"  + "\n"

				+"f_eval = 'out.txt'"  + "\n"
				+"df = read_csv(f_eval, "  + "\n"
				+"              sep='\t',"  + "\n"
				+"              header=None,"   + "\n"
				+"              names=['lmtype', 'lmorder', 'testfile', 'perplexity'])"  + "\n"

				+"df = df[df['testfile'] == 'test' ]"  + "\n"
				+"df_r = df.pivot('lmorder', 'lmtype', 'perplexity')"  + "\n"
				+"print(df_r)"  + "\n"

				+"fig, axes = plt.subplots(nrows=1, ncols=2)"  + "\n"
				+"#df_r[['OneBackoffKneserNeyLM','BerkeleyLM','KneserNeyLM','KneserNeyLMRecursive','PoptKneserNeyLMRecursive']].plot(ax=axes[0])"  + "\n"
				+"#df_r[['OneBackoffKneserNeyLM_oov','BerkeleyLM_oov','KneserNeyLM_oov','KneserNeyLMRecursive_oov','PoptKneserNeyLMRecursive_oov']].plot(ax=axes[1])"  + "\n"
				+"df_r[['StupidBackoffLM','KneserNeyLM','BerkeleyLM','PoptKneserNeyLMRecursive']].plot(ax=axes[0])"  + "\n"
				+"df_r[['StupidBackoffLM_oov','KneserNeyLM_oov','BerkeleyLM_oov','PoptKneserNeyLMRecursive_oov']].plot(ax=axes[1])"  + "\n"
				+"for ax in axes: ax.legend(loc='best')"  + "\n"

				+"plt.show()"  + "\n"
				+"plt.ylabel('Perplexity')"  + "\n"
				
				+"\"", _base_dir, true, false);
		
//		ProcessUtils.run_process("/usr/local/bin/python3 plot.py out.txt", _base_dir, true);
	}

	@Test
	public void berkeleylm() throws Exception{
		IntStream.rangeClosed(2, _n).parallel().forEach( n -> {
			LanguageModel<String> lm = LanguageModelHelper.createBerkelyLmFromTxtFilesInDirectory(_provider, _train_dir, n, -6.735585680080748f, _discount, _mincount, true);
			runtest(lm);
		});
	}
	
	@Test
	public void stupidBackoffLm() throws Exception{
		IntStream.rangeClosed(2, _n).parallel().forEach( n -> {
			LanguageModel<String> lm = LanguageModelHelper.createLuceneBasedKneserNeyLMFromTxtFilesInDirectory(_provider, _train_dir, n, _mincount, _discount, false);
			lm = new StupidBackoffLM(n, new File(_train_dir, ".lmindex"),  .4);
			runtest(lm);
		});
	}

	@Test
	public void kneserneylm() throws Exception{
		IntStream.rangeClosed(2, _n).parallel().forEach( n -> {
			LanguageModel<String> lm = LanguageModelHelper.createLuceneBasedKneserNeyLMFromTxtFilesInDirectory(_provider, _train_dir, n, _mincount, _discount, false);
			runtest(lm);
		});
	}

	@Test
	public void kneserneylmPoptRecursive() throws Exception{
		IntStream.rangeClosed(2, _n).parallel().forEach( n -> {
			LanguageModel<String> lm = LanguageModelHelper.createLuceneBasedKneserNeyLMFromTxtFilesInDirectory(_provider, _train_dir, n, _mincount, _discount, false);
			lm = new PoptKneserNeyLMRecursive(n, new File(_train_dir, ".lmindex"),  _discount);
			runtest(lm);
		});
	}

	@Test
	public void kneserneylmRecursive() throws Exception{
		IntStream.rangeClosed(2, _n).parallel().forEach( n -> {
			LanguageModel<String> lm = LanguageModelHelper.createLuceneBasedKneserNeyLMFromTxtFilesInDirectory(_provider, _train_dir, n, _mincount, _discount, false);
			lm = new KneserNeyLMRecursive(n, new File(_train_dir, ".lmindex"),  _discount);
			runtest(lm);
		});
	}
	
	@Test
	public void kneserneylmOneBackoff() throws Exception{
		IntStream.rangeClosed(2, _n).parallel().forEach( n -> {
			LanguageModel<String> lm = LanguageModelHelper.createLuceneBasedKneserNeyLMFromTxtFilesInDirectory(_provider, _train_dir, n, _mincount, _discount, false);
			lm = new OneBackoffKneserNeyLM(n, new File(_train_dir, ".lmindex"),  _discount);
			runtest(lm);
		});
	}

	@SuppressWarnings("unchecked")
	public void runtest(LanguageModel<String> lm) {
		try {

			AbstractStringProvider provider = new PreTokenizedStringProvider();
			provider.setLanguageModel(lm);

			if(LOG.isDebugEnabled()){
				System.out.println(provider.getSequenceLog10Probability(_tiny_test));
				Arrays.stream(provider.getNgrams(_tiny_test)).map(x->{
					try{return provider.getNgramAsIds(x);}catch(Exception e){return new int[0];}
				}).map(x->{
					try{return provider.getNgramAsWords((int[])x);}catch(Exception e){return Collections.emptyList();}
				}).forEach(x -> {
					try{System.out.println(x + " " + provider.getLanguageModel().getNgramLogProbability((List<String>)x));}catch(Exception e){}
				});
			}

			double tp = provider.getPerplexity(_tiny_test, false);
			double p = provider.getPerplexity(_test, false);
			String o = String.format("%1$s\t%2$d\ttinytest\t%3$6.3e%n%1$s\t%2$d\ttest\t%4$6.3e", lm.getClass().getSimpleName(), lm.getOrder(), tp, p);
			System.out.println(o);
			_pw.println(o);
			
			tp = provider.getPerplexity(_tiny_test, true);
			p = provider.getPerplexity(_test, true);
			o = String.format("%1$s\t%2$d\ttinytest\t%3$6.3e%n%1$s\t%2$d\ttest\t%4$6.3e", lm.getClass().getSimpleName() + "_oov", lm.getOrder(), tp, p);
			System.out.println(o);
			_pw.println(o);

			//		System.out.println(_provider.getPerplexity("3fre 4wfc wr4fff fwe45t fsa4f"));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
