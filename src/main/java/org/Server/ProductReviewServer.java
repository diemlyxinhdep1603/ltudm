package org.Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class ProductReviewServer {
    private int port;
    private getReviewTIKIProduct tiki;  // Object to handle TIKI product reviews
    private getReviewSendoProduct sendo; // Object to handle SENDO product reviews
    private getReviewAmazonProduct amazon; // Object to handle AMAZON product reviews
    private String currentPlatform; // Track which platform to use for reviews

    // Constructor
    public ProductReviewServer(int port) {
        this.port = port;
        this.tiki = new getReviewTIKIProduct();  // Initialize the helper classes
        this.sendo = new getReviewSendoProduct();
        this.amazon = new getReviewAmazonProduct();
        this.currentPlatform = "TIKI"; // Default platform
    }

    // Start server and create socket for each client
    public void start() {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server đang lắng nghe tại cổng: " + port);
            while (true) {
                Socket socket = server.accept();
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("Lỗi khởi tạo máy chủ: " + e.getMessage());
        }
    }

    // Xử lý kết nối của khách hàng
    private void handleClient(Socket socket) {
        System.out.println("Kết nối từ khách hàng: " + socket.getRemoteSocketAddress());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {
            String request;
            while ((request = reader.readLine()) != null) {
                System.out.println("Server nhận: " + request);
                if (request.equalsIgnoreCase("bye")) {
                    System.out.println("Khách hàng yêu cầu đóng kết nối.");
                    break;
                }
                
                // Check if the request is changing platforms
                if (request.startsWith("PLATFORM:")) {
                    String platform = request.substring(9).trim();
                    
                    // Kiểm tra tính hợp lệ của platform
                    if (Valid_Input_Data.isValidPlatform(platform)) {
                        currentPlatform = platform;
                        writer.println("Đã chuyển sang nền tảng: " + currentPlatform);
                        writer.println("<END>");
                        continue;
                    } else {
                        // Lấy thông báo lỗi chi tiết
                        String errorMessage = Valid_Input_Data.getLastErrorMessage();
                        if (errorMessage.isEmpty()) {
                            errorMessage = "Nền tảng không hợp lệ. Chỉ chấp nhận TIKI, SENDO, AMAZON";
                        }
                        writer.println("Lỗi: " + errorMessage);
                        writer.println("<END>");
                        continue;
                    }
                }
                
                // Process client request for product search
                // Kiểm tra tính hợp lệ của từ khóa tìm kiếm
                if (!Valid_Input_Data.isValidKeyword(request)) {
                    // Lấy thông báo lỗi chi tiết
                    String errorMessage = Valid_Input_Data.getLastErrorMessage();
                    if (errorMessage.isEmpty()) {
                        errorMessage = "Từ khóa tìm kiếm không hợp lệ";
                    }
                    writer.println("Lỗi: " + errorMessage);
                    writer.println("<END>");
                    continue;
                }
                
                String response = processProductRequest(request);
                writer.println(response);
                writer.println("<END>"); // Thông báo kết thúc gửi dữ liệu
            }
        } catch (IOException e) {
            System.err.println("Lỗi xử lý kết nối khách hàng: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Lỗi đóng kết nối: " + e.getMessage());
            }
        }
    }

    // Xử lý yêu cầu tìm kiếm sản phẩm dựa trên platform hiện tại
    private String processProductRequest(String productName) {
        switch (currentPlatform) {
            case "TIKI":
                return processTikiProductRequest(productName);
            case "SENDO":
                return processSendoProductRequest(productName);
            case "AMAZON":
                return processAmazonProductRequest(productName);
            default:
                return "Lỗi: Nền tảng không được hỗ trợ: " + currentPlatform;
        }
    }
    
    // Xử lý yêu cầu tìm kiếm sản phẩm trên Tiki
    private String processTikiProductRequest(String productName) {
        try {
            // Cố gắng lấy reviews dựa trên tên chính xác trước
            String productReviews = tiki.getProductReviews(productName);
            
            // Nếu không tìm thấy sản phẩm nào, thử lấy gợi ý
            if (productReviews.contains("Không tìm thấy sản phẩm")) {
                Map<String, String> suggestions = tiki.getSuggestedProducts(productName);
                if (!suggestions.isEmpty()) {
                    // Nếu có gợi ý, lấy gợi ý đầu tiên và xử lý
                    String firstSuggestion = suggestions.keySet().iterator().next();
                    System.out.println("Sử dụng gợi ý: " + firstSuggestion);
                    return tiki.getProductReviewsFromSuggestion(firstSuggestion);
                } else {
                    return "Không tìm thấy sản phẩm nào cho từ khóa: " + productName;
                }
            }
            
            return productReviews;
        } catch (Exception e) {
            System.err.println("Lỗi tìm kiếm sản phẩm: " + e.getMessage());
            // Trả về lỗi với định dạng 'Lỗi:' để GUI có thể nhận diện
            return "Lỗi: Không thể tìm kiếm sản phẩm - " + e.getMessage();
        }
    }
    
    // Xử lý yêu cầu tìm kiếm sản phẩm trên Sendo
    private String processSendoProductRequest(String productName) {
        // Hiện tại chưa triển khai, trả về thông báo
        return "Lỗi: Đang phát triển chức năng tìm kiếm trên SENDO";
    }
    
    // Xử lý yêu cầu tìm kiếm sản phẩm trên Amazon
    private String processAmazonProductRequest(String productName) {
        // Hiện tại chưa triển khai, trả về thông báo
        return "Lỗi: Đang phát triển chức năng tìm kiếm trên AMAZON";
    }

    public static void main(String[] args) {
        ProductReviewServer server = new ProductReviewServer(1234);
        server.start();
    }
}
