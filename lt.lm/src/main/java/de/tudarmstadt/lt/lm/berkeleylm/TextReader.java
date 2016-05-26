package de.tudarmstadt.lt.lm.berkeleylm;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.lm.service.AbstractStringProvider;
import de.tudarmstadt.lt.lm.util.Properties;
import edu.berkeley.nlp.lm.WordIndexer;
import edu.berkeley.nlp.lm.collections.Iterators;
import edu.berkeley.nlp.lm.io.IOUtils;
import edu.berkeley.nlp.lm.io.LmReader;
import edu.berkeley.nlp.lm.io.LmReaderCallback;
import edu.berkeley.nlp.lm.util.Logger;
import edu.berkeley.nlp.lm.util.LongRef;


/**
 * 
 * @author Adam Pauls, Steffen Remus
 *
 */
public class TextReader implements LmReader<LongRef, LmReaderCallback<LongRef>>{

	private final static org.slf4j.Logger LOG = LoggerFactory.getLogger(TextReader.class);

	private final WordIndexer<String> wordIndexer;

	private final Iterable<String> lineIterator;

	private final AbstractStringProvider stringProvider;

	public TextReader(final List<String> inputFiles, final WordIndexer<String> wordIndexer, AbstractStringProvider stringProvider) {
		this(getLineIterator(inputFiles), wordIndexer, stringProvider);

	}

	public TextReader(Iterable<String> lineIterator, final WordIndexer<String> wordIndexer, AbstractStringProvider stringProvider) {
		this.lineIterator = lineIterator;
		this.wordIndexer = wordIndexer;
		this.stringProvider = stringProvider;
	}

	/**
	 * Reads newline-separated plain text from inputFiles, and writes an ARPA lm
	 * file to outputFile. If files have a .gz suffix, then they will be
	 * (un)zipped as necessary.
	 * 
	 * @param inputFiles
	 * @param outputFile
	 */
	@Override
	public void parse(final LmReaderCallback<LongRef> callback) {
		readFromFiles(callback);
	}

	private void readFromFiles(final LmReaderCallback<LongRef> callback) {
		Logger.startTrack("Reading in ngrams from raw text");

		countNgrams(lineIterator, callback);
		Logger.endTrack();

	}

	/**
	 * @param <W>
	 * @param wordIndexer
	 * @param maxOrder
	 * @param allLinesIterator
	 * @param callback
	 * @param ngrams
	 * @return
	 */
	private void countNgrams(final Iterable<String> allLinesIterator, final LmReaderCallback<LongRef> callback) {
		long numLines = 0;

		for (final String line : allLinesIterator) {
			if (numLines % 10000 == 0) Logger.logs("On line " + numLines);
			numLines++;

			List<String> words = Collections.emptyList();
			try{
				words = stringProvider.tokenizeSentence(line, Properties.defaultLanguageCode());
			}catch(Exception e){LOG.warn("Could not reach stringprovider for tokenizing sentence.");}

			final int[] sent = new int[words.size() + 2];
			sent[0] = wordIndexer.getOrAddIndex(wordIndexer.getStartSymbol());
			sent[sent.length - 1] = wordIndexer.getOrAddIndex(wordIndexer.getEndSymbol());
			for (int i = 0; i < words.size(); ++i) {
				sent[i + 1] = wordIndexer.getOrAddIndexFromString(words.get(i));
			}
			callback.call(sent, 0, sent.length, new LongRef(1L), line);
		}
		callback.cleanup();
	}

	/**
	 * @param files
	 * @return
	 */
	private static Iterable<String> getLineIterator(final Iterable<String> files) {
		final Iterable<String> allLinesIterator = Iterators.flatten(new Iterators.Transform<String, Iterator<String>>(files.iterator()){
			@Override
			protected Iterator<String> transform(final String file) {
				try {
					return IOUtils.lineIterator(file);
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		return allLinesIterator;
	}

}
