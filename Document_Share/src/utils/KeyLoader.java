package utils;

import java.io.IOException;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;

public class KeyLoader {

    public static void saveKeys(KeyPair keyPair, String publicKeyFile, String privateKeyFile) throws IOException {
        Files.write(Paths.get(publicKeyFile), keyPair.getPublic().getEncoded());
        Files.write(Paths.get(privateKeyFile), keyPair.getPrivate().getEncoded());
    }

    public static PublicKey loadPublicKey(String publicKeyFile) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(publicKeyFile));
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    public static PrivateKey loadPrivateKey(String privateKeyFile) throws Exception {
        byte[] keyBytes = Files.readAllBytes(Paths.get(privateKeyFile));
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    public static boolean keysExist(String publicKeyFile, String privateKeyFile) {
        return Files.exists(Paths.get(publicKeyFile)) && Files.exists(Paths.get(privateKeyFile));
    }
}
