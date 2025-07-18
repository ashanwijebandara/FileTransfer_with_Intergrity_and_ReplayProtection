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

public class Client {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1234;
    private static final int CLIENT_RECEIVE_PORT = 5678;

    private static final String ALICE_PRIVATE_KEY = "alice_private.key";
    private static final String ALICE_PUBLIC_KEY = "alice_public.key"; // Optional if you want to send it to Bob
    private static final String BOB_PUBLIC_KEY = "bob_public.key";

    public static void main(String[] args) {
        // Create directory for received files
        new File("ClientFiles/").mkdirs();

        try {
            // Generate or load RSA key pair
            if (!KeyLoader.keysExist(ALICE_PUBLIC_KEY, ALICE_PRIVATE_KEY)) {
                KeyPair keyPair = RSAUtils.generateKeyPair();
                KeyLoader.saveKeys(keyPair, ALICE_PUBLIC_KEY, ALICE_PRIVATE_KEY);
                System.out.println("Generated and saved new RSA key pair.");
            }

            // Load keys
            PrivateKey privateKey = KeyLoader.loadPrivateKey(ALICE_PRIVATE_KEY);
            PublicKey publicKey = KeyLoader.loadPublicKey(ALICE_PUBLIC_KEY);

            // Set keys in FileTransferHandler
            FileTransferHandler.setSenderPrivateKey(privateKey);    // For signing outgoing files
            FileTransferHandler.setReceiverPrivateKey(privateKey); // For decrypting received files
            FileTransferHandler.setSenderPublicKey(publicKey);     // For signature verification

            System.out.println("Loaded keys successfully.");

            // Load partner's public key
            if (new File(BOB_PUBLIC_KEY).exists()) {
                PublicKey partnerPublicKey = KeyLoader.loadPublicKey(BOB_PUBLIC_KEY);
                FileTransferHandler.setReceiverPublicKey(partnerPublicKey);  // For encrypting files to partner
                System.out.println("Loaded Bob's public key.");
            } else {
                System.err.println("Bob's public key not found!");
            }
        } catch (Exception e) {
            System.err.println("Failed to load or generate keys: " + e.getMessage());
            return;
        }

        // GUI Setup
        JFrame jFrame = new JFrame("Secure File Transfer");
        jFrame.setSize(650, 450);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setLayout(new BorderLayout(10, 10));
        jFrame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);

        JLabel jlTitle = new JLabel("Secure File Transfer Client");
        jlTitle.setFont(new Font("Arial", Font.BOLD, 28));
        jlTitle.setBorder(new EmptyBorder(30, 0, 10, 0));
        jlTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel jlFileName = new JLabel("Choose a file to send");
        jlFileName.setFont(new Font("Arial", Font.BOLD, 20));
        jlFileName.setBorder(new EmptyBorder(50, 0, 0, 0));
        jlFileName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton jbChooseFile = new JButton("Choose File");
        jbChooseFile.setFont(new Font("Arial", Font.BOLD, 20));
        jbChooseFile.setBackground(new Color(173, 216, 230));
        jbChooseFile.setForeground(Color.WHITE);
        jbChooseFile.setBorder(BorderFactory.createLineBorder(new Color(135, 206, 250), 2));

        JButton jbSendFile = new JButton("Send File");
        jbSendFile.setFont(new Font("Arial", Font.BOLD, 20));
        jbSendFile.setBackground(new Color(220, 20, 60));
        jbSendFile.setForeground(Color.WHITE);
        jbSendFile.setBorder(BorderFactory.createLineBorder(new Color(220, 20, 60), 2));

        mainPanel.add(jlTitle);
        mainPanel.add(jlFileName);
        mainPanel.add(jbChooseFile);
        mainPanel.add(jbSendFile);
        jFrame.add(mainPanel, BorderLayout.CENTER);

        File[] fileToSend = new File[1];

        jbChooseFile.addActionListener(e -> {
            JFileChooser jFileChooser = new JFileChooser();
            jFileChooser.setDialogTitle("Choose a file to send");
            if (jFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                fileToSend[0] = jFileChooser.getSelectedFile();
                jlFileName.setText("Selected file: " + fileToSend[0].getName());
                jlFileName.setForeground(Color.BLACK);
            }
        });

        jbSendFile.addActionListener(e -> {
            if (fileToSend[0] == null) {
                jlFileName.setText("Please choose a file");
                jlFileName.setForeground(Color.RED);
            } else {
                try {
                    FileTransferHandler.sendFile(fileToSend[0], SERVER_ADDRESS, SERVER_PORT);
                    System.out.println("File sent: " + fileToSend[0].getName());
                    JOptionPane.showMessageDialog(null, "File sent successfully!");
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error sending file: " + ex.getMessage());
                }
            }
        });

        jFrame.setVisible(true);

        // Start receiver thread
        new Thread(Client::startReceiver).start();
    }

    private static void startReceiver() {
        try (ServerSocket serverSocket = new ServerSocket(CLIENT_RECEIVE_PORT)) {
            System.out.println("Ready to receive files on port " + CLIENT_RECEIVE_PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                FileTransferHandler.receiveFile(socket, "ClientFiles/");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}