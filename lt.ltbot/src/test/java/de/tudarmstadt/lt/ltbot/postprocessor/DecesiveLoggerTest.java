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

import java.io.File;
import java.io.IOException;

import org.archive.modules.CrawlURI;
import org.archive.net.UURI;
import org.archive.net.UURIFactory;
import org.archive.spring.ConfigPath;
import org.json.JSONException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;

import de.tudarmstadt.lt.utilities.IOUtils;
import de.tudarmstadt.lt.ltbot.postprocessor.SharedConstants;

/**
 *
 * @author Steffen Remus
 **/
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DecesiveLoggerTest {

	static CrawlURI curi;
	static DecesiveValueLogger2 l;

	@BeforeClass
	public static void init() throws IOException {
		TemporaryFolder f = new TemporaryFolder();
		f.create();
		File temp_folder = f.getRoot();
		System.out.println("created temporary folder: " + temp_folder.getAbsolutePath());


		UURI uuri = UURIFactory.getInstance("http://localtest/test");
		curi = new CrawlURI(uuri);
		l = new DecesiveValueLogger2();
		l.setPath(new ConfigPath("test", temp_folder.getAbsolutePath()));
		l.start();
	}

	@AfterClass
	public static void teardown() {
		l.stop();
	}

	@Test
	public void a_noContent1() throws InterruptedException, JSONException, IOException {
		System.out.println("1");
		l.process(curi);
		Assert.assertTrue(IOUtils.readFile(new File(l.getPath().getFile(), l.getLogfile()).getAbsolutePath()).isEmpty());
	}

	@Test
	public void a_noContent2() throws InterruptedException, JSONException, IOException {
		System.out.println("2");
		curi.getExtraInfo().put("test", "test");
		l.process(curi);
		Assert.assertTrue(IOUtils.readFile(new File(l.getPath().getFile(), l.getLogfile()).getAbsolutePath()).isEmpty());
	}

	@Test
	public void a_noContent3() throws InterruptedException, JSONException, IOException {
		System.out.println("3");
		curi.getExtraInfo().put(SharedConstants.EXTRA_INFO_PERPLEXITY, String.format("%012g", 1e3));
		l.process(curi);
		Assert.assertTrue(IOUtils.readFile(new File(l.getPath().getFile(), l.getLogfile()).getAbsolutePath()).isEmpty());
	}

	@Test
	public void a_noContent4() throws InterruptedException, JSONException, IOException {
		System.out.println("4");
		curi.getExtraInfo().put("whatever", System.currentTimeMillis());
		l.process(curi);
		Assert.assertTrue(IOUtils.readFile(new File(l.getPath().getFile(), l.getLogfile()).getAbsolutePath()).isEmpty());
	}

	@Test
	public void b_content1() throws InterruptedException, JSONException, IOException {
		System.out.println("5");
		CrawlURI pred_curi = new CrawlURI(curi.getBaseURI());
		pred_curi.getExtraInfo().put(SharedConstants.EXTRA_INFO_ASSIGNED_SCHEDULING_DIRECTIVE, 3);
		pred_curi.getExtraInfo().put(SharedConstants.EXTRA_INFO_PERPLEXITY, String.format("%012g", 1e1));
		curi.setFullVia(pred_curi);
		curi.setVia(curi.getBaseURI());
		l.process(curi);
		checkContentLastLine(true);
	}

	@Test
	public void b_content2() throws InterruptedException, JSONException, IOException {
		System.out.println("6");
		curi.getExtraInfo().put(SharedConstants.EXTRA_INFO_PLAINTEXT_ABBREVIATED, "hello world");
		l.process(curi);
		checkContentLastLine(true);
	}

	@Test
	public void b_content3() throws InterruptedException, JSONException, IOException {
		System.out.println("7");
		curi.getExtraInfo().put(SharedConstants.EXTRA_INFO_ASSIGNED_SCHEDULING_DIRECTIVE, 1);
		l.process(curi);
		checkContentLastLine(true);
	}


	@Test
	public void b_content4() throws InterruptedException, JSONException, IOException {
		System.out.println("8");
		curi.getExtraInfo().put(SharedConstants.EXTRA_INFO_PLAINTEXT_ABBREVIATED, "hello world 50xA+1xB: AAAAAAAAAAAAAAAAAAAAAAAAAAAAB");
		l.process(curi);
		checkContentLastLine(true);
	}

	@Test
	public void b_content5() throws InterruptedException, JSONException, IOException {
		System.out.println("9");
		curi.getExtraInfo().put(SharedConstants.EXTRA_INFO_PERPLEXITY, String.format("%012g", Double.MAX_VALUE));
		l.process(curi);
		checkContentLastLine(true);
	}

	@Test
	public void b_content6() throws InterruptedException, JSONException, IOException {
		System.out.println("10");
		curi.getExtraInfo().put(SharedConstants.EXTRA_INFO_PERPLEXITY, String.format("%012g", Double.POSITIVE_INFINITY));
		l.process(curi);
		checkContentLastLine(true);
	}

	private void checkContentLastLine(boolean content) throws IOException {
		String fileContent = IOUtils.readFile(new File(l.getPath().getFile(), l.getLogfile()).getAbsolutePath());
		System.out.println(fileContent);
		String[] lines = fileContent.split("\n");
		String lastline = lines[lines.length - 1];
		String firstcol = lastline.split("\t")[0].trim();
		Assert.assertSame(content, Long.valueOf(firstcol) > 0);
	}
}
