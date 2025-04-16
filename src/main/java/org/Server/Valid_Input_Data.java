package org.Server;

/**
 * Lớp này cung cấp các phương thức để kiểm tra tính hợp lệ của dữ liệu đầu vào
 */
public class Valid_Input_Data {
    
    // Biến static để lưu thông báo lỗi mới nhất
    private static String lastErrorMessage = "";
    
    /**
     * Lấy thông báo lỗi mới nhất 
     * @return Thông báo lỗi mới nhất
     */
    public static String getLastErrorMessage() {
        return lastErrorMessage;
    }
    
    /**
     * Kiểm tra tính hợp lệ của từ khóa tìm kiếm
     * @param keyword Từ khóa cần kiểm tra
     * @return true nếu từ khóa hợp lệ, false nếu không hợp lệ
     */
    public static boolean isValidKeyword(String keyword) {
        // Từ khóa không được null hoặc trống
        if (keyword == null || keyword.trim().isEmpty()) {
            lastErrorMessage = "Từ khóa tìm kiếm không hợp lệ: Trống";
            System.out.println(lastErrorMessage);
            return false;
        }
        
        // Từ khóa không được quá dài (ví dụ: giới hạn 100 ký tự)
        if (keyword.length() > 100) {
            lastErrorMessage = "Từ khóa tìm kiếm không hợp lệ: Quá dài (> 100 ký tự)";
            System.out.println(lastErrorMessage);
            return false;
        }
        
        // Từ khóa không được chứa ký tự đặc biệt nguy hiểm
        if (keyword.contains("<script>") || keyword.contains("</script>")) {
            lastErrorMessage = "Từ khóa tìm kiếm không hợp lệ: Chứa mã JavaScript";
            System.out.println(lastErrorMessage);
            return false;
        }
        
        lastErrorMessage = ""; // Xóa thông báo lỗi nếu không có lỗi
        return true;
    }
    
    /**
     * Kiểm tra tính hợp lệ của URL
     * @param url URL cần kiểm tra
     * @return true nếu URL hợp lệ, false nếu không hợp lệ
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            lastErrorMessage = "URL không hợp lệ: Trống";
            System.out.println(lastErrorMessage);
            return false;
        }
        
        // URL phải bắt đầu bằng http:// hoặc https://
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            lastErrorMessage = "URL không hợp lệ: Không bắt đầu bằng http:// hoặc https://";
            System.out.println(lastErrorMessage);
            return false;
        }
        
        lastErrorMessage = ""; // Xóa thông báo lỗi nếu không có lỗi
        return true;
    }
    
    /**
     * Kiểm tra tính hợp lệ của ID sản phẩm
     * @param productId ID sản phẩm cần kiểm tra
     * @return true nếu ID hợp lệ, false nếu không hợp lệ
     */
    public static boolean isValidProductId(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            lastErrorMessage = "ID sản phẩm không hợp lệ: Trống";
            System.out.println(lastErrorMessage);
            return false;
        }
        
        // ID sản phẩm thường là số
        try {
            Long.parseLong(productId);
            lastErrorMessage = ""; // Xóa thông báo lỗi nếu không có lỗi
            return true;
        } catch (NumberFormatException e) {
            // Một số platform có thể sử dụng ID không phải số
            // Nếu ID không phải số, kiểm tra các tiêu chí khác
            if (productId.length() > 100) {
                lastErrorMessage = "ID sản phẩm không hợp lệ: Quá dài (> 100 ký tự)";
                System.out.println(lastErrorMessage);
                return false;
            }
            
            // Không nên chứa ký tự đặc biệt
            if (!productId.matches("^[a-zA-Z0-9_-]+$")) {
                lastErrorMessage = "ID sản phẩm không hợp lệ: Chứa ký tự đặc biệt không cho phép";
                System.out.println(lastErrorMessage);
                return false;
            }
        }
        
        lastErrorMessage = ""; // Xóa thông báo lỗi nếu không có lỗi
        return true;
    }
    
    /**
     * Kiểm tra tính hợp lệ của platform
     * @param platform Tên platform cần kiểm tra
     * @return true nếu platform hợp lệ, false nếu không hợp lệ
     */
    public static boolean isValidPlatform(String platform) {
        if (platform == null || platform.trim().isEmpty()) {
            lastErrorMessage = "Platform không hợp lệ: Trống";
            System.out.println(lastErrorMessage);
            return false;
        }
        
        // Chỉ chấp nhận các platform đã định nghĩa
        switch (platform.toUpperCase()) {
            case "TIKI":
            case "ĐIỆN MÁY XANH":

                lastErrorMessage = ""; // Xóa thông báo lỗi nếu không có lỗi
                return true;
            default:
                lastErrorMessage = "Platform không hợp lệ: " + platform + " (Chỉ chấp nhận TIKI, ĐIỆN MÁY XANH)";
                System.out.println(lastErrorMessage);
                return false;
        }
    }
    
    /**
     * Kiểm tra tính hợp lệ của số đánh giá (rating)
     * @param rating Đánh giá cần kiểm tra
     * @return true nếu đánh giá hợp lệ, false nếu không hợp lệ
     */
    public static boolean isValidRating(String rating) {
        if (rating == null || rating.trim().isEmpty()) {
            lastErrorMessage = "Rating không hợp lệ: Trống";
            System.out.println(lastErrorMessage);
            return false;
        }
        
        try {
            double ratingValue = Double.parseDouble(rating);
            // Đánh giá thường từ 0-5 sao
            if (ratingValue < 0 || ratingValue > 5) {
                lastErrorMessage = "Rating không hợp lệ: Nằm ngoài khoảng 0-5";
                System.out.println(lastErrorMessage);
                return false;
            }
            lastErrorMessage = ""; // Xóa thông báo lỗi nếu không có lỗi
            return true;
        } catch (NumberFormatException e) {
            lastErrorMessage = "Rating không hợp lệ: Không phải số";
            System.out.println(lastErrorMessage);
            return false;
        }
    }
    
    /**
     * Kiểm tra tính hợp lệ của nội dung đánh giá
     * @param content Nội dung đánh giá
     * @return true nếu nội dung hợp lệ, false nếu không hợp lệ
     */
    public static boolean isValidReviewContent(String content) {
        // Nội dung có thể trống
        if (content == null) {
            lastErrorMessage = "Nội dung đánh giá không hợp lệ: Null";
            System.out.println(lastErrorMessage);
            return false;
        }
        /*
        // Nội dung không được quá dài (ví dụ: giới hạn 5000 ký tự)
        if (content.length() > 5000) {
            lastErrorMessage = "Nội dung đánh giá không hợp lệ: Quá dài (> 5000 ký tự)";
            System.out.println(lastErrorMessage);
            return false;
        }

         */
        
        // Kiểm tra xem có mã độc hại hay không
        if (content.contains("<script>") || content.contains("</script>")) {
            lastErrorMessage = "Nội dung đánh giá không hợp lệ: Chứa mã JavaScript";
            System.out.println(lastErrorMessage);
            return false;
        }
        
        lastErrorMessage = ""; // Xóa thông báo lỗi nếu không có lỗi
        return true;
    }
}
