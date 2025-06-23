package utils;
import java.io.*;


public class FileUtils {

    public static byte[] readFile(String filePath) throws IOException {
        File file = new File(filePath);
        byte[] fileContent = new byte[(int) file.length()];
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            fileInputStream.read(fileContent);
        }
        return fileContent;
    }

    public static void writeFile(String filePath, byte[] data) throws IOException {
        File file = new File(filePath);

        // Ensure parent directory exists
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();  // Create directories if they don’t exist
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(data);
        }
    }

}