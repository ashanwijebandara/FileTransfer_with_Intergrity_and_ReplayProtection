package utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESUtils {

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 128; // Change to 256 if needed (ensure JCE policy allows it)

    /**
     * Generates a new AES secret key.
     */
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(KEY_SIZE);
        return keyGen.generateKey();
    }

    /**
     * Encrypts the given byte array using AES and returns encrypted bytes.
     */
    public static byte[] encrypt(byte[] data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] encryptedData, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(encryptedData);
    }


    /**
     * Creates a SecretKey object from raw byte array.
     */
    public static SecretKey getKeyFromBytes(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * Converts SecretKey to a Base64 string.
     */
    public static String keyToBase64(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Converts a Base64 string back to SecretKey.
     */
    public static SecretKey keyFromBase64(String base64Key) {
        byte[] decoded = Base64.getDecoder().decode(base64Key);
        return new SecretKeySpec(decoded, ALGORITHM);
    }
}
