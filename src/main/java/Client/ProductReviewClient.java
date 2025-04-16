package Client;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.json.JSONObject;

import java.io.IOException;
/**
 * Client for the Product Review application
 * Handles communication with the server and processes responses
 */
public class ProductReviewClient {
    private String serverHost;
    private int serverPort;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String currentPlatform = "TIKI"; // Default platform

    /**
     * Constructor
     *
     * @param serverHost Server hostname or IP
     * @param serverPort Server port
     */
    public ProductReviewClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    /**
     * Search for product reviews
     *
     * @param productName The product name to search for
     * @return List of reviews or suggestions
     */
    public List<String> searchProduct(String productName) {
        List<String> results = new ArrayList<>();

        try {
            // Connect to server if not already connected
            if (!isConnected()) {
                if (!connect()) {
                    results.add("Không thể kết nối đến máy chủ.");
                    return results;
                }
            }

            // Send product name to server with lazy loading marker
            writer.println("LAZY_LOAD:" + productName);

            // Process response - READ ALL DATA in one single response
            StringBuilder fullResponse = new StringBuilder();
            String response;
            while ((response = reader.readLine()) != null) {
                if (response.equals("<END>")) {
                    break;
                }
                fullResponse.append(response).append("\n");
            }

            // Only add the full complete response if it's not empty
            if (fullResponse.length() > 0) {
                results.add(fullResponse.toString().trim());
            }
        } catch (IOException e) {
            results.add("Lỗi khi tìm kiếm sản phẩm: " + e.getMessage());
        }

        return results;
    }

    /**
     * Change the current platform for product reviews
     *
     * @param platform The platform to change to (TIKI, ĐIỆN MÁY XANH, SENDO, AMAZON)
     * @return The server's response
     */
    public String changePlatform(String platform) {
        try {
            if (!isConnected()) {
                if (!connect()) {
                    return "Không thể kết nối đến máy chủ.";
                }
            }

            // Send platform change request to server
            String request = "PLATFORM:" + platform;
            writer.println(request);

            // Read response
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("<END>")) {
                    break;
                }
                response.append(line).append("\n");
            }

            // Update current platform if successful
            if (response.toString().contains("Đã chuyển sang nền tảng")) {
                currentPlatform = platform;
            }

            return response.toString().trim();
        } catch (IOException e) {
            return "Lỗi khi chuyển đổi nền tảng: " + e.getMessage();
        }
    }

    /**
     * Get the current platform
     *
     * @return The current platform name
     */
    public String getCurrentPlatform() {
        return currentPlatform;
    }

    /**
     * Connect to the server
     *
     * @return true if connection successful, false otherwise
     */
    public boolean connect() {
        try {
            socket = new Socket(serverHost, serverPort);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            return true;
        } catch (IOException e) {
            System.err.println("Lỗi kết nối đến server: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send a request to the server
     *
     * @param request The request to send
     * @return The server's response
     * @throws IOException If an I/O error occurs
     */
    public String sendRequest(String request) throws IOException {
        writer.println(request);
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals("<END>")) {
                break;
            }
            response.append(line).append("\n");
        }
        return response.toString().trim();
    }
    
    /**
     * Tải trang tiếp theo của đánh giá sản phẩm
     * @param productId Mã sản phẩm
     * @param page Số trang cần tải
     * @return Danh sách đánh giá ở trang được chỉ định
     */
    public String loadMoreReviews(String productId, int page) {
        try {
            if (!isConnected()) {
                if (!connect()) {
                    return "Không thể kết nối đến máy chủ.";
                }
            }
            
            // Gửi yêu cầu tải thêm đánh giá với định dạng: LOAD_MORE:productId:page
            String request = "LOAD_MORE:" + productId + ":" + page;
            return sendRequest(request);
        } catch (IOException e) {
            return "Lỗi khi tải thêm đánh giá: " + e.getMessage();
        }
    }
    
    /**
     * Yêu cầu tổng hợp đánh giá sản phẩm bằng AI
     * @param productId Mã sản phẩm
     * @param reviews Chuỗi chứa nội dung các đánh giá
     * @return Kết quả tổng hợp
     */
    public String summarizeReviews(String productId, String reviews) {
        try {
            if (!isConnected()) {
                if (!connect()) {
                    return "Không thể kết nối đến máy chủ.";
                }
            }
            
            // Gửi yêu cầu tổng hợp đánh giá
            String request = "SUMMARIZE:" + productId + ":" + reviews;
            return sendRequest(request);
        } catch (IOException e) {
            return "Lỗi khi tổng hợp đánh giá: " + e.getMessage();
        }
    }
    
    /**
     * Lấy thông tin đánh giá (rating) chính xác từ trang sản phẩm Điện Máy Xanh
     * @param productUrl URL của sản phẩm
     * @return Chuỗi định dạng "average_rating:X.X|total_reviews:Y" hoặc thông báo lỗi
     */
    public String getProductRatingInfo(String productUrl) {
        try {
            if (!isConnected()) {
                if (!connect()) {
                    return "Không thể kết nối đến máy chủ.";
                }
            }
            
            // Gửi yêu cầu lấy thông tin đánh giá với định dạng: GET_RATING_INFO:productUrl
            String request = "GET_RATING_INFO:" + productUrl;
            return sendRequest(request);
        } catch (IOException e) {
            return "Lỗi khi lấy thông tin đánh giá: " + e.getMessage();
        }
    }

    /**
     * Lấy đánh giá theo trang (phương thức sử dụng từ GUI)
     * @param productId Mã sản phẩm
     * @param page Số trang cần tải
     * @return Danh sách đánh giá ở trang được chỉ định
     */
    public List<String> getReviewsByPage(String productId, int page) {
        List<String> results = new ArrayList<>();
        
        try {
            // Connect to server if not already connected
            if (!isConnected()) {
                if (!connect()) {
                    results.add("Không thể kết nối đến máy chủ.");
                    return results;
                }
            }
            
            // Send request for reviews by page
            String request = "LOAD_MORE:" + productId + ":" + page;
            writer.println(request);
            
            // Process response - READ ALL DATA in one single response
            StringBuilder fullResponse = new StringBuilder();
            String response;
            while ((response = reader.readLine()) != null) {
                if (response.equals("<END>")) {
                    break;
                }
                fullResponse.append(response).append("\n");
            }
            
            // Only add the full complete response if it's not empty
            if (fullResponse.length() > 0) {
                results.add(fullResponse.toString().trim());
            }
        } catch (IOException e) {
            results.add("Lỗi khi tải đánh giá trang " + page + ": " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Close the connection to the server
     */
    public void close() {
        try {
            if (writer != null) {
                writer.println("bye");
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }

    /**
     * Check if connected to the server
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }





}