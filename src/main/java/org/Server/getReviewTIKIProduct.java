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
import org.json.JSONException;

public class getReviewTIKIProduct {

    /**
     * Phương thức tiện ích để lấy giá trị từ JSONObject một cách an toàn với bất kỳ kiểu dữ liệu
     * @param json JSONObject cần trích xuất dữ liệu
     * @param key Khóa cần lấy giá trị
     * @return Giá trị dưới dạng chuỗi, trả về "0" nếu không tìm thấy hoặc có lỗi
     */
    private String safeGetValue(JSONObject json, String key) {
        try {
            if (!json.has(key) || json.isNull(key)) {
                return "0";
            }
            
            Object value = json.get(key);
            
            // Xử lý các kiểu dữ liệu khác nhau
            if (value instanceof Integer || value instanceof Long) {
                return String.valueOf(value);
            } else if (value instanceof Double || value instanceof Float) {
                return String.valueOf(value);
            } else if (value instanceof String) {
                return (String) value;
            } else {
                // Đối với các đối tượng phức tạp, chuyển đổi thành chuỗi
                return value.toString();
            }
        } catch (JSONException e) {
            System.err.println("Lỗi khi lấy giá trị cho khóa '" + key + "': " + e.getMessage());
            return "0";
        }
    }
    
