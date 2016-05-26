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
package de.tudarmstadt.lt.lm;

import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Steffen Remus
 **/
public class DummyLM<W> extends AbstractLanguageModel<W> implements LanguageModel<W> {

	private int _order;

	public DummyLM(int order) {
		_order = order;
	}

	@Override
	public int getOrder() {
		return _order;
	}

	@Override
	public W predictNextWord(List<W> history) {
		return null;
	}

	@Override
	public W getWord(int wordId) {
		return null;
	}

	@Override
	public int getWordIndex(W word) {
		return 0;
	}

	@Override
	public double getNgramLogProbability(int[] ngram) {
		return 0;
	}

	@Override
	public double getNgramLogProbability(List<W> ngram) {
		return 0;
	}

	@Override
	public Iterator<List<W>> getNgramIterator() {
		return null;
	}

	@Override
	public Iterator<List<Integer>> getNgramIdIterator() {
		return null;
	}

}
