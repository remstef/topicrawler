package de.tudarmstadt.lt.lm.perplexity;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.tudarmstadt.lt.lm.LanguageModelHelper;
import de.tudarmstadt.lt.lm.berkeleylm.BerkeleyLM;
import de.tudarmstadt.lt.lm.mapbased.CountingLM;
import de.tudarmstadt.lt.lm.service.AbstractStringProvider;
import de.tudarmstadt.lt.lm.service.BreakIteratorStringProvider;
import de.tudarmstadt.lt.lm.service.LMProviderUtils;
import de.tudarmstadt.lt.lm.service.PreTokenizedStringProvider;
import de.tudarmstadt.lt.lm.util.Properties;
import de.tudarmstadt.lt.utilities.LogUtils;

public class PerplexityTest {
	static {
		LogUtils.setLogLevel("de.tudarmstadt.lt.lm", "trace");
	}

	private File _test_folder = new File(ClassLoader.getSystemClassLoader().getResource("testlm").getPath());

	@Test
	public void compareWithNativeBerkeleyPerplexityValues() throws Exception {
		BerkeleyLM<String> lm = (BerkeleyLM<String>)LanguageModelHelper.createBerkelyLmFromTxtFilesInDirectory(new BreakIteratorStringProvider(), _test_folder, 3, -10, -1, 1, false);

		List<String> t = Arrays.asList("the quick brown fox".split(" "));
		System.out.format("Test sentence as tokens: %s %n", t.toString());

		// compute perplexity
		AbstractStringProvider lmp = new PreTokenizedStringProvider();
		lmp.setLanguageModel(lm);
		ModelPerplexity<String> p = new ModelPerplexity<String>(lmp);
		@SuppressWarnings("unchecked")
		List<String>[] ngram_sequence = new List[]{
			Arrays.asList("<s> the".split(" ")),
			Arrays.asList("<s> the quick".split(" ")),
			Arrays.asList("the quick brown".split(" ")),
			Arrays.asList("quick brown fox".split(" ")),
			Arrays.asList("brown fox </s>".split(" ")),
		}; // thats how berkely lm handles sentence boundaries

		for (List<String> ngram : ngram_sequence)
			p.addLog10Prob(ngram);

		double perplexity = p.get();
		System.out.format("Perplexity(%s)=%f; (including properly handled sentence boundaries) %n", t, perplexity);

		// compute perplexity with BerkeleyLM
		double berkeley_sentscore = lm.getNativeSequenceLogProbability(t); // BerkeleyLm itself takes care of sentence boundaries
		System.out.format("BerkeleyLmSentenceScore(%s): %f %n", t, berkeley_sentscore);
		Assert.assertEquals(berkeley_sentscore, lm.getSequenceLogProbability(ngram_sequence), 1E-5d);

		// transformed sentscore should be equal to perplexity value
		long N = ngram_sequence.length; // unfortunately BerkeleyLM does not provide the number of ngrams of from which the probability was calculated
		double perplexity_by_berkeley_sentscore = Math.pow(2, (-(berkeley_sentscore / Math.log10(2)) / N)); // BerkeleyLm uses log10 probabilities
		System.out.format("PerplexityByBerkeleyLmSentenceScore(%s)=%f; (including properly handled sentence boundaries by BerkeleyLm) %n", t, perplexity_by_berkeley_sentscore);

		// assert the equality
		Assert.assertEquals(perplexity_by_berkeley_sentscore, perplexity, 1E-5d);
	}

	@SuppressWarnings({ "static-access", "deprecation" })
	@Test
	public void testPerplexity() throws Exception {
		System.out.println("---");
		CountingLM<String> lm = new CountingLM<String>(3);
		AbstractStringProvider lmp =  new PreTokenizedStringProvider();
		lmp.setLanguageModel(lm);
		for(List<String> ngram : lmp.getNgramSequence(Arrays.asList("The quick brown fox".split(" "))))
			lm.addNgram(ngram);
		for(List<String> ngram : lmp.getNgramSequence(Arrays.asList("The quick brown cat".split(" "))))
			lm.addNgram(ngram);
		ProbDistPerplexity<String> perp = new ProbDistPerplexity<String>(lm);
		List<String> test = Arrays.asList("The quick brown fox".split(" "));
		for (List<String> ngram : LMProviderUtils.getNgramSequence(test, lm.getOrder()))
			perp.addLog2Prob(ngram);

		Assert.assertEquals(perp.calculatePerplexity(lm, lmp, test), perp.get(), 1e-10);

		// 2^(1/2) == 1.4142
		Assert.assertEquals(1.4142, perp.get(), 1e-4);
		System.out.println("---");

		List<String> test2 = Arrays.asList("The quick brown cat".split(" "));
		Assert.assertEquals(1.4142, perp.calculatePerplexity(lm, lmp, test2), 1e-4);
	}

