import java.io.Serializable;
import java.util.Arrays;

public class SecureFilePayload implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fileName;
    private byte[] encryptedFile;       // AES-encrypted file content
    private byte[] encryptedAESKey;     // RSA-encrypted AES key
    private long timestamp;
    private String nonce;
    private byte[] signature;           // Signature of {filename, encryptedFile, encryptedAESKey, nonce, timestamp}

    public SecureFilePayload(String fileName, byte[] encryptedFile, byte[] encryptedAESKey,
                             long timestamp, String nonce, byte[] signature) {
        this.fileName = fileName;
        this.encryptedFile = encryptedFile;
        this.encryptedAESKey = encryptedAESKey;
        this.timestamp = timestamp;
        this.nonce = nonce;
        this.signature = signature;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getEncryptedFile() {
        return encryptedFile;
    }

    public byte[] getEncryptedAESKey() {
        return encryptedAESKey;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getNonce() {
        return nonce;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setEncryptedFile(byte[] encryptedFile) {
        this.encryptedFile = encryptedFile;
    }

    public void setEncryptedAESKey(byte[] encryptedAESKey) {
        this.encryptedAESKey = encryptedAESKey;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        return "SecureFilePayload{" +
                "fileName='" + fileName + '\'' +
                ", encryptedFile.length=" + (encryptedFile != null ? encryptedFile.length : 0) +
                ", encryptedAESKey.length=" + (encryptedAESKey != null ? encryptedAESKey.length : 0) +
                ", timestamp=" + timestamp +
                ", nonce='" + nonce + '\'' +
                ", signature=" + Arrays.toString(signature) +
                '}';
    }
}