    /**
     * Phương thức lấy giá trị Integer an toàn từ JSONObject với giá trị mặc định
     * @param json JSONObject cần trích xuất dữ liệu
     * @param key Khóa cần lấy giá trị
     * @param defaultValue Giá trị mặc định nếu không thể lấy được hoặc có lỗi
     * @return Giá trị int, trả về defaultValue nếu có lỗi
     */
    private int safeGetInt(JSONObject json, String key, int defaultValue) {
        try {
            if (!json.has(key) || json.isNull(key)) {
                return defaultValue;
            }
            
            Object value = json.get(key);
            
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof Long) {
                return ((Long) value).intValue();
            } else if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            } else {
                return defaultValue;
            }
        } catch (JSONException e) {
            System.err.println("Lỗi khi lấy giá trị integer cho khóa '" + key + "': " + e.getMessage());
            return defaultValue;
        }
    }
    
    /**
     * Phương thức lấy giá trị Double an toàn từ JSONObject với giá trị mặc định
     * @param json JSONObject cần trích xuất dữ liệu
     * @param key Khóa cần lấy giá trị
     * @param defaultValue Giá trị mặc định nếu không thể lấy được hoặc có lỗi
     * @return Giá trị double, trả về defaultValue nếu có lỗi
     */
    private double safeGetDouble(JSONObject json, String key, double defaultValue) {
        try {
            if (!json.has(key) || json.isNull(key)) {
                return defaultValue;
            }
            
            Object value = json.get(key);
            
            if (value instanceof Double) {
                return (Double) value;
            } else if (value instanceof Float) {
                return ((Float) value).doubleValue();
            } else if (value instanceof Integer) {
                return ((Integer) value).doubleValue();
            } else if (value instanceof Long) {
                return ((Long) value).doubleValue();
            } else if (value instanceof String) {
                try {
                    return Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            } else {
                return defaultValue;
            }
        } catch (JSONException e) {
            System.err.println("Lỗi khi lấy giá trị double cho khóa '" + key + "': " + e.getMessage());
            return defaultValue;
        }
    }

    // Hàm lấy danh sách tên gợi ý sản phẩm từ API của Tiki
    public Map<String, String> getSuggestedProducts(String searchQuery) {
        Map<String, String> suggestions = new HashMap<>();
        
        // Kiểm tra tính hợp lệ của từ khóa tìm kiếm
        if (!Valid_Input_Data.isValidKeyword(searchQuery)) {
            System.err.println("Từ khóa tìm kiếm không hợp lệ: " + searchQuery);
            return suggestions;
        }
        
        try {
            String encodedQuery = URLEncoder.encode(searchQuery, "UTF-8");
            // Đây là API để tìm tên gợi ý của sản phẩm.
            String api = "https://tiki.vn/api/shopping/v2/featured_keywords?page_name=Search&q=" + encodedQuery;
            
            // Kiểm tra tính hợp lệ của URL API
            if (!Valid_Input_Data.isValidUrl(api)) {
                System.err.println("URL API gợi ý không hợp lệ: " + api);
                return suggestions;
            }
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

    // Hàm tìm kiếm sản phẩm theo từ khóa và lấy ID sản phẩm đầu tiên
    public String findProductId(String productName) {
        // Kiểm tra tính hợp lệ của từ khóa tìm kiếm
        if (!Valid_Input_Data.isValidKeyword(productName)) {
            System.err.println("Từ khóa tìm kiếm không hợp lệ: " + productName);
            return null;
        }
        
        try {
            // Mã hóa tên sản phẩm và tạo URL tìm kiếm
            String encodedQuery = URLEncoder.encode(productName, "UTF-8");
            String apiUrl = "https://tiki.vn/api/v2/products?limit=40&include=advertisement&aggregations=2&q=" + encodedQuery + "&rating=5";
            
            // Kiểm tra tính hợp lệ của URL API
            if (!Valid_Input_Data.isValidUrl(apiUrl)) {
                System.err.println("URL API không hợp lệ: " + apiUrl);
                return null;
            }
            
            // Gọi API và lấy JSON response
            String jsonResponse = Jsoup.connect(apiUrl)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .execute()
                    .body();

            // Phân tích JSON để lấy sản phẩm đầu tiên
            JSONObject json = new JSONObject(jsonResponse);
            JSONArray products = json.getJSONArray("data");

            // Kiểm tra nếu có sản phẩm, lấy ID sản phẩm đầu tiên
            if (products.length() > 0) {
                JSONObject firstProduct = products.getJSONObject(0);
                // Lấy ID trực tiếp từ trường "id"
                String productId = String.valueOf(firstProduct.getInt("id"));
                System.out.println("DEBUG - Tìm thấy ID sản phẩm đầu tiên: " + productId + " với tên: " + firstProduct.optString("name", "Không có tên"));
                return productId;
            }
        } catch (Exception e) {
            System.err.println("Lỗi tìm kiếm sản phẩm: " + e.getMessage());
        }
        return null;
    }

    // Hàm lấy ID sản phẩm ĐẦU TIÊN từ URL gợi ý
    public String getProductIDFromUrl(String productName) {
        // Kiểm tra tính hợp lệ của từ khóa tìm kiếm
        if (!Valid_Input_Data.isValidKeyword(productName)) {
            System.err.println("Từ khóa tìm kiếm không hợp lệ: " + productName);
            return null;
        }
        
        try {
            // Lấy danh sách các gợi ý từ getSuggestedProducts
            Map<String, String> suggestions = getSuggestedProducts(productName);
            if (suggestions.containsKey(productName)){
                String searchUrl = suggestions.get(productName);
                //Mã hóa url và gọi API để lấy danh sách sản phẩm
                String encodeUrl = URLEncoder.encode(searchUrl, "UTF-8");
                String apiUrl = "https://tiki.vn/api/v2/products?limit=40&include=advertisement&aggregations=2&q=" + encodeUrl;
                
                // Kiểm tra tính hợp lệ của URL API
                if (!Valid_Input_Data.isValidUrl(apiUrl)) {
                    System.err.println("URL API không hợp lệ: " + apiUrl);
                    return null;
                }
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
                    // Lấy ID trực tiếp từ trường "id"
                    return String.valueOf(firstProduct.getInt("id"));
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi lấy ID sản phẩm từ URL: " + e.getMessage());
        }
        return null;
    }

    // Hàm lấy thông tin chi tiết sản phẩm từ ID
    public JSONObject loadProductDetails(String productId) {
        // Kiểm tra tính hợp lệ của ID sản phẩm
        if (!Valid_Input_Data.isValidProductId(productId)) {
            System.err.println("ID sản phẩm không hợp lệ: " + productId);
            return null;
        }
        
        try {
            String apiUrl = "https://tiki.vn/api/v2/products/" + productId;
            
            // Kiểm tra tính hợp lệ của URL
            if (!Valid_Input_Data.isValidUrl(apiUrl)) {
                System.err.println("URL API không hợp lệ: " + apiUrl);
                return null;
            }
            
            String jsonResponse = Jsoup.connect(apiUrl)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .execute()
                    .body();
            
            JSONObject productJson = new JSONObject(jsonResponse);
            
            // Debug: Hiển thị thông tin về trường price để kiểm tra kiểu dữ liệu
            if (productJson.has("price")) {
                Object priceObj = productJson.get("price");
                System.out.println("DEBUG - Price: " + priceObj + ", Class: " + priceObj.getClass().getName());
            } else {
                System.out.println("DEBUG - Không tìm thấy trường price trong phản hồi API");
            }
            
            return productJson;
        } catch (Exception e) {
            System.err.println("Lỗi lấy thông tin sản phẩm: " + e.getMessage());
            return null;
        }
    }

    // Hàm lấy đánh giá sản phẩm từ ID
    public JSONObject loadProductReviews(String productId) {
        // Kiểm tra tính hợp lệ của ID sản phẩm
        if (!Valid_Input_Data.isValidProductId(productId)) {
            System.err.println("ID sản phẩm không hợp lệ: " + productId);
            return null;
        }
        
        try {
            String apiUrl = "https://tiki.vn/api/v2/reviews?limit=10&product_id=" + productId;
            
            // Kiểm tra tính hợp lệ của URL
            if (!Valid_Input_Data.isValidUrl(apiUrl)) {
                System.err.println("URL API không hợp lệ: " + apiUrl);
                return null;
            }
            
            String jsonResponse = Jsoup.connect(apiUrl)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .execute()
                    .body();
            return new JSONObject(jsonResponse);
        } catch (Exception e) {
            System.err.println("Lỗi lấy đánh giá sản phẩm: " + e.getMessage());
            return null;
        }
    }

    // Hàm lấy thông tin sản phẩm , DỰA TRÊN ID TRÊN ==> Hiển thị ở OVERVIEW
    public String getProductInfo(String input) {
        String ID = findProductId(input);  // Tìm ID sản phẩm
        if (ID == null){
            return "Không tìm thấy sản phẩm để lấy đánh giá.";
        }
        
        JSONObject product = loadProductDetails(ID);
        if (product == null) {
            return "Lỗi khi lấy thông tin sản phẩm.";
        }
        
        // Sử dụng phương thức an toàn để lấy dữ liệu
        String productName = product.optString("name", "Không có tên");
        String productPrice = safeGetValue(product, "price");
        
        return "Tên sản phẩm: " + productName + " .Có giá là: " + productPrice;
    }

    // Hàm lấy các review của người dùng đã mua sản phẩm
    public String getProductReviews(String productName) {
        // Tìm ID sản phẩm trực tiếp từ từ khóa tìm kiếm
        System.out.println("DEBUG - Bắt đầu tìm kiếm sản phẩm cho từ khóa: " + productName);
        String ID = findProductId(productName);
        
        if (ID == null) {
            System.out.println("DEBUG - Không tìm thấy ID sản phẩm cho từ khóa: " + productName);
            return "Không tìm thấy sản phẩm để lấy đánh giá.";
        }
        
        System.out.println("DEBUG - Đã tìm thấy ID sản phẩm: " + ID + " cho từ khóa: " + productName);

        try {
            // Lấy thông tin chi tiết sản phẩm
            JSONObject productDetails = loadProductDetails(ID);
            if (productDetails == null) {
                return "Lỗi khi lấy thông tin sản phẩm.";
            }
            
            // Lấy thông tin sản phẩm sử dụng phương thức an toàn
            String name = productDetails.optString("name", "Không có tên");
            // Làm sạch tên sản phẩm - thay thế ký tự | bằng -
            name = name.replace("|", "-");
            String price = safeGetValue(productDetails, "price");
            
            // Tìm hình ảnh phù hợp từ mảng images
            String imgUrl = "Không có";
            try {
                if (productDetails.has("images") && !productDetails.isNull("images")) {
                    JSONArray images = productDetails.getJSONArray("images");
                    // Kiểm tra nếu có hình ảnh
                    if (images.length() > 0) {
                        // Thử lấy 3 hình ảnh đầu tiên, nếu có lỗi thì thử hình tiếp theo
                        for (int i = 0; i < Math.min(3, images.length()); i++) {
                            try {
                                JSONObject imageObj = images.getJSONObject(i);
                                // Kiểm tra xem đây có phải là video không
                                if (!imageObj.has("type") || !imageObj.getString("type").equals("video")) {
                                    imgUrl = imageObj.optString("base_url", "Không có");
                                    break;
                                }
                            } catch (Exception e) {
                                System.err.println("Lỗi khi xử lý hình ảnh thứ " + i + ": " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi xử lý mảng hình ảnh: " + e.getMessage());
            }
            
            // Lấy thông tin đánh giá
            System.out.println("DEBUG - Bắt đầu lấy đánh giá cho sản phẩm có ID: " + ID);
            JSONObject reviewDetails = loadProductReviews(ID);
            if (reviewDetails == null) {
                System.out.println("DEBUG - Không lấy được đánh giá cho sản phẩm có ID: " + ID);
                return "Lỗi khi lấy đánh giá sản phẩm.";
            }
            
            // Sử dụng các phương thức an toàn
            int reviewCount = reviewDetails.optInt("reviews_count", reviewDetails.optInt("total", 0));
            double avgRating = safeGetDouble(reviewDetails, "rating_average", 0.0);
            System.out.println("DEBUG - Đánh giá sản phẩm: count=" + reviewCount + ", avgRating=" + avgRating);
            
            JSONArray reviews = reviewDetails.getJSONArray("data");
            System.out.println("DEBUG - Số lượng đánh giá lấy được: " + reviews.length());
            
            // Xây dựng chuỗi phản hồi với định dạng đặc biệt để phân tích ở client
            StringBuilder response = new StringBuilder();
            response.append("Product: ").append(name).append(" | ");
            response.append("Price: ").append(price).append(" | ");
            response.append("Image: ").append(imgUrl).append(" | ");
            response.append("ReviewCount: ").append(reviewCount).append(" | ");
            response.append("AvgRating: ").append(avgRating).append(" | ");
            response.append("Reviews: ");
            
            // Trích xuất từng đánh giá
            for (int i = 0; i < reviews.length(); i++) {
                JSONObject review = reviews.getJSONObject(i);
                
                // Sử dụng các phương thức an toàn để trích xuất dữ liệu
                
                // Trích xuất tên người dùng
                String fullName = "Ẩn danh";
                try {
                    if (review.has("created_by") && !review.isNull("created_by")) {
                        JSONObject createdBy = review.getJSONObject("created_by");
                        if (createdBy.has("full_name") && !createdBy.isNull("full_name")) {
                            fullName = createdBy.getString("full_name");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi khi lấy tên người dùng: " + e.getMessage());
                }
                
                // Trích xuất nội dung bình luận
                String content = "";
                try {
                    content = review.optString("content", "");
                    // Loại bỏ các ký tự đặc biệt có thể gây lỗi khi phân tích
                    content = content.replace("|", "-").replace(";", ",");
                } catch (Exception e) {
                    System.err.println("Lỗi khi lấy nội dung bình luận: " + e.getMessage());
                }
                
                // Trích xuất đánh giá sao
                int rating = safeGetInt(review, "rating", 5); // Mặc định là 5 sao
                
                // Trích xuất ảnh (nếu có)
                String imageUrl = "";
                try {
                    if (review.has("images") && !review.isNull("images")) {
                        JSONArray reviewImages = review.getJSONArray("images");
                        if (reviewImages.length() > 0) {
                            JSONObject image = reviewImages.getJSONObject(0); // Lấy ảnh đầu tiên
                            imageUrl = image.optString("full_path", "");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi khi lấy hình ảnh đánh giá: " + e.getMessage());
                }
                
                // Trích xuất video (nếu có)
                String videoUrl = "";
                try {
                    if (review.has("video_url") && !review.isNull("video_url")) {
                        videoUrl = review.getString("video_url");
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi khi lấy video đánh giá: " + e.getMessage());
                }
                
                // Thêm vào chuỗi phản hồi với định dạng riêng để dễ phân tích
                response.append(fullName).append(" | ").append(rating).append(" | ").append(content).append(" | ")
                       .append(imageUrl).append(" | ").append(videoUrl).append(";");
            }
            
            return response.toString();
            
        } catch (Exception e) {
            return "Lỗi khi xử lý yêu cầu: " + e.getMessage();
        }
    }
    
    // Hàm lấy thông tin đánh giá của sản phẩm từ gợi ý
    public String getProductReviewsFromSuggestion(String searchKeyword) {
        Map<String, String> suggestions = getSuggestedProducts(searchKeyword);

        // Nếu có gợi ý, lấy sản phẩm đầu tiên
        if (!suggestions.isEmpty()) {
            String firstSuggestion = suggestions.keySet().iterator().next();
            String productId = getProductIDFromUrl(firstSuggestion); // Lấy ID từ URL gợi ý

            if (productId != null) {
                try {
                    // Lấy thông tin chi tiết sản phẩm
                    JSONObject productDetails = loadProductDetails(productId);
                    if (productDetails == null) {
                        return "Lỗi khi lấy thông tin sản phẩm.";
                    }
                    
                    // Lấy thông tin sản phẩm sử dụng phương thức an toàn
                    String name = productDetails.optString("name", "Không có tên");
                    // Làm sạch tên sản phẩm - thay thế ký tự | bằng -
                    name = name.replace("|", "-");
                    String price = safeGetValue(productDetails, "price");
                    
                    // Tìm hình ảnh phù hợp từ mảng images
                    String imgUrl = "Không có";
                    try {
                        if (productDetails.has("images") && !productDetails.isNull("images")) {
                            JSONArray images = productDetails.getJSONArray("images");
                            // Kiểm tra nếu có hình ảnh
                            if (images.length() > 0) {
                                // Thử lấy 3 hình ảnh đầu tiên, nếu có lỗi thì thử hình tiếp theo
                                for (int i = 0; i < Math.min(3, images.length()); i++) {
                                    try {
                                        JSONObject imageObj = images.getJSONObject(i);
                                        // Kiểm tra xem đây có phải là video không
                                        if (!imageObj.has("type") || !imageObj.getString("type").equals("video")) {
                                            imgUrl = imageObj.optString("base_url", "Không có");
                                            break;
                                        }
                                    } catch (Exception e) {
                                        System.err.println("Lỗi khi xử lý hình ảnh thứ " + i + ": " + e.getMessage());
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Lỗi khi xử lý mảng hình ảnh: " + e.getMessage());
                    }
                    
                    // Lấy thông tin đánh giá
                    JSONObject reviewDetails = loadProductReviews(productId);
                    if (reviewDetails == null) {
                        return "Lỗi khi lấy đánh giá sản phẩm.";
                    }
                    
                    // Sử dụng các phương thức an toàn
                    int reviewCount = reviewDetails.optInt("reviews_count", reviewDetails.optInt("total", 0));
                    double avgRating = safeGetDouble(reviewDetails, "rating_average", 0.0);
                    JSONArray reviews = reviewDetails.getJSONArray("data");
                    
                    // Xây dựng chuỗi phản hồi
                    StringBuilder response = new StringBuilder();
                    response.append("Product: ").append(name).append(" | ");
                    response.append("Price: ").append(price).append(" | ");
                    response.append("Image: ").append(imgUrl).append(" | ");
                    response.append("ReviewCount: ").append(reviewCount).append(" | ");
                    response.append("AvgRating: ").append(avgRating).append(" | ");
                    response.append("Reviews: ");
                    
                    // Trích xuất từng đánh giá
                    for (int i = 0; i < reviews.length(); i++) {
                        JSONObject review = reviews.getJSONObject(i);
                        
                        // Sử dụng các phương thức an toàn để trích xuất dữ liệu
                        
                        // Trích xuất tên người dùng
                        String fullName = "Ẩn danh";
                        try {
                            if (review.has("created_by") && !review.isNull("created_by")) {
                                JSONObject createdBy = review.getJSONObject("created_by");
                                if (createdBy.has("full_name") && !createdBy.isNull("full_name")) {
                                    fullName = createdBy.getString("full_name");
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi khi lấy tên người dùng: " + e.getMessage());
                        }
                        
                        // Trích xuất nội dung bình luận
                        String content = "";
                        try {
                            content = review.optString("content", "");
                            // Loại bỏ các ký tự đặc biệt có thể gây lỗi khi phân tích
                            content = content.replace("|", "-").replace(";", ",");
                        } catch (Exception e) {
                            System.err.println("Lỗi khi lấy nội dung bình luận: " + e.getMessage());
                        }
                        
                        // Trích xuất đánh giá sao
                        int rating = safeGetInt(review, "rating", 5); // Mặc định là 5 sao
                        
                        // Trích xuất ảnh (nếu có)
                        String imageUrl = "";
                        try {
                            if (review.has("images") && !review.isNull("images")) {
                                JSONArray reviewImages = review.getJSONArray("images");
                                if (reviewImages.length() > 0) {
                                    JSONObject image = reviewImages.getJSONObject(0); // Lấy ảnh đầu tiên
                                    imageUrl = image.optString("full_path", "");
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi khi lấy hình ảnh đánh giá: " + e.getMessage());
                        }
                        
                        // Trích xuất video (nếu có)
                        String videoUrl = "";
                        try {
                            if (review.has("video_url") && !review.isNull("video_url")) {
                                videoUrl = review.getString("video_url");
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi khi lấy video đánh giá: " + e.getMessage());
                        }
                        
                        // Thêm vào chuỗi phản hồi với định dạng riêng để dễ phân tích
                        response.append(fullName).append(" | ").append(rating).append(" | ").append(content).append(" | ")
                               .append(imageUrl).append(" | ").append(videoUrl).append(";");
                    }
                    
                    return response.toString();
                } catch (Exception e) {
                    return "Lỗi khi xử lý yêu cầu: " + e.getMessage();
                }
            }
        }
        return "Không tìm thấy sản phẩm hoặc gợi ý.";
    }
}
