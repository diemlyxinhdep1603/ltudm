package org.Server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.json.JSONObject;
import org.json.JSONArray;

public class getReviewTIKIProduct {

    // Hàm lấy danh sách tên gợi ý sản phẩm từ API của Tiki
    public Map<String, String> getSuggestedProducts(String searchQuery) {
        Map<String, String> suggestions = new HashMap<>();
        try {

            String encodedQuery = URLEncoder.encode(searchQuery, "UTF-8");
            // Đây là API để tìm tên gợi ý của sản phẩm.
            String api = "https://tiki.vn/api/shopping/v2/featured_keywords?page_name=Search&q=" + encodedQuery;
            // Gọi API và lấy JSON response
            String jsonResponse = Jsoup.connect(api)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .execute()
                    .body();

            // Parse JSON để lấy danh sách sản phẩm
            JSONObject json = new JSONObject(jsonResponse);
            JSONArray data = json.getJSONArray("data");

            // Trả về HashMap với key là tên gợi ý và value là URL tìm kiếm
            for (int i = 0; i < data.length(); i++){
                JSONObject item = data.getJSONObject(i);
                String keyword = item.getString("keyword");
                String url = item.getString("url");
                suggestions.put(keyword,url);
            }

        } catch (Exception e) {
            System.err.println("Lỗi lấy danh sách tên gợi ý: " + e.getMessage());
        }
        return suggestions;
    }

    //Hàm lấy ID sản phẩm ĐẦU TIÊN từ URL gợi ý
    public String getProductIDFromUrl(String productName) {
        try {
            // Lấy danh sách các gợi ý từ getSuggestedProducts
            Map<String, String> suggestions = getSuggestedProducts(productName);
            if (suggestions.containsKey(productName)){
                String searchUrl = suggestions.get(productName);
                //Mã hóa url và gọi API để lấy danh sách sản phẩm
                String encodeUrl = URLEncoder.encode(searchUrl, "UTF-8");
                String apiUrl = "https://tiki.vn/api/v2/products?limit=40&include=advertisement&aggregations=2&q=" + encodeUrl;
                //Gọi API và lấy JSON response
                String jsonResponse = Jsoup.connect(apiUrl)
                        .ignoreContentType(true)
                        .userAgent("Mozilla/5.0")
                        .execute()
                        .body();

                JSONObject json = new JSONObject(jsonResponse);
                JSONArray data = json.getJSONArray("data");

                //Kiểm tra nếu có dữ liệu, trả về ID sản phẩm đầu tiên
                if (data.length() > 0 ){
                    JSONObject firstProduct = data.getJSONObject(0);
                    return String.valueOf(firstProduct.getInt("id"));
                }
            }
        } catch (IOException e) {
            System.err.println("Lỗi lấy ID sản phẩm từ URL: " + e.getMessage());
        }
        return null;
    }

    // Hàm lấy thông tin sản phẩm , DỰA TRÊN ID TRÊN ==> Hiển thị ở OVERVIEW
    public String getProductInfo(String input) {
        String ID = getProductIDFromUrl(input);  // Lấy ID từ URL gợi ý
        if (ID == null){
            return "Không tìm thấy sản phẩm để lấy đánh giá.";
        }


        String url = "https://tiki.vn/api/v2/products/" + ID;  // URL API lấy thông tin sản phẩm
        try {
            // Gọi API và lấy dữ liệu sản phẩm
            String jsonResponse = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .execute()
                    .body();

            JSONObject json = new JSONObject(jsonResponse);
            String productName = json.getString("name");

            // Sử dụng optString để tránh lỗi nếu giá trị không phải là String
            String productPrice = json.optString("price", "Giá chưa có");

            return "Tên sản phẩm: " + productName + " .Có giá là: " + productPrice;

        } catch (IOException e) {
            return "Lỗi tra cứu thông tin: " + e.getMessage();
        }
    }

