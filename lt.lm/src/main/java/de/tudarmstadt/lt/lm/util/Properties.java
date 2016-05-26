package de.tudarmstadt.lt.lm.util;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.lt.seg.sentence.ISentenceSplitter;
import de.tudarmstadt.lt.seg.token.ITokenizer;

public class Properties extends de.tudarmstadt.lt.utilities.properties.Properties {

	private static final long serialVersionUID = -2326752828156804949L;

	private final static Logger LOG = LoggerFactory.getLogger(Properties.class);

	public static final String project_name = "de.tudarmstadt.lt.lm";
	
	public static String lmHomeDirectory(){
		return _singleton.getProperty("lt.lm.home", "");
	}

	private static final String pythonBinariesDirectory_dev = "src/main/py";
	public static String pythonBinariesDirectory(){
		String lmhome = _singleton.getProperty("lt.lm.home");
		if(lmhome == null)
			return pythonBinariesDirectory_dev;
		return new File(lmhome, "bin").getAbsolutePath();
	}

	private static final String useCompressedBerkelyLM_default = String.valueOf(false);
	public static boolean useCompressedBerkelyLM() {
		String propvalue = _singleton.getProperty("lt.lm.useCompressedBerkelyLM", useCompressedBerkelyLM_default);
		try {
			return Boolean.valueOf(propvalue.trim());
		} catch (Exception e) {
			LOG.warn(String.format("Could not parse '%s' as boolean. Setting to default value (%s).", propvalue, String.valueOf(useCompressedBerkelyLM_default)));
			_singleton.setProperty("lt.lm.useCompressedBerkelyLM", useCompressedBerkelyLM_default);
			return useCompressedBerkelyLM();
		}
	}

	private static final String defaultLanguageCode_default = "en";
	public static String defaultLanguageCode() {
		return _singleton.getProperty("lt.lm.defaultLanguageCode", defaultLanguageCode_default);
	}

	private static final String maxLengthSplitHeuristic_default = String.valueOf(1000);
	public static int maxLengthSplitHeuristic() {
		String propvalue = _singleton.getProperty("lt.lm.maxLengthSplitHeuristic", maxLengthSplitHeuristic_default);
		try {
			return Integer.parseInt(propvalue.trim());
		} catch (Exception e) {
			LOG.warn(String.format("Could not parse '%s' as integer. Setting to default value (%s).", propvalue, maxLengthSplitHeuristic_default));
			_singleton.setProperty("lt.lm.maxLengthSplitHeuristic", maxLengthSplitHeuristic_default);
			return maxLengthSplitHeuristic();
		}
	}

	private static final String insertSentenceTags_default = String.valueOf(3); // 0 = no insertion; 1 = front; 2 = rear; 3 = both
	public static int insertSentenceTags() {
		String propvalue = _singleton.getProperty("lt.lm.insertSentenceTags", insertSentenceTags_default);
		try {
			return Integer.parseInt(propvalue.trim());
		} catch (Exception e) {
			LOG.warn(String.format("Could not parse '%s' as integer. Setting to default value (%s).", propvalue, insertSentenceTags_default));
			_singleton.setProperty("lt.lm.insertSentenceTags", insertSentenceTags_default);
			return insertSentenceTags();
		}
	}

	private static final String handleBoundaries_default = String.valueOf(0); // -1 = no handling omit too short ngrams; 0 = no handling consider short ngrams; 1 = extending N-1 to the front; 2 = growing N-1 at the front
	public static int handleBoundaries() {
		String propvalue = _singleton.getProperty("lt.lm.handleBoundaries", handleBoundaries_default);
		try {
			return Integer.parseInt(propvalue.trim());
		} catch (Exception e) {
			LOG.warn(String.format("Could not parse '%s' as integer. Setting to default value (%s).", propvalue, handleBoundaries_default));
			_singleton.setProperty("lt.lm.handleBoundaries", handleBoundaries_default);
			return handleBoundaries();
		}
	}

	private static final String knUnkLog10Prob_default = String.valueOf(Float.NaN);
	public static float knUnkLog10Prob() {
		String propvalue = _singleton.getProperty("lt.lm.knUnkLog10Prob", knUnkLog10Prob_default);
		if(propvalue.isEmpty())
			propvalue = knUnkLog10Prob_default;
		try {
			return Float.valueOf(propvalue.trim());
		} catch (Exception e) {
			LOG.warn(String.format("Could not parse '%s' as double. Setting to default value (%s).", propvalue, knUnkLog10Prob_default));
			_singleton.setProperty("lt.lm.knUnkLog10Prob", knUnkLog10Prob_default);
			return knUnkLog10Prob();
		}
	}
	
	private static final String knMaxbackoffrecursions_default = String.valueOf(-1); // -1 = until uniform prob
	public static int knMaxbackoffrecursions() {
		String propvalue = _singleton.getProperty("lt.lm.knMaxbackoffrecursions", knMaxbackoffrecursions_default);
		try {
			return Integer.parseInt(propvalue.trim());
		} catch (Exception e) {
			LOG.warn(String.format("Could not parse '%s' as integer. Setting to default value (%s).", propvalue, knMaxbackoffrecursions_default));
			_singleton.setProperty("lt.lm.knMaxbackoffrecursions", knMaxbackoffrecursions_default);
			return knMaxbackoffrecursions();
		}
	}


