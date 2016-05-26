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
package de.tudarmstadt.lt.ltbot.text;

import org.junit.Test;

import de.tudarmstadt.lt.ltbot.text.UTF8Cleaner;
import de.tudarmstadt.lt.ltbot.text.UTF8CleanerExt;
import de.tudarmstadt.lt.ltbot.text.UTF8CleanerMin;

/**
 *
 * @author Steffen Remus
 **/
public class UTF8CleanerTest {

	@Test
	public void printChars() {

		for (char c : UTF8CleanerMin.DIRTY_UTF8_CHARACTERS)
			System.out.format("%s \t %s \t %d %n", c, Integer.toHexString(c), (int) c);

		System.out.println("=== ===");

		for (char c : UTF8CleanerExt.DIRTY_UTF8_CHARACTERS)
			System.out.format("%s \t %s \t %d %n", c, Integer.toHexString(c), (int) c);

	}

	@Test
	public void printCleaned() {

		String dirty = new String(UTF8CleanerExt.DIRTY_UTF8_CHARACTERS);

		UTF8Cleaner cleaner = new UTF8CleanerMin();
		System.out.println(cleaner.clean(dirty));

		System.out.println("=== ===");

		cleaner = new UTF8CleanerExt();
		System.out.println(cleaner.clean(dirty));

	}

}
