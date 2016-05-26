/*
 * Copyright 2012
 * 
 * Licensed under the Ap* Copyright 2012 Steffen Remus
 * 
 * package de.tud.cohesion.utilities;
 * 
 * import java.util.ArrayList; ache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package de.tudarmstadt.lt.utilities.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Steffen Remus
 * 
 * @param <E>
 */
public class HashBag<E> extends HashMap<E, Integer> implements Bag<E> {

	private static final long serialVersionUID = 5770186642423766622L;
	private int _size;

	public HashBag() {
		super();
		_size = 0;
	}

	public HashBag(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public HashBag(int initialCapacity) {
		super(initialCapacity);
	}

	public HashBag(Map<? extends E, ? extends Integer> m) {
		super(m);
	}

	@Override
	public Integer add(E e) {
		Integer quant = get(e);
		if(quant == null)
			quant = Integer.valueOf(0);
		quant += 1;
		put(e, quant);
		_size++;
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
		_size--;
		return quant;
	}

	@Override
	public Integer get(Object key) {
		Integer q = super.get(key);
		return q != null ? q : Integer.valueOf(0);
	}

	@Override
	public Integer removeAll(E e) {
		_size -= get(e);
		return remove(e);
	}

	@Override
	public List<Entry<E,Integer>> entrySetSortedbyQuantity(boolean decreasing){
		List<Entry<E, Integer>> sortedbyQuantity = new ArrayList<Entry<E, Integer>>(entrySet());
		Collections.sort(sortedbyQuantity, decreasing ? decreasingQuantityComparator : increasingQuantityComparator);
		return sortedbyQuantity;
	}

	@Override
	public int size() {
		return _size;
	}

	@Override
	public Integer getQuantity(E e) {
		Integer r = get(e);
		return r == null ? Integer.valueOf(0) : r;
	}

}
