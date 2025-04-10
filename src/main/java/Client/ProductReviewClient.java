package Client;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Client cho ứng dụng đánh giá sản phẩm.
 * Xử lý giao tiếp với server và xử lý phản hồi.
 */
public class ProductReviewClient {
    private String serverHost;
    private int serverPort;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String currentPlatform = "TIKI"; // Default platform

    /**
     * Constructor
     *
     * @param serverHost Server hostname or IP
     * @param serverPort Server port
     */
    public ProductReviewClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    /**
     * Tìm kiếm đánh giá sản phẩm
     *
     * @param productName: Tên sản phẩm cần tìm kiếm
     * @return Danh sách đánh giá hoặc đề xuất
     */
    public List<String> searchProduct(String productName) {
        List<String> results = new ArrayList<>();

        try {
            // Kết nối tới máy chủ nếu chưa kết nối
            if (!isConnected()) {
                if (!connect()) {
                    results.add("Không thể kết nối đến máy chủ.");
                    return results;
                }
            }

            // Gửi tên sản phẩm - productName đến máy chủ
            writer.println(productName);

            // Process response
            String response;
            while ((response = reader.readLine()) != null) {
                if (response.equals("<END>")) {
                    break;
                }
                results.add(response);
            }
        } catch (IOException e) {
            results.add("Lỗi khi tìm kiếm sản phẩm: " + e.getMessage());
        }

        return results;
    }

    /**
     * Thay đổi nền tảng hiện tại để đánh giá sản phẩm - platform
     *
     * @param platform (TIKI, thegioididong,....)
     * @return The server's response
     */
    public String changePlatform(String platform) {
        try {
            if (!isConnected()) {
                if (!connect()) {
                    return "Không thể kết nối đến máy chủ.";
                }
            }

            // Send platform change request to server
            String request = "PLATFORM:" + platform;
            writer.println(request);

            // Read response
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("<END>")) {
                    break;
                }
                response.append(line).append("\n");
            }

            // Update current platform if successful
            if (response.toString().contains("Đã chuyển sang nền tảng")) {
                currentPlatform = platform;
            }

            return response.toString().trim();
        } catch (IOException e) {
            return "Lỗi khi chuyển đổi nền tảng: " + e.getMessage();
        }
    }

    /**
     * Get the current platform
     *
     * @return The current platform name
     */
    public String getCurrentPlatform() {
        return currentPlatform;
    }

    /**
     * Connect to the server
     *
     * @return true if connection successful, false otherwise
     */
    public boolean connect() {
        try {
            socket = new Socket(serverHost, serverPort);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            return true;
        } catch (IOException e) {
            System.err.println("Lỗi kết nối đến server: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gửi yêu cầu đến máy chủ
     *
     * @param request Yêu cầu gửi
     * @return The server's response
     * @throws IOException If an I/O error occurs
     */
    public String sendRequest(String request) throws IOException {
        writer.println(request);
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals("<END>")) {
                break;
            }
            response.append(line).append("\n");
        }
        return response.toString();
    }

    /**
     * Đóng kết nối đến server
     */
    public void close() {
        try {
            if (writer != null) {
                writer.println("bye");
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }

    /**
     * Check if connected to the server
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}