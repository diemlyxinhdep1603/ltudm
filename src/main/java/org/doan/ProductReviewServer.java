package org.doan;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ProductReviewServer {
    private int port;

    // Constructor
    public ProductReviewServer(int port) {
        this.port = port;
    }

    // Khởi động server
    public void start() {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server đang lắng nghe tại port " + port);
            while (true) {
                Socket socket = server.accept();
                // Xử lý từng client trong thread riêng (hỗ trợ multi-client cơ bản)
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("Lỗi khởi tạo server socket: " + e.getMessage());
        }
    }

    // Xử lý yêu cầu từ client
    private void handleClient(Socket socket) {
        System.out.println("Đã chấp nhận kết nối từ client: " + socket.getRemoteSocketAddress());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {
            String productName;
            while ((productName = reader.readLine()) != null) {
                System.out.println("Server nhận tên sản phẩm: " + productName);
                if (productName.equalsIgnoreCase("bye")) {
                    System.out.println("Server nhận yêu cầu đóng kết nối từ client.");
                    break;
                }
                // Xử lý yêu cầu và trả về dữ liệu giả lập
                String response = processRequest(productName);
                writer.println(response);
                writer.println("<END>"); // Báo hiệu kết thúc phản hồi
            }
        } catch (IOException e) {
            System.err.println("Lỗi kết nối từ client: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Lỗi đóng socket: " + e.getMessage());
            }
        }
    }

    // Xử lý yêu cầu và trả về dữ liệu giả lập
    private String processRequest(String productName) {
        // Dữ liệu giả lập: điểm review, ý kiến, ảnh
        if (productName.equalsIgnoreCase("iPhone 14")) {
            return "Product: iPhone 14 | Tiki: 4.5/5, 'Good phone', img_url | Lazada: 4.7/5, 'Fast delivery', img_url | Shopee: 4.6/5, 'Worth the price', img_url";
        } else if (productName.equalsIgnoreCase("Samsung Galaxy S23")) {
            return "Product: Samsung Galaxy S23 | Tiki: 4.3/5, 'Nice design', img_url | Lazada: 4.8/5, 'Great camera', img_url | Shopee: 4.4/5, 'Battery life good', img_url";
        } else {
            // Trường hợp không tìm thấy sản phẩm
            return "Không tìm thấy sản phẩm '" + productName + "'. Gợi ý: iPhone 14, Samsung Galaxy S23";
        }
    }

    public static void main(String[] args) {
        ProductReviewServer server = new ProductReviewServer(12345);
        server.start();
    }
}