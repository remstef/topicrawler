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
package de.tudarmstadt.lt.ltbot.text;

/**
 *
 * @author Steffen Remus
 * @formatter:off
 **/
public class UTF8CleanerExt extends UTF8CleanerMin implements UTF8Cleaner {

	// dirty means dusty: Untokenizable by stanford parser
	public static char[] DIRTY_UTF8_CHARACTERS = {
		'\u0001', //  (U+1, decimal: 1)
		'\u0002', //  (U+2, decimal: 2)
		'\u0003', //  (U+3, decimal: 3)
		'\u0004', //  (U+4, decimal: 4)
		'\u0005', //  (U+5, decimal: 5)
		'\u0006', //  (U+6, decimal: 6)
		'\u0007', //  (U+7, decimal: 7)
		'\u0008', //  (U+8, decimal: 8)
		'\u000E', //  (U+E, decimal: 14)
		'\u000F', //  (U+F, decimal: 15)
		'\u0010', //  (U+10, decimal: 16)
		'\u0011', //  (U+11, decimal: 17)
		'\u0012', //  (U+12, decimal: 18)
		'\u0013', //  (U+13, decimal: 19)
		'\u0014', //  (U+14, decimal: 20)
		'\u0015', //  (U+15, decimal: 21)
		'\u0016', //  (U+16, decimal: 22)
		'\u0017', //  (U+17, decimal: 23)
		'\u0018', //  (U+18, decimal: 24)
		'\u0019', //  (U+19, decimal: 25)
		'\u001A', //  (U+1A, decimal: 26)
		'\u001B', //  (U+1B, decimal: 27)
		'\u001C', //  (U+1C, decimal: 28)
		'\u001D', //  (U+1D, decimal: 29)
		'\u001E', //  (U+1E, decimal: 30)
		'\u001F', //  (U+1F, decimal: 31)
		'\u007F', //  (U+7F, decimal: 127)
		'\u0081', //  (U+81, decimal: 129)
		'\u0082', // ‚ (U+82, decimal: 130)
		'\u0083', // ƒ (U+83, decimal: 131)
		'\u0084', // „ (U+84, decimal: 132)
		'\u0086', // † (U+86, decimal: 134)
		'\u0087', // ‡ (U+87, decimal: 135)
		'\u0088', //  (U+88, decimal: 136)
		'\u0089', // ‰ (U+89, decimal: 137)
		'\u008A', // Š (U+8A, decimal: 138)
		'\u008B', // ‹ (U+8B, decimal: 139)
		'\u008C', // Œ (U+8C, decimal: 140)
		'\u008D', //   (U+8D, decimal: 141)
		'\u008E', // Ž (U+8E, decimal: 142)
		'\u008F', //   (U+8F, decimal: 143)
		'\u0090', //   (U+90, decimal: 144)
		'\u0095', // • (U+95, decimal: 149)
		'\u0098', // ˜ (U+98, decimal: 152)
		'\u0099', // ™ (U+99, decimal: 153)
		'\u009A', // š (U+9A, decimal: 154)
		'\u009B', // › (U+9B, decimal: 155)
		'\u009C', // œ (U+9C, decimal: 156)
		'\u009D', //  (U+9D, decimal: 157)
		'\u009E', // ž (U+9E, decimal: 158)
		'\u009F', // Ÿ (U+9F, decimal: 159)
		'\u09F3', // ৳ (U+9F3, decimal: 2547)
		'\u0D03', // ഃ (U+D03, decimal: 3331)
		'\u0F9D', //  (U+F9D, decimal: 3997)
		'\u0D4D', //  (U+D4D, decimal: 3405)
		'\u17DB', // ៛ (U+17DB, decimal: 6107)
		'\u200C', //  ‌ (U+200C, decimal: 8204)
		'\u2010', // ‐ (U+2010, decimal: 8208)
		'\u2011', // ‑ (U+2011, decimal: 8209)
		'\u2012', // ‒ (U+2012, decimal: 8210)
		'\u202F', //   (U+202F, decimal: 8239)
		'\u20A1', // ₡ (U+20A1, decimal: 8353)
		'\u20A2', // ₢ (U+20A2, decimal: 8354)
		'\u20A3', // ₣ (U+20A3, decimal: 8355)
		'\u20A5', // ₥ (U+20A5, decimal: 8357)
		'\u20A6', // ₦ (U+20A6, decimal: 8358)
		'\u20A7', // ₧ (U+20A7, decimal: 8359)
		'\u20A8', // ₨ (U+20A8, decimal: 8360)
		'\u20A9', // ₩ (U+20A9, decimal: 8361)
		'\u20AA', // ₪ (U+20AA, decimal: 8362)
		'\u20AB', // ₫ (U+20AB, decimal: 8363)
		'\u20AD', // ₭ (U+20AD, decimal: 8365)
		'\u20AE', // ₮ (U+20AE, decimal: 8366)
		'\u20AF', // ₯ (U+20AF, decimal: 8367)
		'\u20B0', // ₰ (U+20B0, decimal: 8368)
		'\u20B3', // ₳ (U+20B3, decimal: 8371)
		'\u20B4', // ₴ (U+20B4, decimal: 8372)
		'\u20B5', // ₵ (U+20B5, decimal: 8373)
		'\u2160', // Ⅰ (U+2160, decimal: 8544)
		'\u2161', // Ⅱ (U+2161, decimal: 8545)
		'\u2162', //  (U+2162, decimal: 8546)
		'\u2163', //  (U+2163, decimal: 8547)
		'\u2164', //  (U+2164, decimal: 8548)
		'\u2165', //  (U+2165, decimal: 8549)
		'\u2166', //  (U+2166, decimal: 8550)
		'\u2167', //  (U+2167, decimal: 8551)
		'\u2168', //  (U+2168, decimal: 8552)
		'\uD83C', //  (U+D83C, decimal: 55356)
		'\uD83D', //  (U+D83D, decimal: 55357)
		'\uE716', //  (U+E716, decimal: 59158)
		'\uE776', //  (U+E776, decimal: 59254)
		'\uF020', //  (U+F020, decimal: 61472)
		'\uF044', //  (U+F044, decimal: 61508)
		'\uF074', //  (U+F074, decimal: 61556)
		'\uFEFF', // Byte Order Mark (BOM)
		'\uFFFC', // ￼ (U+FFFC, decimal: 65532)
		'\uFFFD', // � (U+FFFD, decimal: 65533)
	};

	@Override
	public char[] dirtyChars() {
		return DIRTY_UTF8_CHARACTERS;
	}

}
