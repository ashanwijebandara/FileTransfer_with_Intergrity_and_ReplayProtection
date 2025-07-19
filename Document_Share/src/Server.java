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

    // Key file paths
    private static final String BOB_PRIVATE_KEY = "bob_private.key";
    private static final String BOB_PUBLIC_KEY = "bob_public.key";
    private static final String ALICE_PUBLIC_KEY = "alice_public.key";

    public static void main(String[] args) {
        new File("ServerFiles/").mkdirs();

        try {
            // Load or generate Bob's key pair
            if (!KeyLoader.keysExist(BOB_PUBLIC_KEY, BOB_PRIVATE_KEY)) {
                KeyPair keyPair = RSAUtils.generateKeyPair();
                KeyLoader.saveKeys(keyPair, BOB_PUBLIC_KEY, BOB_PRIVATE_KEY);
                System.out.println("Generated new RSA key pair for Bob.");
            }

            // Set Bob's private key (for decrypting AES key)
            PrivateKey bobPrivateKey = KeyLoader.loadPrivateKey(BOB_PRIVATE_KEY);
            FileTransferHandler.setPrivateKey(bobPrivateKey);
            System.out.println("Loaded Bob's private key.");

            // Load Alice's public key (for verifying signature)
            if (new File(ALICE_PUBLIC_KEY).exists()) {
                PublicKey alicePublicKey = KeyLoader.loadPublicKey(ALICE_PUBLIC_KEY);
                FileTransferHandler.setPublicKey(alicePublicKey);
                System.out.println("Loaded Alice's public key.");
            } else {
                System.err.println("Alice's public key not found. Cannot verify signature.");
                return;
            }

        } catch (Exception e) {
            System.err.println("Key loading error: " + e.getMessage());
            return;
        }

        // GUI Setup
        JFrame jFrame = new JFrame("Bob");
        jFrame.setSize(650, 450);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setLayout(new BorderLayout(10, 10));
        jFrame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);

        JLabel jlTitle = new JLabel("Bob File Receiver/Sender");
        jlTitle.setFont(new Font("Arial", Font.BOLD, 28));
        jlTitle.setBorder(new EmptyBorder(30, 0, 10, 0));
        jlTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel jlFileName = new JLabel("Choose a file to send to Alice");
        jlFileName.setFont(new Font("Arial", Font.BOLD, 20));
        jlFileName.setBorder(new EmptyBorder(50, 0, 0, 0));
        jlFileName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton jbChooseFile = new JButton("Choose File");
        jbChooseFile.setFont(new Font("Arial", Font.BOLD, 20));
        jbChooseFile.setBackground(new Color(173, 216, 230));
        jbChooseFile.setForeground(Color.WHITE);
        jbChooseFile.setBorder(BorderFactory.createLineBorder(new Color(135, 206, 250), 2));

        JButton jbSendBack = new JButton("Send File");
        jbSendBack.setFont(new Font("Arial", Font.BOLD, 20));
        jbSendBack.setBackground(new Color(60, 179, 113));
        jbSendBack.setForeground(Color.WHITE);
        jbSendBack.setBorder(BorderFactory.createLineBorder(new Color(46, 139, 87), 2));

        mainPanel.add(jlTitle);
        mainPanel.add(jlFileName);
        mainPanel.add(jbSendBack);
        jFrame.add(mainPanel, BorderLayout.CENTER);
        jFrame.setVisible(true);

        File[] lastReceivedFile = new File[1];

        // Start receiving thread
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
                System.out.println("Server listening on port " + SERVER_PORT);
                while (true) {
                    Socket socket = serverSocket.accept();
                    FileTransferHandler.receiveFile(socket, "ServerFiles/");
                    jlFileName.setText("Received file successfully");
                    jlFileName.setForeground(new Color(34, 139, 34));
                    System.out.println("File received by Bob.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();


        // Send response file back to Alice
        jbSendBack.addActionListener(e -> {
            if (lastReceivedFile[0] == null || !lastReceivedFile[0].exists()) {
                JOptionPane.showMessageDialog(null, "No file to send.");
                return;
            }
            try {
                FileTransferHandler.sendFile(lastReceivedFile[0], "localhost", CLIENT_RECEIVE_PORT);
                JOptionPane.showMessageDialog(null, "Sent file back to Alice securely!");
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error sending file: " + ex.getMessage());
            }
        });
    }
}
