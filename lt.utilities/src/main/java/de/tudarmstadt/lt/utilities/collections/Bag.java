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
package de.tudarmstadt.lt.utilities.collections;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Steffen Remus
 */
public interface Bag<E> extends Map<E, Integer> {

	final static Comparator<Entry<?, Integer>> increasingQuantityComparator = new Comparator<Entry<?, Integer>>() {
		@Override
		public int compare(Entry<?, Integer> o1,
				Entry<?, Integer> o2) {
			return o2.getValue().compareTo(o1.getValue());
		}
	};

	final static Comparator<Entry<?, Integer>> decreasingQuantityComparator = new Comparator<Entry<?, Integer>>() {
		@Override
		public int compare(Entry<?, Integer> o1,
				Entry<?, Integer> o2) {
			return o2.getValue().compareTo(o1.getValue());
		}
	};

	/**
	 * add element and return the new quantity
	 */
	public Integer add(E e);

	/**
	 * add element and return the new quantity
	 */
	public Integer getQuantity(E e);


	/**
	 * remove element and return the new quantity
	 */
	public Integer removeOne(E e);

	/**
	 * remove all elements quantity and return old quantity
	 */
	public Integer removeAll(E e);

	/**
	 * get a quantity sorted entry set
	 */
	public List<Entry<E, Integer>> entrySetSortedbyQuantity(boolean decreasing);


}
