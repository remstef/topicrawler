package de.tudarmstadt.lt.lm.berkeleylm;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.berkeleylm.BerkeleyStringReader;
import de.tudarmstadt.lt.lm.berkeleylm.KneserNeyLmReaderCallbackWrapper;
import edu.berkeley.nlp.lm.ArrayEncodedNgramLanguageModel;
import edu.berkeley.nlp.lm.ArrayEncodedProbBackoffLm;
import edu.berkeley.nlp.lm.ConfigOptions;
import edu.berkeley.nlp.lm.NgramLanguageModel;
import edu.berkeley.nlp.lm.NgramLanguageModel.StaticMethods;
import edu.berkeley.nlp.lm.StringWordIndexer;
import edu.berkeley.nlp.lm.StupidBackoffLm;
import edu.berkeley.nlp.lm.array.LongArray;
import edu.berkeley.nlp.lm.io.ArpaLmReader;
import edu.berkeley.nlp.lm.io.FirstPassCallback;
import edu.berkeley.nlp.lm.io.KneserNeyFileWritingLmReaderCallback;
import edu.berkeley.nlp.lm.io.KneserNeyLmReaderCallback;
import edu.berkeley.nlp.lm.io.LmReaders;
import edu.berkeley.nlp.lm.io.NgramMapAddingCallback;
import edu.berkeley.nlp.lm.io.TextReader;
import edu.berkeley.nlp.lm.map.HashNgramMap;
import edu.berkeley.nlp.lm.map.NgramMap;
import edu.berkeley.nlp.lm.map.NgramMapWrapper;
import edu.berkeley.nlp.lm.util.LongRef;
import edu.berkeley.nlp.lm.values.CountValueContainer;
import edu.berkeley.nlp.lm.values.KneserNeyCountValueContainer;
import edu.berkeley.nlp.lm.values.KneserNeyCountValueContainer.KneserNeyCounts;
import edu.berkeley.nlp.lm.values.ProbBackoffPair;

public class BerkeleyLmPlayground {

	Logger LOG = LoggerFactory.getLogger(BerkeleyLmPlayground.class);

