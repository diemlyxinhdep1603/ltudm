package org.Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class ProductReviewServer {
    private int port;
    private getReviewTIKIProduct tikiProductHelper;  // Object to handle TIKI product reviews

    // Constructor
    public ProductReviewServer(int port) {
        this.port = port;
        this.tikiProductHelper = new getReviewTIKIProduct();  // Initialize the helper class
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
        List<String> suggestions = tikiProductHelper.searchSimilarProducts(productName);

        if (suggestions.isEmpty()) {
            return "Không tìm thấy sản phầm nào cho'" + productName + "'. Gợi ý tên sản phẩm tương tự:\n" + String.join(", ", suggestions);
        } else {
            StringBuilder response = new StringBuilder();
            String firstProduct = suggestions.get(0);  // Get the first product from suggestions
            response.append("Product info:\n").append(tikiProductHelper.getProductInfo(firstProduct)).append("\n");
            response.append("Product reviews:\n").append(tikiProductHelper.getProductReviews(firstProduct));
            return response.toString();
        }
    }

    public static void main(String[] args) {
        ProductReviewServer server = new ProductReviewServer(1234);
        server.start();
    }
}
