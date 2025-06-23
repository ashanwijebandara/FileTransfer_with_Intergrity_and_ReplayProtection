package utils;

import java.security.*;

public class RSAUtils {

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048); // Key size
        return keyGen.generateKeyPair();
    }

    public static byte[] sign(byte[] data, PrivateKey privateKey) throws Exception {
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(privateKey);
        privateSignature.update(data);
        return privateSignature.sign();
    }

    public static boolean verify(byte[] data, byte[] signature, PublicKey publicKey) throws Exception {
        Signature publicSignature = Signature.getInstance("SHA256withRSA");
        publicSignature.initVerify(publicKey);
        publicSignature.update(data);
        return publicSignature.verify(signature);
    }
}