	public static void createStupidBackoff() {

		List<String> text1 = Arrays.asList("hallo schöne neue welt".split(" "));


		List<String> text2 = Arrays.asList("hoppe hoppe reiter wenn er fällt dann schreit er".split(" "));


		// String[] text3 = "oh wunder eine schöne neue erde".split(" ");

		int order = 3;
		boolean compress = false;
		boolean contextEncoded = false;
		boolean reversed = true;

		ConfigOptions opts = new ConfigOptions();
		final StringWordIndexer wordIndexer = new StringWordIndexer();
		wordIndexer.setStartSymbol(ArpaLmReader.START_SYMBOL);
		wordIndexer.setEndSymbol(ArpaLmReader.END_SYMBOL);
		wordIndexer.setUnkSymbol(ArpaLmReader.UNK_SYMBOL);

		// first pass
		FirstPassCallback<LongRef> valueAddingCallback = new FirstPassCallback<LongRef>(reversed);
		// BerkeleyStringReader lmreader = new BerkeleyStringReader(text1, wordIndexer);
		// TextReader<String> lmreader = new TextReader<String>(Arrays.asList("test.txt"), wordIndexer);
		// lmreader.parse(valueAddingCallback);
		new BerkeleyStringReader(text1, wordIndexer).parse(valueAddingCallback);
		new BerkeleyStringReader(text2, wordIndexer).parse(valueAddingCallback);


		// // 1.5 pass
		// KneserNeyLmReaderCallback<String> callback = new KneserNeyLmReaderCallback<String>(wordIndexer, order);
		// lmreader.parse(callback);



		// second pass
		// lmreader = new TextReader<String>(Arrays.asList("test.txt"), wordIndexer);

		final LongArray[] numNgramsForEachWord = valueAddingCallback.getNumNgramsForEachWord();



		//
		final CountValueContainer values = new CountValueContainer(valueAddingCallback.getValueCounter(), opts.valueRadix, contextEncoded, valueAddingCallback.getNumNgramsForEachOrder());
		// final NgramMap<LongRef> map = buildMapCommon(opts, wordIndexer, numNgramsForEachWord, valueAddingCallback.getNumNgramsForEachOrder(), reversed,
		// lmreader, values, compress);
		// StupidBackoffLm<String> lm = new StupidBackoffLm<String>(numNgramsForEachWord.length, wordIndexer, map, opts);


		// new BerkeleyStringReader(text2, wordIndexer).parse(callback);
		// new BerkeleyStringReader(text3, wordIndexer).parse(callback);
		// callback.cleanup();
		// wordIndexer.trimAndLock();

		// ValueContainer<ProbBackoffPair> values = new UncompressedProbBackoffValueContainer(callback.getValueCounter(), opts.valueRadix, false, callback.getNumNgramsForEachOrder());
		//
		NgramMap<LongRef> map = HashNgramMap.createImplicitWordHashNgramMap(values, opts, valueAddingCallback.getNumNgramsForEachWord(), reversed);
		// NgramMap<LongRef> map = HashNgramMap.createExplicitWordHashNgramMap(values, opts, order, reversed);
		final NgramMapAddingCallback<LongRef> ngramMapAddingCallback = new NgramMapAddingCallback<LongRef>(map, null);

		// lmreader.parse(ngramMapAddingCallback);
		new BerkeleyStringReader(text1, wordIndexer).parse(ngramMapAddingCallback);
		new BerkeleyStringReader(text2, wordIndexer).parse(ngramMapAddingCallback);

		List<int[]> failures = ngramMapAddingCallback.getFailures();
		System.out.println(failures);

		// new BerkeleyStringReader(text1, wordIndexer).parse(ngramMapAddingCallback);
		// new BerkeleyStringReader(text2, wordIndexer).parse(ngramMapAddingCallback);
		// new BerkeleyStringReader(text3, wordIndexer).parse(ngramMapAddingCallback);

		// final List<int[]> failures = ngramMapAddingCallback.getFailures();

		StupidBackoffLm<String> lm = new StupidBackoffLm<String>(order, wordIndexer, map, opts);
		// ArrayEncodedProbBackoffLm<String> lm = new ArrayEncodedProbBackoffLm<String>(order, wordIndexer, map, opts);
		// lm = ArrayEncodedCachingLmWrapper.wrapWithCacheThreadSafe(lm);

		System.out.println(StaticMethods.sample(new Random(), lm));
		System.out.println(Arrays.toString(StaticMethods.toIntArray(Arrays.asList("schöne neue welt erde globus".split(" ")), lm)));
		System.out.println(StaticMethods.toObjectList(new int[] { 0, 1, 2, 3, 4, 5, 6 }, lm));
		System.out.format("%f \t %f %n",
				lm.getLogProb(Arrays.asList("schöne neue welt".split(" "))),
				lm.getLogProb(Arrays.asList("hoppe reiter schreit er".split(" "))));

	}

