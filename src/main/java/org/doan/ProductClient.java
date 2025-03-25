package org.doan;

import java.io.*;
import java.net.Socket;

public class ProductClient {
    private String host;
    private int port;

    public ProductClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String searchProduct(String productName) throws IOException {
        try (Socket socket = new Socket(host, port);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            writer.println(productName);
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals("<END>")) {
                    break;
                }
                response.append(line).append("\n");
            }
            return response.toString().trim();
        }
    }
}