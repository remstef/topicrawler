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


/**
 * Pseudo symbols must be in line with BerkeleyLM and SRILM !!!
 *
 * @author Steffen Remus
 **/
public enum PseudoSymbol {
	//	ArpaLmReader.START_SYMBOL;
	//	ArpaLmReader.END_SYMBOL;
	//	ArpaLmReader.UNK_SYMBOL;

	SEQUENCE_START("<s>"),
	SEQUENCE_END("</s>"),
	UNKOWN_WORD("<unk>"),
	;

	private String _string_representation;

	private PseudoSymbol(String string_representation) {
		_string_representation = string_representation;
	}

	public String asString() {
		return _string_representation;
	}

}
