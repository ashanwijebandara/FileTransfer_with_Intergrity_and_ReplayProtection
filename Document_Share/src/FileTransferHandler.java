import utils.AESUtils;
import utils.FileUtils;
import utils.RSAUtils;

import java.io.*;
import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

public class FileTransferHandler {

    // AES key (symmetric, shared securely beforehand)
    private static final byte[] AES_KEY_BYTES = "1234567890123456".getBytes(); // 128-bit AES

    // RSA key holders (set externally)
    private static PrivateKey PRIVATE_KEY;
    private static PublicKey PUBLIC_KEY;

    // Replay protection
    private static final Set<String> usedNonces = new HashSet<>();
    private static final long ALLOWED_TIME_WINDOW_MS = 5 * 60 * 1000; // 5 minutes

    // 🔐 Public setter methods (called from Client/Server)
    public static void setPrivateKey(PrivateKey key) {
        PRIVATE_KEY = key;
    }

    public static void setPublicKey(PublicKey key) {
        PUBLIC_KEY = key;
    }

    // Generate random nonce
    private static String generateNonce() {
        byte[] nonceBytes = new byte[16];
        new SecureRandom().nextBytes(nonceBytes);
        return Base64.getEncoder().encodeToString(nonceBytes);
    }

    // Serialize Java object to byte array
    private static byte[] serializeObject(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }

    // Deserialize byte array to SecureFilePayload
    private static SecureFilePayload deserializePayload(byte[] data) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (SecureFilePayload) ois.readObject();
        }
    }

    // ✅ Sender (Alice or Bob)
    public static void sendFile(File file, String host, int port) throws IOException {
        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            byte[] fileContent = FileUtils.readFile(file.getAbsolutePath());
            long timestamp = System.currentTimeMillis();
            String nonce = generateNonce();

            // Step 1: Create payload
            SecureFilePayload payload = new SecureFilePayload(
                    file.getName(), fileContent, timestamp, nonce, null
            );

            // Step 2: Sign with RSA private key (excluding signature itself)
            byte[] dataToSign = serializeObject(new SecureFilePayload(
                    payload.getFileName(),
                    payload.getFileContent(),
                    payload.getTimestamp(),
                    payload.getNonce(),
                    null
            ));

            byte[] signature = RSAUtils.sign(dataToSign, PRIVATE_KEY);
            payload.setSignature(signature);

            // Step 3: Encrypt full payload using AES
            byte[] serializedPayload = serializeObject(payload);
            byte[] encryptedPayload = AESUtils.encrypt(serializedPayload, AESUtils.getKeyFromBytes(AES_KEY_BYTES));

            // Step 4: Send over socket
            out.writeInt(encryptedPayload.length);
            out.write(encryptedPayload);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Failed to send file: " + e.getMessage());
        }
    }

    // ✅ Receiver (Alice or Bob)
    public static void receiveFile(Socket socket, String saveDir) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream())) {

            int length = in.readInt();
            if (length <= 0) return;

            byte[] encryptedPayload = new byte[length];
            in.readFully(encryptedPayload);

            // Step 1: Decrypt using AES
            byte[] decryptedPayload = AESUtils.decrypt(encryptedPayload, AESUtils.getKeyFromBytes(AES_KEY_BYTES));
            SecureFilePayload payload = deserializePayload(decryptedPayload);

            // Step 2: Replay attack protection - timestamp check
            long currentTime = System.currentTimeMillis();
            if (Math.abs(currentTime - payload.getTimestamp()) > ALLOWED_TIME_WINDOW_MS) {
                System.err.println("Rejected: Timestamp out of range.");
                return;
            }

            // Step 3: Replay attack protection - nonce uniqueness
            if (usedNonces.contains(payload.getNonce())) {
                System.err.println("Rejected: Replay detected (nonce reused).");
                return;
            }

            // Step 4: RSA Signature verification
            byte[] dataToVerify = serializeObject(new SecureFilePayload(
                    payload.getFileName(),
                    payload.getFileContent(),
                    payload.getTimestamp(),
                    payload.getNonce(),
                    null
            ));

            boolean isValid = RSAUtils.verify(dataToVerify, payload.getSignature(), PUBLIC_KEY);
            if (!isValid) {
                System.err.println("Rejected: RSA signature invalid.");
                return;
            }

            // Step 5: Store file
            usedNonces.add(payload.getNonce());
            File outputFile = new File(saveDir + payload.getFileName());
            FileUtils.writeFile(outputFile.getAbsolutePath(), payload.getFileContent());

            System.out.println("File received and saved: " + outputFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error receiving file: " + e.getMessage());
        }
    }
}