	// private static <V, W> List<int[]> tryBuildingNgramMap(final ConfigOptions opts, final WordIndexer<W> wordIndexer,
	// final LmReader<V, ? super NgramMapAddingCallback<V>> lmReader, NgramMap<V> map) {
	// final NgramMapAddingCallback<V> ngramMapAddingCallback = new NgramMapAddingCallback<V>(map, null);
	// lmReader.parse(ngramMapAddingCallback);
	// if (opts.lockIndexer)
	// wordIndexer.trimAndLock();
	// final List<int[]> failures = ngramMapAddingCallback.getFailures();
	// return failures;
	// }
	//
	// private static <V> AbstractNgramMap<V> createNgramMap(final ConfigOptions opts, final LongArray[] numNgramsForEachWord, final long[] numNgramsForEachOrder,
	// final boolean reversed, final ValueContainer<V> values, final boolean compress) {
	//		return compress ? new CompressedNgramMap<V>((CompressibleValueContainer<V>) values, numNgramsForEachOrder, opts) : HashNgramMap
	//				.createImplicitWordHashNgramMap(values, opts, numNgramsForEachWord, reversed);
	// }
	//
	// private static <W, V extends Comparable<V>> NgramMap<V> buildMapCommon(final ConfigOptions opts, final WordIndexer<W> wordIndexer,
	// final LongArray[] numNgramsForEachWord, final long[] numNgramsForEachOrder, final boolean reversed,
	// final LmReader<V, ? super NgramMapAddingCallback<V>> lmReader, final ValueContainer<V> values, final boolean compress) {
	// Logger.startTrack("Adding n-grams");
	// NgramMap<V> map = createNgramMap(opts, numNgramsForEachWord, numNgramsForEachOrder, reversed, values, compress);
	//
	// final List<int[]> failures = tryBuildingNgramMap(opts, wordIndexer, lmReader, map);
	// Logger.endTrack();
	// if (!failures.isEmpty()) {
	// Logger.startTrack(failures.size() + " missing suffixes or prefixes were found, doing another pass to add n-grams");
	// for (final int[] failure : failures) {
	// final int ngramOrder = failure.length - 1;
	// final int headWord = failure[reversed ? 0 : ngramOrder];
	// numNgramsForEachOrder[ngramOrder]++;
	// numNgramsForEachWord[ngramOrder].incrementCount(headWord, 1);
	// }
	//
	// // try to clear some memory
	// for (int ngramOrder = 0; ngramOrder < numNgramsForEachOrder.length; ++ngramOrder) {
	// values.clearStorageForOrder(ngramOrder);
	// }
	// final ValueContainer<V> newValues = values.createFreshValues(numNgramsForEachOrder);
	// map.clearStorage();
	// map = createNgramMap(opts, numNgramsForEachWord, numNgramsForEachOrder, reversed, newValues, compress);
	// lmReader.parse(new NgramMapAddingCallback<V>(map, failures));
	// Logger.endTrack();
	// }
	// return map;
	// }

	public static void createKneserNeySimple() {

		List<String> text1 = Arrays.asList("hallo schöne neue welt".split(" "));
		List<String> text2 = Arrays.asList("hoppe hoppe reiter wenn er fällt dann schreit er".split(" "));
		List<String> text3 = Arrays.asList("oh wunder eine schöne neue erde".split(" "));

		int order = 4;
		final StringWordIndexer wordIndexer = new StringWordIndexer();
		wordIndexer.setStartSymbol(ArpaLmReader.START_SYMBOL);
		wordIndexer.setEndSymbol(ArpaLmReader.END_SYMBOL);
		wordIndexer.setUnkSymbol(ArpaLmReader.UNK_SYMBOL);

		KneserNeyLmReaderCallbackWrapper<String> callback = new KneserNeyLmReaderCallbackWrapper<String>(wordIndexer, order, new ConfigOptions());
		new BerkeleyStringReader(text1, wordIndexer).parse(callback);
		new BerkeleyStringReader(text2, wordIndexer).parse(callback);
		new BerkeleyStringReader(text3, wordIndexer).parse(callback);
		callback.cleanup();
		wordIndexer.trimAndLock();
		ArrayEncodedNgramLanguageModel<String> lm = callback;

		// lm = ArrayEncodedCachingLmWrapper.wrapWithCacheThreadSafe(lm);

		System.out.println(StaticMethods.sample(new Random(), lm));
		System.out.println(Arrays.toString(StaticMethods.toIntArray(Arrays.asList("schöne neue welt erde globus".split(" ")), lm)));
		System.out.println(StaticMethods.toObjectList(new int[] { 0, 1, 2, 3, 4, 5, 6 }, lm));
		System.out.format("%f \t %f \t %f %n",
				lm.getLogProb(Arrays.asList("schöne neue welt".split(" "))),
				lm.getLogProb(Arrays.asList("hoppe reiter schreit er".split(" "))),
				Math.exp(lm.scoreSentence(Arrays.asList("hoppe hoppe reiter wenn er fällt dann schreit er".split(" ")))));

		// NgramMap<ProbBackoffPair> nmap = ((ArrayEncodedProbBackoffLm<String>) lm).getNgramMap();
		NgramMap<KneserNeyCounts> nmap = callback.getNgramMap();
		NgramMapWrapper<String, KneserNeyCounts> nmapw = new NgramMapWrapper<String, KneserNeyCountValueContainer.KneserNeyCounts>(nmap, wordIndexer);
		for (Entry<List<String>, KneserNeyCounts> e : nmapw.entrySet()) {
			System.out.println(e.getKey() + "\t" + e.getValue().tokenCounts);
		}

		System.out.println(nmapw);

	}

