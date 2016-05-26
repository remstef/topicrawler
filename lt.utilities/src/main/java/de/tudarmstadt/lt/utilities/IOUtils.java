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
package de.tudarmstadt.lt.utilities;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;

import org.apache.commons.io.LineIterator;

import de.tudarmstadt.lt.utilities.io.SimpleByteArrayInputStream;

/**
 *
 * @author Steffen Remus
 */
public class IOUtils {

	private IOUtils() { /* DO NOT INSTANTIATE */ 	}

	private static Charset _utf8_charset = Charset.forName("UTF-8");

	public static String readFileUTF8(String file) throws FileNotFoundException, IOException {
		return readFile(file, _utf8_charset);
	}

	public static String readFile(String file) throws FileNotFoundException, IOException {
		return readFile(file, Charset.defaultCharset());
	}

	public static String readFile(String file, Charset charset) throws FileNotFoundException, IOException {
		return read(new FileInputStream(file), charset);
	}
	
	public static String read(InputStream is) throws FileNotFoundException, IOException {
		return read(is, Charset.defaultCharset());
	}
	
	public static String read(InputStream is, Charset charset) throws FileNotFoundException, IOException {
		SimpleByteArrayInputStream in = new SimpleByteArrayInputStream(new BufferedInputStream(is));
		byte[] bytes = in.readAll();
		in.close();
		String result = stringFromBytes(bytes, charset);
		return result;
	}

	public static String stringFromBytesUTF8(byte[] bytes) {
		return stringFromBytes(bytes, _utf8_charset);
	}

	public static String stringFromBytes(byte[] bytes) {
		return stringFromBytes(bytes, Charset.defaultCharset());
	}

	public static String stringFromBytes(byte[] bytes, Charset charset) {
		return new String(bytes, charset);
	}

	public static class CountingLineIterator extends LineIterator {

		public CountingLineIterator(Reader reader) throws IllegalArgumentException {
			super(reader);
		}

		private long _current_line_number = 0l;

		public long getCurentLineNumber() {
			return _current_line_number;
		}

		@Override
		public String nextLine() {
			String line = super.nextLine();
			_current_line_number++;
			return line;
		}

	}

}
