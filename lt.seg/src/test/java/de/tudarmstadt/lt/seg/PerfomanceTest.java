package de.tudarmstadt.lt.seg;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.junit.Ignore;
import org.junit.Test;

import de.tudarmstadt.lt.seg.app.Segmenter;
import de.tudarmstadt.lt.seg.sentence.RuleSplitter;

public class PerfomanceTest {

	@Test
	public void testRuleTokenizerLarge() throws ClassNotFoundException{
		Segmenter.main(
				"--file testdocs.txt --parallel 8 -l --debug -o /dev/null"
				.split(" "));
	}
	
	@Test
	@Ignore
	public void testRuleTokenizer() throws ClassNotFoundException, FileNotFoundException{
		new RuleSplitter().initParam("default", false).init(new BufferedReader(new FileReader("testdocs.txt"))).forEach(System.out::println);
//		 new RuleTokenizer().init(new FileReader("testdocs.txt")).forEach(System.out::println);
	}
	
}
