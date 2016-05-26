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

import java.util.Iterator;

/**
 * @author Steffen Remus
 *
 */
public class SegmentationUtils {

	private SegmentationUtils(){ /* DO NOT INSTANTIATE */ }

	/**
	 * taken from Chris' perl tokenizers
	 */
	public final static int[] boundaries = new int[]{
		'\ufeff',
		'!',
		'"',
		'#',
		'$',
		'&',
		'\'',
		'(',
		')',
		'*',
		'+',
		',',
		'.',
		'/',
		':',
		';',
		'<',
		'=',
		'>',
		'?',
		'[',
		'\\',
		']',
		'^',
		'`',
		'{',
		'|',
		'}',
		'¡',
		'¢',
		'£',
		'¤',
		'¥',
		'«',
		'®',
		'»',
		'¿',
		'۔',
		'฿',
		'‘',
		'’',
		'‚',
		'“',
		'”',
		'„',
		'‹',
		'›',
		'₤',
		'₦',
		'₨',
		'₩',
		'₪',
		'₫',
		'€',
		'₱',
		'™',
		'〈',
		'〉',
		'《',
		'》',
		'「',
		'」',
		'『',
		'』',
		'元',
		'￥'
	};

	public final static int[] chartypes = new int[] {
		Character.UNASSIGNED,
		Character.UPPERCASE_LETTER,
		Character.LOWERCASE_LETTER,		 	
		Character.TITLECASE_LETTER,
		Character.MODIFIER_LETTER,
		Character.OTHER_LETTER,
		Character.NON_SPACING_MARK,
		Character.ENCLOSING_MARK,
		Character.COMBINING_SPACING_MARK,
		Character.DECIMAL_DIGIT_NUMBER,
		Character.LETTER_NUMBER,
		Character.OTHER_NUMBER,
		Character.SPACE_SEPARATOR,
		Character.LINE_SEPARATOR,
		Character.PARAGRAPH_SEPARATOR,
		Character.CONTROL,
		Character.FORMAT,
		Character.PRIVATE_USE,
		Character.SURROGATE,
		Character.DASH_PUNCTUATION,
		Character.START_PUNCTUATION,
		Character.END_PUNCTUATION,
		Character.CONNECTOR_PUNCTUATION,
		Character.OTHER_PUNCTUATION,
		Character.MATH_SYMBOL,
		Character.CURRENCY_SYMBOL,
		Character.MODIFIER_SYMBOL,
		Character.OTHER_SYMBOL,
		Character.INITIAL_QUOTE_PUNCTUATION,
		Character.FINAL_QUOTE_PUNCTUATION		
	};

	public static boolean inRange(int l, int u, int v, int... v_){
		boolean r = v >= l && v <= u;
		if(!r)
			return false;
		for(int v__ : v_)
			if(!(r &= (v__ >= l && v__ <= u)))
				return false;
		return true;
	}

	public static int[] JAVA_EMPTY_SPACE_ESCAPE_SEQUENCES = new int[]{
		//		'\ufeff',
		'\t',
		'\b',
		'\n',
		'\r',
		'\f'
	};

	public static String[] JAVA_EMPTY_SPACE_ESCAPE_SEQUENCE_LITERALS = new String[]{
		//		"<BOM>",
		"\\t",
		"\\b",
		"\\n",
		"\\r",
		"\\f"
	};

	public static String replaceEmptySpaceWithLiteral(String text){
		StringBuilder b = new StringBuilder();
		outer: for(int cp : text.codePoints().toArray()){
			for(int j = 0; j < JAVA_EMPTY_SPACE_ESCAPE_SEQUENCES.length; j++)
				if(cp == JAVA_EMPTY_SPACE_ESCAPE_SEQUENCES[j]){
					b.append(JAVA_EMPTY_SPACE_ESCAPE_SEQUENCE_LITERALS[j]);
					continue outer;
				}
			if(cp != '\u0020' && charIsEmptySpace(cp))
				b.append(String.format("\\u%04x", cp));
			else
				b.appendCodePoint(cp);
		}
		return b.toString();
	}

	public static boolean charIsLineSeparator(int codepoint){
		return 
				codepoint == (int)'\n' ||
				codepoint == (int)'\r' ||
				codepoint == (int)'\f' ||
				Character.getType(codepoint) == Character.LINE_SEPARATOR;
	}

