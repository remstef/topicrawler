/*
 * Copyright 2012
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package de.tudarmstadt.lt.utilities.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 
 * @author Steffen Remus
 * 
 * @param <E>
 */
public class TreeBag<E> extends TreeMap<E, Integer> implements Bag<E> {

	private static final long serialVersionUID = -4846894099215164962L;

	public TreeBag() {
		super();
	}

	public TreeBag(Comparator<? super E> comparator) {
		super(comparator);
	}

	public TreeBag(Map<? extends E, ? extends Integer> m) {
		super(m);
	}

	public TreeBag(SortedMap<E, ? extends Integer> m) {
		super(m);
	}

	@Override
	public Integer add(E e) {
		Integer quant = get(e);
		if(quant == null)
			quant = Integer.valueOf(0);
		quant += 1;
		put(e, quant);
		return quant;
	}

	@Override
	public Integer removeOne(E e) {
		Integer quant = get(e);
		if(quant == null)
			return Integer.valueOf(0);
		quant -= 1;
		if(quant.equals(Integer.valueOf(0)))
			remove(e);
		else
			put(e, quant);
		return quant;
	}

	@Override
	public Integer removeAll(E e) {
		return remove(e);
	}

	@Override
	public List<Entry<E,Integer>> entrySetSortedbyQuantity(boolean decreasing){
		List<Entry<E, Integer>> sortedbyQuantity = new ArrayList<Entry<E, Integer>>(entrySet());
		Collections.sort(sortedbyQuantity, decreasing ? decreasingQuantityComparator : increasingQuantityComparator);
		return sortedbyQuantity;
	}

	@Override
	public Integer getQuantity(E e) {
		Integer r = get(e);
		return r == null ? Integer.valueOf(0) : r;
	}

}
