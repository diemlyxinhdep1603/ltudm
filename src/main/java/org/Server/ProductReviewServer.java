package org.Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ProductReviewServer {
    private int port;
    private getReviewTIKIProduct tiki;
    private getReviewSendoProduct sendo;
    private getReviewAmazonProduct amazon;
    private String currentPlatform;
    private static final int N_THREADS = 10; // Number of threads in the pool
    private ExecutorService executorService;

    // Constructor
    public ProductReviewServer(int port) {
        this.port = port;
        this.tiki = new getReviewTIKIProduct();
        this.sendo = new getReviewSendoProduct();
        this.amazon = new getReviewAmazonProduct();
        this.currentPlatform = "TIKI";
        this.executorService = Executors.newFixedThreadPool(N_THREADS);
    }

    // Start server and create socket for each client
    public void start() {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server đang lắng nghe tại cổng: " + port);

            // Shutdown hook to close ExecutorService on application exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Đang shutdown ExecutorService...");
                shutdownExecutor(executorService);
                System.out.println("ExecutorService đã shutdown.");
            }));

            while (!executorService.isShutdown()) {
                try {
                    Socket socket = server.accept();
                    System.out.println("Client mới đã kết nối: " + socket.getRemoteSocketAddress());
                    executorService.execute(() -> handleClient(socket));
                } catch (IOException e) {
                    if (executorService.isShutdown()) {
                        System.out.println("Server socket đã đóng do executor shutdown.");
                        break;
                    }
                    System.err.println("Lỗi khi chấp nhận kết nối client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Lỗi khởi tạo máy chủ: " + e.getMessage());
            shutdownExecutor(executorService);
        } finally {
            if (!executorService.isTerminated()) {
                System.out.println("Thực hiện shutdown cuối cùng cho ExecutorService...");
                shutdownExecutor(executorService);
            }
            System.out.println("Server đã dừng.");
        }
    }

    // Handle client connection
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
            case "SENDO":
                return processSendoProductRequest(productName);
            case "AMAZON":
                return processAmazonProductRequest(productName);
            default:
                return "Lỗi: Nền tảng không được hỗ trợ: " + currentPlatform;
        }
    }

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

    private String processSendoProductRequest(String productName) {
        return "Lỗi: Đang phát triển chức năng tìm kiếm trên SENDO";
    }

    private String processAmazonProductRequest(String productName) {
        return "Lỗi: Đang phát triển chức năng tìm kiếm trên AMAZON";
    }

    private static void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("ExecutorService không thể dừng hẳn.");
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        ProductReviewServer server = new ProductReviewServer(1234);
        server.start();
    }
}