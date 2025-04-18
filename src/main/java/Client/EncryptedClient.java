package Client;

import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import java.net.SocketTimeoutException;

import org.Server.CryptoUtils;
import org.Server.EncryptionLogger;

/**
 * Extended ProductReviewClient with encryption capabilities
 * Adds hybrid encryption (AES+RSA) to client-server communication
 */
public class EncryptedClient extends ProductReviewClient {
    private KeyPair clientKeyPair;
    private PublicKey serverPublicKey;
    private boolean keyExchangeCompleted = false;
    private static final String LOG_PREFIX = "[EncryptedClient] ";
    
    // References to parent class private fields
    private Socket socketRef;
    private BufferedReader readerRef;
    private PrintWriter writerRef;
    private String currentPlatformRef = "TIKI";
    
    /**
     * Constructor
     *
     * @param serverHost Server hostname or IP
     * @param serverPort Server port
     */
    public EncryptedClient(String serverHost, int serverPort) {
        super(serverHost, serverPort);
        try {
            // Generate client keys
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            clientKeyPair = keyPairGenerator.generateKeyPair();
            System.out.println(LOG_PREFIX + "Initialized with RSA key pair");
        } catch (Exception e) {
            System.err.println(LOG_PREFIX + "Error initializing encryption: " + e.getMessage());
        }
    }
    
