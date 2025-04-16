package org.Server;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 * Class kiểm tra và ghi log khi tìm kiếm trên Điện Máy Xanh
 */
public class DMXLogSearchTest {
    public static void main(String[] args) {
        String logFilePath = "dmx_search_log.txt";
        PrintWriter logWriter = null;
        
        try {
            // Tạo writer để ghi log
            logWriter = new PrintWriter(new FileWriter(logFilePath, true));
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String header = "===== ĐANG KIỂM TRA TÌM KIẾM ĐIỆN MÁY XANH - " + dateFormat.format(new Date()) + " =====";
            System.out.println(header);
            logWriter.println(header);
            
            // Tạo thư mục logs nếu chưa tồn tại
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdir();
            }
            
            // Nhận từ khóa tìm kiếm từ người dùng hoặc sử dụng mặc định
            String keyword = "panasonic"; // Mặc định
            System.out.print("Nhập từ khóa tìm kiếm (nhấn Enter để dùng 'panasonic'): ");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                keyword = input;
            }
            
            String info = "Tìm kiếm với từ khóa: " + keyword;
            System.out.println(info);
            logWriter.println(info);
            
            // Khởi tạo đối tượng để tìm kiếm
            System.out.println("Khởi tạo đối tượng getReviewDMXProduct...");
            getReviewDMXProduct dmxReviewer = new getReviewDMXProduct();
            logWriter.println("Đã khởi tạo đối tượng getReviewDMXProduct");
            
            // Ghi log kết quả tìm kiếm và lấy đánh giá trang đầu tiên
            System.out.println("Đang thực hiện tìm kiếm...");
            long startTime = System.currentTimeMillis();
            String searchResult = dmxReviewer.getProductReviewsWithPagination(keyword, 1);
            long endTime = System.currentTimeMillis();
            
            // Lưu kết quả vào file riêng
            String resultFileName = "logs/dmx_result_" + keyword + "_" + System.currentTimeMillis() + ".txt";
            FileWriter resultWriter = new FileWriter(resultFileName);
            resultWriter.write(searchResult);
            resultWriter.close();
            
            logWriter.println("Thời gian tìm kiếm: " + (endTime - startTime) + "ms");
            logWriter.println("Đã lưu kết quả tìm kiếm vào file: " + resultFileName);
            
            // Hiển thị thông báo kết quả
            System.out.println("Thời gian tìm kiếm: " + (endTime - startTime) + "ms");
            
            // Xác định nếu tìm kiếm thành công
            boolean searchSuccess = searchResult != null && 
                                  !searchResult.startsWith("Không") && 
                                  !searchResult.startsWith("Lỗi") &&
                                  !searchResult.contains("\"status\":\"suggestions\"");
            
            if (searchSuccess) {
                System.out.println("Tìm kiếm thành công! Kết quả đã được lưu trong file: " + resultFileName);
                
                // Trích xuất URL sản phẩm từ kết quả
                String[] lines = searchResult.split("\n");
                String productUrl = null;
                String productName = null;
                
                // Tìm thông tin sản phẩm
                for (String line : lines) {
                    if (line.startsWith("PRODUCT_URL:")) {
                        productUrl = line.substring("PRODUCT_URL:".length());
                    }
                    if (line.startsWith("PRODUCT_NAME:")) {
                        productName = line.substring("PRODUCT_NAME:".length());
                    }
                    if (productUrl != null && productName != null) break;
                }
                
                if (productName != null) {
                    System.out.println("Sản phẩm tìm thấy: " + productName);
                    logWriter.println("Sản phẩm tìm thấy: " + productName);
                }
            } else {
                System.out.println("Tìm kiếm không trả về sản phẩm cụ thể. Vui lòng kiểm tra file log để biết thêm chi tiết.");
                if (searchResult.contains("\"status\":\"suggestions\"")) {
                    System.out.println("Đã tìm thấy một số gợi ý sản phẩm, xem chi tiết trong file: " + resultFileName);
                }
            }
            
            // Đóng và giải phóng tài nguyên
            System.out.println("Đang dọn dẹp tài nguyên...");
            dmxReviewer.cleanup();
            
            String footer = "===== KẾT THÚC KIỂM TRA =====\n";
            System.out.println(footer);
            logWriter.println(footer);
            
        } catch (Exception e) {
            if (logWriter != null) {
                logWriter.println("LỖI: " + e.getMessage());
                e.printStackTrace(logWriter);
            }
            System.err.println("Lỗi khi thực hiện kiểm tra: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (logWriter != null) {
                logWriter.close();
            }
            
            System.out.println("\nĐã hoàn thành kiểm tra. Kiểm tra file dmx_search_log.txt để xem log đầy đủ.");
            System.out.println("Các file HTML và screenshot đã được lưu để phân tích khi cần thiết.");
        }
    }
}