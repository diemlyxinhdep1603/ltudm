package GUI;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Monitor for encryption status and logs
 * Provides a GUI to view encryption activities
 */
public class EncryptionMonitor extends JFrame {
    private JTextArea logTextArea;
    private JLabel statusLabel;
    private JButton refreshButton;
    private Timer logUpdateTimer;
    private static final String LOG_FOLDER = "logs";
    private static final String ENCRYPTION_LOG_FILE = "encryption.log";
    private long lastReadPosition = 0;
    
    /**
     * Constructor
     */
    public EncryptionMonitor() {
        setTitle("Encryption Monitor");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        initComponents();
        startLogUpdateTimer();
    }
    
    /**
     * Initialize GUI components
     */
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // Status panel at the top
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        statusLabel = new JLabel("Encryption Status: Active");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(Color.GREEN.darker());
        
        JLabel timeLabel = new JLabel();
        updateTimeLabel(timeLabel);
        
        // Update time every second
        Timer timeTimer = new Timer(1000, e -> updateTimeLabel(timeLabel));
        timeTimer.start();
        
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(timeLabel, BorderLayout.EAST);
        
        // Log area in the center
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        DefaultCaret caret = (DefaultCaret) logTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        JScrollPane scrollPane = new JScrollPane(logTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // Control panel at the bottom
        JPanel controlPanel = new JPanel();
        
        refreshButton = new JButton("Refresh Logs");
        refreshButton.addActionListener(e -> refreshLogs());
        
        JButton clearButton = new JButton("Clear Display");
        clearButton.addActionListener(e -> logTextArea.setText(""));
        
        controlPanel.add(refreshButton);
        controlPanel.add(clearButton);
        
        // Add panels to the frame
        add(statusPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        
        // Initial log read
        refreshLogs();
    }
    
    /**
     * Update time label with current time
     * 
     * @param label Label to update
     */
    private void updateTimeLabel(JLabel label) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        label.setText(sdf.format(new Date()));
    }
    
    /**
     * Start timer to update logs automatically
     */
    private void startLogUpdateTimer() {
        logUpdateTimer = new Timer(5000, e -> refreshLogs());
        logUpdateTimer.start();
    }
    
    /**
     * Refresh log display
     */
    private void refreshLogs() {
        File logFile = new File(LOG_FOLDER + File.separator + ENCRYPTION_LOG_FILE);
        if (!logFile.exists()) {
            logTextArea.setText("Log file not found: " + logFile.getAbsolutePath());
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            StringBuilder newContent = new StringBuilder();
            String line;
            
            // Skip to last read position if not the first read
            if (lastReadPosition > 0) {
                reader.skip(lastReadPosition);
            }
            
            while ((line = reader.readLine()) != null) {
                newContent.append(line).append("\n");
            }
            
            // Update last read position
            lastReadPosition = logFile.length();
            
            // Append new content to text area
            if (newContent.length() > 0) {
                logTextArea.append(newContent.toString());
                
                // Scroll to bottom
                logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
            }
            
        } catch (IOException e) {
            logTextArea.append("Error reading log file: " + e.getMessage() + "\n");
        }
    }
    
    /**
     * Update encryption status
     * 
     * @param active Whether encryption is active
     */
    public void updateStatus(boolean active) {
        if (active) {
            statusLabel.setText("Encryption Status: Active");
            statusLabel.setForeground(Color.GREEN.darker());
        } else {
            statusLabel.setText("Encryption Status: Inactive");
            statusLabel.setForeground(Color.RED);
        }
    }
    
    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EncryptionMonitor monitor = new EncryptionMonitor();
            monitor.setVisible(true);
        });
    }
} 