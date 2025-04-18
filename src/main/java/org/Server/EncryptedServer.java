package org.Server;

import java.io.*;
import java.net.*;
import java.security.PublicKey;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extended ProductReviewServer with encryption capabilities
 * Adds hybrid encryption (AES+RSA) to client-server communication
 */
public class EncryptedServer extends ProductReviewServer {
    private KeyManager keyManager;
    private ConcurrentHashMap<String, PublicKey> connectedClients;
    private getReviewTIKIProduct tikiObj;
    private getReviewDMXProduct dmxObj;
    private int portNumber;
    
    /**
     * Constructor
     * 
     * @param port Server port
     */
    public EncryptedServer(int port) {
        super(port);
        portNumber = port;
        keyManager = KeyManager.getInstance();
        connectedClients = new ConcurrentHashMap<>();
        tikiObj = new getReviewTIKIProduct();
        dmxObj = new getReviewDMXProduct();
        System.out.println("Initialized encrypted server");
    }
    
    /**
     * Get the server port 
     */
    public int getPort() {
        return portNumber;
    }
    
    /**
     * Get TIKI product review handler
     */
    public getReviewTIKIProduct getTiki() {
        return tikiObj;
    }
    
    /**
     * Get DMX product review handler
     */
    public getReviewDMXProduct getDmx() {
        return dmxObj;
    }
    
