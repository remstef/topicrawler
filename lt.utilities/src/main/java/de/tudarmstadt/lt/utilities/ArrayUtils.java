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

import java.util.Arrays;
import java.util.Comparator;

/**
 *
 * @author Steffen Remus
 */
public class ArrayUtils {

	private ArrayUtils() { /* DO NOT INSTANTIATE */ }

	public static <T> T[] getConcatinatedArray(T[] a, T[] b) {
		T[] c = Arrays.copyOf(a, a.length + b.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}

	public static <T> int[] sortIdsByValue(final T[] values, final Comparator<T> comparator) {
		Integer[] ids = new Integer[values.length];
		for (int id = 0; id < ids.length; ids[id] = id++);
		Arrays.sort(ids, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return comparator.compare(values[o1], values[o2]);
			}
		});
		return org.apache.commons.lang.ArrayUtils.toPrimitive(ids);
	}

}
