package org.Server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Lớp tiện ích để ghi log cho quá trình tìm kiếm Điện Máy Xanh
 */
public class DMXSearchLogger {
    private static final String LOG_FILE = "dmx_search_log.txt";
    private static PrintWriter logWriter = null;
    private static boolean isInitialized = false;
    
    /**
     * Khởi tạo logger
     */
    public static synchronized void init() {
        if (!isInitialized) {
            try {
                logWriter = new PrintWriter(new FileWriter(LOG_FILE, true));
                isInitialized = true;
                
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                logWriter.println("\n===== BẮT ĐẦU GHI LOG - " + dateFormat.format(new Date()) + " =====");
                logWriter.flush();
            } catch (IOException e) {
                System.err.println("Không thể khởi tạo logger: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Ghi log
     * 
     * @param message Thông điệp cần ghi
     */
    public static synchronized void log(String message) {
        if (!isInitialized) {
            init();
        }
        
        if (logWriter != null) {
            logWriter.println(message);
            logWriter.flush();
            
            // In ra console để dễ theo dõi
            System.out.println(message);
        }
    }
    
    /**
     * Ghi log lỗi
     * 
     * @param error Thông điệp lỗi
     * @param exception Exception để ghi stack trace
     */
    public static synchronized void logError(String error, Exception exception) {
        if (!isInitialized) {
            init();
        }
        
        if (logWriter != null) {
            logWriter.println("ERROR: " + error);
            if (exception != null) {
                exception.printStackTrace(logWriter);
            }
            logWriter.flush();
            
            // In ra console để dễ theo dõi
            System.err.println("ERROR: " + error);
            if (exception != null) {
                exception.printStackTrace();
            }
        }
    }
    
    /**
     * Đóng logger
     */
    public static synchronized void close() {
        if (logWriter != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            logWriter.println("===== KẾT THÚC GHI LOG - " + dateFormat.format(new Date()) + " =====\n");
            logWriter.flush();
            logWriter.close();
            isInitialized = false;
        }
    }
}