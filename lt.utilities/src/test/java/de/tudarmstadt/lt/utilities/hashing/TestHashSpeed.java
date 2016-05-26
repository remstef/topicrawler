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
package de.tudarmstadt.lt.utilities.hashing;

import java.nio.charset.Charset;

/**
 *
 * Imported 12/01/2015 by Steffen Remus from
 *  https://github.com/yonik/java_util
 *
 ***
 *
 * @author yonik
 *
 */
public class TestHashSpeed {

    static  Charset utf8Charset = Charset.forName("UTF-8");

    public static void main(String[] args) throws Exception {
        int arg = 0;
        int size = Integer.parseInt(args[arg++]);
        int iter = Integer.parseInt(args[arg++]);
        String method = args[arg++];

        byte[] arr = new byte[size];
        for (int i=0; i<arr.length; i++) {
            arr[i] = (byte)(i & 0x7f);
        }
        String s = new String(arr, "UTF-8");

        int ret = 0;
        long start = System.currentTimeMillis();

        if (method == null || method.equals("murmur32"))  {
            for (int i = 0; i<iter; i++) {
                // change offset and len so internal conditionals aren't predictable
                int offset = ret & 0x03;
                int len = arr.length - offset - ((ret>>3)&0x03);
                ret += MurmurHash3.murmurhash3_x86_32(arr, offset, len, i);
            }
        } else if (method.equals("slow_string")) {
            for (int i = 0; i<iter; i++) {
                // change offset and len so internal conditionals aren't predictable
                int offset = ret & 0x03;
                int len = arr.length - offset - ((ret>>3)&0x03);
                byte[] utf8 = s.getBytes(utf8Charset);
                ret += MurmurHash3.murmurhash3_x86_32(utf8, offset, len, i);
            }
        } else if (method.equals("fast_string")) {
            for (int i = 0; i<iter; i++) {
                // change offset and len so internal conditionals aren't predictable
                int offset = ret & 0x03;
                int len = arr.length - offset - ((ret>>3)&0x03);
                ret += MurmurHash3.murmurhash3_x86_32(s, offset, len, i);
            }
        } else {
            throw new RuntimeException("Unknown method " + method);
        }

        long end = System.currentTimeMillis();

        System.out.println("method="+method + " result="+ ret + " throughput=" + 1000 * ((double)size)*iter/(end-start) );

    }

}