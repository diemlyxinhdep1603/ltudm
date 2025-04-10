package org.Server;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import java.net.URLEncoder;

public class DebugSearch {
    public static void main(String[] args) {
        try {
            // 1. Kiểm tra tìm kiếm sản phẩm với từ khóa "laptop gaming i7"
            String searchQuery = "laptop gaming i7";
            System.out.println("Tìm kiếm sản phẩm với từ khóa: " + searchQuery);

            String encodedQuery = URLEncoder.encode(searchQuery, "UTF-8");
            String apiUrl = "https://tiki.vn/api/v2/products?limit=40&include=advertisement&aggregations=2&q=" + encodedQuery + "&rating=5";

            // Gọi API
            String jsonResponse = Jsoup.connect(apiUrl)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .execute()
                    .body();

            // Phân tích JSON để lấy sản phẩm đầu tiên
            JSONObject json = new JSONObject(jsonResponse);
            JSONArray products = json.getJSONArray("data");

            if (products.length() > 0) {
                // Hiển thị thông tin về 3 sản phẩm đầu tiên
                for (int i = 0; i < Math.min(3, products.length()); i++) {
                    JSONObject product = products.getJSONObject(i);

                    int id = product.getInt("id");
                    String name = product.getString("name");
                    Object priceObj = product.get("price");

                    System.out.println("\nSản phẩm #" + (i+1));
                    System.out.println("ID: " + id);
                    System.out.println("Tên: " + name);
                    System.out.println("Giá: " + priceObj + " (Kiểu: " + priceObj.getClass().getName() + ")");

                    // Thử lấy thông tin chi tiết sản phẩm
                    System.out.println("\nLấy thông tin chi tiết cho sản phẩm ID = " + id);
                    String detailApiUrl = "https://tiki.vn/api/v2/products/" + id;
                    String detailResponse = Jsoup.connect(detailApiUrl)
                            .ignoreContentType(true)
                            .userAgent("Mozilla/5.0")
                            .execute()
                            .body();

                    JSONObject productDetail = new JSONObject(detailResponse);
                    if (productDetail.has("price")) {
                        Object detailPrice = productDetail.get("price");
                        System.out.println("Chi tiết Giá: " + detailPrice + " (Kiểu: " + detailPrice.getClass().getName() + ")");
                    }

                    // Thử lấy đánh giá sản phẩm
                    System.out.println("\nLấy đánh giá cho sản phẩm ID = " + id);
                    String reviewApiUrl = "https://tiki.vn/api/v2/reviews?limit=10&product_id=" + id;
                    String reviewResponse = Jsoup.connect(reviewApiUrl)
                            .ignoreContentType(true)
                            .userAgent("Mozilla/5.0")
                            .execute()
                            .body();

                    JSONObject reviewJson = new JSONObject(reviewResponse);
                    if (reviewJson.has("data")) {
                        JSONArray reviews = reviewJson.getJSONArray("data");
                        System.out.println("Số lượng đánh giá: " + reviews.length());

                        // Hiển thị thông tin của đánh giá đầu tiên nếu có
                        if (reviews.length() > 0) {
                            JSONObject firstReview = reviews.getJSONObject(0);
                            int rating = firstReview.getInt("rating");
                            String content = firstReview.optString("content", "Không có nội dung");
                            System.out.println("Đánh giá đầu tiên - Rating: " + rating + ", Content: " + content);
                        }
                    }
                }
            } else {
                System.out.println("Không tìm thấy sản phẩm nào cho từ khóa: " + searchQuery);
            }

        } catch (Exception e) {
            System.err.println("Lỗi khi debug tìm kiếm: " + e.getMessage());
            e.printStackTrace();
        }
    }
}