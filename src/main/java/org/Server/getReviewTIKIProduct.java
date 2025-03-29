package org.Server;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.json.JSONObject;
import org.json.JSONArray;

public class getReviewTIKIProduct {

    // Hàm tìm kiếm sản phẩm tương tự
    public List<String> searchSimilarProducts(String searchQuery) {
        List<String> productSuggestions = new ArrayList<>();
        try {
            // Tạo URL API tìm kiếm trên Tiki
            String encodedQuery = URLEncoder.encode(searchQuery, "UTF-8");
            String apiUrl = "https://tiki.vn/api/v2/products?limit=40&include=advertisement&aggregations=2&q=" + encodedQuery + "&rating=5";

            // Gọi API và lấy JSON response
            String jsonResponse = Jsoup.connect(apiUrl)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .execute()
                    .body();

            // Parse JSON để lấy danh sách sản phẩm
            JSONObject json = new JSONObject(jsonResponse);
            JSONArray products = json.getJSONArray("data");

            // Kiểm tra nếu không tìm thấy sản phẩm
            if (products.length() == 0) {
                return productSuggestions;  // Trả về danh sách rỗng
            }

            // Lưu tất cả các tên sản phẩm tìm thấy vào danh sách
            for (int i = 0; i < products.length(); i++) {
                JSONObject product = products.getJSONObject(i);
                String productName = product.getString("name");
                productSuggestions.add(productName);
            }

        } catch (Exception e) {
            System.err.println("Lỗi khi tìm kiếm sản phẩm: " + e.getMessage());
        }
        return productSuggestions;
    }

    // Hàm lấy thông tin chi tiết sản phẩm
    public String getProductInfo(String productName) {
        try {
            // Tạo URL API tìm kiếm sản phẩm
            String encodedQuery = URLEncoder.encode(productName, "UTF-8");
            String apiUrl = "https://tiki.vn/api/v2/products?limit=40&include=advertisement&aggregations=2&q=" + encodedQuery;

            // Gọi API và lấy JSON response
            String jsonResponse = Jsoup.connect(apiUrl)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .execute()
                    .body();

            // Parse JSON để lấy sản phẩm đầu tiên
            JSONObject json = new JSONObject(jsonResponse);
            JSONArray products = json.getJSONArray("data");

            if (products.length() == 0) {
                return "Không tìm thấy thông tin sản phẩm.";
            }

            // Lấy sản phẩm đầu tiên và thông tin chi tiết
            JSONObject firstProduct = products.getJSONObject(0);
            String productId = String.valueOf(firstProduct.get("id"));
            JSONObject productDetails = loadProductDetails(productId);

            String name = productDetails.getString("name");
            int price = productDetails.getInt("price");
            String imgUrl = productDetails.getJSONArray("images").getJSONObject(0).getString("base_url");

            return "Tên sản phẩm: " + name + "\nGiá: " + price + " VND\nHình ảnh: " + imgUrl;

        } catch (Exception e) {
            return "Lỗi khi lấy thông tin sản phẩm: " + e.getMessage();
        }
    }

    // Hàm lấy đánh giá sản phẩm
    public String getProductReviews(String productName) {
        try {
            // Tạo URL API tìm kiếm sản phẩm
            String encodedQuery = URLEncoder.encode(productName, "UTF-8");
            String apiUrl = "https://tiki.vn/api/v2/products?limit=40&include=advertisement&aggregations=2&q=" + encodedQuery;

            // Gọi API và lấy JSON response
            String jsonResponse = Jsoup.connect(apiUrl)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .execute()
                    .body();

            // Parse JSON để lấy sản phẩm đầu tiên
            JSONObject json = new JSONObject(jsonResponse);
            JSONArray products = json.getJSONArray("data");

            if (products.length() == 0) {
                return "Không tìm thấy sản phẩm để lấy đánh giá.";
            }

            // Lấy productId của sản phẩm đầu tiên
            JSONObject firstProduct = products.getJSONObject(0);
            String productId = String.valueOf(firstProduct.get("id"));

            // Lấy đánh giá của sản phẩm
            JSONObject reviewDetails = loadProductReviews(productId);
            int reviewCount = reviewDetails.getInt("reviews_count");
            double avgRating = reviewDetails.getDouble("rating_average");
            JSONArray reviews = reviewDetails.getJSONArray("data");

            // Xây dựng chuỗi kết quả đánh giá
            StringBuilder response = new StringBuilder();
            response.append("Số lượng đánh giá: ").append(reviewCount).append("\n");
            response.append("Đánh giá trung bình: ").append(avgRating).append("\n");
            response.append("Chi tiết đánh giá:\n");
            for (int i = 0; i < reviews.length(); i++) {
                JSONObject review = reviews.getJSONObject(i);
                JSONObject createdBy = review.getJSONObject("created_by");
                String fullName = createdBy.has("full_name") ? createdBy.getString("full_name") : "Ẩn danh";
                String content = review.getString("content");
                int rating = review.getInt("rating");
                response.append(fullName).append(" (").append(rating).append("): ").append(content).append("\n");
            }

            return response.toString();

        } catch (Exception e) {
            return "Lỗi khi lấy đánh giá sản phẩm: " + e.getMessage();
        }
    }

    // Hàm hỗ trợ lấy chi tiết sản phẩm
    private JSONObject loadProductDetails(String productId) throws IOException {
        String apiUrl = "https://tiki.vn/api/v2/products/" + productId;
        String jsonResponse = Jsoup.connect(apiUrl).ignoreContentType(true).execute().body();
        return new JSONObject(jsonResponse);
    }

    // Hàm hỗ trợ lấy đánh giá sản phẩm
    private JSONObject loadProductReviews(String productId) throws IOException {
        String apiUrl = "https://tiki.vn/api/v2/reviews?limit=10&product_id=" + productId;
        String jsonResponse = Jsoup.connect(apiUrl).ignoreContentType(true).execute().body();
        return new JSONObject(jsonResponse);
    }
}