    // Hàm lấy các review của người dùng đã mua sản phẩm
    public String getProductReviews(String productName) {
        String ID = getProductIDFromUrl(productName);
        if (ID == null) {
            // Nếu không tìm thấy sản phẩm chính xác, lấy gợi ý gần đúng và lấy đánh giá từ gợi ý đầu tiên
            Map<String, String> suggestions = getSuggestedProducts(productName);
            if (!suggestions.isEmpty()) {
                // Lấy gợi ý đầu tiên (bạn có thể thay đổi chiến lược lấy gợi ý khác)
                String firstSuggestion = suggestions.keySet().iterator().next();
                System.out.println("Sử dụng gợi ý: " + firstSuggestion);
                ID = getProductIDFromUrl(firstSuggestion); // Lấy ID của sản phẩm gợi ý
            }
        }

        if (ID == null) {
            return "Không tìm thấy sản phẩm để lấy đánh giá.";
        }

        String apiUrl = "https://tiki.vn/api/v2/reviews?limit=10&product_id=" + ID;
        try {
            String jsonResponse = Jsoup.connect(apiUrl)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .execute()
                    .body();

            JSONObject json = new JSONObject(jsonResponse);
            int reviewCount = json.optInt("total", 0);
            double avgRating = json.optDouble("rating_average", 0);
            JSONArray reviews = json.optJSONArray("data");

            if (reviews == null || reviews.length() == 0) {
                return "Không tìm thấy đánh giá cho sản phẩm.";
            }

            StringBuilder response = new StringBuilder();
            response.append("Số lượng đánh giá: ").append(reviewCount).append("\n");
            response.append("Đánh giá trung bình: ").append(avgRating).append("\n");
            response.append("Chi tiết đánh giá:\n");

            for (int i = 0; i < reviews.length(); i++) {
                JSONObject review = reviews.getJSONObject(i);
                JSONObject createdBy = review.optJSONObject("created_by");
                String fullName = createdBy != null && createdBy.has("full_name") ? createdBy.getString("full_name") : "Ẩn danh";
                String content = review.optString("content", "Không có nội dung.");
                int rating = review.optInt("rating", 0);

                // Trích xuất ảnh (nếu có)
                JSONArray mediaArray = review.optJSONArray("media");
                String imageUrl = "";
                if (mediaArray != null && mediaArray.length() > 0) {
                    JSONObject media = mediaArray.getJSONObject(0);
                    imageUrl = media.optString("url", "Không có ảnh.");
                }

                response.append(fullName).append(" (").append(rating).append("): ").append(content)
                        .append("\nẢnh: ").append(imageUrl).append("\n");
            }

            return response.toString();
        } catch (Exception e) {
            return "Lỗi khi lấy đánh giá sản phẩm: " + e.getMessage();
        }
    }
    // Hàm lấy thông tin đánh giá của sản phẩm từ gợi ý
    public String getProductReviewsFromSuggestion(String searchKeyword) {
        Map<String, String> suggestions = getSuggestedProducts(searchKeyword);

        // Nếu có gợi ý, lấy sản phẩm đầu tiên
        if (!suggestions.isEmpty()) {
            String firstSuggestion = suggestions.keySet().iterator().next();
            String productId = getProductIDFromUrl(firstSuggestion); // Lấy ID từ URL gợi ý

            // Lấy đánh giá của sản phẩm gợi ý
            return getProductReviewsByID(productId);
        }
        return "Không tìm thấy sản phẩm hoặc gợi ý.";
    }

    // Hàm lấy đánh giá của sản phẩm từ ID
    public String getProductReviewsByID(String productID) {
        String apiUrl = "https://tiki.vn/api/v2/reviews?limit=10&product_id=" + productID;
        try {
            String jsonResponse = Jsoup.connect(apiUrl)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .execute()
                    .body();

            JSONObject json = new JSONObject(jsonResponse);
            int reviewCount = json.optInt("total", 0);
            double avgRating = json.optDouble("rating_average", 0);
            JSONArray reviews = json.optJSONArray("data");

            if (reviews == null || reviews.length() == 0) {
                return "Không tìm thấy đánh giá cho sản phẩm.";
            }

            StringBuilder response = new StringBuilder();
            response.append("Số lượng đánh giá: ").append(reviewCount).append("\n");
            response.append("Đánh giá trung bình: ").append(avgRating).append("\n");
            response.append("Chi tiết đánh giá:\n");

            for (int i = 0; i < reviews.length(); i++) {
                JSONObject review = reviews.getJSONObject(i);
                String content = review.optString("content", "Không có nội dung.");
                int rating = review.optInt("rating", 0);
                response.append("Đánh giá ").append(rating).append(": ").append(content).append("\n");
            }

            return response.toString();
        } catch (Exception e) {
            return "Lỗi khi lấy đánh giá sản phẩm: " + e.getMessage();
        }
    }


}
