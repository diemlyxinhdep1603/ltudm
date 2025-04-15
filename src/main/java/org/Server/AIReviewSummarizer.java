package org.Server;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AIReviewSummarizer {

    private static final String AI_STUDIO_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final String API_KEY = "AIzaSyCCgeuh9AwVzc3AJ___OpNWgwGqDjH9UIs";

    public String summarizeReviews(String reviews) {
        // Kiểm tra tính hợp lệ của input
        if (reviews == null || reviews.trim().isEmpty()) {
            System.err.println("Không có đánh giá nào để tổng hợp.");
            return "Không có đánh giá nào để tổng hợp.";
        }

        // Kiểm tra nội dung đánh giá
        if (!Valid_Input_Data.isValidReviewContent(reviews)) {
            System.err.println("Nội dung đánh giá không hợp lệ: " + Valid_Input_Data.getLastErrorMessage());
            return "Nội dung đánh giá không hợp lệ.";
        }

        try {
            // Tạo URL từ endpoint và thêm API Key
            URL url = new URL(AI_STUDIO_API_URL + "?key=" + API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Cấu hình yêu cầu HTTP
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Tạo JSON request body
            String jsonInputString = "{\"contents\": [{\"parts\":[{\"text\": \"Tổng hợp các đánh giá sau thành một ý kiến chung nhất, " +
                    "trình bày rõ ràng bao gồm điểm nổi bật của sản phẩm và nhược, điểm nổi bật và nhược điểm hãy viết in hoa và in đậm, ngoài ra các ý kiến bên dưới chỉ cần gạch đầu dòng là được. Xin cảm ơn: " +
                    reviews.replace("\"", "\\\"") + "\"}]}]}";
            System.out.println("JSON Request: " + jsonInputString); // Debug

            // Gửi dữ liệu
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Kiểm tra phản hồi từ server
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Đọc phản hồi thành công
                try (java.util.Scanner s = new java.util.Scanner(conn.getInputStream(), StandardCharsets.UTF_8).useDelimiter("\\A")) {
                    String jsonResponse = s.hasNext() ? s.next() : "";
                    String extractedText = extractTextFromResponse(jsonResponse);
                    return extractedText.isEmpty() ? "Không thể tổng hợp đánh giá." : extractedText;
                }
            } else {
                // Đọc phản hồi lỗi
                try (java.util.Scanner s = new java.util.Scanner(conn.getErrorStream(), StandardCharsets.UTF_8).useDelimiter("\\A")) {
                    String errorResponse = s.hasNext() ? s.next() : "";
                    System.err.println("Lỗi từ API: " + errorResponse);
                    return "Lỗi khi gửi dữ liệu đến API: " + responseCode + "\n" + errorResponse;
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi dữ liệu đến API: " + e.getMessage());
            return "Lỗi khi gửi dữ liệu đến API: " + e.getMessage();
        }
    }


    /**
     * Trích xuất nội dung từ phản hồi JSON của API
     *
     * @param jsonResponse Phản hồi JSON từ API
     * @return Nội dung từ trường "text"
     */
    private String extractTextFromResponse(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            JSONArray candidates = json.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject firstCandidate = candidates.getJSONObject(0);
                JSONObject content = firstCandidate.getJSONObject("content");
                JSONArray parts = content.getJSONArray("parts");
                if (parts.length() > 0) {
                    String text = parts.getJSONObject(0).getString("text");
                    return text.trim().isEmpty() ? "" : text;
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi phân tích phản hồi JSON: " + e.getMessage());
        }
        return "";
    }
}
