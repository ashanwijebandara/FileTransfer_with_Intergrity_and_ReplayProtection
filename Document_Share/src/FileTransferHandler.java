import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.util.*;
import java.util.Base64;

public class FileTransferHandler {

    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE = 128;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private static PublicKey receiverPublicKey;   // Used to encrypt AES key when sending
    private static PrivateKey senderPrivateKey;  // Used to sign when sending
    private static PublicKey senderPublicKey;    // Used to verify when receiving
    private static PrivateKey receiverPrivateKey;// Used to decrypt AES key when receiving

    private static final Set<String> usedNonces = new HashSet<>();

    public static void setReceiverPublicKey(PublicKey key) {
        receiverPublicKey = key;
    }

    public static void setSenderPrivateKey(PrivateKey key) {
        senderPrivateKey = key;
    }

    public static void setSenderPublicKey(PublicKey key) {
        senderPublicKey = key;
    }

    public static void setReceiverPrivateKey(PrivateKey key) {
        receiverPrivateKey = key;
    }

    public static void sendFile(File file, String ip, int port) throws IOException {
        if (receiverPublicKey == null || senderPrivateKey == null) {
            throw new IllegalStateException("Required keys not set for sending");
        }

        try (Socket socket = new Socket(ip, port);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            // Generate AES key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE);
            SecretKey aesKey = keyGen.generateKey();

            // Encrypt file with AES
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            byte[] iv = SecureRandom.getInstanceStrong().generateSeed(GCM_IV_LENGTH);

            Cipher aesCipher = Cipher.getInstance(AES_TRANSFORMATION);
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encryptedFile = aesCipher.doFinal(fileBytes);

            // Encrypt AES key with RSA
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.ENCRYPT_MODE, receiverPublicKey);
            byte[] encryptedAESKey = rsaCipher.doFinal(aesKey.getEncoded());

            // Create payload with metadata
            String nonce = UUID.randomUUID().toString();
            String timestamp = Long.toString(System.currentTimeMillis());
            String payload = "filename=" + file.getName() + ";nonce=" + nonce + ";timestamp=" + timestamp + ";iv=" + Base64.getEncoder().encodeToString(iv);

            // Create signature
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(encryptedFile);
            digest.update(encryptedAESKey);
            digest.update(payload.getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest();

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(senderPrivateKey);
            signature.update(hash);
            byte[] rsaSignature = signature.sign();

            // Send all data
            dos.writeInt(encryptedFile.length);
            dos.write(encryptedFile);
            dos.writeInt(encryptedAESKey.length);
            dos.write(encryptedAESKey);
            dos.writeInt(payload.getBytes().length);
            dos.write(payload.getBytes());
            dos.writeInt(rsaSignature.length);
            dos.write(rsaSignature);

        } catch (Exception e) {
            throw new IOException("Failed to send file", e);
        }
    }

    public static void receiveFile(Socket socket, String savePath) {
        if (receiverPrivateKey == null || senderPublicKey == null) {
            System.err.println("Required keys not set for receiving");
            return;
        }

        try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            // Read all components
            byte[] encryptedFile = new byte[dis.readInt()];
            dis.readFully(encryptedFile);

            byte[] encryptedAESKey = new byte[dis.readInt()];
            dis.readFully(encryptedAESKey);

            byte[] payloadBytes = new byte[dis.readInt()];
            dis.readFully(payloadBytes);
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);

            byte[] signatureBytes = new byte[dis.readInt()];
            dis.readFully(signatureBytes);

            // Parse payload
            Map<String, String> payloadMap = parsePayload(payload);
            String filename = payloadMap.get("filename");
            String nonce = payloadMap.get("nonce");
            long timestamp = Long.parseLong(payloadMap.get("timestamp"));
            byte[] iv = Base64.getDecoder().decode(payloadMap.get("iv"));

            // Verify signature
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(encryptedFile);
            digest.update(encryptedAESKey);
            digest.update(payload.getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest();

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(senderPublicKey);
            signature.update(hash);
            boolean validSignature = signature.verify(signatureBytes);

            if (!validSignature) {
                System.err.println("Invalid Signature: File may be tampered with!");
                return;
            }

            // Check for replay attacks
            if (usedNonces.contains(nonce)) {
                System.err.println("Replay attack detected: duplicate nonce");
                return;
            }
            if (System.currentTimeMillis() - timestamp > 5 * 60 * 1000) {
                System.err.println("Stale message: timestamp expired");
                return;
            }
            usedNonces.add(nonce);

            // Decrypt AES key
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, receiverPrivateKey);
            byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAESKey);
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

            // Decrypt file
            Cipher aesCipher = Cipher.getInstance(AES_TRANSFORMATION);
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] decryptedFile = aesCipher.doFinal(encryptedFile);

            // Save file
            File outputFile = new File(savePath + filename);
            Files.write(outputFile.toPath(), decryptedFile);
            System.out.println("File received and saved as: " + outputFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> parsePayload(String payload) {
        Map<String, String> map = new HashMap<>();
        String[] parts = payload.split(";");
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }
}