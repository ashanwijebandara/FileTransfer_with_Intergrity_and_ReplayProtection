package utils;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

public class RSAUtils {

    private static final int KEY_SIZE = 2048;
    private static final String ALGORITHM = "RSA";

    /**
     * Generates an RSA KeyPair with 2048-bit key size.
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyGen.initialize(KEY_SIZE);
        return keyGen.generateKeyPair();
    }

    /**
     * Signs the input data using the sender's private key.
     */
    public static byte[] sign(byte[] data, PrivateKey privateKey) throws GeneralSecurityException {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(data);
        return signer.sign();
    }

    /**
     * Verifies the digital signature with the sender's public key.
     */
    public static boolean verify(byte[] data, byte[] signature, PublicKey publicKey) throws GeneralSecurityException {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(data);
        return verifier.verify(signature);
    }

    /**
     * Encrypts data using a public RSA key (used for encrypting AES key).
     */
    public static byte[] encrypt(byte[] data, PublicKey publicKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    /**
     * Decrypts RSA-encrypted data using the receiver's private RSA key.
     */
    public static byte[] decrypt(byte[] encryptedData, PrivateKey privateKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encryptedData);
    }

    /**
     * Encodes byte array to Base64 string.
     */
    public static String toBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Decodes Base64 string to byte array.
     */
    public static byte[] fromBase64(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Converts PublicKey to Base64 String.
     */
    public static String publicKeyToBase64(PublicKey publicKey) {
        return toBase64(publicKey.getEncoded());
    }

    /**
     * Converts PrivateKey to Base64 String.
     */
    public static String privateKeyToBase64(PrivateKey privateKey) {
        return toBase64(privateKey.getEncoded());
    }

    /**
     * Converts Base64 String to PublicKey.
     */
    public static PublicKey base64ToPublicKey(String base64) throws GeneralSecurityException {
        byte[] keyBytes = fromBase64(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
        return kf.generatePublic(spec);
    }

    /**
     * Converts Base64 String to PrivateKey.
     */
    public static PrivateKey base64ToPrivateKey(String base64) throws GeneralSecurityException {
        byte[] keyBytes = fromBase64(base64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
        return kf.generatePrivate(spec);
    }
}
