package org.Server;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.json.JSONObject;
import org.json.JSONArray;

public class getReviewAmazonProduct {

    // Hàm tìm kiếm sản phẩm tương tự
    public List<String> searchSimilarProducts(String searchQuery) {
        List<String> productSuggestions = new ArrayList<>();
        try {
            // Tạo URL tìm kiếm trên Amazon
            String encodedQuery = URLEncoder.encode(searchQuery, "UTF-8");
            String apiUrl = "https://www.amazon.com/s?k=" + encodedQuery;

            // Gọi trang và lấy HTML response
            Document doc = Jsoup.connect(apiUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get();

            // Parse HTML để lấy danh sách sản phẩm
            doc.select("div.s-main-slot div.s-result-item").forEach(element -> {
                String productName = element.select("h2 a span").text();
                if (!productName.isEmpty()) {
                    productSuggestions.add(productName);
                }
            });

        } catch (Exception e) {
            System.err.println("Lỗi khi tìm kiếm sản phẩm: " + e.getMessage());
        }
        return productSuggestions;
    }

    // Hàm lấy thông tin chi tiết sản phẩm
    public String getProductInfo(String productName) {
        try {
            // Tạo URL tìm kiếm sản phẩm
            String encodedQuery = URLEncoder.encode(productName, "UTF-8");
            String apiUrl = "https://www.amazon.com/s?k=" + encodedQuery;

            // Gọi trang và lấy HTML response
            Document doc = Jsoup.connect(apiUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get();

            // Parse HTML để lấy sản phẩm đầu tiên
            String productId = doc.select("div.s-main-slot div.s-result-item").first().attr("data-asin");
            String productUrl = "https://www.amazon.com/dp/" + productId;

            // Gọi trang sản phẩm để lấy thông tin chi tiết
            Document productDoc = Jsoup.connect(productUrl).get();
            String name = productDoc.select("#productTitle").text();
            String price = productDoc.select("#priceblock_ourprice").text();
            String imgUrl = productDoc.select("#imgTagWrapperId img").attr("src");

            return "Tên sản phẩm: " + name + "\nGiá: " + price + "\nHình ảnh: " + imgUrl;

        } catch (Exception e) {
            return "Lỗi khi lấy thông tin sản phẩm: " + e.getMessage();
        }
    }

    // Hàm lấy đánh giá sản phẩm
    public String getProductReviews(String productName) {
        try {
            // Tạo URL tìm kiếm sản phẩm
            String encodedQuery = URLEncoder.encode(productName, "UTF-8");
            String apiUrl = "https://www.amazon.com/s?k=" + encodedQuery;

            // Gọi trang và lấy HTML response
            Document doc = Jsoup.connect(apiUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get();

            // Parse HTML để lấy sản phẩm đầu tiên
            String productId = doc.select("div.s-main-slot div.s-result-item").first().attr("data-asin");
            String productUrl = "https://www.amazon.com/dp/" + productId;

            // Gọi trang sản phẩm để lấy đánh giá
            Document productDoc = Jsoup.connect(productUrl).get();
            JSONArray reviews = new JSONArray();

            productDoc.select(".review").forEach(reviewElement -> {
                String reviewContent = reviewElement.select(".review-text-content span").text();
                String reviewerName = reviewElement.select(".a-profile-name").text();
                String rating = reviewElement.select(".a-icon-alt").text();
                JSONObject review = new JSONObject();
                review.put("reviewer", reviewerName);
                review.put("rating", rating);
                review.put("content", reviewContent);
                reviews.put(review);
            });

            return reviews.toString();

        } catch (Exception e) {
            return "Lỗi khi lấy đánh giá sản phẩm: " + e.getMessage();
        }
    }
}