    /**
     * Start the server
     */
    @Override
    public void start() {
        // Initialize key manager
        keyManager = KeyManager.getInstance();
        
        try (ServerSocket server = new ServerSocket(getPort())) {
            System.out.println("Encrypted server is listening on port: " + getPort());
            System.out.println("Server public key: " + keyManager.getServerPublicKeyString().substring(0, 32) + "...");
            
            // Start the client key exchange handler
            push_IP();
            
            while (true) {
                Socket socket = server.accept();
                String clientAddress = socket.getRemoteSocketAddress().toString();
                System.out.println("New encrypted connection from: " + clientAddress);
                
                new Thread(() -> handleEncryptedClient(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting encrypted server: " + e.getMessage());
            EncryptionLogger.logError("Server startup", e.getMessage());
        }
    }
    
    /**
     * Handle encrypted client connection
     * 
     * @param socket Client socket
     */
    private void handleEncryptedClient(Socket socket) {
        String clientAddress = socket.getRemoteSocketAddress().toString();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {
            
            // First exchange - get client's public key and send server's public key
            String clientKeyMessage = reader.readLine();
            
            if (clientKeyMessage != null) {
                String clientKeyStr;
                // Kiểm tra nếu message bắt đầu bằng các tiền tố đã biết
                if (clientKeyMessage.startsWith("CLIENT_KEY:")) {
                    clientKeyStr = clientKeyMessage.substring(11);
                } else if (clientKeyMessage.startsWith("KEY_EXCHANGE:")) {
                    clientKeyStr = clientKeyMessage.substring(12);
                } else {
                    // Trường hợp không có tiền tố - giả định đây là khóa công khai
                    clientKeyStr = clientKeyMessage;
                }
                
                try {
                    System.out.println("Processing client key: " + clientKeyStr.substring(0, Math.min(20, clientKeyStr.length())) + "...");
                    PublicKey clientKey = CryptoUtils.stringToPublicKey(clientKeyStr);
                    
                    // Send server's public key
                    writer.println("SERVER_KEY:" + keyManager.getServerPublicKeyString());
                    writer.println("<END>");
                    
                    // Register client
                    String clientId = clientAddress;
                    keyManager.registerClientKey(clientId, clientKey);
                    connectedClients.put(clientId, clientKey);
                    
                    EncryptionLogger.logOperation("Key exchange", clientKeyStr.length(), false);
                    
                    // Process encrypted messages
                    processEncryptedMessages(socket, reader, writer, clientId);
                    
                } catch (Exception e) {
                    System.err.println("Error processing client key: " + e.getMessage());
                    e.printStackTrace();
                    EncryptionLogger.logError("Key exchange", e.getMessage());
                    writer.println("ERROR:Invalid key format");
                    writer.println("<END>");
                }
            } else {
                System.err.println("Invalid client key message: null");
                writer.println("ERROR:Expected client key");
                writer.println("<END>");
            }
            
        } catch (IOException e) {
            System.err.println("Error handling encrypted client: " + e.getMessage());
            EncryptionLogger.logError("Client handling", e.getMessage());
        } finally {
            connectedClients.remove(clientAddress);
            System.out.println("Closed encrypted connection from: " + clientAddress);
        }
    }
    
    /**
     * Process encrypted messages from client
     * 
     * @param socket Client socket
     * @param reader Socket reader
     * @param writer Socket writer
     * @param clientId Client identifier
     */
    private void processEncryptedMessages(Socket socket, BufferedReader reader, PrintWriter writer, String clientId) throws IOException {
        String encryptedRequest;
        
        // Reuse server's handler state variables
        boolean isSummarizeConnection = false;
        String currentPlatform = "TIKI";
        
        // Get client's public key
        PublicKey clientKey = connectedClients.get(clientId);
        
        while ((encryptedRequest = reader.readLine()) != null) {
            if (encryptedRequest.equalsIgnoreCase("bye")) {
                System.out.println("Client requested disconnection: " + clientId);
                break;
            }
            
            try {
                // Decrypt the request
                CryptoUtils.EncryptedPackage encryptedPackage = CryptoUtils.EncryptedPackage.fromString(encryptedRequest);
                String decryptedRequest = CryptoUtils.decryptHybrid(encryptedPackage, keyManager.getServerPrivateKey());
                
                EncryptionLogger.logCommunication(clientId, "Request", encryptedRequest.length(), decryptedRequest.length());
                System.out.println("Decrypted request: " + decryptedRequest);
                
                // Process the request as in the original server
                String response;
                
                // Check for summarize request first - special handling
                if (decryptedRequest.startsWith("SUMMARIZE:")) {
                    String[] parts = decryptedRequest.substring(10).split(":", 2);
                    if (parts.length == 2) {
                        String productId = parts[0];
                        String reviewsContent = parts[1];
                        
                        // Call AI for review summarization
                        AIReviewSummarizer summarizer = new AIReviewSummarizer();
                        response = summarizer.summarizeReviews(reviewsContent);
                        isSummarizeConnection = true;
                    } else {
                        response = "ERROR:Invalid summarize request format";
                    }
                }
                // Platform change
                else if (decryptedRequest.startsWith("PLATFORM:")) {
                    String platform = decryptedRequest.substring(9).trim();
                    if (platform.equals("ĐIỆN MÁY XANH") || Valid_Input_Data.isValidPlatform(platform)) {
                        currentPlatform = platform;
                        response = "Đã chuyển sang nền tảng: " + currentPlatform;
                    } else {
                        String errorMessage = Valid_Input_Data.getLastErrorMessage();
                        if (errorMessage.isEmpty()) {
                            errorMessage = "Nền tảng không hợp lệ. Chỉ chấp nhận TIKI, ĐIỆN MÁY XANH";
                        }
                        response = "Lỗi: " + errorMessage;
                    }
                }
                // Load more reviews
                else if (decryptedRequest.startsWith("LOAD_MORE:")) {
                    String[] parts = decryptedRequest.substring(10).split(":");
                    if (parts.length == 2) {
                        String productId = parts[0];
                        int page = Integer.parseInt(parts[1]);
                        
                        if (currentPlatform.equals("TIKI")) {
                            response = getTiki().getProductReviewsWithPagination(productId, page);
                        } else if (currentPlatform.equals("ĐIỆN MÁY XANH")) {
                            getFullReviewsUtilDMX dmxLazy = new getFullReviewsUtilDMX(getDmx());
                            response = dmxLazy.getReviewsWithLazyLoading(productId, page);
                        } else {
                            response = "Lỗi: Chức năng tải thêm đánh giá chưa được hỗ trợ cho nền tảng " + currentPlatform;
                        }
                    } else {
                        response = "ERROR:Invalid load more request format";
                    }
                }
                // Get rating info
                else if (decryptedRequest.startsWith("GET_RATING_INFO:")) {
                    String productUrl = decryptedRequest.substring(15).trim();
                    
                    if (productUrl.contains("dienmayxanh.com") || productUrl.contains("diemmayxanh.com")) {
                        response = getDmx().getProductRatingInfo(productUrl);
                    } else {
                        response = "Lỗi: URL không được hỗ trợ. Chỉ hỗ trợ URL từ dienmayxanh.com";
                    }
                }
                // Lazy load product
                else if (decryptedRequest.startsWith("LAZY_LOAD:")) {
                    String productName = decryptedRequest.substring(10);
                    
                    if (currentPlatform.equals("TIKI")) {
                        response = processEncryptedTikiRequest(productName);
                    } else if (currentPlatform.equals("ĐIỆN MÁY XANH")) {
                        response = processEncryptedDMXRequest(productName);
                    } else {
                        response = "Lỗi: Nền tảng " + currentPlatform + " không được hỗ trợ";
                    }
                }
                // Unknown request
                else {
                    response = "ERROR:Unknown request type";
                }
                
                // Encrypt the response
                CryptoUtils.EncryptedPackage encryptedResponse = CryptoUtils.encryptHybrid(response, clientKey);
                String encryptedResponseStr = encryptedResponse.toString();
                
                // Send the encrypted response
                writer.println(encryptedResponseStr);
                writer.println("<END>");
                
                EncryptionLogger.logCommunication(clientId, "Response", encryptedResponseStr.length(), response.length());
                
                // Skip further requests if this is a summarize connection
                if (isSummarizeConnection && !decryptedRequest.startsWith("SUMMARIZE:")) {
                    continue;
                }
                
            } catch (Exception e) {
                System.err.println("Error processing encrypted message: " + e.getMessage());
                EncryptionLogger.logError("Message processing", e.getMessage());
                writer.println("ERROR:Error processing encrypted message");
                writer.println("<END>");
            }
        }
    }
    
    /**
     * Process a Tiki product request
     */
    private String processEncryptedTikiRequest(String productName) {
        try {
            // Handle page loading requests
            if (productName.startsWith("LOAD_PAGE:")) {
                String[] parts = productName.substring(10).split(":");
                String productId = parts[0];
                int page = Integer.parseInt(parts[1]);
                return getTiki().getProductReviewsByPage(productId, page);
            }
            
            // Handle regular product search
            String productReviews = getTiki().getProductReviewsFirstPage(productName);
            
            if (productReviews.contains("Không tìm thấy sản phẩm")) {
                // Try suggestions
                return "Không tìm thấy sản phẩm nào cho từ khóa: " + productName;
            }
            
            return productReviews;
        } catch (Exception e) {
            return "Lỗi: Không thể tìm kiếm sản phẩm - " + e.getMessage();
        }
    }
    
    /**
     * Process a DMX product request
     */
    private String processEncryptedDMXRequest(String productName) {
        try {
            // Handle page loading requests
            if (productName.startsWith("LOAD_PAGE:")) {
                String[] parts = productName.substring(10).split(":");
                String productId = parts[0];
                int page = Integer.parseInt(parts[1]);
                return getDmx().getProductReviewsByPage(productId, page);
            }
            
            // Handle regular product search
            String productReviews = getDmx().getProductReviewsFirstPage(productName);
            
            if (productReviews.contains("Không tìm thấy sản phẩm")) {
                // Try suggestions
                return "Không tìm thấy sản phẩm nào cho từ khóa: " + productName;
            }
            
            return productReviews;
        } catch (Exception e) {
            return "Lỗi: Không thể lấy đánh giá từ Điện Máy Xanh - " + e.getMessage();
        }
    }
    
    /**
     * Main method to start the encrypted server
     */
    public static void main(String[] args) {
        int port = 1234;
        EncryptedServer server = new EncryptedServer(port);
        server.start();
    }
} 