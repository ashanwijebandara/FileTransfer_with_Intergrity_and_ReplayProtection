import utils.KeyLoader;
import utils.RSAUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Server {

    private static final int SERVER_PORT = 1234;
    private static final int CLIENT_RECEIVE_PORT = 5678;
    private static final String CLIENT_IP = "localhost";

    // Key file paths
    private static final String BOB_PRIVATE_KEY = "bob_private.key";
    private static final String BOB_PUBLIC_KEY = "bob_public.key"; // optional, for sharing with Alice
    private static final String ALICE_PUBLIC_KEY = "alice_public.key";

    private JFrame jFrame;
    private JLabel jlStatus;
    private File[] fileToSend = new File[1];

    public static void main(String[] args) {
        new Server().startServer();
    }

    public void startServer() {
        // Create directory for received files
        new File("ServerFiles/").mkdirs();

        // Key Initialization
        try {
            // Generate or load RSA key pair if not exists
            if (!KeyLoader.keysExist(BOB_PUBLIC_KEY, BOB_PRIVATE_KEY)) {
                KeyPair keyPair = RSAUtils.generateKeyPair();
                KeyLoader.saveKeys(keyPair, BOB_PUBLIC_KEY, BOB_PRIVATE_KEY);
                System.out.println("Generated and saved new RSA key pair for server.");
            }

            // Load server's keys
            PrivateKey privateKey = KeyLoader.loadPrivateKey(BOB_PRIVATE_KEY);
            PublicKey publicKey = KeyLoader.loadPublicKey(BOB_PUBLIC_KEY);

            // Set keys in FileTransferHandler
            FileTransferHandler.setSenderPrivateKey(privateKey);    // For signing outgoing files
            FileTransferHandler.setReceiverPrivateKey(privateKey); // For decrypting received files
            FileTransferHandler.setSenderPublicKey(publicKey);    // For signature verification

            System.out.println("Loaded server keys successfully.");

            // Load client's public key
            if (new File(ALICE_PUBLIC_KEY).exists()) {
                PublicKey clientPublicKey = KeyLoader.loadPublicKey(ALICE_PUBLIC_KEY);
                FileTransferHandler.setReceiverPublicKey(clientPublicKey); // For encrypting files to client
                System.out.println("Loaded client's public key.");
            } else {
                System.err.println("Client's public key not found!");
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize keys: " + e.getMessage());
            return;
        }

        // GUI Setup
        initializeGUI();

        // Start receiver thread
        new Thread(this::startReceiver).start();
    }

    private void initializeGUI() {
        jFrame = new JFrame("Secure File Transfer Server");
        jFrame.setSize(700, 500);
        jFrame.setLayout(new BorderLayout(10, 10));
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);

        JLabel jlTitle = new JLabel("Secure File Transfer Server");
        jlTitle.setFont(new Font("Arial", Font.BOLD, 28));
        jlTitle.setBorder(new EmptyBorder(30, 0, 10, 0));
        jlTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        jlStatus = new JLabel("Status: Ready");
        jlStatus.setFont(new Font("Arial", Font.PLAIN, 16));
        jlStatus.setBorder(new EmptyBorder(20, 0, 10, 0));
        jlStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton jbChooseFile = new JButton("Choose File to Send");
        styleButton(jbChooseFile, new Color(70, 130, 180));

        JButton jbSendFile = new JButton("Send File");
        styleButton(jbSendFile, new Color(34, 139, 34));

        mainPanel.add(jlTitle);
        mainPanel.add(jlStatus);
        mainPanel.add(jbChooseFile);
        mainPanel.add(jbSendFile);

        jFrame.add(mainPanel, BorderLayout.CENTER);

        // Button actions
        jbChooseFile.addActionListener(e -> chooseFile());
        jbSendFile.addActionListener(e -> sendFile());

        jFrame.setVisible(true);
    }

    private void styleButton(JButton button, Color color) {
        button.setFont(new Font("Arial", Font.BOLD, 18));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select File to Send");
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            fileToSend[0] = chooser.getSelectedFile();
            jlStatus.setText("Selected: " + fileToSend[0].getName());
            jlStatus.setForeground(Color.BLUE);
        }
    }

    private void sendFile() {
        if (fileToSend[0] == null) {
            jlStatus.setText("Please select a file first");
            jlStatus.setForeground(Color.RED);
            return;
        }

        try {
            FileTransferHandler.sendFile(fileToSend[0], CLIENT_IP, CLIENT_RECEIVE_PORT);
            jlStatus.setText("File sent successfully: " + fileToSend[0].getName());
            jlStatus.setForeground(new Color(0, 100, 0));
            JOptionPane.showMessageDialog(jFrame, "File sent successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            jlStatus.setText("Failed to send file");
            jlStatus.setForeground(Color.RED);
            JOptionPane.showMessageDialog(jFrame, "Error sending file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void startReceiver() {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Server started on port " + SERVER_PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New connection from: " + socket.getInetAddress());

                // Handle each connection in a new thread for better scalability
                new Thread(() -> {
                    try {
                        FileTransferHandler.receiveFile(socket, "ServerFiles/");
                    } catch (Exception e) {
                        System.err.println("Error handling file transfer: " + e.getMessage());
                    }
                }).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}