/*
 *   Copyright 2013
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

import java.io.File;
import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import org.apache.commons.io.FileUtils;
import org.archive.crawler.framework.CrawlController;
import org.archive.modules.CrawlURI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.lt.lm.LanguageModel;
import de.tudarmstadt.lt.lm.LanguageModelHelper;
import de.tudarmstadt.lt.lm.service.AbstractStringProvider;
import de.tudarmstadt.lt.lm.service.BreakIteratorStringProvider;
import de.tudarmstadt.lt.lm.util.Properties;
import de.tudarmstadt.lt.ltbot.text.UTF8CleanerExt;
import de.tudarmstadt.lt.ltbot.writer.SentenceMaker;

/**
 *
 * @author Steffen Remus
 */
public abstract class DecesiveValuePerplexityTest extends ProcessorTestbase {

	static DecesiveValueProducerPerplexity _prio;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		setupPriorityPostProcessor();
	}

	@Override
	@After
	public void tearDown() throws AccessException, RemoteException, NotBoundException {
		_prio.stop();
		super.tearDown();
	}

	private void setupPriorityPostProcessor() throws Exception {
		// declare variables
		File src_dir = new File(ClassLoader.getSystemClassLoader().getResource("testlm").getPath());
		int lm_order = 5;

		// setup test languagemodelserver
		setup_LM_Server(src_dir, lm_order);

		// setup priority post processor
		_prio = new DecesiveValueProducerPerplexity();
		_prio.setRmihost(_host);
		_prio.setRmiport(_rmiport);
		_prio.setServiceID(_serviceId);
		_prio.getTextExtractor().setUtf8Cleaner(new UTF8CleanerExt());
		_prio.setCrawlController(new CrawlController());
		_prio.setSentenceMaker(new SentenceMaker());
		_prio.start();
	}

	abstract void setup_LM_Server(File src_dir, int lm_order) throws Exception;

	@Test
	public void test_local() throws Exception {
		File f = new File(ClassLoader.getSystemClassLoader().getResource("untokenizable.txt").getPath());
		String text = FileUtils.readFileToString(f, "UTF-8");
		_prio.computePerplexity(text);//, "http://localtest/test");
	}

	@Test
	public void test_remote() throws InterruptedException, IOException {
		String uri = "http://www.jil.go.jp/index.htm";
		//		String uri = "http://www.eduserver.de/termine/termineausgabeliste_e.html";
		//		String uri = "http://www.bildung-weltweit.de/bisy.html?a=6106&mstn=14";
		//		String uri = "http://www.fwu-mediathek.de/search?template=plain&mask=fach&fach=220&pid=9jodl4hr8mikkr2cgvbek0vhv0";
		//		String uri = "http://www.fwu-mediathek.de/search?template=plain&mask=fach&fach=100&pid=is74un8lpkjchv9rfno3pk3bm2";


		CrawlURI curi = crawlURI(uri);

		String content = _prio.getTextExtractor().getCleanedUtf8PlainText(curi);
		System.out.println(content);
		//		// print content as bytes
		//		byte[] b = content.getBytes("UTF-8");
		//		for (int i = 0; i < b.length; i++)
		//			System.out.format("%d %s %n", b[i] & 0xff, (char) b[i]);

		_prio.process(curi);

		System.out.println(_prio.report());

	}
	
//	public static class TestUimaStringProvider extends DecesiveValuePerplexityTest{
//		@Override
//		void setup_LM_Server(File src_dir, int lm_order) throws Exception{
//			final AbstractStringProvider stub = new BreakIteratorStringProvider();
//			stub.publish(LocateRegistry.getRegistry(_host, _rmiport), _serviceId, _serviceport);
//			// load lm
//			LanguageModel<String> lm = LanguageModelHelper.createBerkelyLmFromTxtFilesInDirectory(stub, src_dir, lm_order, Properties.kneserNeyUnkLog2Prob());
//			stub.setLanguageModel(lm);
//		}
//	}

	public static class TestBreakIteratorStringProvider extends DecesiveValuePerplexityTest{
		@Override
		void setup_LM_Server(File src_dir, int lm_order) throws Exception{
			final AbstractStringProvider stub = new BreakIteratorStringProvider();
			stub.publish(LocateRegistry.getRegistry(_host, _rmiport), _serviceId, _serviceport);
			// load lm
			LanguageModel<String> lm = LanguageModelHelper.createBerkelyLmFromTxtFilesInDirectory(stub, src_dir, lm_order, Properties.knUnkLog10Prob(), .7, 1, true);
			stub.setLanguageModel(lm);
		}
	}

}