	public static void main(String[] args) {
		createFileFromText();
	}

	public static void createFileFromText() {
		// MakeKneserNeyArpaFromText.main(new String[] { "3", "lmorder3.arpa", "in/1.txt" });
		String txtfile = "src/test/resources/test.txt";
		String arpafile = "_svnignore/test.arpa.gz";
		String binfile = "_svnignore/test.blm.gz";

		// if (!new File(arpafile).exists()) {
		final StringWordIndexer wordIndexer = new StringWordIndexer();
		wordIndexer.setStartSymbol(ArpaLmReader.START_SYMBOL);
		wordIndexer.setEndSymbol(ArpaLmReader.END_SYMBOL);
		wordIndexer.setUnkSymbol(ArpaLmReader.UNK_SYMBOL);
		ConfigOptions opts = new ConfigOptions();
		opts.kneserNeyDiscounts = new double[] { 0.75f, 0.6f, 0.6f };
		opts.kneserNeyMinCounts = new double[] { 0, 0, 0, 0, 0, 0, 0 };

		final TextReader<String> reader = new TextReader<String>(Arrays.asList(txtfile), wordIndexer);
		KneserNeyLmReaderCallback<String> kneserNeyReader = new KneserNeyLmReaderCallback<String>(wordIndexer, 3, opts);
		reader.parse(kneserNeyReader);
		// NgramLanguageModel<String> lm = kneserNeyReader;

		kneserNeyReader.parse(new KneserNeyFileWritingLmReaderCallback<String>(new File(arpafile), wordIndexer));
		//		}
		//		if (!new File(binfile).exists()) {
		//			// HASH OPT
		NgramLanguageModel<String> lm = LmReaders.readArrayEncodedLmFromArpa(arpafile, false);
		//			// CONTEXT OPT
		//			// NgramLanguageModel<String> lm = LmReaders.readContextEncodedLmFromArpa(arpafile);
		//			// HASH COMPRESS OPT
		//			// NgramLanguageModel<String> lm = LmReaders.readArrayEncodedLmFromArpa(arpafile, true);
		//
		//			LmReaders.writeLmBinary(lm, binfile);
		//		}
		//
		//		// NgramLanguageModel<String> lm = LmReaders.readLmBinary(binfile);
		//		NgramLanguageModel<String> lm = LmReaders.readArrayEncodedLmFromArpa(arpafile, false);


		System.out.println(lm.getLogProb(Arrays.asList("Hallo")));
		System.out.println(lm.getLogProb(Arrays.asList("schöne", "neue", "welt")));
		System.out.println(lm.getLogProb(Arrays.asList("schöne", "neue", "pups")));
		System.out.println(lm.getLogProb(Arrays.asList("schöne", "neue", "globus")));
		System.out.println(lm.getLogProb(Arrays.asList("schöne", "neue", "erde")));

		// LmReaders.readn


	}

