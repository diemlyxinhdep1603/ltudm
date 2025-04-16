package org.Server;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Lớp tiện ích để lấy toàn bộ đánh giá cho sản phẩm từ nhiều trang từ Điện Máy Xanh
 * Sử dụng phương pháp lazy loading (tải từng trang một khi người dùng yêu cầu)
 */
public class getFullReviewsUtilDMX {
    
    private final getReviewDMXProduct scraper;
    
    public getFullReviewsUtilDMX(getReviewDMXProduct scraper) {
        this.scraper = scraper;
    }
    
    /**
     * Lấy đánh giá ở trang được chỉ định với giao diện lazy loading
     * 
     * @param productUrl URL sản phẩm
     * @param page Trang cần lấy
     * @return Chuỗi JSON chứa thông tin sản phẩm và đánh giá
     */
    public String getReviewsWithLazyLoading(String productUrl, int page) {
        try {
            System.out.println("Lazy Loading: Bắt đầu lấy đánh giá trang " + page + " cho sản phẩm: " + productUrl);
            
            // Lấy thông tin chi tiết sản phẩm
            JSONObject productDetails = scraper.getProductDetails(productUrl);
            if (productDetails == null) {
                return "Lỗi khi lấy thông tin sản phẩm.";
            }
            
            // Lấy thông tin rating và tổng số trang nếu chưa có
            if (page == 1) {
                scraper.getTotalReviewPages(productUrl);
            }
            
            // Lấy đánh giá ở trang được chỉ định
            JSONArray reviewsPage = scraper.getProductReviews(productUrl, page);
            int totalReviews = scraper.getProductRatingCount();
            double avgRating = scraper.getProductRatingAverage();
            
            // Tính toán tổng số trang dựa trên số lượng đánh giá
            int totalPages = (int) Math.ceil(totalReviews / 20.0);
            if (totalPages < 1) totalPages = 1;
            
            // Điện Máy Xanh có tối đa 50 trang đánh giá
            if (totalPages > 50) {
                totalPages = 50;
            }
            
            // Kiểm tra lại nếu tổng số trang tính toán khác với số trang thực tế
            if (page > totalPages) {
                System.out.println("Cảnh báo: Yêu cầu trang " + page + " nhưng chỉ có " + totalPages + " trang");
            }
            
            // Debug để kiểm tra số trang
            System.out.println("Tổng số trang đánh giá: " + totalPages + " (tổng số đánh giá: " + totalReviews + ")");
            
            // Xây dựng phản hồi
            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("product", productDetails);
            response.put("reviews", reviewsPage);
            response.put("total_pages", totalPages);
            response.put("current_page", page);
            
            // Thêm thông tin rating
            JSONObject ratingInfo = new JSONObject();
            ratingInfo.put("average_rating", avgRating);
            ratingInfo.put("review_count", totalReviews);
            response.put("rating_info", ratingInfo);
            
            return response.toString();
            
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy đánh giá lazy loading: " + e.getMessage());
            e.printStackTrace();
            return "Lỗi: " + e.getMessage();
        }
    }
    
