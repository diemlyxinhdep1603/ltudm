package org.doan;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import org.jsoup.Jsoup;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;

public class ProductReviewServer {
    private int port;

    public ProductReviewServer(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server đang lắng nghe tại port " + port);
            while (true) {
                Socket socket = server.accept();
                new Thread(() -> handleClient(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("Lỗi khởi tạo server socket: " + e.getMessage());
        }
    }

    private void handleClient(Socket socket) {
        System.out.println("Đã chấp nhận kết nối từ client: " + socket.getRemoteSocketAddress());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {
            String productName;
            while ((productName = reader.readLine()) != null) {
                System.out.println("Server nhận tên sản phẩm: " + productName);
                if (productName.equalsIgnoreCase("bye")) {
                    System.out.println("Server nhận yêu cầu đóng kết nối từ client.");
                    break;
                }
                String response = processRequest(productName);
                writer.println(response);
                writer.println("<END>");
            }
        } catch (IOException e) {
            System.err.println("Lỗi kết nối từ client: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Lỗi đóng socket: " + e.getMessage());
            }
        }
    }

    private String processRequest(String searchQuery) {
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
                return "Không tìm thấy sản phẩm nào cho từ khóa: " + searchQuery;
            }

            // Lấy productId của sản phẩm đầu tiên
            JSONObject firstProduct = products.getJSONObject(0);
            // Cách sửa 1
            //int productIdInt = firstProduct.getInt("id");         // Lấy giá trị id dưới dạng số nguyên
            //String productId = String.valueOf(productIdInt);      // Chuyển đổi thành chuỗi
            // Cách sửa 2
            String productId = String.valueOf(firstProduct.get("id"));  // Lấy và chuyển đổi trực tiếp

            // Tiếp tục xử lý chi tiết sản phẩm và đánh giá
            JSONObject productDetails = loadProductDetails(productId);
            String name = productDetails.getString("name");
            int price = productDetails.getInt("price");
            String imgUrl = productDetails.getJSONArray("images").getJSONObject(0).getString("base_url");

            JSONObject reviewDetails = loadProductReviews(productId);
            int reviewCount = reviewDetails.getInt("reviews_count");
            double avgRating = reviewDetails.getDouble("rating_average");
            JSONArray reviews = reviewDetails.getJSONArray("data");

            // Xây dựng phản hồi
            StringBuilder response = new StringBuilder();
            response.append("Product: ").append(name).append(" | ");
            response.append("Price: ").append(price).append(" | ");
            response.append("Image: ").append(imgUrl).append(" | ");
            response.append("ReviewCount: ").append(reviewCount).append(" | ");
            response.append("AvgRating: ").append(avgRating).append(" | ");
            response.append("Reviews: ");
            for (int i = 0; i < reviews.length(); i++) {
                JSONObject review = reviews.getJSONObject(i);
                // Lấy đối tượng created_by
                JSONObject createdBy = review.getJSONObject("created_by");
                String fullName;
                // Kiểm tra sự tồn tại của full_name trong created_by
                if (createdBy.has("full_name")) {
                    fullName = createdBy.getString("full_name");
                } else {
                    fullName = "Ẩn danh"; // Giá trị mặc định nếu không có full_name
                }
                String content = review.getString("content");
                int rating = review.getInt("rating");
                response.append(fullName).append(": ").append(content).append(" (").append(rating).append(");");
            }
            return response.toString();

        } catch (Exception e) {
            return "Lỗi khi xử lý yêu cầu: " + e.getMessage();
        }
    }

    private JSONObject loadProductDetails(String productId) throws IOException {
        String apiUrl = "https://tiki.vn/api/v2/products/" + productId;
        String jsonResponse = Jsoup.connect(apiUrl).ignoreContentType(true).execute().body();
        return new JSONObject(jsonResponse);
    }

    private JSONObject loadProductReviews(String productId) throws IOException {
        String apiUrl = "https://tiki.vn/api/v2/reviews?limit=10&product_id=" + productId;
        String jsonResponse = Jsoup.connect(apiUrl).ignoreContentType(true).execute().body();
        return new JSONObject(jsonResponse);
    }

    public static void main(String[] args) {
        ProductReviewServer server = new ProductReviewServer(12345);
        server.start();
    }
}