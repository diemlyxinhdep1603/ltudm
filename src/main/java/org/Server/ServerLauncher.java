package org.Server;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Server launcher that allows starting either encrypted or non-encrypted server
 */
public class ServerLauncher {
    private static final int DEFAULT_PORT = 1234;
    
    /**
     * Main method
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }
    
    /**
     * Create and show launcher GUI
     */
    private static void createAndShowGUI() {
        // Create main frame
        JFrame frame = new JFrame("Product Review Server Launcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 350);
        frame.setLocationRelativeTo(null);
        
        // Create panel with GridBagLayout for better control
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Title label
        JLabel titleLabel = new JLabel("Product Review Server Launcher", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(titleLabel, gbc);
        
        // Port input
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel portLabel = new JLabel("Port:");
        JTextField portField = new JTextField(Integer.toString(DEFAULT_PORT), 5);
        portPanel.add(portLabel);
        portPanel.add(portField);
        panel.add(portPanel, gbc);
        
        // IP Registration section
        JPanel ipPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox registerIpCheckBox = new JCheckBox("Đăng ký IP với API (cho phép client tự động kết nối)", true);
        ipPanel.add(registerIpCheckBox);
        panel.add(ipPanel, gbc);
        
        // Encryption option
        JCheckBox encryptionCheckBox = new JCheckBox("Bật mã hóa (AES+RSA Hybrid)", true);
        panel.add(encryptionCheckBox, gbc);
        
        // Logging option
        JCheckBox loggingCheckBox = new JCheckBox("Bật ghi log chi tiết", true);
        panel.add(loggingCheckBox, gbc);
        
        // Status area
        JTextArea statusArea = new JTextArea(3, 40);
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setBorder(BorderFactory.createTitledBorder("Trạng thái"));
        JScrollPane statusScroll = new JScrollPane(statusArea);
        panel.add(statusScroll, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        
        JButton startButton = new JButton("Khởi động Server");
        startButton.addActionListener(e -> {
            try {
                // Get port
                int port = DEFAULT_PORT;
                try {
                    port = Integer.parseInt(portField.getText());
                    if (port < 1 || port > 65535) {
                        throw new NumberFormatException("Port phải nằm trong khoảng 1-65535");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, 
                            "Số port không hợp lệ. Sử dụng port mặc định " + DEFAULT_PORT,
                            "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                    port = DEFAULT_PORT;
                    portField.setText(Integer.toString(DEFAULT_PORT));
                }
                
                // Create logs directory if logging is enabled
                if (loggingCheckBox.isSelected()) {
                    File logsDir = new File("logs");
                    if (!logsDir.exists()) {
                        logsDir.mkdir();
                        statusArea.append("Đã tạo thư mục logs\n");
                    }
                }
                
                // Create keys directory if encryption is enabled
                if (encryptionCheckBox.isSelected()) {
                    File keysDir = new File("keys");
                    if (!keysDir.exists()) {
                        keysDir.mkdir();
                        statusArea.append("Đã tạo thư mục keys\n");
                    }
                }
                
                final int finalPort = port;
                final boolean shouldRegisterIp = registerIpCheckBox.isSelected();
                final boolean isEncrypted = encryptionCheckBox.isSelected();
                
                // Start server in a new thread
                new Thread(() -> {
                    try {
                        if (isEncrypted) {
                            EncryptedServer server = new EncryptedServer(finalPort);
                            // Chỉ đẩy IP lên API nếu người dùng chọn
                            if (shouldRegisterIp) {
                                SwingUtilities.invokeLater(() -> 
                                    statusArea.append("Đang đăng ký IP server lên API...\n"));
                                server.push_IP();
                                SwingUtilities.invokeLater(() -> 
                                    statusArea.append("Đã đăng ký IP server thành công\n"));
                            }
                            SwingUtilities.invokeLater(() -> 
                                statusArea.append("Khởi động server mã hóa trên port " + finalPort + "...\n"));
                            server.start();
                        } else {
                            ProductReviewServer server = new ProductReviewServer(finalPort);
                            // Chỉ đẩy IP lên API nếu người dùng chọn
                            if (shouldRegisterIp) {
                                SwingUtilities.invokeLater(() -> 
                                    statusArea.append("Đang đăng ký IP server lên API...\n"));
                                server.push_IP();
                                SwingUtilities.invokeLater(() -> 
                                    statusArea.append("Đã đăng ký IP server thành công\n"));
                            }
                            SwingUtilities.invokeLater(() -> 
                                statusArea.append("Khởi động server trên port " + finalPort + "...\n"));
                            server.start();
                        }
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> 
                            statusArea.append("Lỗi: " + ex.getMessage() + "\n"));
                    }
                }).start();
                
                // Disable start button
                startButton.setEnabled(false);
                startButton.setText("Server đang chạy");
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, 
                        "Lỗi khi khởi động server: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                statusArea.append("Lỗi: " + ex.getMessage() + "\n");
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