    /**
     * Phương thức thử nghiệm để lấy tất cả trang đánh giá và hiển thị tiến độ
     * 
     * @param productUrl URL sản phẩm
     * @return Chuỗi JSON chứa kết quả
     */
    public String getAllReviewsWithProgress(String productUrl) {
        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;
        
        try {
            System.out.println("Bắt đầu lấy tất cả đánh giá với tiến độ cho sản phẩm: " + productUrl);
            
            // Lấy thông tin chi tiết sản phẩm
            JSONObject productDetails = scraper.getProductDetails(productUrl);
            if (productDetails == null) {
                return "Lỗi khi lấy thông tin sản phẩm.";
            }
            
            // Lấy tổng số trang đánh giá (cũng sẽ cập nhật thông tin rating)
            int totalPages = scraper.getTotalReviewPages(productUrl);
            System.out.println("Tổng số trang đánh giá: " + totalPages);
            
            // Lấy số lượng đánh giá và điểm trung bình
            int totalReviews = scraper.getProductRatingCount();
            double avgRating = scraper.getProductRatingAverage();
            
            // Nếu không có thông tin số trang chính xác, thử lấy từ totalReviews
            if (totalPages <= 1 && totalReviews > 20) {
                totalPages = (int) Math.ceil(totalReviews / 20.0);
                System.out.println("Tính lại tổng số trang từ số đánh giá: " + totalPages);
            }
            
            // Điện Máy Xanh thường giới hạn hiển thị tối đa 50 trang đánh giá
            if (totalPages > 50) {
                totalPages = 50;
                System.out.println("Giới hạn số trang xuống 50 (điện máy xanh thường chỉ hiển thị tối đa 50 trang)");
            }
            
            // Khởi tạo Playwright để lấy đánh giá theo cách Test.java
            playwright = Playwright.create();
            browser = playwright.webkit().launch();
            context = browser.newContext();
            page = context.newPage();
            
            // Mảng lưu trữ tất cả đánh giá
            JSONArray allReviews = new JSONArray();
            
            // Khởi tạo mảng chứa các trang đánh giá
            JSONArray pagesArray = new JSONArray();
            
            // Lặp qua từng trang để lấy đánh giá
            for (int currentPage = 1; currentPage <= totalPages; currentPage++) {
                String reviewUrl;
                if (productUrl.startsWith("http")) {
                    String path = productUrl.replaceFirst("https?://[^/]+", "");
                    reviewUrl = "https://www.dienmayxanh.com" + path + "/danh-gia?page=" + currentPage;
                } else {
                    reviewUrl = "https://www.dienmayxanh.com" + productUrl + "/danh-gia?page=" + currentPage;
                }
                
                System.out.println("Đang lấy đánh giá trang " + currentPage + "/" + totalPages + ": " + reviewUrl);
                
                // Truy cập trang đánh giá
                page.navigate(reviewUrl);
                page.waitForLoadState(LoadState.NETWORKIDLE);
                
                // Lưu HTML để debug
                try {
                    FileWriter writer = new FileWriter("dmx_review_page_" + currentPage + ".html");
                    writer.write(page.content());
                    writer.close();
                } catch (IOException e) {
                    System.err.println("Lỗi khi lưu HTML: " + e.getMessage());
                }
                
                // Lấy tất cả đánh giá theo selector từ Test.java
                var reviews = page.locator(".cmt-txt");
                int reviewCount = reviews.count();
                
                System.out.println("Tìm thấy " + reviewCount + " đánh giá trên trang " + currentPage);
                
                // Nếu không có đánh giá và không phải trang đầu, dừng lại
                if (reviewCount == 0 && currentPage > 1) {
                    System.out.println("Không còn đánh giá, dừng ở trang " + currentPage);
                    break;
                }
                
                // Lấy thông tin đánh giá
                JSONArray pageReviews = new JSONArray();
                
                // Duyệt qua từng đánh giá
                for (int i = 0; i < reviewCount; i++) {
                    // Tìm tên người đánh giá
                    String reviewerName = "Khách hàng";
                    var nameElements = page.locator(".cmt-top .txtname");
                    if (nameElements.count() > i) {
                        reviewerName = nameElements.nth(i).textContent().trim();
                    }
                    
                    // Tìm nội dung đánh giá
                    String content = reviews.nth(i).textContent().trim();
                    
                    // Tìm điểm đánh giá (mặc định là 5 nếu không tìm thấy)
                    int rating = 5;
                    // Có thể thêm selector để lấy rating chính xác nếu có
                    
                    // Tìm thời gian đánh giá
                    String time = "";
                    var timeElements = page.locator(".cmt-top .cmt-time");
                    if (timeElements.count() > i) {
                        time = timeElements.nth(i).textContent().trim();
                    }
                    
                    // Tìm hình ảnh nếu có
                    JSONArray imageArray = new JSONArray();
                    var imageElements = page.locator(".cmt-botimg .owl-popup");
                    // Duyệt qua các phần tử hình ảnh liên quan đến đánh giá này
                    // Code này cần cải thiện để lấy đúng hình ảnh cho từng đánh giá
                    
                    // Tạo object cho mỗi đánh giá
                    JSONObject reviewObj = new JSONObject();
                    reviewObj.put("reviewer_name", reviewerName);
                    reviewObj.put("content", content);
                    reviewObj.put("rating", rating);
                    reviewObj.put("time", time);
                    reviewObj.put("images", imageArray);
                    
                    // Thêm vào mảng đánh giá của trang hiện tại
                    pageReviews.put(reviewObj);
                    // Thêm vào mảng tổng đánh giá
                    allReviews.put(reviewObj);
                }
                
                // Thêm thông tin trang vào mảng trang
                JSONObject pageInfo = new JSONObject();
                pageInfo.put("page_number", currentPage);
                pageInfo.put("reviews", pageReviews);
                pagesArray.put(pageInfo);
                
                System.out.println("Đã lấy được " + pageReviews.length() + " đánh giá từ trang " + currentPage);
                System.out.println("Tổng số đánh giá đã lấy: " + allReviews.length());
            }
            
            // Lưu tổng hợp đánh giá vào file JSON
            try {
                FileWriter writer = new FileWriter("full_reviews_data.json");
                writer.write(allReviews.toString(2)); // Định dạng đẹp
                writer.close();
                System.out.println("Đã lưu dữ liệu đánh giá đầy đủ vào full_reviews_data.json");
            } catch (IOException e) {
                System.err.println("Lỗi khi lưu dữ liệu JSON: " + e.getMessage());
            }
            
            // Xây dựng phản hồi
            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("product", productDetails);
            response.put("all_reviews", allReviews);
            response.put("pages", pagesArray);
            response.put("total_pages", totalPages);
            response.put("total_reviews", totalReviews);
            
            // Thêm thông tin rating
            JSONObject ratingInfo = new JSONObject();
            ratingInfo.put("average_rating", avgRating);
            ratingInfo.put("review_count", totalReviews);
            response.put("rating_info", ratingInfo);
            
            return response.toString();
            
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy tất cả đánh giá với tiến độ: " + e.getMessage());
            e.printStackTrace();
            return "Lỗi: " + e.getMessage();
        } finally {
            try {
                // Đóng các tài nguyên
                if (page != null) page.close();
                if (context != null) context.close();
                if (browser != null) browser.close();
                if (playwright != null) playwright.close();
            } catch (Exception e) {
                System.err.println("Lỗi khi đóng tài nguyên: " + e.getMessage());
            }
        }
    }
}