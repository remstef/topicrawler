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

import java.util.regex.Pattern;

/**
 *
 * @author Steffen Remus
 */
public class StringUtils {

	private StringUtils() { /* DO NOT INSTANTIATE */ 	}

	private static Pattern EMPTYSPACE_END = Pattern.compile("[\\s\\u00A0]+$");
	private static Pattern EMPTYSPACE_START = Pattern.compile("^[\\s\\u00A0]+");
	private static Pattern EMPTYSPACE = Pattern.compile("[\\s\\u00A0]+");

	public static String ltrim(String text){
		return EMPTYSPACE_START.matcher(text).replaceFirst("");
	}

	public static String rtrim(String text){
		return EMPTYSPACE_END.matcher(text).replaceFirst("");
	}

	public static String trim(String text){
		return ltrim(rtrim(text));
	}

	public static String trim_and_replace_emptyspace(String text, String replacement){
		return EMPTYSPACE.matcher(ltrim(rtrim(text))).replaceAll(replacement);
	}

}
