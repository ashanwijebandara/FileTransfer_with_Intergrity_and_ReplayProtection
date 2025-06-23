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
        // Ensure ServerFiles directory exists
        new File("ServerFiles/").mkdirs();

        // 🔐 Load or generate Bob's private key
        try {
            if (!KeyLoader.keysExist(BOB_PUBLIC_KEY, BOB_PRIVATE_KEY)) {
                KeyPair keyPair = RSAUtils.generateKeyPair();
                KeyLoader.saveKeys(keyPair, BOB_PUBLIC_KEY, BOB_PRIVATE_KEY);
                System.out.println("Generated and saved new RSA key pair for Bob.");
            }

            PrivateKey privateKey = KeyLoader.loadPrivateKey(BOB_PRIVATE_KEY);
            FileTransferHandler.setPrivateKey(privateKey);
            System.out.println("Loaded Bob's private key.");
        } catch (Exception e) {
            System.err.println("Failed to load or generate Bob's keys: " + e.getMessage());
            return;
        }

        // 🔐 Load Alice's public key to verify incoming signatures
        try {
            if (new File(ALICE_PUBLIC_KEY).exists()) {
                PublicKey alicePub = KeyLoader.loadPublicKey(ALICE_PUBLIC_KEY);
                FileTransferHandler.setPublicKey(alicePub);
                System.out.println("Loaded Alice's public key for verification.");
            }
        } catch (Exception e) {
            System.err.println("Failed to load Alice's public key: " + e.getMessage());
        }

        // GUI
        jFrame = new JFrame("Bob");
        jFrame.setSize(700, 500);
        jFrame.setLayout(new BorderLayout(10, 10));
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);

        JLabel jlTitle = new JLabel("Bob File Sender/Receiver");
        jlTitle.setFont(new Font("Arial", Font.BOLD, 28));
        jlTitle.setBorder(new EmptyBorder(30, 0, 10, 0));
        jlTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        jlStatus = new JLabel("No file selected");
        jlStatus.setFont(new Font("Arial", Font.BOLD, 20));
        jlStatus.setBorder(new EmptyBorder(20, 0, 10, 0));
        jlStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton jbChooseFile = new JButton("Choose File to Send to Alice");
        jbChooseFile.setFont(new Font("Arial", Font.BOLD, 18));
        jbChooseFile.setBackground(new Color(173, 216, 230));
        jbChooseFile.setForeground(Color.WHITE);
        jbChooseFile.setBorder(BorderFactory.createLineBorder(new Color(135, 206, 250), 2));
        jbChooseFile.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton jbSendFile = new JButton("Send File to Alice");
        jbSendFile.setFont(new Font("Arial", Font.BOLD, 18));
        jbSendFile.setBackground(new Color(34, 139, 34));
        jbSendFile.setForeground(Color.WHITE);
        jbSendFile.setBorder(BorderFactory.createLineBorder(new Color(34, 139, 34), 2));
        jbSendFile.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(jlTitle);
        mainPanel.add(jlStatus);
        mainPanel.add(jbChooseFile);
        mainPanel.add(jbSendFile);

        jFrame.add(mainPanel, BorderLayout.CENTER);
        jFrame.setVisible(true);

        jbChooseFile.addActionListener(e -> {
            JFileChooser jFileChooser = new JFileChooser();
            jFileChooser.setDialogTitle("Choose a file to send");
            if (jFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                fileToSend[0] = jFileChooser.getSelectedFile();
                jlStatus.setText("Selected file: " + fileToSend[0].getName());
                jlStatus.setForeground(Color.BLACK);
            }
        });

        jbSendFile.addActionListener(e -> {
            if (fileToSend[0] == null) {
                jlStatus.setText("Please choose a file first");
                jlStatus.setForeground(Color.RED);
            } else {
                try {
                    FileTransferHandler.sendFile(fileToSend[0], CLIENT_IP, CLIENT_RECEIVE_PORT);
                    System.out.println("Bob sent = " + fileToSend[0].getName());
                    JOptionPane.showMessageDialog(null, "File sent to client successfully!");
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error sending file: " + ex.getMessage());
                }
            }
        });

        // Start background thread to receive files from client
        new Thread(this::startReceiver).start();
    }

    private void startReceiver() {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Server listening for incoming files on port " + SERVER_PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                FileTransferHandler.receiveFile(socket, "ServerFiles/");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
