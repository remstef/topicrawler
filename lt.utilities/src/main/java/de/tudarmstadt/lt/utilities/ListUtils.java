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
import java.util.List;

/**
 *
 * @author Steffen Remus
 */
public class ListUtils {

	private ListUtils() { /* DO NOT INSTANTIATE */ }

	public static <T> int[] sortIdsByValue(final List<T> values, final Comparator<T> comparator) {
		Integer[] ids = new Integer[values.size()];
		for (int id = 0; id < ids.length; ids[id] = id++);
		Arrays.sort(ids, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return comparator.compare(values.get(o1), values.get(o2));
			}
		});
		return org.apache.commons.lang.ArrayUtils.toPrimitive(ids);
	}

	public static <T> T getLastElement(final List<T> values) {
		return values.get(values.size() - 1);
	}

}
