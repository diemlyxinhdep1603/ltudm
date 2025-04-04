package org.Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class ProductReviewServer {
    private int port;
    private getReviewTIKIProduct tiki;  // Object to handle TIKI product reviews

    // Constructor
    public ProductReviewServer(int port) {
        this.port = port;
        this.tiki = new getReviewTIKIProduct();  // Initialize the helper class
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
            String productName;
            while ((productName = reader.readLine()) != null) {
                System.out.println("Server nhận: " + productName);
                if (productName.equalsIgnoreCase("bye")) {
                    System.out.println("Khách hàng yêu cầu đóng kết nối.");
                    break;
                }
                // Process client request
                String response = processProductRequest(productName);
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

    // Xử lý yêu cầu tìm kiếm sản phẩm
    private String processProductRequest(String productName) {
        // Try to get the reviews based on the exact name first
        String productReviews = tiki.getProductReviews(productName);

        // If no reviews found, return suggestions and check reviews of suggested products
        if (productReviews.contains("Không tìm thấy sản phẩm")) {
            Map<String, String> suggestions = tiki.getSuggestedProducts(productName);
            if (!suggestions.isEmpty()) {
                StringBuilder suggestionMessage = new StringBuilder("Không tìm thấy sản phẩm. Các gợi ý gần đúng:\n");
                for (Map.Entry<String, String> entry : suggestions.entrySet()) {
                    suggestionMessage.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");

                    // Now check reviews for the suggested product
                    String suggestionReviews = tiki.getProductReviewsFromSuggestion(entry.getValue());
                    suggestionMessage.append("Đánh giá cho ").append(entry.getKey()).append(":\n").append(suggestionReviews).append("\n");
                }
                return suggestionMessage.toString();
            } else {
                return "Không tìm thấy sản phẩm và cũng không có gợi ý nào.";
            }
        }

        return "Sản phẩm tìm thấy: " + productReviews;
    }

    public static void main(String[] args) {
        ProductReviewServer server = new ProductReviewServer(1234);
        server.start();
    }
}
