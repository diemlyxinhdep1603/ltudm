package Client;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

import GUI.ProductReviewGUI;
import GUI.EncryptionMonitor;

/**
 * Client launcher that allows starting either encrypted or non-encrypted client
 */
public class ClientLauncher {
    
    private static String autodetectedServerIP = "localhost";
    private static JLabel statusLabel;
    
    /**
     * Main method
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Tự động lấy IP server từ API trong một luồng khác
        new Thread(() -> {
            try {
                IPFetcher ipFetcher = new IPFetcher();
                String ip = ipFetcher.fetch_IP();
                if (ip != null && !ip.isEmpty()) {
                    autodetectedServerIP = ip;
                    if (statusLabel != null) {
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Đã phát hiện server: " + autodetectedServerIP);
                            statusLabel.setForeground(Color.GREEN.darker());
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println("Không thể tự động lấy IP server: " + e.getMessage());
                if (statusLabel != null) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Không thể tự động phát hiện server");
                        statusLabel.setForeground(Color.RED);
                    });
                }
            }
        }).start();
        
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }
    
    /**
     * Create and show launcher GUI
     */
    private static void createAndShowGUI() {
        // Create main frame
        JFrame frame = new JFrame("Product Review Client Launcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(550, 400);
        frame.setLocationRelativeTo(null);
        
        // Create panel with GridBagLayout for better control
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Title label
        JLabel titleLabel = new JLabel("Product Review Client Launcher", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(titleLabel, gbc);
        
        // Status label
        statusLabel = new JLabel("Đang tự động phát hiện server...", JLabel.CENTER);
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        statusLabel.setForeground(Color.BLUE);
        panel.add(statusLabel, gbc);
        
        // Server settings
        JPanel serverPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        serverPanel.setBorder(BorderFactory.createTitledBorder("Cài đặt Server"));
        
        JLabel hostLabel = new JLabel("Server Host:", JLabel.RIGHT);
        JTextField hostField = new JTextField(autodetectedServerIP, 15);
        
        JLabel portLabel = new JLabel("Server Port:", JLabel.RIGHT);
        JTextField portField = new JTextField("1234", 5);
        
        JCheckBox autoConnectCheckBox = new JCheckBox("Sử dụng IP tự động phát hiện", true);
        JLabel dummyLabel = new JLabel(""); // Placeholder
        
        // Update host field when auto-connect state changes
        autoConnectCheckBox.addActionListener(e -> {
            hostField.setEnabled(!autoConnectCheckBox.isSelected());
            if (autoConnectCheckBox.isSelected()) {
                hostField.setText(autodetectedServerIP);
            }
        });
        
        // Disable host field initially since auto-connect is checked by default
        hostField.setEnabled(!autoConnectCheckBox.isSelected());
        
        serverPanel.add(hostLabel);
        serverPanel.add(hostField);
        serverPanel.add(portLabel);
        serverPanel.add(portField);
        serverPanel.add(autoConnectCheckBox);
        serverPanel.add(dummyLabel);
        
        panel.add(serverPanel, gbc);
        
        // Client options
        JPanel optionsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Tùy chọn Client"));
        
        JCheckBox encryptionCheckBox = new JCheckBox("Bật mã hóa (AES+RSA Hybrid)", true);
        JCheckBox monitorCheckBox = new JCheckBox("Hiển thị màn hình theo dõi mã hóa", true);
        
        optionsPanel.add(encryptionCheckBox);
        optionsPanel.add(monitorCheckBox);
        
        panel.add(optionsPanel, gbc);
        
        // Progress area
        JTextArea progressArea = new JTextArea(3, 40);
        progressArea.setEditable(false);
        progressArea.setLineWrap(true);
        progressArea.setWrapStyleWord(true);
        progressArea.setBorder(BorderFactory.createTitledBorder("Thông tin"));
        JScrollPane progressScroll = new JScrollPane(progressArea);
        panel.add(progressScroll, gbc);
        
        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        
        JButton startButton = new JButton("Khởi động Client");
        startButton.addActionListener(e -> {
            try {
                progressArea.append("Đang khởi tạo client...\n");
                
                // Get server host
                String host;
                if (autoConnectCheckBox.isSelected()) {
                    host = autodetectedServerIP;
                    progressArea.append("Sử dụng IP tự động phát hiện: " + host + "\n");
                } else {
                    host = hostField.getText().trim();
                    progressArea.append("Sử dụng IP thủ công: " + host + "\n");
                }
                
                if (host.isEmpty()) {
                    throw new IllegalArgumentException("Địa chỉ server không được để trống");
                }
                
                // Get port
                int port;
                try {
                    port = Integer.parseInt(portField.getText());
                    if (port < 1 || port > 65535) {
                        throw new NumberFormatException("Port phải nằm trong khoảng 1-65535");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, 
                            "Số port không hợp lệ. Sử dụng port mặc định 1234",
                            "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                    port = 1234;
                    portField.setText("1234");
                }
                
                // Create client and GUI
                ProductReviewClient client;
                progressArea.append("Đang kết nối đến " + host + ":" + port + "...\n");
                
                if (encryptionCheckBox.isSelected()) {
                    // Create encrypted client
                    progressArea.append("Khởi tạo client mã hóa...\n");
                    client = new EncryptedClient(host, port);
                    
                    // Show encryption monitor if selected
                    if (monitorCheckBox.isSelected()) {
                        // Create logs directory
                        File logsDir = new File("logs");
                        if (!logsDir.exists()) {
                            logsDir.mkdir();
                            progressArea.append("Đã tạo thư mục logs\n");
                        }
                        
                        progressArea.append("Khởi động màn hình theo dõi mã hóa...\n");
                        SwingUtilities.invokeLater(() -> {
                            EncryptionMonitor monitor = new EncryptionMonitor();
                            monitor.setVisible(true);
                        });
                    }
                } else {
                    // Create standard client
                    progressArea.append("Khởi tạo client tiêu chuẩn...\n");
                    client = new ProductReviewClient(host, port);
                }
                
                // Test connection
                progressArea.append("Kiểm tra kết nối...\n");
                if (!client.connect()) {
                    progressArea.append("Không thể kết nối đến server!\n");
                    // Still allow to continue, but warn user
                    int choice = JOptionPane.showConfirmDialog(frame,
                            "Không thể kết nối đến server. Bạn vẫn muốn tiếp tục?",
                            "Lỗi kết nối", JOptionPane.YES_NO_OPTION);
                    if (choice != JOptionPane.YES_OPTION) {
                        progressArea.append("Hủy khởi động client.\n");
                        return;
                    }
                }
                
                // Create and show GUI
                progressArea.append("Khởi động giao diện chính...\n");
                SwingUtilities.invokeLater(() -> {
                    ProductReviewGUI gui = new ProductReviewGUI(client);
                    gui.setVisible(true);
                });
                
                // Close launcher
                progressArea.append("Khởi động thành công!\n");
                JOptionPane.showMessageDialog(frame, 
                        "Client đã khởi động thành công!",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                frame.dispose();
                
            } catch (Exception ex) {
                progressArea.append("Lỗi: " + ex.getMessage() + "\n");
                JOptionPane.showMessageDialog(frame, 
                        "Lỗi khi khởi động client: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JButton exitButton = new JButton("Thoát");
        exitButton.addActionListener(e -> System.exit(0));
        
        buttonPanel.add(startButton);
        buttonPanel.add(exitButton);
        
        panel.add(buttonPanel, gbc);
        
        // Add panel to frame
        frame.getContentPane().add(panel);
        
        // Display frame
        frame.setVisible(true);
    }
} 