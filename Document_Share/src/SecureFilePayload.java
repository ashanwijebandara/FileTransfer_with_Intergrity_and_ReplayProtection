import java.io.Serializable;
import java.util.Arrays;

public class SecureFilePayload implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fileName;
    private byte[] fileContent;
    private long timestamp;
    private String nonce;
    private byte[] signature;

    public SecureFilePayload(String fileName, byte[] fileContent, long timestamp, String nonce, byte[] signature) {
        this.fileName = fileName;
        this.fileContent = fileContent;
        this.timestamp = timestamp;
        this.nonce = nonce;
        this.signature = signature;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getFileContent() {
        return fileContent;
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

    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
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
                ", fileContent.length=" + (fileContent != null ? fileContent.length : 0) +
                ", timestamp=" + timestamp +
                ", nonce='" + nonce + '\'' +
                ", signature=" + Arrays.toString(signature) +
                '}';
    }
}