	public <W> void getNgrams() {
		ArrayEncodedNgramLanguageModel<W> _blm = null;

		NgramMapWrapper<W, ?> mapWrapper;
		if (_blm instanceof ArrayEncodedProbBackoffLm) {
			NgramMap<ProbBackoffPair> map = ((ArrayEncodedProbBackoffLm<W>) _blm).getNgramMap();
			mapWrapper = new NgramMapWrapper<W, ProbBackoffPair>(map, _blm.getWordIndexer());
			LOG.debug(String.format("%-50.50s \t %-6s \t %s", "ngram", "prob", "backoff"));
			for (Entry<List<W>, ?> entry : mapWrapper.entrySet()) {
				ProbBackoffPair c = (ProbBackoffPair) entry.getValue();
				LOG.debug(String.format("%-50.50s \t %.4f \t %.4f",
						entry.getKey().toString(),
						c.prob,
						c.backoff
						));
			}
		}
		else if (_blm instanceof KneserNeyLmReaderCallbackWrapper) {
			NgramMap<KneserNeyCounts> map = ((KneserNeyLmReaderCallbackWrapper<W>) _blm).getNgramMap();
			mapWrapper = new NgramMapWrapper<W, KneserNeyCounts>(map, _blm.getWordIndexer());
			LOG.debug(String.format("%-50.50s \t %s \t %-5.5s \t %-5.5s \t %s \t %s \t %s", "ngram", "dd", "isOne", "isTwo", "ld", "rd", "t"));
			for (Entry<List<W>, ?> entry : mapWrapper.entrySet()) {
				KneserNeyCounts c = (KneserNeyCounts) entry.getValue();
				LOG.debug(String.format("%-50.50s \t %d \t %-5.5b \t %-5.5b \t %d \t %d \t %d",
						entry.getKey().toString(),
						c.dotdotTypeCounts,
						c.isOneCount,
						c.isTwoCount,
						c.leftDotTypeCounts,
						c.rightDotTypeCounts,
						c.tokenCounts
						));
			}
		}
		else throw new Error(String.format(
				"Language model must be either an ArrayEncodedProbBackoffLm or a KneserNeyLmReaderCallbackWrapper instance, but is an %s instance.",
				_blm.getClass().getSimpleName()
				));
	}


