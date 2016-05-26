package de.tudarmstadt.lt.lm;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.IteratorUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.berkeleylm.BerkeleyLM;
import de.tudarmstadt.lt.lm.mapbased.CountingLM;
import de.tudarmstadt.lt.lm.mapbased.LaplaceSmoothedLM;
import de.tudarmstadt.lt.lm.service.BreakIteratorStringProvider;
import de.tudarmstadt.lt.lm.service.LMProviderUtils;
import de.tudarmstadt.lt.lm.util.Properties;
import de.tudarmstadt.lt.utilities.LogUtils;

public class LanguageModelTest {

	static{
		LogUtils.setLogLevel("de.tudarmstadt.lt.lm", "trace");
	}


	private Set<List<String>> tri_grams = new HashSet<List<String>>(Arrays.asList(
			Arrays.asList("<s>", "<s>", "the"),
			Arrays.asList("<s>", "the", "quick"),
			Arrays.asList("the", "quick", "brown"),
			Arrays.asList("quick", "brown", "cat"),
			Arrays.asList("quick", "brown", "fox"),
			Arrays.asList("brown", "fox", "</s>"),
			Arrays.asList("brown", "cat", "</s>"),
			Arrays.asList("cat", "</s>", "</s>"),
			Arrays.asList("fox", "</s>", "</s>")
			
			));


	private final static Logger LOG = LoggerFactory.getLogger(LanguageModelTest.class);
	private final static int MAX_NGRAM_ORDER = 3;
	private static File _temp_folder;
	private static File _testresources_folder;

	@BeforeClass
	public static void setupTests() throws IOException {
		_testresources_folder = new File(ClassLoader.getSystemClassLoader().getResource("testlm").getPath());
	}

	@Before
	public void setupTest() throws IOException {
		TemporaryFolder f = new TemporaryFolder();
		f.create();
		_temp_folder = f.getRoot();
		System.out.println("created temporary folder: " + _temp_folder.getAbsolutePath());
	}


	@Test
	public void testSequenceHandling() throws Exception {
		List<String> seq = Arrays.asList("^ This is a very simple sequence . $".split(" "));
		// remember setting in project.properties
		String orig = Properties.get().getProperty("lt.lm.handleBoundaries");

		Properties.get().setProperty("lt.lm.handleBoundaries", "0");
		List<String>[] ngrams = LMProviderUtils.getNgramSequence(seq, 4);
		System.out.println(Arrays.asList(ngrams));
		Assert.assertArrayEquals("^ This is a".split(" "), ngrams[0].toArray());
		Assert.assertArrayEquals("This is a very".split(" "), ngrams[1].toArray());

		Properties.get().setProperty("lt.lm.handleBoundaries", "1");
		ngrams = LMProviderUtils.getNgramSequence(seq, 4);
		System.out.println(Arrays.asList(ngrams));
		Assert.assertArrayEquals("^ ^ ^ This".split(" "), ngrams[0].toArray());
		Assert.assertArrayEquals("^ ^ This is".split(" "), ngrams[1].toArray());

		Properties.get().setProperty("lt.lm.handleBoundaries", "2");
		ngrams = LMProviderUtils.getNgramSequence(seq, 4);
		System.out.println(Arrays.asList(ngrams));
		Assert.assertArrayEquals("^ This".split(" "), ngrams[0].toArray());
		Assert.assertArrayEquals("^ This is".split(" "), ngrams[1].toArray());
		Assert.assertArrayEquals("^ This is a".split(" "), ngrams[2].toArray());
		Assert.assertArrayEquals("This is a very".split(" "), ngrams[3].toArray());

		// reset original setting in project.properties
		Properties.get().setProperty("lt.lm.handleBoundaries", orig);
	}

	@Test
	public void createAndSaveBerkelyLMFromTextFile() throws Exception {
		LOG.debug("-- createBerkelyLMFromTextFile --");
		// File tmp = new File(_testresources_folder, "test.arpa.gz");
//		File tmp = new File(_temp_folder, "test.arpa.gz");
		
		BerkeleyLM<String> language_model = (BerkeleyLM<String>)LanguageModelHelper.createBerkelyLmFromTxtFilesInDirectory(new BreakIteratorStringProvider(), _testresources_folder, MAX_NGRAM_ORDER, -10f, -1, 1, false);

		assertEquals(8, language_model.get().getWordIndexer().numWords());

		for (int i = 0; i < language_model.get().getWordIndexer().numWords(); i++)
			LOG.debug(String.format("%.4f <- %s",
					language_model.getNgramLogProbability(new int[] { i }),
					language_model.getWord(i)));

		// LanguageModelHelper.saveAsBinary(language_model, new File(_testresources_folder, "test.blm.gz"));
		LanguageModelHelper.saveAsBinary(language_model, new File(_temp_folder, "test.blm.gz"));
	}

