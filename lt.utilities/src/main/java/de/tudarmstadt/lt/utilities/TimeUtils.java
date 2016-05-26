/*
 *   Copyright 2015
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author Steffen Remus
 *
 */
public class TimeUtils {

	private TimeUtils() { /* DO NOT INSTANTIATE */ }

	// use ThreadLocal because simpleDateFormat is not!! thread safe. 
	private static final ThreadLocal<SimpleDateFormat> _ISO_8601_UTC = new ThreadLocal<SimpleDateFormat>(){
		protected SimpleDateFormat initialValue() {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH); 
			df.setTimeZone(TimeZone.getTimeZone("UTC"));
			return df;
		};
	};
	
	private static final ThreadLocal<SimpleDateFormat> _ISO_8601 = new ThreadLocal<SimpleDateFormat>(){
		protected SimpleDateFormat initialValue() { 
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
		};
	};
	
	private static final ThreadLocal<SimpleDateFormat> _DF17 = new ThreadLocal<SimpleDateFormat>(){
		protected SimpleDateFormat initialValue() { 
			return new SimpleDateFormat("yyyyMMddHHmmssSSS");
		};
	};
	
	public static String get_ISO_8601_UTC(){
		return _ISO_8601_UTC.get().format(new Date());
	}
	
	public static String get_ISO_8601(){
		String result = _ISO_8601.get().format(new Date());
		// strip out "GMT"
		result = result.replace("GMT", "");// result.substring(0, 19) + result.substring(22, result.length()); 
		return result;
	}
	
	public static String getSimple17(){
		return _DF17.get().format(new Date());
	}

}
