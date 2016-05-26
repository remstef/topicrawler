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

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author remus
 */
public class BagTest {

	@Test
	public void test() {
		Bag<String> b = new HashBag<String>();
		System.out.println(b.get("hallo"));
		b.add("hallo");
		b.add("hallo");
		System.out.println(b.get("hallo"));
		b.removeOne("hallo");
		System.out.println(b.get("hallo"));
	}
	
	@Test
	public void test2(){
		FixedSizeSortedSet<Double> s = new FixedSizeSortedSet<Double>(3);
		s.add(1d);
		s.add(2d);
		s.add(3d);
		s.add(4d);
		s.add(3.5d);
		Assert.assertSame(3, s.size());
		System.out.println(s);
		
	}

}
