
package org.Server;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.*;
import java.net.*;
import java.util.Map;

public class ProductReviewServer {
    private int port;
    private int udpPort = 9876; // Port for UDP broadcasting
    private getReviewTIKIProduct tiki;
    private String currentPlatform;

    public ProductReviewServer(int port) {
        this.port = port;
        this.tiki = new getReviewTIKIProduct();
        this.currentPlatform = "TIKI";
    }
    public void push_IP() {
        try {
            // Tạo socket để lấy địa chỉ IP cục bộ
            Socket socket = new Socket("daotao.sgu.edu.vn", 80);
            String localIP = socket.getLocalAddress().toString().substring(1);
            System.out.println("Địa chỉ server : " + localIP);
            socket.close(); // Đóng socket sau khi sử dụng

            // URL API để cập nhật IP
            String api = "https://retoolapi.dev/9EKKBD/data/1";
            String jsonData = "{\"ip\":\"" + localIP + "\"}";

            // Thực hiện yêu cầu PUT để cập nhật IP
            Jsoup.connect(api)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .header("Content-Type", "application/json")
                    .requestBody(jsonData)
                    .method(Connection.Method.PUT)
                    .execute();

            System.out.println("Đã cập nhật địa chỉ IP thành công.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        //new Thread(this::startUDPListener).start(); // Start UDP listener in a new thread
        //
        push_IP();
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

    private void startUDPListener() {
        try (DatagramSocket udpSocket = new DatagramSocket(udpPort)) {
            byte[] buffer = new byte[256];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                if (received.equals("DISCOVER_SERVER_REQUEST")) {
                    InetAddress clientAddress = packet.getAddress();
                    int clientPort = packet.getPort();
                    String response = "DISCOVER_SERVER_RESPONSE:" + port;
                    byte[] responseData = response.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddress, clientPort);
                    udpSocket.send(responsePacket);
                }
            }
        } catch (IOException e) {
            System.err.println("Lỗi UDP: " + e.getMessage());
        }
    }

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

                if (request.startsWith("PLATFORM:")) {
                    String platform = request.substring(9).trim();
                    if (Valid_Input_Data.isValidPlatform(platform)) {
                        currentPlatform = platform;
                        writer.println("Đã chuyển sang nền tảng: " + currentPlatform);
                        writer.println("<END>");
                        continue;
                    } else {
                        String errorMessage = Valid_Input_Data.getLastErrorMessage();
                        if (errorMessage.isEmpty()) {
                            errorMessage = "Nền tảng không hợp lệ. Chỉ chấp nhận TIKI, SENDO, AMAZON";
                        }
                        writer.println("Lỗi: " + errorMessage);
                        writer.println("<END>");
                        continue;
                    }
                }

                if (!Valid_Input_Data.isValidKeyword(request)) {
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
                writer.println("<END>");
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

    private String processProductRequest(String productName) {
        switch (currentPlatform) {
            case "TIKI":
                return processTikiProductRequest(productName);
            case "ĐIỆN MÁY XANH":
                return processDMXProductRequest(productName);
            default:
                return "Lỗi: Nền tảng không được hỗ trợ: " + currentPlatform;
        }
    }

    /*
    private String processTikiProductRequest(String productName) {
        try {
            String productReviews = tiki.getProductReviews(productName);
            if (productReviews.contains("Không tìm thấy sản phẩm")) {
                Map<String, String> suggestions = tiki.getSuggestedProducts(productName);
                if (!suggestions.isEmpty()) {
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
            return "Lỗi: Không thể tìm kiếm sản phẩm - " + e.getMessage();
        }
    }

     */
    private String processTikiProductRequest(String productName) {
        try {
            String productReviews = tiki.getProductReviews(productName);
            String summarizedReview = tiki.summarizeReviewsWithAI(productName);

            if (productReviews.contains("Không tìm thấy sản phẩm")) {
                Map<String, String> suggestions = tiki.getSuggestedProducts(productName);
                if (!suggestions.isEmpty()) {
                    String firstSuggestion = suggestions.keySet().iterator().next();
                    System.out.println("Sử dụng gợi ý: " + firstSuggestion);
                    productReviews = tiki.getProductReviewsFromSuggestion(firstSuggestion);
                    summarizedReview = tiki.summarizeReviewsWithAI(firstSuggestion);
                } else {
                    return "Không tìm thấy sản phẩm nào cho từ khóa: " + productName;
                }
            }

            // Kiểm tra và kết hợp kết quả
            StringBuilder response = new StringBuilder(productReviews);
            if (!summarizedReview.isEmpty() && !summarizedReview.contains("Lỗi") &&
                    !summarizedReview.equals("Không có đánh giá nào để tổng hợp.")) {
                response.append("\n\nTổng hợp đánh giá:\n").append(summarizedReview);
            } else {
                response.append("\n\nTổng hợp đánh giá: Không có tóm tắt đánh giá do thiếu dữ liệu hoặc lỗi.");
            }

            return response.toString();
        } catch (Exception e) {
            System.err.println("Lỗi tìm kiếm sản phẩm: " + e.getMessage());
            return "Lỗi: Không thể tìm kiếm sản phẩm - " + e.getMessage();
        }
    }


    private String processDMXProductRequest(String productName) {
        return "Lỗi: Đang phát triển chức năng tìm kiếm trên SENDO";
    }



    public static void main(String[] args) {
        ProductReviewServer server = new ProductReviewServer(1234);
        server.start();
    }
}
