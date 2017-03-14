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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * @author Steffen Remus
 *
 */
public enum SegmentType {
	
	
	
	CONTROL("¶",
			Character.CONTROL,
			Character.FORMAT,
			Character.SURROGATE),
	
	TEXT("T", SegmentationUtils.chartypes),
	
	PARAGRAPH("§",
		Arrays.stream(SegmentationUtils.chartypes).filter( x -> 
		x != Character.PARAGRAPH_SEPARATOR).toArray()),
	
	SENTENCE("S",
		Arrays.stream(SegmentationUtils.chartypes).filter( x -> 
		x != Character.PARAGRAPH_SEPARATOR && 
		x != Character.DASH_PUNCTUATION && 
		x != Character.START_PUNCTUATION && 
		x != Character.END_PUNCTUATION).toArray()),

	SENTENCE_BOUNDARY(". ",
			Character.PARAGRAPH_SEPARATOR,
			Character.DASH_PUNCTUATION,
			Character.START_PUNCTUATION, 
			Character.END_PUNCTUATION),

	PHRASE("P",
		Arrays.stream(SegmentationUtils.chartypes).filter( x -> 
		x != Character.NON_SPACING_MARK  && 
		x != Character.ENCLOSING_MARK  && 
		x != Character.COMBINING_SPACING_MARK && 
		x != Character.DASH_PUNCTUATION && 
		x != Character.START_PUNCTUATION && 
		x != Character.END_PUNCTUATION && 
		x != Character.CONNECTOR_PUNCTUATION && 
		x != Character.OTHER_PUNCTUATION && 
		x != Character.INITIAL_QUOTE_PUNCTUATION && 
		x != Character.FINAL_QUOTE_PUNCTUATION && 
		x != Character.SPACE_SEPARATOR && 
		x != Character.LINE_SEPARATOR && 
		x != Character.PARAGRAPH_SEPARATOR).toArray()),
	
	WORD("w",
			Character.UPPERCASE_LETTER, 
			Character.LOWERCASE_LETTER, 
			Character.TITLECASE_LETTER, 
			Character.MODIFIER_LETTER, 
			Character.OTHER_LETTER),

	// \u1e87
	WORD_WITH_NUMBER("w0",
			Character.UPPERCASE_LETTER, 
			Character.LOWERCASE_LETTER, 
			Character.TITLECASE_LETTER, 
			Character.MODIFIER_LETTER, 
			Character.OTHER_LETTER, 
			Character.DECIMAL_DIGIT_NUMBER, 
			Character.LETTER_NUMBER, 
			Character.OTHER_NUMBER
			),
			
	// #
	NUMBER("0",
			Character.DECIMAL_DIGIT_NUMBER, 
			Character.LETTER_NUMBER, 
			Character.OTHER_NUMBER),
			
	WORD_UPPERCASE("W",
			Character.UPPERCASE_LETTER,
			Character.TITLECASE_LETTER
			),

	WORD_LOWERCASE("l",
			Character.LOWERCASE_LETTER,
			Character.MODIFIER_LETTER,
			Character.OTHER_LETTER
			),

	PUNCT(".",
			Character.NON_SPACING_MARK,
			Character.ENCLOSING_MARK,
			Character.COMBINING_SPACING_MARK,
			Character.DASH_PUNCTUATION,
			Character.START_PUNCTUATION,
			Character.END_PUNCTUATION,
			Character.CONNECTOR_PUNCTUATION,
			Character.OTHER_PUNCTUATION,
			Character.INITIAL_QUOTE_PUNCTUATION,
			Character.FINAL_QUOTE_PUNCTUATION),
		
	EMPTY_SPACE(" ",
			Character.SPACE_SEPARATOR,
			Character.LINE_SEPARATOR,
			Character.PARAGRAPH_SEPARATOR,
			Character.CONTROL
			),

	NON_WORD("₩",
		Arrays.stream(SegmentationUtils.chartypes).filter(x -> 
		x != Character.UPPERCASE_LETTER &&
		x != Character.LOWERCASE_LETTER &&
		x != Character.TITLECASE_LETTER && 
		x != Character.MODIFIER_LETTER &&
		x != Character.OTHER_LETTER).toArray()),

	UNKNOWN("�"),
	
	EMAIL("📧"),
	
	DATE("📅"),
	
	TIME("⌚"),
	
	PHONE("☎"),
	
	META("📓"),

	EMO("☺"),  
	
	URI("💻"),
	
	ABBRV("㍱"),
	
	REF("˃"),

	UNSPECIFIED("⸮"),
	
	;
	
	public static EnumSet<SegmentType> TOKEN_TYPES = EnumSet.range(SegmentType.WORD,  SegmentType.NON_WORD);
	
	private final Set<Integer> _char_types;
	
	private final String _symbol;
	
	private SegmentType(String symbol, int... chartypes){
		_symbol = symbol;
		Set<Integer> char_types = new HashSet<>();
		for(int chartype : chartypes)
			char_types.add(chartype);
		_char_types = Collections.synchronizedSet(Collections.unmodifiableSet(char_types));
	}
	
	public Set<Integer> allowedCharacterTypes(){
		return _char_types;	
	}
	
	public static SegmentType infer(Set<Integer> character_types){
		// compute overlap of charactertypes
		long min_diff = Long.MAX_VALUE;
		SegmentType min_st = SegmentType.UNKNOWN;
		
		for(final SegmentType st : TOKEN_TYPES){
			long inters = character_types.stream().parallel().filter(x -> st._char_types.contains(x)).count();
			long rest = character_types.size() - inters;
			if(rest  > 0)
				continue;
			long diff = st._char_types.size() - inters; 
			if(diff < min_diff){
				min_diff = diff;
				min_st = st;
			}
		}
		
		return min_st;
	}
	
	public String symbol(){
		return _symbol;
	}
	
}
