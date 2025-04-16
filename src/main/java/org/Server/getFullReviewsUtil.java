package org.Server;

import com.microsoft.playwright.Page;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp tiện ích để lấy toàn bộ đánh giá cho sản phẩm từ nhiều trang
 */
public class getFullReviewsUtil {
    
    private final getReviewDMXProduct scraper;
    
    public getFullReviewsUtil(getReviewDMXProduct scraper) {
        this.scraper = scraper;
    }
    
    /**
     * Lấy toàn bộ đánh giá của một sản phẩm từ tất cả các trang
     * 
     * @param productUrl URL sản phẩm
     * @return Chuỗi JSON chứa đánh giá từ tất cả các trang
     */
    public String getAllReviewsForProduct(String productUrl) {
        try {
            System.out.println("Đang lấy toàn bộ đánh giá cho sản phẩm: " + productUrl);
            
            // Lấy thông tin chi tiết sản phẩm
            JSONObject productDetails = scraper.getProductDetails(productUrl);
            if (productDetails == null) {
                return "Lỗi khi lấy thông tin sản phẩm.";
            }
            
            // Lấy tổng số trang đánh giá
            int totalPages = scraper.getTotalReviewPages(productUrl);
            System.out.println("Tổng số trang đánh giá: " + totalPages);
            
            // Tạo mảng để lưu tất cả các đánh giá
            JSONArray allReviews = new JSONArray();
            
            // Lặp qua từng trang và lấy đánh giá
            for (int page = 1; page <= totalPages; page++) {
                System.out.println("Đang lấy đánh giá trang " + page + "/" + totalPages);
                JSONArray pageReviews = scraper.getProductReviews(productUrl, page);
                
                // Nếu không còn đánh giá nào thì dừng
                if (pageReviews.length() == 0 && page > 1) {
                    System.out.println("Không còn đánh giá nào ở trang " + page);
                    break;
                }
                
                // Thêm đánh giá từ trang hiện tại vào mảng tổng
                for (int i = 0; i < pageReviews.length(); i++) {
                    allReviews.put(pageReviews.getJSONObject(i));
                }
                
                System.out.println("Đã lấy được " + pageReviews.length() + " đánh giá từ trang " + page);
                
                // Ghi log tổng số đánh giá đã lấy được
                System.out.println("Tổng số đánh giá đã lấy: " + allReviews.length());
                
                // Nếu đã lấy ít hơn 20 đánh giá ở trang này, có thể không còn trang nào nữa
                if (pageReviews.length() < 20 && page < totalPages) {
                    System.out.println("Có vẻ đã hết đánh giá, dừng ở trang " + page);
                    break;
                }
            }
            
            // Xây dựng phản hồi
            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("product", productDetails);
            response.put("reviews", allReviews);
            response.put("total_reviews", allReviews.length());
            
            // Lưu dữ liệu đánh giá để debug nếu cần
            try {
                FileWriter reviewWriter = new FileWriter("full_reviews_data.json");
                reviewWriter.write(response.toString(2));
                reviewWriter.close();
                System.out.println("Đã lưu dữ liệu đánh giá đầy đủ vào full_reviews_data.json");
            } catch (IOException e) {
                System.out.println("Không thể lưu dữ liệu đánh giá: " + e.getMessage());
            }
            
            return response.toString();
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy toàn bộ đánh giá: " + e.getMessage());
            e.printStackTrace();
            return "Lỗi: " + e.getMessage();
        }
    }
    
    /**
     * Tính điểm đánh giá trung bình và tổng số đánh giá
     * 
     * @param allReviews Mảng JSON chứa tất cả đánh giá
     * @return JSONObject chứa điểm trung bình và tổng số đánh giá
     */
    public JSONObject calculateRatingStatistics(JSONArray allReviews) {
        int totalRatings = allReviews.length();
        double sumRating = 0;
        
        // Đếm số lượng rating theo từng điểm số
        int[] ratingCounts = new int[6]; // index 0 không dùng, 1-5 tương ứng với số sao
        
        for (int i = 0; i < totalRatings; i++) {
            JSONObject review = allReviews.getJSONObject(i);
            int rating = review.getInt("rating");
            sumRating += rating;
            
            // Đảm bảo rating nằm trong khoảng hợp lệ (1-5)
            if (rating >= 1 && rating <= 5) {
                ratingCounts[rating]++;
            }
        }
        
        // Tính điểm trung bình
        double averageRating = (totalRatings > 0) ? (sumRating / totalRatings) : 0;
        
        // Làm tròn đến 1 chữ số thập phân
        averageRating = Math.round(averageRating * 10) / 10.0;
        
        // Tạo đối tượng JSON để trả về
        JSONObject statistics = new JSONObject();
        statistics.put("average_rating", averageRating);
        statistics.put("total_reviews", totalRatings);
        
        // Thêm thông tin chi tiết về số lượng đánh giá theo từng điểm số
        JSONObject ratingDistribution = new JSONObject();
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.put(String.valueOf(i), ratingCounts[i]);
        }
        statistics.put("rating_distribution", ratingDistribution);
        
        return statistics;
    }
}