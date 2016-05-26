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

import java.nio.charset.Charset;


/**
 *
 * @author Steffen Remus
 * @formatter:off
 **/
public class UTF8CleanerMin implements UTF8Cleaner {

	public static char REPLACEMENT = '\u00BF'; // ¿
	public static char[] DIRTY_UTF8_CHARACTERS = {
		'\uFEFF', // Byte Order Mark (BOM)
		'\uFFFD', // � (U+FFFD, decimal: 65533)
	};

	public static final String[] DIRTY_UTF8;
	public static final String[] DIRTY_UTF8_CHARACTER_REPLACEMENTS;
	public static final Charset UTF8 = Charset.forName("UTF-8");
	static {
		DIRTY_UTF8 = new String[DIRTY_UTF8_CHARACTERS.length];
		DIRTY_UTF8_CHARACTER_REPLACEMENTS = new String[DIRTY_UTF8_CHARACTERS.length];
		for (int i = 0; i < DIRTY_UTF8_CHARACTERS.length; i++){
			DIRTY_UTF8[i] = new String(new byte[] { (byte) DIRTY_UTF8_CHARACTERS[i] }, UTF8);
			DIRTY_UTF8_CHARACTER_REPLACEMENTS[i] = String.format("#&x%s;", Integer.toHexString(DIRTY_UTF8_CHARACTERS[i]));
		}
	}

	@Override
	public char[] dirtyChars() {
		return DIRTY_UTF8_CHARACTERS;
	}

	@Override
	public String clean(String dirty) {
		char[] chars = dirty.toCharArray();
		for(int i = 0; i < chars.length; i++)
			for (char d : dirtyChars())
				if(chars[i] == d)
					chars[i] = REPLACEMENT;
		return new String(chars);
	}

}
