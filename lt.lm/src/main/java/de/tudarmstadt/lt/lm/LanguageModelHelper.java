package de.tudarmstadt.lt.lm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.app.GenerateNgramIndex;
import de.tudarmstadt.lt.lm.berkeleylm.BerkeleyLM;
import de.tudarmstadt.lt.lm.berkeleylm.KneserNeyLmReaderCallbackWrapper;
import de.tudarmstadt.lt.lm.berkeleylm.TextReader;
import de.tudarmstadt.lt.lm.lucenebased.CountingStringLM;
import de.tudarmstadt.lt.lm.lucenebased.KneserNeyLM;
import de.tudarmstadt.lt.lm.mapbased.CountingLM;
import de.tudarmstadt.lt.lm.service.AbstractStringProvider;
import de.tudarmstadt.lt.lm.util.Properties;
import edu.berkeley.nlp.lm.ArrayEncodedNgramLanguageModel;
import edu.berkeley.nlp.lm.ArrayEncodedProbBackoffLm;
import edu.berkeley.nlp.lm.ConfigOptions;
import edu.berkeley.nlp.lm.StringWordIndexer;
import edu.berkeley.nlp.lm.WordIndexer;
import edu.berkeley.nlp.lm.io.ArpaLmReader;
import edu.berkeley.nlp.lm.io.KneserNeyFileWritingLmReaderCallback;
import edu.berkeley.nlp.lm.io.LmReaders;


public class LanguageModelHelper {

	private static Logger LOG = LoggerFactory.getLogger(LanguageModelHelper.class);



	private LanguageModelHelper(){ /*DO NOT INSTANTIATE */ }

	public static <W> BerkeleyLM<W> readFromBinary(File file) {
		@SuppressWarnings("unchecked")
		ArrayEncodedNgramLanguageModel<W> berkeley_language_model = (ArrayEncodedNgramLanguageModel<W>) LmReaders.readLmBinary(file.getAbsolutePath());
		return new BerkeleyLM<W>(berkeley_language_model);
	}

	public static <W> void saveAsBinary(BerkeleyLM<W> blm, File file) {
		LmReaders.writeLmBinary(blm.get(), file.getAbsolutePath());
	}

