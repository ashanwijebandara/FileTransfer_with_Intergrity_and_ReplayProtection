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

    // Key file paths
    private static final String ALICE_PRIVATE_KEY = "alice_private.key";
    private static final String ALICE_PUBLIC_KEY = "alice_public.key"; // optional
    private static final String BOB_PUBLIC_KEY = "bob_public.key";

    public static void main(String[] args) {
        new File("ClientFiles/").mkdirs();

        try {
            // Load or generate Alice's key pair
            if (!KeyLoader.keysExist(ALICE_PUBLIC_KEY, ALICE_PRIVATE_KEY)) {
                KeyPair keyPair = RSAUtils.generateKeyPair();
                KeyLoader.saveKeys(keyPair, ALICE_PUBLIC_KEY, ALICE_PRIVATE_KEY);
                System.out.println("Generated new RSA key pair for Alice.");
            }

            // Set Alice's private key (used for signing)
            PrivateKey alicePrivateKey = KeyLoader.loadPrivateKey(ALICE_PRIVATE_KEY);
            FileTransferHandler.setPrivateKey(alicePrivateKey);
            System.out.println("Loaded Alice's private key.");

            // Set Bob's public key (used for encrypting AES key)
            if (new File(BOB_PUBLIC_KEY).exists()) {
                PublicKey bobPublicKey = KeyLoader.loadPublicKey(BOB_PUBLIC_KEY);
                FileTransferHandler.setPublicKey(bobPublicKey);
                System.out.println("Loaded Bob's public key.");
            } else {
                System.err.println("Bob's public key not found. Cannot encrypt AES key.");
                return;
            }

        } catch (Exception e) {
            System.err.println("Key loading error: " + e.getMessage());
            return;
        }

        // GUI Setup
        JFrame jFrame = new JFrame("Alice");
        jFrame.setSize(650, 500);  // Increased height for extra button
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setLayout(new BorderLayout(10, 10));
        jFrame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);

        JLabel jlTitle = new JLabel("Alice File Sender/Receiver");
        jlTitle.setFont(new Font("Arial", Font.BOLD, 28));
        jlTitle.setBorder(new EmptyBorder(30, 0, 10, 0));
        jlTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel jlFileName = new JLabel("Choose a file to send to Bob");
        jlFileName.setFont(new Font("Arial", Font.BOLD, 20));
        jlFileName.setBorder(new EmptyBorder(30, 0, 10, 0));
        jlFileName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton jbChooseFile = new JButton("Choose File");
        jbChooseFile.setFont(new Font("Arial", Font.BOLD, 20));
        jbChooseFile.setBackground(new Color(173, 216, 230));
        jbChooseFile.setForeground(Color.WHITE);
        jbChooseFile.setBorder(BorderFactory.createLineBorder(new Color(135, 206, 250), 2));
        jbChooseFile.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton jbSendFile = new JButton("Send File");
        jbSendFile.setFont(new Font("Arial", Font.BOLD, 20));
        jbSendFile.setBackground(new Color(220, 20, 60));
        jbSendFile.setForeground(Color.WHITE);
        jbSendFile.setBorder(BorderFactory.createLineBorder(new Color(220, 20, 60), 2));
        jbSendFile.setAlignmentX(Component.CENTER_ALIGNMENT);

//        JButton jbSendBack = new JButton("Send Last Received File to Bob");
//        jbSendBack.setFont(new Font("Arial", Font.BOLD, 20));
//        jbSendBack.setBackground(new Color(60, 179, 113));
//        jbSendBack.setForeground(Color.WHITE);
//        jbSendBack.setBorder(BorderFactory.createLineBorder(new Color(46, 139, 87), 2));
//        jbSendBack.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(jlTitle);
        mainPanel.add(jlFileName);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(jbChooseFile);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(jbSendFile);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 30)));
//        mainPanel.add(jbSendBack);
        jFrame.add(mainPanel, BorderLayout.CENTER);

        File[] fileToSend = new File[1];
        File[] lastReceivedFile = new File[1];

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
                    System.out.println("Alice sent = " + fileToSend[0].getName());
                    JOptionPane.showMessageDialog(null, "File sent to server securely!");
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error sending file: " + ex.getMessage());
                }
            }
        });

//        jbSendBack.addActionListener(e -> {
//            if (lastReceivedFile[0] == null || !lastReceivedFile[0].exists()) {
//                JOptionPane.showMessageDialog(null, "No received file to send.");
//                return;
//            }
//            try {
//                FileTransferHandler.sendFile(lastReceivedFile[0], SERVER_ADDRESS, SERVER_PORT);
//                JOptionPane.showMessageDialog(null, "Sent last received file back to Bob securely!");
//            } catch (IOException ex) {
//                ex.printStackTrace();
//                JOptionPane.showMessageDialog(null, "Error sending file: " + ex.getMessage());
//            }
//        });

        jFrame.setVisible(true);

        // Start thread to receive responses from server
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(CLIENT_RECEIVE_PORT)) {
                System.out.println("Client ready to receive files on port " + CLIENT_RECEIVE_PORT);
                while (true) {
                    Socket socket = serverSocket.accept();
                    File receivedFile = FileTransferHandler.receiveFile(socket, "ClientFiles/");
                    if (receivedFile != null) {
                        lastReceivedFile[0] = receivedFile;
                        jlFileName.setText("Received file: " + receivedFile.getName());
                        jlFileName.setForeground(new Color(34, 139, 34));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