	public static String escapeNonAscii(String text) {
		StringBuilder b = new StringBuilder();
		for(int i=0; i < text.length(); i++) {
			int cp = Character.codePointAt(text, i);
			int charCount = Character.charCount(cp);
			if (charCount > 1)
				i += charCount - 1; // 2.
			if (cp < 128) // ascii
				b.appendCodePoint(cp);
			else // non-ascii
				b.append(String.format("\\u%04x", cp));
		}
		return b.toString();
	}
	public static boolean charTypeIsEmptySpace(int chartype){
		return SegmentType.EMPTY_SPACE.allowedCharacterTypes().contains(chartype);
	}

	public static boolean charIsEmptySpace(int codepoint){
		return charTypeIsEmptySpace(Character.getType(codepoint));
	}

	public static String reversed(String text) {
		return new StringBuilder(text).reverse().toString();
	}

	public static String convert (String text) {
		StringBuilder b = new StringBuilder();
		int[] codePoints = text.codePoints().toArray();
		for(int i = 0; i < codePoints.length;){
			int cp = codePoints[i++];
			if (cp == '\\') {
				cp = codePoints[i++];
				if(cp == 'u') {
					// Read the xxxx
					int value=0;
					for (int j=0; j<4; j++) {
						cp = codePoints[i++];
						switch (cp) {
						case '0': case '1': case '2': case '3': case '4':
						case '5': case '6': case '7': case '8': case '9':
							value = (value << 4) + cp - '0';
							break;
						case 'a': case 'b': case 'c':
						case 'd': case 'e': case 'f':
							value = (value << 4) + 10 + cp - 'a';
							break;
						case 'A': case 'B': case 'C':
						case 'D': case 'E': case 'F':
							value = (value << 4) + 10 + cp - 'A';
							break;
						default:
							throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
						}
					}
					b.appendCodePoint(value);
				} else {
					if (cp == 't') cp = '\t';
					else if (cp == 'r') cp = '\r';
					else if (cp == 'n') cp = '\n';
					else if (cp == 'f') cp = '\f';
					b.appendCodePoint(cp);
				}
			} else {
				b.appendCodePoint(cp);
			}
		}
		return b.toString();
	}


	public static Iterable<Segment> mergeConsectutiveTypes(Iterable<Segment> segments){
		return new Iterable<Segment>() {
			@Override
			public Iterator<Segment> iterator() {
				/* merge consecutive types if they are not words */
				final Iterator<Segment> segments_iter = segments.iterator();
				return new Iterator<Segment>() {
					Iterator<Segment> iter = segments_iter;
					SegmentType _last_type = null;
					Segment _s;
					@Override
					public boolean hasNext() {
						if(!iter.hasNext()){
							return false;
						}
						_s = iter.next();
						if(_last_type == null){
							_last_type = _s.type;
							return true;
						}
						while(!(_s.isWord() || _s.type == SegmentType.WORD_WITH_NUMBER) && _last_type == _s.type && iter.hasNext())
							_s = iter.next();
						if(!(_s.isWord() || _s.type == SegmentType.WORD_WITH_NUMBER) && _last_type == _s.type) // final check if iter.hasNext was false
							return false;
						_last_type = _s.type;
						return true;
					}

					@Override
					public Segment next() {
						return _s;
					}
				};	
			}
		};
	}

	public static Iterable<String> mergeConsecutiveTokens(Iterable<String> tokens){
		return new Iterable<String>(){
			@Override
			public Iterator<String> iterator() {
				/* merge consecutive types if they are not words */
				final Iterator<String> token_iter = tokens.iterator();
				return new Iterator<String>() {
					Iterator<String> iter = token_iter;
					String _last_token = null;
					String _token;
					@Override
					public boolean hasNext() {
						if(!iter.hasNext()){
							return false;
						}
						_token = iter.next();
						if(_last_token == null){
							_last_token = _token;
							return true;
						}
						while(_last_token.equals(_token) && iter.hasNext())
							_token = iter.next();
						if(_last_token.equals(_token))
							return false;
						_last_token = _token;
						return true;
					}
					@Override
					public String next() {
						return _token;
					}
				};	
			}
		};
	}
}
