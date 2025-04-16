package Client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Lớp hỗ trợ ghi log các phản hồi từ server để debug
 */
public class ResponseLogger {
    private static final String LOG_FOLDER = "response_logs";
    private static boolean initialized = false;

    /**
     * Khởi tạo thư mục log nếu chưa tồn tại
     */
    public static void init() {
        // Tạo thư mục logs nếu chưa tồn tại
        File logDir = new File(LOG_FOLDER);
        if (!logDir.exists()) {
            if (logDir.mkdirs()) {
                System.out.println("Đã tạo thư mục logs: " + logDir.getAbsolutePath());
            } else {
                System.err.println("Không thể tạo thư mục logs");
            }
        }
        initialized = true;
    }
    
    /**
     * Ghi nội dung phản hồi vào file log
     * 
     * @param platform Nền tảng (TIKI, ĐIỆN MÁY XANH, etc)
     * @param keyword Từ khóa tìm kiếm
     * @param response Nội dung phản hồi
     */
    public static void logResponse(String platform, String keyword, String response) {
        if (!initialized) {
            init();
        }
        
        try {
            // Tạo tên file log
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = dateFormat.format(new Date());
            String sanitizedKeyword = keyword.replaceAll("[^a-zA-Z0-9\\-]", "_");
            String logFileName = LOG_FOLDER + "/" + platform + "_" + sanitizedKeyword + "_" + timestamp + ".log";
            
            // Ghi log
            File logFile = new File(logFileName);
            try (FileWriter writer = new FileWriter(logFile)) {
                writer.write("Nền tảng: " + platform + "\n");
                writer.write("Từ khóa: " + keyword + "\n");
                writer.write("Thời gian: " + new Date() + "\n");
                writer.write("========== PHẢN HỒI ==========\n");
                writer.write(response);
                writer.write("\n========== KẾT THÚC ==========\n");
            }
            
            System.out.println("Đã ghi log phản hồi vào file: " + logFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Lỗi khi ghi log phản hồi: " + e.getMessage());
        }
    }
    
    /**
     * Ghi log lỗi khi phân tích phản hồi
     * 
     * @param platform Nền tảng
     * @param keyword Từ khóa tìm kiếm
     * @param response Phản hồi gốc
     * @param error Thông báo lỗi
     */
    public static void logError(String platform, String keyword, String response, String error) {
        if (!initialized) {
            init();
        }
        
        try {
            // Tạo tên file log
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = dateFormat.format(new Date());
            String sanitizedKeyword = keyword.replaceAll("[^a-zA-Z0-9\\-]", "_");
            String logFileName = LOG_FOLDER + "/ERROR_" + platform + "_" + sanitizedKeyword + "_" + timestamp + ".log";
            
            // Ghi log
            File logFile = new File(logFileName);
            try (FileWriter writer = new FileWriter(logFile)) {
                writer.write("LỖI PHÂN TÍCH PHẢN HỒI\n");
                writer.write("Nền tảng: " + platform + "\n");
                writer.write("Từ khóa: " + keyword + "\n");
                writer.write("Thời gian: " + new Date() + "\n");
                writer.write("Thông báo lỗi: " + error + "\n");
                writer.write("========== PHẢN HỒI GỐC ==========\n");
                writer.write(response);
                writer.write("\n========== KẾT THÚC ==========\n");
            }
            
            System.out.println("Đã ghi log lỗi vào file: " + logFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Lỗi khi ghi log lỗi: " + e.getMessage());
        }
    }
}