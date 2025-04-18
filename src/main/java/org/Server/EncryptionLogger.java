package org.Server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

/**
 * Utility class for logging encryption and decryption operations
 * Provides detailed logs of all cryptographic activities
 */
public class EncryptionLogger {
    private static final Logger logger = Logger.getLogger("EncryptionLogger");
    private static final String LOG_FOLDER = "logs";
    private static final String ENCRYPTION_LOG_FILE = "encryption.log";
    private static Handler fileHandler;
    private static boolean initialized = false;

    static {
        try {
            // Ensure log directory exists
            File logDir = new File(LOG_FOLDER);
            if (!logDir.exists()) {
                logDir.mkdir();
            }

            // Initialize logger
            initialize();
        } catch (Exception e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize the logger with file and console handlers
     */
    public static synchronized void initialize() throws IOException {
        if (initialized) {
            return;
        }

        // Create log file if it doesn't exist
        String logFilePath = LOG_FOLDER + File.separator + ENCRYPTION_LOG_FILE;
        if (!Files.exists(Paths.get(logFilePath))) {
            Files.createFile(Paths.get(logFilePath));
        }

        // Configure logger
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        // Create and configure file handler
        fileHandler = new FileHandler(logFilePath, true);
        fileHandler.setFormatter(new CustomFormatter());
        fileHandler.setLevel(Level.ALL);
        logger.addHandler(fileHandler);

        // Add console handler
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new CustomFormatter());
        consoleHandler.setLevel(Level.INFO);
        logger.addHandler(consoleHandler);

        initialized = true;
        logger.info("Encryption logger initialized");
    }

    /**
     * Log encryption operation
     * @param operationType Type of operation
     * @param dataLength Length of data being encrypted
     * @param encrypted Flag indicating if this is encryption (true) or decryption (false)
     */
    public static void logOperation(String operationType, int dataLength, boolean encrypted) {
        try {
            if (!initialized) {
                initialize();
            }
            
            String operation = encrypted ? "Encrypted" : "Decrypted";
            logger.info(String.format("%s: %s %d bytes using %s", 
                         operation, operationType, dataLength, 
                         encrypted ? "AES+RSA" : "RSA+AES"));
        } catch (Exception e) {
            System.err.println("Failed to log operation: " + e.getMessage());
        }
    }

    /**
     * Log key generation
     * @param keyType Type of key generated
     * @param keySize Size of key in bits
     */
    public static void logKeyGeneration(String keyType, int keySize) {
        try {
            if (!initialized) {
                initialize();
            }
            
            logger.info(String.format("Generated %s key (%d bits)", keyType, keySize));
        } catch (Exception e) {
            System.err.println("Failed to log key generation: " + e.getMessage());
        }
    }

    /**
     * Log error during encryption/decryption
     * @param operation Operation that failed
     * @param error Error message
     */
    public static void logError(String operation, String error) {
        try {
            if (!initialized) {
                initialize();
            }
            
            logger.severe(String.format("Error during %s: %s", operation, error));
        } catch (Exception e) {
            System.err.println("Failed to log error: " + e.getMessage());
        }
    }

    /**
     * Log successful client-server communication with encryption
     * @param clientInfo Client information
     * @param messageType Type of message
     * @param encryptedSize Size of encrypted data
     * @param originalSize Size of original data
     */
    public static void logCommunication(String clientInfo, String messageType, 
                                      int encryptedSize, int originalSize) {
        try {
            if (!initialized) {
                initialize();
            }
            
            logger.info(String.format("Communication with %s: %s message, %d bytes encrypted (%d bytes original)", 
                         clientInfo, messageType, encryptedSize, originalSize));
        } catch (Exception e) {
            System.err.println("Failed to log communication: " + e.getMessage());
        }
    }

    /**
     * Custom log formatter
     */
    private static class CustomFormatter extends Formatter {
        private static final DateTimeFormatter dateFormatter = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            
        @Override
        public String format(LogRecord record) {
            LocalDateTime dateTime = LocalDateTime.now();
            String formattedDate = dateTime.format(dateFormatter);
            
            return String.format("[%s] [%s] %s%n", 
                     formattedDate, 
                     record.getLevel().getName(), 
                     record.getMessage());
        }
    }

    /**
     * Close the logger
     */
    public static void close() {
        if (fileHandler != null) {
            fileHandler.close();
        }
    }
} 