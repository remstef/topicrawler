/*
 *   Copyright 2014
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
package de.tudarmstadt.lt.seg;

import java.util.EnumSet;

/**
 * 
 * @author Steffen Remus
 *
 */
public class Segment {

	public SegmentType type;

	public final StringBuffer text = new StringBuffer();

	/**
	 * begin is inclusive [begin,end)
	 */
	public int begin;

	/**
	 * end is exclusive [begin,end)
	 */
	public int end;

	public boolean hasZeroLength(){
		return begin == end;
	}
	
	public int length(){
		assert(text.length() == end - begin);
		return end - begin;
	}

	public String asString(){
		return text.toString();
	}

	public String asNormalizedString(int level){
		
		String result = text.toString();

		if(level >= 1 && (type == SegmentType.NON_WORD || type == SegmentType.UNKNOWN)){ // reduce non-word characters
			StringBuilder b = text.codePoints().boxed().reduce(new StringBuilder(), (x, y) -> {
				if(x.length() == 0 || x.codePointBefore(x.length()) != y)
					return x.appendCodePoint(y);
				return x;
			},(l,r) -> {
				return l.append(r.toString());
			});
			result = b.toString();
		}
		
		if(level >= 1 && type == SegmentType.CONTROL){ // reduce non-word characters
			result = type.symbol();
		}
		
		if(level >= 2 && (type == SegmentType.EMPTY_SPACE || type == SegmentType.PUNCT)){
			// replace numbers, punctuation and empty spaces with a single symbol, no matter how long the number once was
			result = type.symbol();
		}

		if(level >= 3){
			if(type == SegmentType.WORD_WITH_NUMBER){ // replace consecutive digits within a word
				StringBuilder b = text.codePoints().boxed().reduce(new StringBuilder(), (x, y) -> {
					if(x.length() == 0){
						if(Character.isDigit(y))
							return x.append(SegmentType.NUMBER.symbol());
						else
							return x.appendCodePoint(y);
					}
					if(Character.isDigit(y)){
						if(x.codePointBefore(x.length()) != SegmentType.NUMBER.symbol().codePointAt(0))
							return x.append(SegmentType.NUMBER.symbol());
						else 
							return x;
					}
					return x.appendCodePoint(y);
				},(l,r) -> {
					return l.append(r.toString());
				});
				result = b.toString();
			}
			if(EnumSet.of(SegmentType.NUMBER, SegmentType.DATE, SegmentType.PHONE, SegmentType.TIME).contains(type))
				result = type.symbol();
		}
		
		if(level >= 4){
			if(EnumSet.complementOf(EnumSet.of(SegmentType.WORD, SegmentType.WORD_LOWERCASE, SegmentType.WORD_UPPERCASE, SegmentType.WORD_WITH_NUMBER, SegmentType.SENTENCE, SegmentType.ABBRV, SegmentType.PARAGRAPH, SegmentType.TEXT)).contains(type))
				result = type.symbol();
		}
		
		if(level >= 5)
			result = result.toLowerCase();

		return result;
	}

	public boolean isPhrasal(){
		return text.codePoints().allMatch( cp -> SegmentType.PHRASE.allowedCharacterTypes().contains(Character.getType(cp))); 
	}

	public boolean isEmpty(){
		return type == SegmentType.EMPTY_SPACE; 
	}
	
	public boolean isPartOfSentence(){
		return type == SegmentType.SENTENCE || type == SegmentType.SENTENCE_BOUNDARY; 
	}
	
	public boolean isWord(){
		return type == SegmentType.WORD ||   
				type == SegmentType.WORD_UPPERCASE ||
				type == SegmentType.WORD_LOWERCASE;
	}

	// TODO: replace with type.ordninal in range [x,y]. needs reordering of Segmenttypes 
	public boolean isReadable(){
//		SegmentType.WORD,
//		SegmentType.NUMBER,
//		SegmentType.WORD_WITH_NUMBER, 
//		SegmentType.WORD_UPPERCASE,
//		SegmentType.WORD_LOWERCASE,
//		SegmentType.PUNCT
		return EnumSet.complementOf(EnumSet.of(
					SegmentType.CONTROL,
					SegmentType.UNKNOWN,
					SegmentType.EMPTY_SPACE
				)).contains(type); 
		

	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if(SegmentType.TOKEN_TYPES.contains(type))
			return String.format(
					"%-15s %s %-20s '%s' (n: '%s') ", 
					String.format("[%d,%d):", begin, end),
					isPhrasal() ? "p" : "n", 
					"("+type.toString()+"|"+type.symbol()+")", 
					isEmpty() ? SegmentationUtils.replaceEmptySpaceWithLiteral(text.toString()) : text.toString(), 
					type == SegmentType.WORD_WITH_NUMBER ? asNormalizedString(0) + "|" + asNormalizedString(3) : asNormalizedString(0));
		return String.format(
				"%-15s %-20s '%s'", 
				String.format("[%d,%d):", begin, end), 
				"("+type.toString()+"|"+type.symbol()+")", 
				SegmentationUtils.escapeNonAscii(SegmentationUtils.replaceEmptySpaceWithLiteral(text.toString())));
	}

}
