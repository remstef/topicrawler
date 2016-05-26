package de.tudarmstadt.lt.lm.berkeleylm;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.tudarmstadt.lt.lm.LanguageModelHelper;
import de.tudarmstadt.lt.lm.service.BreakIteratorStringProvider;
import de.tudarmstadt.lt.utilities.LogUtils;

public class BerkeleyLmTest {
	static {
		LogUtils.setLogLevel("de.tudarmstadt.lt.lm", "trace");
	}

	static String _inputPath = ClassLoader.getSystemClassLoader().getResource("testlm").getPath();
	static File _temp_folder;

	@Before
	public void setupTest() throws IOException {
		TemporaryFolder f = new TemporaryFolder();
		f.create();
		_temp_folder = f.getRoot();
		System.out.println("created temporary folder: " + _temp_folder.getAbsolutePath());
	}

	@Test
	public void testNgrams() throws Exception {
		
		BerkeleyLM<String> blm = (BerkeleyLM<String>)LanguageModelHelper.createBerkelyLmFromTxtFilesInDirectory(new BreakIteratorStringProvider(), new File(_inputPath), 3, -10, -1, 1, false);
		for (int i = 1; i <= blm.getOrder(); i++) {
			for (Iterator<List<String>> iter = blm.getNgramIterator(i); iter.hasNext();) {
				List<String> ngram = iter.next();
				System.out.println(ngram);
			}

		}
	}

}
