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
    private static PrivateKey senderPrivateKey;   // Used to sign when sending
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

            // 1. Generate random AES symmetric key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE);
            SecretKey aesKey = keyGen.generateKey();

            // 2. Encrypt file using AES
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            byte[] iv = SecureRandom.getInstanceStrong().generateSeed(GCM_IV_LENGTH);

            Cipher aesCipher = Cipher.getInstance(AES_TRANSFORMATION);
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encryptedFile = aesCipher.doFinal(fileBytes);

            // 3. Encrypt AES key with receiver's RSA public key
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.ENCRYPT_MODE, receiverPublicKey);
            byte[] encryptedAESKey = rsaCipher.doFinal(aesKey.getEncoded());

            // 4. Generate nonce and timestamp
            String nonce = UUID.randomUUID().toString();
            String timestamp = Long.toString(System.currentTimeMillis());

            // 5. Create payload
            String payload = String.format("filename=%s;nonce=%s;timestamp=%s;iv=%s",
                    file.getName(),
                    nonce,
                    timestamp,
                    Base64.getEncoder().encodeToString(iv));

            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8); // Store as bytes
            System.out.println("=== SENDER SIDE DEBUG ===");
            System.out.println("Original File Size: " + fileBytes.length);
            System.out.println("Encrypted File Size: " + encryptedFile.length);
            System.out.println("Encrypted Key Size: " + encryptedAESKey.length);
            System.out.println("Payload: " + payload);
            System.out.println("Payload Bytes: " + Arrays.toString(payloadBytes));
            // 6. Create signature - use the payloadBytes directly
            byte[] dataToSign = concatenateByteArrays(encryptedFile, encryptedAESKey, payloadBytes);
            byte[] hash = getSHA256Hash(dataToSign);

            System.out.println("Data being signed (first 16 bytes): " +
                    Arrays.toString(Arrays.copyOf(dataToSign, 16)));
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(senderPrivateKey);
//            signature.update(hash);
            signature.update(dataToSign);
            byte[] rsaSignature = signature.sign();
            System.out.println("Generated Signature: " + Base64.getEncoder().encodeToString(rsaSignature));

            // 7. Send all components - send the payloadBytes we stored
            writeData(dos, encryptedFile);
            writeData(dos, encryptedAESKey);
            writeData(dos, rsaSignature);
            writeData(dos, payloadBytes);

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
            // 1. Read all components
            byte[] encryptedFile = readData(dis);
            byte[] encryptedAESKey = readData(dis);
            byte[] signatureBytes = readData(dis);
            byte[] payloadBytes = readData(dis);
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);

            // 2. Parse payload
            Map<String, String> payloadMap = parsePayload(payload);
            String filename = payloadMap.get("filename");
            String nonce = payloadMap.get("nonce");
            long timestamp = Long.parseLong(payloadMap.get("timestamp"));
            byte[] iv = Base64.getDecoder().decode(payloadMap.get("iv"));

            System.out.println("\n=== RECEIVER SIDE DEBUG ===");
            System.out.println("Received Encrypted File Size: " + encryptedFile.length);
            System.out.println("Received Encrypted Key Size: " + encryptedAESKey.length);
            System.out.println("Received Payload: " + payload);
            System.out.println("Received Payload Bytes: " + Arrays.toString(payloadBytes));
            // 3. Verify signature
            byte[] dataToVerify = concatenateByteArrays(encryptedFile, encryptedAESKey, payloadBytes);
            byte[] hash = getSHA256Hash(dataToVerify);
            System.out.println("Data to Verify Hash: " + Base64.getEncoder().encodeToString(hash));
            System.out.println("Received Signature: " + Base64.getEncoder().encodeToString(signatureBytes));
            System.out.println("Stored Sender Public Key: " +
                    Base64.getEncoder().encodeToString(senderPublicKey.getEncoded()));

            System.out.println("Data being verified (first 16 bytes): " +
                    Arrays.toString(Arrays.copyOf(dataToVerify, 16)));
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(senderPublicKey);
//            signature.update(hash);
            signature.update(dataToVerify);
            boolean validSignature = signature.verify(signatureBytes);

            System.out.println("signature" + signature);
            System.out.println("signatureBytes" + signatureBytes);

// Before verification
            System.out.println("Sender Public Key Format: " + senderPublicKey.getFormat());
            System.out.println("Sender Public Key Algorithm: " + senderPublicKey.getAlgorithm());
            System.out.println("Hash being verified: " + Base64.getEncoder().encodeToString(hash));
            System.out.println("Received Signature: " + Base64.getEncoder().encodeToString(signatureBytes));

// After verification
            System.out.println("Signature valid: " + validSignature);

            if (!validSignature) {
                System.err.println("Invalid Signature: File may be tampered with!");
                return;
            }

            // 4. Check for replay attacks
            if (usedNonces.contains(nonce)) {
                System.err.println("Replay attack detected: duplicate nonce");
                return;
            }
            if (System.currentTimeMillis() - timestamp > 5 * 60 * 1000) {
                System.err.println("Stale message: timestamp expired");
                return;
            }
            usedNonces.add(nonce);

            // 5. Decrypt AES key with receiver's RSA private key
            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsaCipher.init(Cipher.DECRYPT_MODE, receiverPrivateKey);
            byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAESKey);
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

            // 6. Decrypt file using AES key
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

    private static byte[] concatenateByteArrays(byte[]... arrays) throws IOException {
        int totalLength = 0;
        for (byte[] array : arrays) {
            totalLength += array.length;
        }

        byte[] result = new byte[totalLength];
        int currentPos = 0;

        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, currentPos, array.length);
            currentPos += array.length;
        }

        return result;
    }

    private static byte[] getSHA256Hash(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }

    private static void writeData(DataOutputStream dos, byte[] data) throws IOException {
        dos.writeInt(data.length);
        dos.write(data);
    }

    private static byte[] readData(DataInputStream dis) throws IOException {
        byte[] data = new byte[dis.readInt()];
        dis.readFully(data);
        return data;
    }
}