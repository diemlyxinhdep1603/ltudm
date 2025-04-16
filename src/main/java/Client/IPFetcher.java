package Client;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.json.JSONObject;

import java.io.IOException;

public class IPFetcher {
    public String fetch_IP() {
        try {
            // URL API để lấy địa chỉ IP
            String api = "https://retoolapi.dev/9EKKBD/data/1";

            // Thực hiện yêu cầu GET để lấy dữ liệu
            Document doc = Jsoup.connect(api)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .header("Content-Type", "application/json")
                    .method(Connection.Method.GET)
                    .execute()
                    .parse();

            // Chuyển đổi dữ liệu JSON
            JSONObject jsonObject = new JSONObject(doc.text());

            // Lấy địa chỉ IP
            String ip = jsonObject.getString("ip");
            System.out.println("Địa chỉ IP của Server: " + ip);
            return ip;
        } catch (IOException e) {
            e.printStackTrace();
            return null; // Trả về null nếu có lỗi
        }
    }
}