    /**
     * Connect to the server with encryption
     *
     * @return true if connection successful, false otherwise
     */
    @Override
    public boolean connect() {
        try {
            // Establish socket connection
            if (super.connect()) {
                System.out.println(LOG_PREFIX + "Socket connected successfully");
                
                // Add delay to ensure socket is fully established
                try {
                    Thread.sleep(1000); // Increased from 500ms to 1000ms
                } catch (InterruptedException e) {
                    // Ignore
                }
                
                // Get references from parent class
                this.socketRef = getParentSocket();
                this.readerRef = getParentReader();
                this.writerRef = getParentWriter();
                
                // Explicitly check socket before key exchange
                if (this.socketRef == null || !this.socketRef.isConnected() || this.socketRef.isClosed()) {
                    System.err.println(LOG_PREFIX + "Socket is not properly connected before key exchange");
                    super.close();
                    return false;
                }
                
                // Perform key exchange
                if (exchangeKeys()) {
                    System.out.println(LOG_PREFIX + "Connection and key exchange completed successfully");
                    return true;
                } else {
                    System.out.println(LOG_PREFIX + "Key exchange failed, closing connection");
                    super.close();
                    return false;
                }
            } else {
                System.out.println(LOG_PREFIX + "Failed to connect socket");
                return false;
            }
        } catch (Exception e) {
            System.err.println(LOG_PREFIX + "Error during encrypted connection: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Exchange keys with the server
     *
     * @return true if key exchange successful, false otherwise
     */
    private boolean exchangeKeys() {
        try {
            // Double check socket is still connected
            if (socketRef == null || !socketRef.isConnected() || socketRef.isClosed()) {
                System.err.println(LOG_PREFIX + "Socket not connected during key exchange");
                return false;
            }

            // First, send client's public key to server
            String encodedPublicKey = CryptoUtils.keyToString(clientKeyPair.getPublic());
            System.out.println(LOG_PREFIX + "Sending public key to server: " + encodedPublicKey.substring(0, 20) + "...");
            
            // Thêm tiền tố "CLIENT_KEY:" để phù hợp với mong đợi của server
            writerRef.println("CLIENT_KEY:" + encodedPublicKey);
            writerRef.flush();
            
            // Then, receive server's public key
            System.out.println(LOG_PREFIX + "Waiting for server public key...");
            
            // Set timeout for receiving server response
            socketRef.setSoTimeout(15000); // 15 seconds timeout
            
            // Read response line by line
            String line;
            StringBuilder responseBuilder = new StringBuilder();
            
            while ((line = readerRef.readLine()) != null) {
                if (line.equals("<END>")) {
                    break;
                }
                responseBuilder.append(line);
            }
            
            // Reset to infinite timeout for normal operation
            socketRef.setSoTimeout(0);
            
            String response = responseBuilder.toString();
            
            if (response.startsWith("SERVER_KEY:")) {
                String serverPublicKeyStr = response.substring("SERVER_KEY:".length());
                System.out.println(LOG_PREFIX + "Received server public key: " + 
                    (serverPublicKeyStr.length() > 20 ? serverPublicKeyStr.substring(0, 20) + "..." : serverPublicKeyStr));
                
                try {
                    // Parse the server's public key
                    serverPublicKey = CryptoUtils.stringToPublicKey(serverPublicKeyStr);
                    System.out.println(LOG_PREFIX + "Server public key parsed successfully");
                    
                    // Mark key exchange as complete
                    keyExchangeCompleted = true;
                    return true;
                } catch (Exception e) {
                    System.err.println(LOG_PREFIX + "Error parsing server public key: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            } else {
                System.err.println(LOG_PREFIX + "Received unexpected response from server: " + response);
                return false;
            }
        } catch (IOException e) {
            System.err.println(LOG_PREFIX + "Error during key exchange: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Send an encrypted request to the server
     *
     * @param request The request to send
     * @return The server's response
     * @throws IOException If an I/O error occurs
     */
    @Override
    public String sendRequest(String request) throws IOException {
        if (!isConnected() || !keyExchangeCompleted) {
            if (!connect()) {
                throw new IOException("Cannot connect to server or key exchange failed");
            }
        }
        
        try {
            System.out.println(LOG_PREFIX + "Original request: " + request);
            
            // Encrypt the request
            CryptoUtils.EncryptedPackage encryptedPackage = CryptoUtils.encryptHybrid(request, serverPublicKey);
            String encryptedRequestStr = encryptedPackage.toString();
            
            // Send encrypted request
            writerRef.println(encryptedRequestStr);
            writerRef.flush();
            
            // Read encrypted response
            StringBuilder encryptedResponse = new StringBuilder();
            String line;
            while ((line = readerRef.readLine()) != null) {
                if (line.equals("<END>")) {
                    break;
                }
                encryptedResponse.append(line);
            }
            
            // If response starts with ERROR:, it's an unencrypted error message
            if (encryptedResponse.toString().startsWith("ERROR:")) {
                System.err.println(LOG_PREFIX + "Server returned an error: " + encryptedResponse.toString());
                return encryptedResponse.toString();
            }
            
            // Decrypt the response
            CryptoUtils.EncryptedPackage responsePackage = CryptoUtils.EncryptedPackage.fromString(encryptedResponse.toString());
            String decryptedResponse = CryptoUtils.decryptHybrid(responsePackage, clientKeyPair.getPrivate());
            
            System.out.println(LOG_PREFIX + "Response received and decrypted (length: " + decryptedResponse.length() + " bytes)");
            
            return decryptedResponse;
        } catch (Exception e) {
            throw new IOException("Error processing encrypted request: " + e.getMessage(), e);
        }
    }
    
    /**
     * Search for product reviews
     *
     * @param productName The product name to search for
     * @return List of reviews or suggestions
     */
    @Override
    public List<String> searchProduct(String productName) {
        List<String> results = new ArrayList<>();
        
        try {
            // Connect to server if not already connected
            if (!isConnected() || !keyExchangeCompleted) {
                if (!connect()) {
                    results.add("Không thể kết nối đến máy chủ.");
                    return results;
                }
            }
            
            // Send product name to server with lazy loading marker
            String response = sendRequest("LAZY_LOAD:" + productName);
            
            // Only add the full complete response if it's not empty
            if (response.length() > 0) {
                results.add(response);
            }
        } catch (IOException e) {
            results.add("Lỗi khi tìm kiếm sản phẩm: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Change the current platform for product reviews
     *
     * @param platform The platform to change to
     * @return The server's response
     */
    @Override
    public String changePlatform(String platform) {
        try {
            if (!isConnected() || !keyExchangeCompleted) {
                if (!connect()) {
                    return "Không thể kết nối đến máy chủ.";
                }
            }
            
            // Send platform change request to server
            String request = "PLATFORM:" + platform;
            String response = sendRequest(request);
            
            // Update current platform if successful
            if (response.contains("Đã chuyển sang nền tảng")) {
                currentPlatformRef = platform;
            }
            
            return response;
        } catch (IOException e) {
            return "Lỗi khi chuyển đổi nền tảng: " + e.getMessage();
        }
    }
    
    /**
     * Get the current platform
     */
    @Override
    public String getCurrentPlatform() {
        return currentPlatformRef;
    }
    
    /**
     * Load more reviews
     */
    @Override
    public String loadMoreReviews(String productId, int page) {
        try {
            if (!isConnected() || !keyExchangeCompleted) {
                if (!connect()) {
                    return "Không thể kết nối đến máy chủ.";
                }
            }
            
            // Send request for more reviews
            String request = "LOAD_MORE:" + productId + ":" + page;
            return sendRequest(request);
        } catch (IOException e) {
            return "Lỗi khi tải thêm đánh giá: " + e.getMessage();
        }
    }
    
    /**
     * Summarize reviews
     */
    @Override
    public String summarizeReviews(String productId, String reviews) {
        try {
            if (!isConnected() || !keyExchangeCompleted) {
                if (!connect()) {
                    return "Không thể kết nối đến máy chủ.";
                }
            }
            
            // Send summarize request
            String request = "SUMMARIZE:" + productId + ":" + reviews;
            return sendRequest(request);
        } catch (IOException e) {
            return "Lỗi khi tổng hợp đánh giá: " + e.getMessage();
        }
    }
    
    /**
     * Get product rating info
     */
    @Override
    public String getProductRatingInfo(String productUrl) {
        try {
            if (!isConnected() || !keyExchangeCompleted) {
                if (!connect()) {
                    return "Không thể kết nối đến máy chủ.";
                }
            }
            
            // Send rating info request
            String request = "GET_RATING_INFO:" + productUrl;
            return sendRequest(request);
        } catch (IOException e) {
            return "Lỗi khi lấy thông tin đánh giá: " + e.getMessage();
        }
    }
    
    /**
     * Get reviews by page
     */
    @Override
    public List<String> getReviewsByPage(String productId, int page) {
        List<String> results = new ArrayList<>();
        
        try {
            // Connect to server if not already connected
            if (!isConnected() || !keyExchangeCompleted) {
                if (!connect()) {
                    results.add("Không thể kết nối đến máy chủ.");
                    return results;
                }
            }
            
            // Send request to load page of reviews
            String response = loadMoreReviews(productId, page);
            
            // Only add the full complete response if it's not empty
            if (response.length() > 0) {
                results.add(response);
            }
        } catch (Exception e) {
            results.add("Lỗi khi tải đánh giá trang " + page + ": " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Close the connection
     */
    @Override
    public void close() {
        try {
            if (isConnected() && writerRef != null) {
                writerRef.println("bye");
                System.out.println(LOG_PREFIX + "Closing encrypted connection");
            }
        } catch (Exception e) {
            System.err.println(LOG_PREFIX + "Error closing connection: " + e.getMessage());
        } finally {
            super.close();
        }
    }
    
    /**
     * Check if connected to the server
     */
    @Override
    public boolean isConnected() {
        return super.isConnected() && keyExchangeCompleted;
    }
    
    /**
     * Get socket (used for reflection only)
     */
    private Socket getSocket() {
        return socketRef;
    }
    
    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        EncryptedClient client = new EncryptedClient("localhost", 1234);
        if (client.connect()) {
            System.out.println("Connected to encrypted server");
            
            // Test platform change
            String platformResponse = client.changePlatform("TIKI");
            System.out.println("Platform response: " + platformResponse);
            
            // Test product search
            List<String> searchResults = client.searchProduct("iphone");
            for (String result : searchResults) {
                System.out.println("Search result: " + result.substring(0, Math.min(100, result.length())) + "...");
            }
            
            client.close();
        } else {
            System.out.println("Failed to connect to encrypted server");
        }
    }
} 