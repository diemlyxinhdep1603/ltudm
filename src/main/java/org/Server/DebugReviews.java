package org.Server;

import org.json.JSONArray;
import org.json.JSONObject;

public class DebugReviews {
    public static void main(String[] args) {
        getReviewTIKIProduct reviewGetter = new getReviewTIKIProduct();

        // Thử tìm kiếm một sản phẩm
        String productName = "iphone 13";
        String productId = reviewGetter.findProductId(productName);

        if (productId != null) {
            System.out.println("Tìm thấy sản phẩm ID: " + productId);

            // Lấy thông tin chi tiết sản phẩm
            JSONObject productDetails = reviewGetter.loadProductDetails(productId);

            if (productDetails != null) {
                System.out.println("Tên sản phẩm: " + productDetails.getString("name"));

                // Lấy thông tin đánh giá
                JSONObject reviewDetails = reviewGetter.loadProductReviews(productId);

                if (reviewDetails != null) {
                    System.out.println("Số lượng đánh giá: " + reviewDetails.getInt("reviews_count"));
                    System.out.println("Đánh giá trung bình: " + reviewDetails.getDouble("rating_average"));

                    // In ra danh sách đánh giá
                    JSONArray reviews = reviewDetails.getJSONArray("data");
                    System.out.println("Số lượng đánh giá trả về: " + reviews.length());

                    for (int i = 0; i < Math.min(5, reviews.length()); i++) {
                        JSONObject review = reviews.getJSONObject(i);

                        // Trích xuất tên người dùng
                        JSONObject createdBy = review.getJSONObject("created_by");
                        String fullName = createdBy.has("full_name") ? createdBy.getString("full_name") : "Ẩn danh";

                        // Trích xuất nội dung và đánh giá
                        String content = review.getString("content");
                        int rating = review.getInt("rating");

                        System.out.println("\nĐánh giá #" + (i+1));
                        System.out.println("Người đánh giá: " + fullName);
                        System.out.println("Đánh giá: " + rating);
                        System.out.println("Nội dung: " + content.substring(0, Math.min(100, content.length())) + "...");

                        // Trích xuất ảnh (nếu có)
                        if (review.has("images") && review.getJSONArray("images").length() > 0) {
                            JSONArray images = review.getJSONArray("images");
                            JSONObject image = images.getJSONObject(0);
                            System.out.println("Ảnh: " + image.getString("full_path"));
                        }
                    }
                }
            }
        } else {
            System.out.println("Không tìm thấy sản phẩm!");
        }
    }
}