	private static final String tokenizer_default = de.tudarmstadt.lt.seg.token.DiffTokenizer.class.getName();
	@SuppressWarnings("unchecked")
	public static Class<ITokenizer> tokenizer() {
		String propvalue = _singleton.getProperty("lt.lm.tokenizer", tokenizer_default);
		try {
			return (Class<ITokenizer>)Class.forName(propvalue.trim());
		} catch (Exception e) {
			LOG.warn(String.format("Could not parse '%s'. Setting to default value (%s).", propvalue, tokenizer_default));
			_singleton.setProperty("lt.lm.tokenizer", tokenizer_default);
			return tokenizer();
		}
	}

	private static final String sentenceSplitter_default = de.tudarmstadt.lt.seg.sentence.RuleSplitter.class.getName();
	@SuppressWarnings("unchecked")
	public static Class<ISentenceSplitter> sentenceSplitter() {
		String propvalue = _singleton.getProperty("lt.lm.sentenceSplitter", sentenceSplitter_default);
		try {
			return (Class<ISentenceSplitter>)Class.forName(propvalue.trim());
		} catch (Exception e) {
			LOG.warn(String.format("Could not parse '%s'. Setting to default value (%s).", propvalue, sentenceSplitter_default));
			_singleton.setProperty("lt.lm.sentenceSplitter", sentenceSplitter_default);
			return sentenceSplitter();
		}
	}


	private static final String onedocperline_default = Boolean.toString(false);
	public static boolean onedocperline() {
		String propvalue = _singleton.getProperty("lt.lm.onedocperline", onedocperline_default);
		try {
			return Boolean.valueOf(propvalue.trim());
		} catch (Exception e) {
			LOG.warn(String.format("Could not parse '%s'. Setting to default value (%s).", propvalue, onedocperline_default));
			_singleton.setProperty("lt.lm.onedocperline", onedocperline_default);
			return onedocperline();
		}
	}

	private static final String tokenfilter_default = String.valueOf(5); // 0-5
	public static int tokenfilter() {
		String propvalue = _singleton.getProperty("lt.lm.tokenfilter", tokenfilter_default);
		try {
			return Integer.parseInt(propvalue.trim());
		} catch (Exception e) {
			LOG.warn(String.format("Could not parse '%s' as integer. Setting to default value (%s).", propvalue, tokenfilter_default));
			_singleton.setProperty("lt.lm.tokenfilter", tokenfilter_default);
			return tokenfilter();
		}
	}
	
	private static final String tokennormalize_default = String.valueOf(2); // 0-4
	public static int tokennormalize() {
		String propvalue = _singleton.getProperty("lt.lm.tokennormalize", tokennormalize_default);
		try {
			return Integer.parseInt(propvalue.trim());
		} catch (Exception e) {
			LOG.warn(String.format("Could not parse '%s' as integer. Setting to default value (%s).", propvalue, tokennormalize_default));
			_singleton.setProperty("lt.lm.tokennormalize", tokennormalize_default);
			return tokennormalize();
		}
	}
	
	private static final String merge_default = String.valueOf(1);
	public static int merge() {
		String propvalue = _singleton.getProperty("lt.lm.merge", merge_default);
		try {
			return Integer.parseInt(propvalue.trim());
		} catch (Exception e) {
			LOG.warn(String.format("Could not parse '%s'. Setting to default value (%s).", propvalue, merge_default));
			_singleton.setProperty("lt.lm.merge", merge_default);
			return merge();
		}
	}

	private static final String knUniformBackoff_default = String.valueOf(true);
	public static boolean knUniformBackoff() {
		String propvalue = _singleton.getProperty("lt.lm.knUniformBackoff", knUniformBackoff_default);
		try {
			return Boolean.valueOf(propvalue.trim());
		} catch (Exception e) {
			LOG.warn(String.format("Could not parse '%s' as boolean. Setting to default value (%s).", propvalue, String.valueOf(knUniformBackoff_default)));
			_singleton.setProperty("lt.lm.knUniformBackoff", knUniformBackoff_default);
			return knUniformBackoff();
		}
	}

	
	private static final String ramBufferPercentage_default = String.valueOf(.6d);
	public static float ramBufferPercentage() {
		String propvalue = _singleton.getProperty("lt.lm.ramBufferPercentage", ramBufferPercentage_default);
		try {
			return Float.valueOf(propvalue.trim());
		} catch (Exception e) {
			LOG.warn(String.format("Could not parse '%s' as double. Setting to default value (%s).", propvalue, ramBufferPercentage_default));
			_singleton.setProperty("lt.lm.ramBufferPercentage", ramBufferPercentage_default);
			return ramBufferPercentage();
		}
	}

//	private static final String sortTempdir_default = System.getProperty("java.io.tmpdir");
	public static String additionalSortParam() {
		return _singleton.getProperty("lt.lm.sortparam");
	}
	
}