	// private String binaryModelFileName;
	// private String vocabolaryFileName;
	// //private NgramMapWrapper<String, LongRef> nmw;
	// private StupidBackoffLm sblm;
	// NgramMap<LongRef> nm;
	//
	// private NgramProcessing() {
	//
	// }
	//
	// public NgramProcessing(String binaryModelFileName, String vocabolaryFileName) {
	// this.binaryModelFileName = binaryModelFileName;
	// this.vocabolaryFileName = vocabolaryFileName;
	// }
	//
	// public void readModel() {
	//
	// //String binary_example = "/home/bcoppola/sw/berkeleylm-1.1.2/examples/google.binary_Bc";
	// //String vocabFile_example = "/home/bcoppola/sw/berkeleylm-1.1.2/test/edu/berkeley/nlp/lm/io/googledir/1gms/vocab_cs.gz";
	// //String binary = "/users/bcoppola/ngram/en.blm.gz";
	// //String vocabFile = "/data5/Tools/GoogleNGrams/DVD1/data/1gms/vocab_cs.gz";
	// System.out.println("Loading ngram model...");
	// sblm = LmReaders.readGoogleLmBinary(this.binaryModelFileName, this.vocabolaryFileName);
	// //nmw = LmReaders.readNgramMapFromBinary(this.binaryModelFileName, this.vocabolaryFileName);
	// System.out.println("Model loaded.");
	// System.out.println("-------------");
	// System.out.println("Extracting NgramMap");
	// nm = sblm.getNgramMap();
	// //System.out.println("NgramMapWrapper.longSize() = "+nmw.longSize());
	// System.out.println("NgramMap.getMaxNgramOrder() = "+nm.getMaxNgramOrder());
	// System.out.println("NgramMap.getNumNgrams(0) = "+nm.getNumNgrams(0));
	// System.out.println("NgramMap.getNumNgrams(1) = "+nm.getNumNgrams(1));
	// System.out.println("NgramMap.getNumNgrams(2) = "+nm.getNumNgrams(2));
	// System.out.println("NgramMap.getNumNgrams(3) = "+nm.getNumNgrams(3));
	// System.out.println("NgramMap.getNumNgrams(4) = "+nm.getNumNgrams(4));
	// System.out.println("");
	// System.out.println("nm.getNgramsForOrder(4) first 10K DUMP:");
	// Iterable<NgramMap.Entry<LongRef>> v4 = nm.getNgramsForOrder(4);
	// Iterator<NgramMap.Entry<LongRef>> vi = v4.iterator();
	// for (int i = 0; i < 10000; i++) {
	// NgramMap.Entry<LongRef> e = vi.next();
	// int[] key = e.key;
	// LongRef val = e.value;
	// for(int j = 0; j < 5; j++) {
	// System.out.print(key[j]+" ");
	// }
	// System.out.println("- "+val);
	// }
	// System.out.println();
	// //System.out.println("Size is: "+sblm.size());
	// }
	//
	// public long getScore(String ngramString) {
	// if (nm == null) {
	// System.out.println("Model not loaded.");
	// return -2;
	// }
	// List<String> ll = new LinkedList<String>();
	// String[] tokens = ngramString.split("\\s+");
	// for (String token : tokens) {
	// ll.add(token);
	// }
	// WordIndexer widx = sblm.getWordIndexer();
	// int[] ngram = StaticMethods.toArrayFromStrings(widx, ll);
	// long score = sblm.getRawCount(ngram, 1, ngram.length);
	//
	// //Map<List<String>, LongRef> map = this.nmw.getMapForOrder(ll.size()-1);
	// //LongRef score = map.get(ll);
	//
	// return score;
	// }
	//
	//
	// public void InteractiveQuerying() {
	//
	// if (nm == null) {
	// System.out.println("Model not loaded.");
	// return;
	// }
	//
	// BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	//
	// while(true) {
	// String input = "";
	// List<String> ll = new LinkedList<String>();
	//
	// try {
	// System.out.print("Input: ");
	// input = br.readLine();
	// } catch (IOException ioe) {
	// ioe.printStackTrace();
	// }
	//
	// while (!input.equals(".")) {
	// ll.add(input);
	// try {
	// System.out.print("Input: ");
	// input = br.readLine();
	// } catch (IOException ioe) {
	// ioe.printStackTrace();
	// }
	// }
	// long score = sblm.getRawCount(StaticMethods.toArrayFromStrings(sblm.getWordIndexer(), ll), 0, ll.size()); // 278->221)
	//
	// //LongRef score = this.getScore(ngramString)
	//
	// //Map<List<String>, LongRef> map = this.nmw.getMapForOrder(ll.size()-1);
	// //LongRef score = map.get(ll);
	//
	// System.out.println("Score: "+score);
	// }
	// }
	//
	//
	// public static void main(String args[]) {
	// NgramProcessing np = new NgramProcessing("/users/bcoppola/ngram/en.blm.gz", "/data5/Tools/GoogleNGrams/DVD1/data/1gms/vocab_cs.gz");
	// np.readModel();
	// List<String> ll = new LinkedList<String>();
	// // Alstroemeria and Eucalyptus 278
	// // Alstroemeria and Asiatic 74
	// // Alstroemeria and Bomarea 54
	// // Alstroemeria and Carnations 87
	// // Alstroemeria and Eucalyptus 278
	// // Alstroemeria and Gerbera 147
	// // Alstroemeria and Roses 56
	// ll.add("Alstroemeria");
	// ll.add("and");
	// ll.add("Gerbera");
	//
	// //long score = np.sblm.getRawCount(StaticMethods.toArrayFromStrings(np.sblm.getWordIndexer(), ll), 1, ll.size()); // 278->221) //long score = sblm.getRawCount(StaticMethods.toArrayFromStrings(sblm.getWordIndexer(), ll), 0, ll.size());
	// //Map<List<String>, LongRef> map = np.nmw.getMapForOrder(ll.size()-1);
	// //LongRef score1 = map.get(ll);
	// //System.out.println("Score 1: "+score1);
	// //NgramMap<LongRef> nm = np.nmw.getNgramMap();
	// //LongRef score2 = nm.get(ngram, startPos, endPos);
	//
	//
	//
	// np.InteractiveQuerying();
	// }

}
