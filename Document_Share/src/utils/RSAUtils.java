package utils;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

/**
 * Utility class for RSA key generation, encryption, decryption,
 * signing, and verification, along with Base64 encoding/decoding support.
 */
public class RSAUtils {

    private static final int KEY_SIZE = 2048;
    private static final String ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    /**
     * Generates a new RSA KeyPair.
     *
     * @return KeyPair containing public and private keys
     * @throws NoSuchAlgorithmException if RSA algorithm is not supported
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        keyGen.initialize(KEY_SIZE);
        return keyGen.generateKeyPair();
    }

    /**
     * Encrypts the given data using the provided RSA public key.
     *
     * @param data       Data to encrypt
     * @param publicKey  RSA public key
     * @return Encrypted data as byte array
     * @throws GeneralSecurityException if encryption fails
     */
//    public static byte[] encrypt(byte[] data, PublicKey publicKey) throws GeneralSecurityException {
//        Cipher cipher = Cipher.getInstance(ALGORITHM);
//        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
//        return cipher.doFinal(data);
//    }

    /**
     * Decrypts the given data using the provided RSA private key.
     *
     * @param encryptedData Encrypted data
     * @param privateKey    RSA private key
     * @return Decrypted data as byte array
     * @throws GeneralSecurityException if decryption fails
     */
    public static byte[] decrypt(byte[] encryptedData, PrivateKey privateKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encryptedData);
    }

    /**
     * Signs the input data using the given private key.
     *
     * @param data        Data to sign
     * @param privateKey  RSA private key
     * @return Digital signature
     * @throws GeneralSecurityException if signing fails
     */
    public static byte[] sign(byte[] data, PrivateKey privateKey) throws GeneralSecurityException {
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    /**
     * Verifies the digital signature using the corresponding public key.
     *
     * @param data       Original data
     * @param signature  Digital signature to verify
     * @param publicKey  RSA public key
     * @return true if the signature is valid; false otherwise
     * @throws GeneralSecurityException if verification fails
     */
    public static boolean verify(byte[] data, byte[] signature, PublicKey publicKey) throws GeneralSecurityException {
        Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
        sig.initVerify(publicKey);
        sig.update(data);
        return sig.verify(signature);
    }

    /**
     * Converts byte array to Base64-encoded string.
     */
    public static String toBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Converts Base64-encoded string to byte array.
     */
    public static byte[] fromBase64(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Converts PublicKey to Base64 string.
     */
    public static String publicKeyToBase64(PublicKey publicKey) {
        return toBase64(publicKey.getEncoded());
    }

    /**
     * Converts PrivateKey to Base64 string.
     */
    public static String privateKeyToBase64(PrivateKey privateKey) {
        return toBase64(privateKey.getEncoded());
    }

    /**
     * Converts Base64-encoded string to PublicKey object.
     *
     * @param base64 Base64-encoded public key string
     * @return PublicKey object
     * @throws GeneralSecurityException if conversion fails
     */
    public static PublicKey base64ToPublicKey(String base64) throws GeneralSecurityException {
        byte[] keyBytes = fromBase64(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
        return kf.generatePublic(spec);
    }

    /**
     * Converts Base64-encoded string to PrivateKey object.
     *
     * @param base64 Base64-encoded private key string
     * @return PrivateKey object
     * @throws GeneralSecurityException if conversion fails
     */
    public static PrivateKey base64ToPrivateKey(String base64) throws GeneralSecurityException {
        byte[] keyBytes = fromBase64(base64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
        return kf.generatePrivate(spec);
    }
    public static byte[] encrypt(byte[] data, PublicKey publicKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

}
