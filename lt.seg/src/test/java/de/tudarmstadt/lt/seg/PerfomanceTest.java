package de.tudarmstadt.lt.seg;

import org.junit.Test;

import de.tudarmstadt.lt.seg.app.Segmenter;

public class PerfomanceTest {

	@Test
	public void testRuleTokenizerLarge() throws ClassNotFoundException{
		Segmenter.main(
				"--file testdocs.txt --out /dev/null --parallel 8 -l"
				.split(" "));
	}
	
}
