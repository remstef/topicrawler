package de.tudarmstadt.lt.utilities;

import org.junit.Assert;
import org.junit.Test;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Created by Steffen Remus.
 */
public class HashUtilTests {

    @Test
    public void test_byte_array_size() throws Exception {

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update("the quick brown fox jumped over the lazy dog @ )(*)#*)@#(*_!".getBytes("UTF-8")); // Change this to "UTF-16" if needed
        byte[] digest = md.digest();
        System.out.println(digest.length);
        System.out.println(Arrays.toString(digest));

        md = MessageDigest.getInstance("SHA-256");
        md.update("the quick brown fox".getBytes("UTF-8")); // Change this to "UTF-16" if needed
        digest = md.digest();
        System.out.println(digest.length);
        System.out.println(Arrays.toString(digest));

        md = MessageDigest.getInstance("SHA-256");
        md.update("the".getBytes("UTF-8")); // Change this to "UTF-16" if needed
        digest = md.digest();
        System.out.println(digest.length);
        System.out.println(Arrays.toString(digest));

        md = MessageDigest.getInstance("SHA-256");
        md.update("t".getBytes("UTF-8")); // Change this to "UTF-16" if needed
        digest = md.digest();
        System.out.println(digest.length);
        System.out.println(Arrays.toString(digest));
        
    }
    
    @Test
    public void testHexConversion() throws NoSuchAlgorithmException {
    	MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update("the quick brown fox jumped over the lazy dog @ )(*)#*)@#(*_!".getBytes());
        byte[] digest = md.digest();
    	
    	String hex = HashUtils.encodeHexString(digest);
        byte[] decoded = HashUtils.decodeHexString(hex);
        
        System.out.println(hex);
        System.out.println(HashUtils.encodeHexString(decoded));
        System.out.println(Arrays.toString(decoded));
        System.out.println(Arrays.toString(digest));
        
        Assert.assertArrayEquals(digest, decoded);
        Assert.assertEquals(HashUtils.encodeHexString(digest), HashUtils.encodeHexString(decoded));
        
	}

}
