package Client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private String host;
    private int port;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // Khởi tạo Client socket và gắn kết stream
    public void start() {
        try (Socket socket = new Socket(host, port)) {
            System.out.println("Đã kết nối đến server " + socket.getRemoteSocketAddress());
            startCommunication(socket);
        } catch (IOException e) {
            System.err.println("Lỗi kết nối đến server: " + e.getMessage());
        }
    }

    // Gắn kết stream
    private void startCommunication(Socket socket) {
        try (Scanner scanner = new Scanner(System.in);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {
            String userInput;
            while (true) {
                System.out.print("Nhập dữ liệu: ");
                userInput = scanner.nextLine();
                writer.println(userInput); // Gửi dữ liệu --> server
                String response;
                while((response = reader.readLine())!=null){
                    if(response.equals("<END>")) // Server sẽ gửi chuỗi <END> để báo hiệu kết thúc tin nhắn.
                        break;
                    System.out.println(response);
                }
                if(userInput.equalsIgnoreCase("bye")){
                    System.out.println("Client gửi yêu cầu đóng kết nối.");
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Lỗi gửi/nhận dữ liệu: " + e.getMessage());
        }

    }

    public static void main(String[] args) {
        Client client = new Client("localhost", 1234);
        client.start();
    }
}