	@SuppressWarnings("static-access")
	@Test
	public void testModelPerplexity() throws Exception {
		System.out.println("---");
		int reset_boundary_property = Properties.handleBoundaries();
		Properties.get().setProperty("lt.lm.handleBoundaries", "0");
		CountingLM<String> lm = new CountingLM<String>(3);
		AbstractStringProvider lmp =  new PreTokenizedStringProvider();
		lmp.setLanguageModel(lm);
		for(List<String> ngram : lmp.getNgramSequence(Arrays.asList("The quick brown fox".split(" "))))
			lm.addNgram(ngram);
		for(List<String> ngram : lmp.getNgramSequence(Arrays.asList("The quick brown cat".split(" "))))
			lm.addNgram(ngram);
		ModelPerplexity<String> perp = new ModelPerplexity<String>(lmp);
		List<String> test = Arrays.asList("The quick brown fox".split(" "));
		for (List<String> ngram : LMProviderUtils.getNgramSequence(test, lm.getOrder()))
			perp.addLog10Prob(ngram);

		Assert.assertEquals(perp.calculatePerplexity(lm, lmp, test, false), perp.get(), 1e-10);

		// 2 ngrams: [[The, quick, brown], [quick, brown, fox]]
		// 2^(1/2) == 1.41421 == (1/2)^(-1/2)
		Assert.assertEquals(1.41421, perp.get(), 1e-4);
		System.out.println("---");

		List<String> test2 = Arrays.asList("The quick brown cat".split(" "));
		Assert.assertEquals(1.41421, perp.calculatePerplexity(lm, lmp, test2, false), 1e-4);
		
		Properties.get().setProperty("lt.lm.handleBoundaries", String.valueOf(reset_boundary_property));
	}

	@SuppressWarnings("static-access")
	@Test
	public void testPerplexityStability() throws Exception {
		BerkeleyLM<String> lm = (BerkeleyLM<String>)LanguageModelHelper.createBerkelyLmFromTxtFilesInDirectory(new BreakIteratorStringProvider(), _test_folder, 3, -10, -1, 1, false);
		AbstractStringProvider lmp =  new PreTokenizedStringProvider();
		lmp.setLanguageModel(lm);
		ModelPerplexity<String> perp = new ModelPerplexity<String>(lmp);
		//		List<String> test = Arrays.asList("<s> The quick brown fox </s>".split(" "));
		List<String> test1 = Arrays.asList("<s> d sd sdv sdf vsf vw evw ev".split(" "));
		List<String> test2 = Arrays.asList("<s> d sd sdv sdf vsf vw nj evw ev vf".split(" "));

		Assert.assertEquals(perp.calculatePerplexity(lm, lmp, test1, false), perp.calculatePerplexity(lm, lmp, test2, false), 1e-10);
 	}
	
	@Test
	public void oneMoreTest() throws Exception{
		int b = Properties.handleBoundaries();
		int s = Properties.insertSentenceTags();
		
		Properties.get().setProperty("lt.lm.handleBoundaries", "0");
		Properties.get().setProperty("lt.lm.insertSentenceTags", "3");
		AbstractStringProvider p =  new BreakIteratorStringProvider();
		BerkeleyLM<String> lm = (BerkeleyLM<String>)LanguageModelHelper.createBerkelyLmFromTxtFilesInDirectory(p, _test_folder, 3, -10, -1, 1, false);
		p.setLanguageModel(lm);
		Properties.get().setProperty("lt.lm.handleBoundaries", "2");
		Properties.get().setProperty("lt.lm.insertSentenceTags", "1");
		
		List<String> test1 = Arrays.asList("<s> the quick brown fox".split(" "));
		List<String> test2 = Arrays.asList("<s> the quick black fox".split(" "));
		List<String> test3 = Arrays.asList("<s> fox".split(" "));
		List<String> test4 = Arrays.asList("<s> the".split(" "));
		List<String> test5 = Arrays.asList("<s> unk".split(" "));
		
		Assert.assertEquals(
				ModelPerplexity.calculatePerplexity(lm, p, test1, false), 
				p.getPerplexity("the quick brown fox" , false), 
				1e-10);
		
		Assert.assertEquals(
				ModelPerplexity.calculatePerplexity(lm, p, test2, false), 
				p.getPerplexity("the quick black fox" , false), 
				1e-10);
		
		Assert.assertEquals(
				ModelPerplexity.calculatePerplexity(lm, p, test3, false), 
				p.getPerplexity("fox" , false), 
				1e-10);
		
		Assert.assertEquals(
				ModelPerplexity.calculatePerplexity(lm, p, test4, false), 
				p.getPerplexity("the" , false), 
				1e-10);
		
		Assert.assertEquals(
				ModelPerplexity.calculatePerplexity(lm, p, test5, false), 
				p.getPerplexity("unk" , false), 
				1e-10);
		
		Properties.get().setProperty("lt.lm.handleBoundaries", Integer.toString(b));
		Properties.get().setProperty("lt.lm.insertSentenceTags", Integer.toString(s));
		
	}
}