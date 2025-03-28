package org.doan;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

public class GUI extends JFrame {
    private JTextField txtProductName;
    private JButton btnSearch;
    private DefaultListModel<String> reviewListModel;
    private JList<String> reviewList;
    private JLabel lblImage;
    private JLabel lblProductName;
    private JLabel lblPrice;
    private JLabel lblAvgRating;
    private ProductClient client;

    public GUI() {
        setTitle("Tổng Hợp Review Sản Phẩm");
        setSize(770, 530);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout(10, 10));

        // Khởi tạo client
        client = new ProductClient("localhost", 12345);

        // Top panel
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        txtProductName = new JTextField(20);
        btnSearch = new JButton(" Tìm Kiếm");
        btnSearch.setFont(new Font("Arial", Font.BOLD, 14));
        btnSearch.setBackground(new Color(52, 152, 219));
        btnSearch.setForeground(Color.WHITE);
        topPanel.add(txtProductName, BorderLayout.CENTER);
        topPanel.add(btnSearch, BorderLayout.EAST);
        getContentPane().add(topPanel, BorderLayout.NORTH);

        // Center panel
        JPanel centerPanel = new JPanel(new BorderLayout());
        JPanel detailsPanel = new JPanel(new GridLayout(3, 1));
        lblProductName = new JLabel("Tên sản phẩm: ");
        lblPrice = new JLabel("Giá: ");
        lblAvgRating = new JLabel("Đánh giá trung bình: ");
        detailsPanel.add(lblProductName);
        detailsPanel.add(lblPrice);
        detailsPanel.add(lblAvgRating);

        reviewListModel = new DefaultListModel<>();
        reviewList = new JList<>(reviewListModel);
        reviewList.setFont(new Font("Roboto", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(reviewList);
        centerPanel.add(detailsPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(centerPanel, BorderLayout.CENTER);

        // Image panel (đặt ở WEST thay vì SOUTH để có nhiều không gian hơn)
        JPanel imagePanel = new JPanel();
        lblImage = new JLabel();
        lblImage.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        lblImage.setPreferredSize(new Dimension(200, 200));
        lblImage.setMinimumSize(new Dimension(200, 200));
        imagePanel.add(lblImage);
        getContentPane().add(imagePanel, BorderLayout.WEST);

        // Sự kiện nút tìm kiếm
        btnSearch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String productName = txtProductName.getText().trim();
                if (!productName.isEmpty()) {
                    searchProduct(productName);
                } else {
                    JOptionPane.showMessageDialog(null, "Vui lòng nhập tên sản phẩm!");
                }
            }
        });

        setVisible(true);
    }

    private void searchProduct(String productName) {
        try {
            String response = client.searchProduct(productName);
            if (response.startsWith("Lỗi") || response.startsWith("Không tìm thấy")) {
                JOptionPane.showMessageDialog(null, response);
                return;
            }

            // Phân tích dữ liệu từ server
            String[] parts = response.split(" \\| ");
            if (parts.length < 6) {
                JOptionPane.showMessageDialog(null, "Dữ liệu từ server không đầy đủ.");
                return;
            }

            String name = parts[0].split(": ")[1];
            String price = parts[1].split(": ")[1];
            String imageUrl = parts[2].split(": ")[1];
            String reviewCount = parts[3].split(": ")[1];
            String avgRating = parts[4].split(": ")[1];
            String[] reviews = parts[5].split(": ")[1].split(";");

            // Cập nhật GUI
            lblProductName.setText("Tên sản phẩm: " + name);
            lblPrice.setText("Giá: " + price + " VNĐ");
            lblAvgRating.setText("Đánh giá trung bình: " + avgRating + "/5");

            reviewListModel.clear();
            for (String review : reviews) {
                if (!review.trim().isEmpty()) {
                    reviewListModel.addElement(review);
                }
            }

            // Hiển thị ảnh với co giãn
            try {
                URL url = new URL(imageUrl);
                Image image = ImageIO.read(url);
                if (image == null) {
                    lblImage.setText("Không thể tải hình ảnh từ URL.");
                    lblImage.setIcon(null);
                    System.err.println("Không thể tải hình ảnh từ URL: " + imageUrl);
                } else {
                    Image scaledImage = image.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                    lblImage.setIcon(new ImageIcon(scaledImage));
                    lblImage.setText(""); // Xóa text nếu có ảnh
                }
            } catch (IOException e) {
                lblImage.setText("Lỗi tải hình ảnh: " + e.getMessage());
                lblImage.setIcon(null);
                System.err.println("Lỗi khi tải hình ảnh: " + e.getMessage());
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Lỗi kết nối server: " + e.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Lỗi xử lý dữ liệu: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GUI::new);
    }
}