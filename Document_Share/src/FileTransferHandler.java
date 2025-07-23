import utils.AESUtils;
import utils.FileUtils;
import utils.RSAUtils;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

public class FileTransferHandler {

    private static PrivateKey PRIVATE_KEY; // Receiver's private key
    private static PublicKey PUBLIC_KEY;   // Sender's public key

    private static final Set<String> usedNonces = new HashSet<>();
    private static final long ALLOWED_TIME_WINDOW_MS = 5 * 60 * 1000; // 5 minutes

    public static void setPrivateKey(PrivateKey key) {
        PRIVATE_KEY = key;
    }

    public static void setPublicKey(PublicKey key) {
        PUBLIC_KEY = key;
    }

    private static String generateNonce() {
        byte[] nonceBytes = new byte[16];
        new SecureRandom().nextBytes(nonceBytes);
        return Base64.getEncoder().encodeToString(nonceBytes);
    }

    private static byte[] serializeObject(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }

    private static SecureFilePayload deserializePayload(byte[] data) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (SecureFilePayload) ois.readObject();
        }
    }

    // =================== SENDER ===================
    public static void sendFile(File file, String host, int port) throws IOException {
        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            byte[] fileBytes = FileUtils.readFile(file.getAbsolutePath());

            // 1. Generate AES key and encrypt file
            Key aesKey = AESUtils.generateKey();
            byte[] encryptedFile = AESUtils.encrypt(fileBytes, (SecretKey) aesKey);

            // 2. Encrypt AES key with receiver's RSA public key
            byte[] encryptedAESKey = RSAUtils.encrypt(aesKey.getEncoded(), PUBLIC_KEY);

            // 3. Generate nonce and timestamp
            String nonce = generateNonce();
            long timestamp = System.currentTimeMillis();

            // 4. Create unsigned payload
            SecureFilePayload unsignedPayload = new SecureFilePayload(
                    file.getName(), encryptedFile, encryptedAESKey, timestamp, nonce, null
            );

            // 5. Sign the serialized payload (excluding signature)
            byte[] dataToSign = serializeObject(unsignedPayload);
            byte[] signature = RSAUtils.sign(dataToSign, PRIVATE_KEY);
            unsignedPayload.setSignature(signature);

            // 6. Serialize and send
            byte[] finalPayload = serializeObject(unsignedPayload);
            out.writeInt(finalPayload.length);
            out.write(finalPayload);

            System.out.println("✅ File sent securely.");

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Failed to send file: " + e.getMessage());
        }
    }

    // =================== RECEIVER ===================
    public static File receiveFile(Socket socket, String saveDir) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream())) {

            int length = in.readInt();
            if (length <= 0) return null;

            byte[] receivedPayload = new byte[length];
            in.readFully(receivedPayload);

            // 1. Deserialize payload
            SecureFilePayload payload = deserializePayload(receivedPayload);

            // 2. Check timestamp freshness
            long now = System.currentTimeMillis();
            if (Math.abs(now - payload.getTimestamp()) > ALLOWED_TIME_WINDOW_MS) {
                System.err.println("❌ Rejected: Timestamp out of range.");
                return null;
            }

            // 3. Check for replay attack using nonce
            if (usedNonces.contains(payload.getNonce())) {
                System.err.println("❌ Rejected: Replay detected (nonce reused).");
                return null;
            }

            // 4. Verify digital signature
            SecureFilePayload tempPayload = new SecureFilePayload(
                    payload.getFileName(),
                    payload.getEncryptedFile(),
                    payload.getEncryptedAESKey(),
                    payload.getTimestamp(),
                    payload.getNonce(),
                    null
            );

            byte[] dataToVerify = serializeObject(tempPayload);
            boolean isVerified = RSAUtils.verify(dataToVerify, payload.getSignature(), PUBLIC_KEY);
            if (!isVerified) {
                System.err.println("❌ Rejected: Invalid RSA signature.");
                return null;
            }

            // 5. Decrypt AES key using receiver's private key
            byte[] aesKeyBytes = RSAUtils.decrypt(payload.getEncryptedAESKey(), PRIVATE_KEY);
            Key aesKey = AESUtils.getKeyFromBytes(aesKeyBytes);

            // 6. Decrypt file content
            byte[] decryptedFile = AESUtils.decrypt(payload.getEncryptedFile(), (SecretKey) aesKey);

            // 7. Save file
            usedNonces.add(payload.getNonce());
            File outputFile = new File(saveDir, payload.getFileName());
            FileUtils.writeFile(outputFile.getAbsolutePath(), decryptedFile);

            System.out.println("✅ File received and saved: " + outputFile.getAbsolutePath());
            return outputFile;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error receiving file: " + e.getMessage());
            return null;
        }
    }
}