	public static LanguageModel<String> createCountingLmTxtFilesInDirectory(AbstractStringProvider stringProvider, File srcdir, int order) {
		CountingLM<String> lm = new CountingLM<String>(order);
		stringProvider.setLanguageModel(lm);

		File[] txtfiles = srcdir.listFiles(new java.io.FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isFile() && pathname.getName().endsWith(".txt");
			}
		});

		String[] basenames = new String[txtfiles.length];
		for(int i = 0; i < basenames.length; i++)
			basenames[i] = txtfiles[i].getName();

		LOG.info(String.format("Reading language model from dir: '%s'; Files: %s.", srcdir.getAbsolutePath(), StringUtils.abbreviate(Arrays.toString(basenames), 200)));

		for(int i = 0; i < txtfiles.length; i++){
			File txtfile = txtfiles[i];
			LOG.debug("Processing file {} / {} ('{}')", i+1, txtfiles.length, txtfile.getAbsolutePath());
			try {
				LineIterator liter = new LineIterator(new BufferedReader(new InputStreamReader(new FileInputStream(txtfile))));
				int lc = 0;
				while(liter.hasNext()){
					LOG.trace("Processing line {})", ++lc);
					for(List<String> ngram : stringProvider.getNgrams(liter.next()))
						lm.addNgram(ngram);
				}
				liter.close();
			} catch (Exception e) {
				LOG.warn("Could not read file {}", txtfile.getAbsolutePath(), e);
			}
		}

		LOG.info("Fixing LM ... ");
		lm.fixItNow();
		LOG.info("Fixing LM finished.");

		return lm;
	}

	public static LanguageModel<String> createLuceneBasedCountingLmFromTxtFilesInDirectory(AbstractStringProvider stringProvider, File srcdir, int order, int mincount, boolean overwrite_existing_index) {
		File index_dir = null;
		try {
			index_dir = GenerateNgramIndex.generate_index(srcdir, stringProvider, 1, order, mincount, overwrite_existing_index);
		} catch (IOException e) {
			LOG.warn("Could not generate index directory.");
			return null;
		}
		CountingStringLM lm = new CountingStringLM(order, index_dir);
		return lm;
	}

	public static LanguageModel<String> createLuceneBasedKneserNeyLMFromTxtFilesInDirectory(AbstractStringProvider stringProvider, File srcdir, int order, int mincount, double discount, boolean overwrite_existing_index) {
		File index_dir = null;
		try {
			index_dir = GenerateNgramIndex.generate_index(srcdir, stringProvider, 1, order, mincount, overwrite_existing_index);
		} catch (IOException e) {
			LOG.warn("Could not generate index directory.");
			return null;
		}
		KneserNeyLM lm = new KneserNeyLM(order, index_dir, discount);
		return lm;
	}
	
	public static LanguageModel<String> createLuceneBasedLMFromTxtFilesInDirectory(Class<? extends CountingStringLM> lmType , AbstractStringProvider stringProvider, File srcdir, int order, int mincount, double discount, boolean overwrite_existing_index) {
		File index_dir = null;
		try {
			index_dir = GenerateNgramIndex.generate_index(srcdir, stringProvider, 1, order, mincount, overwrite_existing_index);
		} catch (IOException e) {
			LOG.warn("Could not generate index directory.");
			return null;
		}
		try {
			Constructor<? extends CountingStringLM> constructor = lmType.getConstructor(int.class, File.class, double.class);
			LanguageModel<String> lm = constructor.newInstance(order, index_dir, discount);
			return lm;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			LOG.error("Could not instantiate language model {} ", lmType.getName(), e);
			throw new Error(e);
		}
	}


	//	/**
	//	 * use <code>createLuceneBasedCountingLmFromTxtFilesInDirectory</code> instead
	//	 * @param stringProvider
	//	 * @param srcdir
	//	 * @param order
	//	 * @return
	//	 */
	//	public static LanguageModel<String> createLuceneBasedCountingLmFromTxtFilesInDirectorySlow(AbstractStringProvider stringProvider, File srcdir, int order) {
	//		File index_dir = new File(srcdir, ".lmindex");
	//		CountingStringLM lm = new CountingStringLM(order, index_dir);
	//		stringProvider.setLanguageModel(lm);
	//
	//		if(!lm.isFix()){
	//
	//			File[] txtfiles = srcdir.listFiles(new java.io.FileFilter() {
	//				@Override
	//				public boolean accept(File pathname) {
	//					return pathname.isFile() && pathname.getName().endsWith(".txt");
	//				}
	//			});
	//
	//			String[] basenames = new String[txtfiles.length];
	//			for(int i = 0; i < basenames.length; i++)
	//				basenames[i] = txtfiles[i].getName();
	//
	//			LOG.info(String.format("Reading language model from dir: '%s'; Files: %s.", srcdir.getAbsolutePath(), StringUtils.abbreviate(Arrays.toString(basenames), 200)));
	//
	//			for(int i = 0; i < txtfiles.length; i++){
	//				File txtfile = txtfiles[i];
	//				LOG.debug("Processing file {} / {} ('{}')", i+1, txtfiles.length, txtfile.getAbsolutePath());
	//				try {
	//					LineIterator liter = new LineIterator(new BufferedReader(new InputStreamReader(new FileInputStream(txtfile))));
	//					int lc = 0;
	//					while(liter.hasNext()){
	//						LOG.trace("Processing line {})", ++lc);
	//						for(List<String> ngram : stringProvider.getNgrams(liter.next()))
	//							lm.addSequence(ngram);
	//					}
	//					liter.close();
	//				} catch (Exception e) {
	//					LOG.warn("Could not read file {}", txtfile.getAbsolutePath(), e);
	//				}
	//			}
	//
	//			LOG.info("Fixing LM ... ");
	//			lm.fixItNow();
	//			LOG.info("Fixing LM finished.");
	//		}
	//		return lm;
	//	}

	public static LanguageModel<String> readFromArpa(File arpa_file, float kneserNeyUnkLog10Prob){
		LOG.info(String.format("Reading language model from file: '%s'.", arpa_file.getAbsolutePath()));
		//		LanguageModel<String> lm = new BerkeleyLM<String>(LmReaders.readArrayEncodedLmFromArpa(arpa_file.getAbsolutePath(), false));

		WordIndexer<String> indexer = new StringWordIndexer();
		indexer = new StringWordIndexer();
		ConfigOptions opts = new ConfigOptions();
		opts.unknownWordLogProb = kneserNeyUnkLog10Prob;
		ArrayEncodedProbBackoffLm<String> lm = LmReaders.readArrayEncodedLmFromArpa(new ArpaLmReader<String>(arpa_file.getAbsolutePath(), indexer, Integer.MAX_VALUE), Properties.useCompressedBerkelyLM(), indexer, opts);
		LOG.info("Finished reading language model from '{}'.", arpa_file);
		BerkeleyLM<String> blm = new BerkeleyLM<String>(lm);

		LOG.info("Finshed reading language model from file: '{}'.", arpa_file.getAbsolutePath());
		return blm;
	}

	public static LanguageModel<String> createBerkelyLmFromTxtFilesInDirectory(AbstractStringProvider stringProvider, File srcdir, int order, float kneserNeyUnkLog10Prob, double discount, int mincount, boolean overwrite) {
		String arpa_file_name = srcdir.getName() + "." + order + ".arpa.gz";
		File arpa_file = new File(srcdir, arpa_file_name);
		if(arpa_file.exists()){
			if (!overwrite)
				return readFromArpa(arpa_file, kneserNeyUnkLog10Prob);
			arpa_file.delete();
		}

		final StringWordIndexer wordIndexer = new StringWordIndexer();
		wordIndexer.setStartSymbol(PseudoSymbol.SEQUENCE_START.asString());
		wordIndexer.setEndSymbol(PseudoSymbol.SEQUENCE_END.asString());
		wordIndexer.setUnkSymbol(PseudoSymbol.UNKOWN_WORD.asString());
		
		ConfigOptions opts = new ConfigOptions();
		opts.kneserNeyMinCounts = new double[order+1];
		Arrays.fill(opts.kneserNeyMinCounts, mincount);
		if(discount < 0){
			opts.kneserNeyDiscounts = null;
		}else{
			opts.kneserNeyDiscounts = new double[order];
			Arrays.fill(opts.kneserNeyDiscounts, discount);
		}
		opts.unknownWordLogProb = kneserNeyUnkLog10Prob;

		String[] txtfiles = srcdir.list(new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String arg1) {
				return new File(arg0,arg1).isFile() && arg1.endsWith(".txt");
			}
		});

		for(int i = 0; i < txtfiles.length; i++)
			txtfiles[i] = new File(srcdir, txtfiles[i]).getAbsolutePath();

		LOG.info(String.format("Reading language model from files: '%s'.", StringUtils.abbreviate(Arrays.toString(txtfiles), 200)));
		LOG.info(String.format("And saving language model to file: '%s'.", arpa_file));

		final TextReader reader = new TextReader(Arrays.asList(txtfiles), wordIndexer, stringProvider);
		KneserNeyLmReaderCallbackWrapper<String> callback = new KneserNeyLmReaderCallbackWrapper<String>(wordIndexer, order, opts);

		reader.parse(callback);
		callback.parse(new KneserNeyFileWritingLmReaderCallback<String>(arpa_file, wordIndexer));

		LOG.info("Finished reading saving language model.");

		return createBerkelyLmFromTxtFilesInDirectory(stringProvider, srcdir, order, kneserNeyUnkLog10Prob, discount, mincount, false);
	}


	// public static <W> void saveAsArpa(BerkeleyLM<W> blm, File file) {
	// KneserNeyFileWritingLmReaderCallback<W> lmwriterCallback = new KneserNeyFileWritingLmReaderCallback<W>(file, blm.get().getWordIndexer());
	// ArrayEncodedNgramLanguageModel<W> berkely_language_model = blm.get();
	// if (!(blm.get() instanceof KneserNeyLmReaderCallback))
	// throw new NotImplementedException("Saving of already exitsing ngram models is not yet implemented. Consider to copy the source file.");
	// ((KneserNeyLmReaderCallback<W>) berkely_language_model).parse(lmwriterCallback);
	// }

	// public static NgramMap<ProbBackoffPair> getNGramMap(BerkeleyLM<String> berkeley_language_model) {
	// ArrayEncodedProbBackoffLm<ProbBackoffPair> blm = berkeley_language_model.get();
	// blm.get
	//
	// }




}
