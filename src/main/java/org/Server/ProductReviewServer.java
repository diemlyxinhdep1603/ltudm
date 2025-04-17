package org.Server;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.*;
import java.net.*;
import java.util.Map;

import org.Server.AIReviewSummarizer;

public class ProductReviewServer {
    private int port;
    private int udpPort = 9876; // Port for UDP broadcasting
    private getReviewTIKIProduct tiki;
    private getReviewDMXProduct dmx;
    private String currentPlatform;

    public ProductReviewServer(int port) {
        this.port = port;
        this.tiki = new getReviewTIKIProduct();
        this.dmx = new getReviewDMXProduct();
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
            // Thêm biến để theo dõi trạng thái kết nối
            boolean isSummarizeConnection = false;
            
            while ((request = reader.readLine()) != null) {
                System.out.println("Server nhận: " + request);
                
                // Kiểm tra nếu là yêu cầu đóng kết nối
                if (request.equalsIgnoreCase("bye")) {
                    System.out.println("Khách hàng yêu cầu đóng kết nối.");
                    break;
                }

                // Kiểm tra nếu là kết nối tổng hợp - đánh dấu toàn bộ kết nối này dành cho tổng hợp
                if (request.startsWith("SUMMARIZE:")) {
                    String[] parts = request.substring(10).split(":", 2);
                    if (parts.length == 2) {
                        String productId = parts[0];
                        String reviewsContent = parts[1];
                        System.out.println("Yêu cầu tổng hợp đánh giá cho sản phẩm " + productId);
                        
                        // Gọi AI để tổng hợp đánh giá
                        AIReviewSummarizer summarizer = new AIReviewSummarizer();
                        String summary = summarizer.summarizeReviews(reviewsContent);
                        
                        writer.println(summary);
                        writer.println("<END>");
                        
                        // Đánh dấu đây là kết nối tổng hợp để bỏ qua các yêu cầu tiếp theo
                        isSummarizeConnection = true;
                        continue;
                    }
                }
                
                // Nếu đã được đánh dấu là kết nối tổng hợp, bỏ qua tất cả các yêu cầu tiếp theo 
                // ngoại trừ "bye" (đã xử lý ở trên)
                if (isSummarizeConnection) {
                    continue;
                }

                if (request.startsWith("PLATFORM:")) {
                    String platform = request.substring(9).trim();
                    // Kiểm tra và chấp nhận "ĐIỆN MÁY XANH" như một nền tảng hợp lệ
                    if (platform.equals("ĐIỆN MÁY XANH") || Valid_Input_Data.isValidPlatform(platform)) {
                        currentPlatform = platform;
                        writer.println("Đã chuyển sang nền tảng: " + currentPlatform);
                        writer.println("<END>");
                        continue;
                    } else {
                        String errorMessage = Valid_Input_Data.getLastErrorMessage();
                        if (errorMessage.isEmpty()) {
                            errorMessage = "Nền tảng không hợp lệ. Chỉ chấp nhận TIKI, ĐIỆN MÁY XANH, SENDO, AMAZON";
                        }
                        writer.println("Lỗi: " + errorMessage);
                        writer.println("<END>");
                        continue;
                    }
                }
                
                // Xử lý yêu cầu tải thêm đánh giá
                if (request.startsWith("LOAD_MORE:")) {
                    String[] parts = request.substring(10).split(":");
                    if (parts.length == 2) {
                        String productId = parts[0];
                        int page = Integer.parseInt(parts[1]);
                        System.out.println("Yêu cầu tải trang " + page + " của sản phẩm " + productId);
                        
                        String response;
                        if (currentPlatform.equals("TIKI")) {
                            response = tiki.getProductReviewsWithPagination(productId, page);
                        } else if (currentPlatform.equals("ĐIỆN MÁY XANH")) {
                            // Sử dụng phương thức lazy loading mới từ class getFullReviewsUtilDMX
                            getFullReviewsUtilDMX dmxLazy = new getFullReviewsUtilDMX(dmx);
                            response = dmxLazy.getReviewsWithLazyLoading(productId, page);
                        } else {
                            response = "Lỗi: Chức năng tải thêm đánh giá chưa được hỗ trợ cho nền tảng " + currentPlatform;
                        }
                        
                        writer.println(response);
                        writer.println("<END>");
                        continue;
                    }
                }
                
                // Xử lý yêu cầu lấy thông tin đánh giá chính xác từ trang sản phẩm Điện Máy Xanh
                if (request.startsWith("GET_RATING_INFO:")) {
                    String productUrl = request.substring(15).trim();
                    System.out.println("Yêu cầu lấy thông tin đánh giá từ URL: " + productUrl);
                    
                    try {
                        // Chỉ hỗ trợ cho Điện Máy Xanh
                        if (productUrl.contains("dienmayxanh.com") || productUrl.contains("diemmayxanh.com")) {
                            String ratingInfo = dmx.getProductRatingInfo(productUrl);
                            System.out.println("Đã lấy được thông tin đánh giá: " + ratingInfo);
                            writer.println(ratingInfo);
                        } else {
                            writer.println("Lỗi: URL không được hỗ trợ. Chỉ hỗ trợ URL từ dienmayxanh.com");
                        }
                    } catch (Exception e) {
                        System.err.println("Lỗi khi lấy thông tin đánh giá: " + e.getMessage());
                        writer.println("Lỗi: " + e.getMessage());
                    }
                    
                    writer.println("<END>");
                    continue;
                }
                
                // Xử lý yêu cầu lazy loading
                if (request.startsWith("LAZY_LOAD:")) {
                    String productName = request.substring(10).trim();
                    if (!Valid_Input_Data.isValidKeyword(productName)) {
                        String errorMessage = Valid_Input_Data.getLastErrorMessage();
                        if (errorMessage.isEmpty()) {
                            errorMessage = "Từ khóa tìm kiếm không hợp lệ";
                        }
                        writer.println("Lỗi: " + errorMessage);
                        writer.println("<END>");
                        continue;
                    }
                    
                    String response = processProductRequest(productName);
                    writer.println(response);
                    writer.println("<END>");
                    continue;
                }

                // Trường hợp còn lại, kiểm tra từ khóa tìm kiếm và xử lý như một yêu cầu tìm kiếm thông thường
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

    private String processTikiProductRequest(String productName) {
        try {
            // Kiểm tra nếu yêu cầu là để tải trang bình luận cụ thể
            if (productName.startsWith("LOAD_PAGE:")) {
                String[] parts = productName.substring(10).split(":");
                String productId = parts[0];
                int page = Integer.parseInt(parts[1]);
                return tiki.getProductReviewsByPage(productId, page);
            }
            
            // Yêu cầu thông thường - chỉ trả về thông tin sản phẩm và trang đầu tiên của bình luận
            String productReviews = tiki.getProductReviewsFirstPage(productName);
            
            if (productReviews.contains("Không tìm thấy sản phẩm")) {
                Map<String, String> suggestions = tiki.getSuggestedProducts(productName);
                if (!suggestions.isEmpty()) {
                    String firstSuggestion = suggestions.keySet().iterator().next();
                    System.out.println("Sử dụng gợi ý: " + firstSuggestion);
                    productReviews = tiki.getProductReviewsFirstPageFromSuggestion(firstSuggestion);
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

    private String processDMXProductRequest(String productName) {
        try {
            // Kiểm tra nếu yêu cầu là để tải trang bình luận cụ thể
            if (productName.startsWith("LOAD_PAGE:")) {
                String[] parts = productName.substring(10).split(":");
                String productId = parts[0];
                int page = Integer.parseInt(parts[1]);
                return dmx.getProductReviewsByPage(productId, page);
            }
            
            // Yêu cầu thông thường - chỉ trả về thông tin sản phẩm và trang đầu tiên của bình luận
            String productReviews = dmx.getProductReviewsFirstPage(productName);
            
            // Xử lý trường hợp không tìm thấy sản phẩm, giống như TIKI
            if (productReviews.contains("Không tìm thấy sản phẩm")) {
                Map<String, String> suggestions = dmx.getSuggestedProducts(productName);
                if (!suggestions.isEmpty()) {
                    String firstSuggestion = suggestions.keySet().iterator().next();
                    System.out.println("Sử dụng gợi ý từ Điện Máy Xanh: " + firstSuggestion);
                    // Gửi URL gợi ý để lấy đánh giá
                    String suggestionUrl = suggestions.get(firstSuggestion);
                    if (suggestionUrl != null && !suggestionUrl.isEmpty()) {
                        return dmx.getProductReviewsWithPagination(suggestionUrl, 1);
                    } else {
                        return "Không tìm thấy sản phẩm nào cho từ khóa: " + productName;
                    }
                } else {
                    return "Không tìm thấy sản phẩm nào cho từ khóa: " + productName;
                }
            }
            
            return productReviews;
        } catch (Exception e) {
            System.err.println("Lỗi xử lý yêu cầu Điện Máy Xanh: " + e.getMessage());
            e.printStackTrace();
            return "Lỗi: Không thể lấy đánh giá từ Điện Máy Xanh - " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        ProductReviewServer server = new ProductReviewServer(1234);
        server.start();
    }
}
