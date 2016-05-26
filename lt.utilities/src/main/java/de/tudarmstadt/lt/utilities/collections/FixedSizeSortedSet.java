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
package de.tudarmstadt.lt.utilities.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * @author Steffen Remus
 *
 */
public class FixedSizeSortedSet<E> extends TreeSet<E>{

	private static final long serialVersionUID = 1L;
	private int _maxsize;

	public FixedSizeSortedSet(int maxsize) {
		super();
		_maxsize = maxsize;
	}

	public FixedSizeSortedSet(int maxsize, Comparator<E> comparator) {
		super(comparator);
		_maxsize = maxsize;
	}


	/* (non-Javadoc)
	 * @see java.util.TreeSet#add(java.lang.Object)
	 */
	@Override
	public boolean add(E e) {
		if(size() >= _maxsize){
			// check if there is a lower element in the set
			// if not it is already the smallest
			if (lower(e)==null)
				return false;
			remove(first());
		}
		return super.add(e);
	}

	/**
	 * @param c
	 * @return always true
	 */
	@Override
	public boolean addAll(Collection<? extends E> c) {
		for(E e : c)
			add(e);
		return true;
	}

}
