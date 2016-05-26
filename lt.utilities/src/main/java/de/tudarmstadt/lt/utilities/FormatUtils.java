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

import java.text.DecimalFormat;

/**
 *
 * @author Steffen Remus
 */
public class FormatUtils {

	private FormatUtils() { /* DO NOT INSTANTIATE */ 	}

	// Formatter is not thread safe, with ThreadLocal it is!
	private static ThreadLocal<DecimalFormat> nf = new ThreadLocal() {
		@Override
		protected Object initialValue() {
			return new DecimalFormat("##0.######E00");
		}
	};

	/**
	 *
	 * Examples:
	 *
	 * 1                           -> 1
	 * 12d                         -> 12
	 * 123d                        -> 123
	 * 1234d                       -> 1.234E03
	 * 12345d                      -> 12.345E03
	 * 123.1234567890d             -> 123.123457
	 * 1234.1234567890d            -> 1.23412346E03
	 * 1.12345678901234567890d     -> 1.12345679
	 * 12345.1234567890d           -> 12.3451235E03
	 * 0.000000000001234567890d    -> 1.23456789E-12
	 *
	 * @param number
	 * @return
	 */
	public static String formatNumber(Number number){
		return nf.get().format(number).replace("E00","");
	}

}
