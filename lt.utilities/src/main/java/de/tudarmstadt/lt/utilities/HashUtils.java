/*
 *   Copyright 2016
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
package de.tudarmstadt.lt.utilities;

import de.tudarmstadt.lt.utilities.hashing.MurmurHash3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * Created by Steffen Remus
 */
public class HashUtils {

	private static Logger LOG = LoggerFactory.getLogger(HashUtils.class);

	private HashUtils(){/* DO NOT INSTANTIATE */}

	public static byte[] string_hash_sha256(final String hashme){
		return string_hash_sha256(hashme, Charset.defaultCharset());
	}

	public static byte[] string_hash_sha256(final String hashme, final String charsetName){
		return string_hash_sha256(hashme, Charset.forName(charsetName));
	}

	public static byte[] string_hash_sha256(final String hashme, final Charset charset){
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(hashme.getBytes(charset)); // Change this to "UTF-16" if needed
			byte[] digest = md.digest();
			return digest;
		}catch(Exception e){
			LOG.warn(String.format("Exception thrown while computing hash: %s %s", e.getClass().getSimpleName(), e.getMessage()));
			LOG.debug("Exception:", e);
			return hashme.getBytes();
		}
	}

	public static int string_hash_sha256_toInt(final String hashme){
		return string_hash_sha256_toInt(hashme, Charset.defaultCharset());
	}

	public static int string_hash_sha256_toInt(final String hashme, final String charsetName){
		return string_hash_sha256_toInt(hashme, Charset.forName(charsetName));
	}

	public static int string_hash_sha256_toInt(final String hashme, final Charset charset){
		byte[] hash = string_hash_sha256(hashme, charset);
		return ByteBuffer.wrap(hash).getInt();
	}

	public static int string_hash_murmur3_32bit(final String hashme){
		return MurmurHash3.murmurhash3_x86_32(hashme, 0, hashme.length(), 42);
	}

	

	public static byte[] decodeHexString(final String hex){
		byte[] bytes = hex.getBytes();
		return decode(bytes);

	}

	public static byte[] decodeHexString(final String hex, final Charset charset){
		byte[] bytes = hex.getBytes(charset);
		return decode(bytes);
	}

	private static final char[] hex_chars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

	// taken from apache commons codec
	public static String encodeHexString(byte[] bytes){
		int l = bytes.length;
		char[] out = new char[l << 1];
		// two characters form the hex value.
		for (int i = 0, j = 0; i < l; i++) {
			out[j++] = hex_chars[(0xF0 & bytes[i]) >>> 4];
			out[j++] = hex_chars[0x0F & bytes[i]];
		}
		return new String(out);
	}

	// taken from apache commons codec
	private static byte[] decode(byte[] chars){
		int len = chars.length;
		if ((len & 0x01) != 0)
			throw new IllegalStateException("Odd number of characters.");
		byte[] out = new byte[len >> 1];
		// two characters form the hex value.
		for (int i = 0, j = 0; j < len; i++) {
			int f = toDigit((char)chars[j], j) << 4;
			j++;
			f = f | toDigit((char)chars[j], j);
			j++;
			out[i] = (byte) (f & 0xFF);
		}
		return out;
	}

	private static int toDigit(char ch, int index) {
		int digit = Character.digit(ch, 16);
		if (digit == -1)
			throw new IllegalStateException("Illegal hexadecimal charcter " + ch + " at index " + index);
		return digit;
	}

}