	@Test
	public void readBerkelyLMFromArpa() throws Exception {
		LOG.debug("-- readBerkelyLMFromArpa --");
		File arpa = new File(_testresources_folder, "testlm."+MAX_NGRAM_ORDER+".arpa.gz");

		LanguageModelHelper.createBerkelyLmFromTxtFilesInDirectory(new BreakIteratorStringProvider(), _testresources_folder, MAX_NGRAM_ORDER, -10f, -1, 1, false);
		BerkeleyLM<String> language_model = (BerkeleyLM<String>)LanguageModelHelper.readFromArpa(arpa,-10f);

		assertEquals(8, language_model.get().getWordIndexer().numWords());

		for (int i = 0; i < language_model.get().getWordIndexer().numWords(); i++)
			LOG.debug(String.format("%.4f <- %s",
					language_model.getNgramLogProbability(new int[] { i }),
					language_model.getWord(i)));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testBerkleyNgrams() throws Exception {
		LOG.debug("-- testNgrams --");
		
		BerkeleyLM<String> language_model = (BerkeleyLM<String>)LanguageModelHelper.createBerkelyLmFromTxtFilesInDirectory(new BreakIteratorStringProvider(), _testresources_folder, MAX_NGRAM_ORDER, -10f, -1, 1, false);

		List<List<String>> ngrams = IteratorUtils.toList(language_model.getNgramIterator());
		System.out.println(ngrams);
		assertEquals(false, ngrams.retainAll(tri_grams));

	}

	@Test
	public void testBerkelyPrediction() throws Exception {
		
		BerkeleyLM<String> lm = (BerkeleyLM<String>)LanguageModelHelper.createBerkelyLmFromTxtFilesInDirectory(new BreakIteratorStringProvider(), _testresources_folder, MAX_NGRAM_ORDER, -10f, -1, 1, false);
		lm.activateCache(true);
		// System.out.println(lm.predictNextWord(Arrays.asList("quick fox jump".split(" "))));
		// System.out.println(lm.predictNextWord(Arrays.asList("bla bla bla bla bla bla bla cat".split(" "))));
		System.out.println(lm.predictNextWord(Arrays.asList("the quick".split(" "))));
		// System.out.println(lm.predictNextWord(Arrays.asList("quick brown".split(" "))));

	}

	@Test
	public void testCountingLM() {
		
		CountingLM<String> lm = (CountingLM<String>)LanguageModelHelper.createCountingLmTxtFilesInDirectory(new BreakIteratorStringProvider(), _testresources_folder, MAX_NGRAM_ORDER);
		LaplaceSmoothedLM<String> lmm = new LaplaceSmoothedLM<String>(lm);

		System.out.println(lm.getNgramLogProbability(Arrays.asList("quick fox jump".split(" "))));
		System.out.println(lmm.getNgramLogProbability(Arrays.asList("quick fox jump".split(" "))));
		System.out.println(lmm.getNgramLogProbability(Arrays.asList("bla bla cat".split(" "))));

		System.out.println(lm.getNgramLogProbability(Arrays.asList("the quick brown".split(" "))));
		System.out.println(lm.getNgramLogProbability(Arrays.asList("quick brown cat".split(" "))));
		System.out.println(lm.getNgramLogProbability(Arrays.asList("quick brown fox".split(" "))));
	}


	@Test
	//	@Ignore // FIXME:
	public void testCountingLMsPrediction() throws IllegalAccessException {
		CountingLM<String> lm = new CountingLM<String>(MAX_NGRAM_ORDER);
		for(List<String> ngram : LMProviderUtils.getNgramSequence(Arrays.asList("The quick brown fox".split(" ")), lm.getOrder()))
			lm.addNgram(ngram);
		for(List<String> ngram : LMProviderUtils.getNgramSequence(Arrays.asList("The quick brown cat".split(" ")), lm.getOrder()))
			lm.addNgram(ngram);

		LaplaceSmoothedLM<String> lmm = new LaplaceSmoothedLM<String>(lm);

		System.out.println(lm.predictNextWord(Arrays.asList("quick fox jump".split(" "))));
		System.out.println(lmm.predictNextWord(Arrays.asList("bla bla bla bla bla bla The quick fox".split(" "))));
		System.out.println(lm.predictNextWord(Arrays.asList("The quick".split(" "))));
		System.out.println(lm.predictNextWord(Arrays.asList("quick brown".split(" "))));
	}

	@Test
	public void testNgramSequences() throws Exception {
		System.out.println(Arrays.asList(
				LMProviderUtils.getNgramSequence(Arrays.asList("<s> <s> Hello brave new world . </s>".split(" ")), MAX_NGRAM_ORDER)));
		// System.out.println(Arrays.asList(
		// AbstractLanguageModel.getNgramSequence(Arrays.asList("".split(" ")), 3)));
		// System.out.println(Arrays.asList(
		// AbstractLanguageModel.getNgramSequence(Arrays.asList("Hallo".split(" ")), 3)));
		// System.out.println(Arrays.asList(
		// AbstractLanguageModel.getNgramSequence(Arrays.asList("Hallo schöne".split(" ")), 3)));
		// System.out.println(Arrays.asList(
		// AbstractLanguageModel.getNgramSequence(Arrays.asList("Hallo schöne neue".split(" ")), 3)));

	}

	@Test
	public void testReadWithBreakIteratorProvider() throws Exception{
		LanguageModel<String> lm = LanguageModelHelper.createBerkelyLmFromTxtFilesInDirectory(new BreakIteratorStringProvider(), _testresources_folder, MAX_NGRAM_ORDER, Properties.knUnkLog10Prob(), -1, 1, false);


		System.out.println(lm.predictNextWord(Arrays.asList("quick fox jump".split(" "))));
		System.out.println(lm.predictNextWord(Arrays.asList("bla bla bla bla bla bla the quick fox".split(" "))));
		System.out.println(lm.predictNextWord(Arrays.asList("the quick".split(" "))));
		System.out.println(lm.predictNextWord(Arrays.asList("quick brown".split(" "))));
	}


}
