package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GUI extends JFrame {
    private JTextField txtProductName; // Ô nhập tên sản phẩm
    private JButton btnSearch; // Nút gửi yêu cầu
    private DefaultListModel<String> reviewListModel; // Model cho danh sách review
    private JList<String> reviewList; // Danh sách hiển thị review
    private JLabel lblImage; // Hiển thị ảnh sản phẩm

    public GUI() {
        setTitle("Tổng Hợp Review Sản Phẩm");
        setSize(770, 530);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Hiển thị giữa màn hình
        getContentPane().setLayout(new BorderLayout(10, 10));

        // **HEADER - Ô nhập sản phẩm & Nút tìm kiếm**
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        txtProductName = new JTextField(20);
        btnSearch = new JButton(" Tìm Kiếm");
        btnSearch.setFont(new Font("Arial", Font.BOLD, 14));
        btnSearch.setBackground(new Color(52, 152, 219));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);

        topPanel.add(txtProductName, BorderLayout.CENTER);
        topPanel.add(btnSearch, BorderLayout.EAST);
        getContentPane().add(topPanel, BorderLayout.NORTH);

        // **CENTER - Khu vực hiển thị danh sách review**
        reviewListModel = new DefaultListModel<>();
        reviewList = new JList<>(reviewListModel);
        reviewList.setFont(new Font("Arial", Font.PLAIN, 14));
        reviewList.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(reviewList);
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        // **FOOTER - Hiển thị ảnh sản phẩm**
        JPanel imagePanel = new JPanel();
        lblImage = new JLabel();
        lblImage.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        lblImage.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        lblImage.setPreferredSize(new Dimension(200, 200));

        imagePanel.add(lblImage);
        getContentPane().add(imagePanel, BorderLayout.SOUTH);

        // **Sự kiện khi nhấn nút "Tìm Kiếm"**
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

    // **Hàm xử lý tìm kiếm sản phẩm (Giả lập dữ liệu)**
    private void searchProduct(String productName) {
        // Xóa dữ liệu cũ
        reviewListModel.clear();

        // Dữ liệu review giả lập
        reviewListModel.addElement(" - Sản phẩm rất tốt!");
        reviewListModel.addElement(" - Giao hàng hơi chậm.");
        reviewListModel.addElement(" - Đúng mô tả, sẽ mua lần nữa!");

        // Hiển thị hình ảnh sản phẩm (Dùng ảnh mặc định)
        lblImage.setIcon(new ImageIcon("resources/sample-product.jpg")); // Thay bằng đường dẫn ảnh thật
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GUI::new);
    }
}
