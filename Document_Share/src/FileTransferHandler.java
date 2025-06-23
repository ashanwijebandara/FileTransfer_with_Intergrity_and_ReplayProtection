import utils.FileUtils;

import java.io.*;
import java.net.Socket;

public class FileTransferHandler {
    public static void sendFile(File file, String host, int port) throws IOException {
        try (Socket socket = new Socket(host, port);
             DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
             FileInputStream fileInputStream = new FileInputStream(file)) {

            byte[] fileNameBytes = file.getName().getBytes();
            byte[] fileContentBytes = new byte[(int) file.length()];
            fileInputStream.read(fileContentBytes);

            dataOutputStream.writeInt(fileNameBytes.length);
            dataOutputStream.write(fileNameBytes);
            dataOutputStream.writeInt(fileContentBytes.length);
            dataOutputStream.write(fileContentBytes);
        }
    }

    public static void receiveFile(Socket socket, String saveDir) throws IOException {
        try (DataInputStream dataInputStream = new DataInputStream(socket.getInputStream())) {
            int fileNameLength = dataInputStream.readInt();
            byte[] fileNameBytes = new byte[fileNameLength];
            dataInputStream.readFully(fileNameBytes);
            String fileName = new String(fileNameBytes);

            int fileContentLength = dataInputStream.readInt();
            byte[] fileContentBytes = new byte[fileContentLength];
            dataInputStream.readFully(fileContentBytes);

            File outputFile = new File(saveDir + fileName);
            FileUtils.writeFile(outputFile.getAbsolutePath(), fileContentBytes);
            System.out.println("File received and saved: " + outputFile.getAbsolutePath());
        }
    }
}
