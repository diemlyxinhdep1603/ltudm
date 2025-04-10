package GUI;
import Client.ProductReviewClient;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.imageio.ImageIO;

/**
 * Giao diện người dùng chính cho ứng dụng Product Review với khả năng co giãn theo kích thước cửa sổ
 */
public class ProductReviewGUI extends JFrame {
    private DefaultListModel<String> reviewListModel;
    private JLabel lblTieuDe;
    private JLabel lblThongTinSanPham;
    private JTextField textInfor;
    private JPanel panelSanPham;
    private JPanel panelButton;
    private JButton btnTiki;
    private JButton btnSendo;
    private JButton btnAmazon;
    private JTable table;
    private JScrollPane tableScrollPane;
    private JLabel lblOverView;
    private JLabel lblProductImage;
    private JPanel imagePanel;

    // Khởi tạo đối tượng Client để giao tiếp với server
    private ProductReviewClient client;

    // Đối tượng xử lý gợi ý tìm kiếm
    private ProductSuggestionProvider suggestionProvider;

    // Nền tảng hiện tại
    private String currentPlatform = "TIKI";

    // Thông tin hiện tại về sản phẩm, được lưu riêng cho mỗi nền tảng
    private static class PlatformData {
        String productName = "";
        String productPrice = "";
        String productImage = "";
        int reviewCount = 0;
        double avgRating = 0.0;
        String searchKeyword = "";
        DefaultTableModel tableModel = new DefaultTableModel(
                new Object[][] {},
                new String[] {"Name Reviewer", "Rate", "Comments", "Ảnh", "Video"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho phép chỉnh sửa ô
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 3) {
                    return ImageIcon.class; // Cột Ảnh chứa ImageIcon
                }
                return Object.class;
            }
        };
    }

    // Map lưu trữ dữ liệu theo nền tảng
    private Map<String, PlatformData> platformDataMap = new HashMap<>();

    public ProductReviewGUI() {
        // Khởi tạo client kết nối đến server
        client = new ProductReviewClient("localhost", 1234);

        // Khởi tạo dữ liệu cho mỗi nền tảng
        platformDataMap.put("TIKI", new PlatformData());
        platformDataMap.put("SENDO", new PlatformData());
        platformDataMap.put("AMAZON", new PlatformData());

        setTitle("Tổng Hợp Review Sản Phẩm");
        setSize(1333, 720);
        setMinimumSize(new Dimension(800, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Sử dụng BorderLayout cho toàn bộ cửa sổ để hỗ trợ co giãn
        getContentPane().setLayout(new BorderLayout(5, 5));

        // Panel cho phần trên cùng (tiêu đề và tìm kiếm)
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        // Panel tiêu đề
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        lblTieuDe = new JLabel("TỔNG HỢP REVIEW SẢN PHẨM");
        lblTieuDe.setFont(new Font("Times New Roman", Font.BOLD, 20));
        lblTieuDe.setForeground(Color.BLUE);
        titlePanel.add(lblTieuDe);
        topPanel.add(titlePanel, BorderLayout.NORTH);

        // Panel tìm kiếm
        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));

        // Label và ô tìm kiếm
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        lblThongTinSanPham = new JLabel("Nhập tên sản phẩm:");
        lblThongTinSanPham.setFont(new Font("Arial", Font.BOLD, 14));
        lblThongTinSanPham.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        inputPanel.add(lblThongTinSanPham, BorderLayout.WEST);

        textInfor = new JTextField();
        inputPanel.add(textInfor, BorderLayout.CENTER);

        // Nút tìm kiếm
        JButton btnSearch = new JButton("Tìm kiếm");
        btnSearch.setFont(new Font("Arial", Font.BOLD, 14));
        btnSearch.setBackground(Color.DARK_GRAY);
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.addActionListener(e -> searchProduct());
        btnSearch.setPreferredSize(new Dimension(120, 30));
        inputPanel.add(btnSearch, BorderLayout.EAST);

        searchPanel.add(inputPanel, BorderLayout.CENTER);

        // Khởi tạo đối tượng gợi ý tìm kiếm
        suggestionProvider = new ProductSuggestionProvider(textInfor);

        topPanel.add(searchPanel, BorderLayout.CENTER);

        // Thêm panel trên cùng vào content pane
        getContentPane().add(topPanel, BorderLayout.NORTH);

        // Panel nút chuyển đổi nền tảng
        panelButton = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        panelButton.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        // Nút TIKI
        btnTiki = new JButton("TIKI");
        formatButton(btnTiki);
        btnTiki.setBackground(new Color(0, 150, 136)); // Nền tảng mặc định
        btnTiki.addActionListener(e -> showPlatform("TIKI"));
        panelButton.add(btnTiki);

        // Nút SENDO
        btnSendo = new JButton("SENDO");
        formatButton(btnSendo);
        btnSendo.addActionListener(e -> showPlatform("SENDO"));
        panelButton.add(btnSendo);

        // Nút AMAZON/LAZADA
        btnAmazon = new JButton("AMAZON");
        formatButton(btnAmazon);
        btnAmazon.addActionListener(e -> showPlatform("AMAZON"));
        panelButton.add(btnAmazon);

        topPanel.add(panelButton, BorderLayout.SOUTH);

        // Panel chứa bảng kết quả và hình ảnh
        panelSanPham = new JPanel(new BorderLayout(10, 5));
        panelSanPham.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(panelSanPham, BorderLayout.CENTER);

        // Panel hình ảnh sản phẩm
        imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBorder(BorderFactory.createTitledBorder("Hình Ảnh Sản Phẩm"));
        imagePanel.setPreferredSize(new Dimension(250, 0)); // Tăng kích thước chiều rộng

        lblProductImage = new JLabel("Không có hình ảnh");
        lblProductImage.setHorizontalAlignment(JLabel.CENTER);
        lblProductImage.setVerticalAlignment(JLabel.CENTER);
        // Thêm đệm cho label để ảnh không sát với viền
        lblProductImage.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        imagePanel.add(lblProductImage, BorderLayout.CENTER);

        panelSanPham.add(imagePanel, BorderLayout.WEST);

        // Panel nội dung chính - bảng đánh giá
        JPanel mainContentPanel = new JPanel(new BorderLayout(5, 5));

        // Tạo bảng với các cột đã chỉ định
        table = new JTable(platformDataMap.get(currentPlatform).tableModel);
        table.setRowHeight(100); // Đủ cao để hiển thị hình ảnh

        // Cấu hình hiển thị và căn lề cho các cột
        // Cột Name Reviewer
        TableColumn nameColumn = table.getColumnModel().getColumn(0);
        nameColumn.setPreferredWidth(150);
        DefaultTableCellRenderer nameRenderer = new DefaultTableCellRenderer();
        nameRenderer.setHorizontalAlignment(JLabel.CENTER);
        nameColumn.setCellRenderer(nameRenderer);

        // Cột Rate
        TableColumn rateColumn = table.getColumnModel().getColumn(1);
        rateColumn.setPreferredWidth(50);
        DefaultTableCellRenderer rateRenderer = new DefaultTableCellRenderer();
        rateRenderer.setHorizontalAlignment(JLabel.CENTER);
        rateColumn.setCellRenderer(rateRenderer);

        // Cột Comments
        TableColumn commentColumn = table.getColumnModel().getColumn(2);
        commentColumn.setPreferredWidth(450);
        DefaultTableCellRenderer commentRenderer = new DefaultTableCellRenderer();
        commentRenderer.setHorizontalAlignment(JLabel.LEFT);
        commentColumn.setCellRenderer(commentRenderer);

        // Cột ảnh và video được cấu hình bởi renderer riêng
        TableColumn imageColumn = table.getColumnModel().getColumn(3);
        imageColumn.setPreferredWidth(100);

        TableColumn videoColumn = table.getColumnModel().getColumn(4);
        videoColumn.setPreferredWidth(100);

        tableScrollPane = new JScrollPane(table);
        mainContentPanel.add(tableScrollPane, BorderLayout.CENTER);

        // Thêm panel chính vào panel sản phẩm
        panelSanPham.add(mainContentPanel, BorderLayout.CENTER);

        // Nút tổng quan - Overview
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        JPanel overviewButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnOverViewButton = new JButton("OVERVIEW");
        btnOverViewButton.setFont(new Font("Arial", Font.BOLD, 14));
        btnOverViewButton.setBackground(Color.DARK_GRAY);
        btnOverViewButton.setForeground(Color.WHITE);
        btnOverViewButton.setFocusPainted(false);
        btnOverViewButton.setPreferredSize(new Dimension(150, 35));
        btnOverViewButton.addActionListener(e -> showOverview());
        overviewButtonPanel.add(btnOverViewButton);
        bottomPanel.add(overviewButtonPanel, BorderLayout.NORTH);

        // Label hiển thị tổng quan
        lblOverView = new JLabel("");
        lblOverView.setBorder(new LineBorder(Color.BLACK, 1));
        lblOverView.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.BLACK, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        //Thiết lập kích thước cố định
        lblOverView.setPreferredSize(new Dimension(800, 60));
        bottomPanel.add(lblOverView, BorderLayout.CENTER);

        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        reviewListModel = new DefaultListModel<>();

        // Thêm sự kiện Enter cho ô tìm kiếm
        textInfor.addActionListener(e -> searchProduct());

        // Xử lý khi đóng cửa sổ
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Đóng kết nối với server khi đóng ứng dụng
                if (client != null) {
                    client.close();
                }
                super.windowClosing(e);
            }
        });

        setVisible(true);
    }

    /**
     * Định dạng nút
     */
    private void formatButton(JButton button) {
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(120, 35));
    }

    /**
     * Hiển thị hình ảnh sản phẩm
     */
    private void displayProductImage(String imageUrl) {
        try {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // Tải hình ảnh từ URL
                URL url = new URL(imageUrl);
                Image image = ImageIO.read(url);

                if (image != null) {
                    // Lấy kích thước khung hình ảnh sản phẩm
                    int panelWidth = imagePanel.getWidth() - 20; // Giảm 20px để có đệm
                    int panelHeight = imagePanel.getHeight() - 30; // Giảm 30px cho tiêu đề và đệm

                    // Đảm bảo kích thước tối thiểu
                    if (panelWidth < 100) panelWidth = 180;
                    if (panelHeight < 100) panelHeight = 200;

                    // Tính tỷ lệ khung hình để giữ nguyên tỷ lệ ảnh
                    double imageRatio = (double) image.getWidth(null) / image.getHeight(null);
                    int width = panelWidth;
                    int height = (int) (width / imageRatio);

                    // Nếu chiều cao tính toán vượt quá chiều cao khung, tính lại
                    if (height > panelHeight) {
                        height = panelHeight;
                        width = (int) (height * imageRatio);
                    }

                    // Co giãn hình ảnh để vừa với label và giữ tỷ lệ
                    Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    lblProductImage.setIcon(new ImageIcon(scaledImage));
                    lblProductImage.setText("");

                    // In ra log để debug
                    System.out.println("Kích thước panel: " + panelWidth + "x" + panelHeight);
                    System.out.println("Kích thước ảnh đã co giãn: " + width + "x" + height);
                } else {
                    lblProductImage.setIcon(null);
                    lblProductImage.setText("Không thể tải hình ảnh");
                }
            } else {
                lblProductImage.setIcon(null);
                lblProductImage.setText("Không có hình ảnh");
            }
        } catch (Exception e) {
            lblProductImage.setIcon(null);
            lblProductImage.setText("Lỗi: " + e.getMessage());
            System.err.println("Lỗi hiển thị hình ảnh: " + e.getMessage());
        }
    }

    /**
     * Tìm kiếm sản phẩm và hiển thị kết quả
     */
    private void searchProduct() {
        String productName = textInfor.getText().trim();
        if (productName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên sản phẩm cần tìm kiếm", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Hiển thị con trỏ đang xử lý
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        try {
            // Kết nối đến server nếu chưa kết nối
            if (!client.isConnected()) {
                if (!client.connect()) {
                    JOptionPane.showMessageDialog(this, "Không thể kết nối đến máy chủ", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    setCursor(Cursor.getDefaultCursor());
                    return;
                }
            }

            // Tìm kiếm sản phẩm từ server
            List<String> results = client.searchProduct(productName);

            // Phân tích và hiển thị kết quả
            processSearchResults(results);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tìm kiếm: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        } finally {
            // Đặt lại con trỏ chuột
            setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * Xử lý kết quả tìm kiếm từ server
     */
    private void processSearchResults(List<String> results) {
        if (results.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy kết quả.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Xử lý kết quả
        for (String result : results) {
            // Kiểm tra xem có phải là thông báo lỗi từ server không
            if (result.startsWith("Lỗi:")) {
                // Hiển thị thông báo lỗi chi tiết từ server
                JOptionPane.showMessageDialog(this, result, "Lỗi từ server", JOptionPane.ERROR_MESSAGE);
                return;
            } else if (result.startsWith("Product:")) {
                // Đây là một kết quả tìm kiếm thành công
                parseProductInfo(result);
                return;
            } else if (result.contains("Không tìm thấy sản phẩm")) {
                JOptionPane.showMessageDialog(this, result, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }

        // Nếu không tìm thấy thông tin sản phẩm và không có lỗi cụ thể
        StringBuilder allMessages = new StringBuilder("Không thể phân tích kết quả từ server.\n\nNội dung phản hồi:\n");
        for (String result : results) {
            allMessages.append(result).append("\n");
        }
        JOptionPane.showMessageDialog(this, allMessages.toString(), "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Phân tích thông tin sản phẩm từ chuỗi phản hồi
     */
    private void parseProductInfo(String productInfo) {
        try {
            // Phân tích chuỗi thông tin sản phẩm
            // Format: "Product: name | Price: price | Image: url | ReviewCount: count | AvgRating: rating | Reviews: reviews"
            String[] parts = productInfo.split(" \\| ");

            // Kiểm tra số lượng phần tử
            if (parts.length < 6) {
                JOptionPane.showMessageDialog(this, "Định dạng phản hồi không đúng: " + productInfo, "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Lấy dữ liệu platform hiện tại
            PlatformData platformData = platformDataMap.get(currentPlatform);

            // Trích xuất thông tin sản phẩm và lưu vào dữ liệu platform
            platformData.productName = parts[0].substring(parts[0].indexOf(":") + 1).trim();
            platformData.productPrice = parts[1].substring(parts[1].indexOf(":") + 1).trim();
            platformData.productImage = parts[2].substring(parts[2].indexOf(":") + 1).trim();
            platformData.reviewCount = Integer.parseInt(parts[3].substring(parts[3].indexOf(":") + 1).trim());
            platformData.avgRating = Double.parseDouble(parts[4].substring(parts[4].indexOf(":") + 1).trim());
            platformData.searchKeyword = textInfor.getText().trim();

            // Phân tích đánh giá - lấy phần Reviews: ... từ chuỗi phản hồi
            String reviewsPart = productInfo.substring(productInfo.indexOf("Reviews:") + 8).trim();

            // Đảm bảo khớp với cách định dạng trong getReviewTIKIProduct.java
            // (mỗi đánh giá cách nhau bởi ';')
            String[] reviews = reviewsPart.split(";");

            // Cập nhật bảng và hiển thị thông tin
            updateTableWithReviews(reviews);
            displayProductImage(platformData.productImage);

            // Cập nhật thông tin tổng quan với định dạng số thập phân cho đánh giá
            String formattedRating = String.format("%.1f", platformData.avgRating); // Hiển thị 1 chữ số thập phân
            lblOverView.setText("Sản phẩm: " + platformData.productName + " | Giá: " + platformData.productPrice + " VNĐ | Số đánh giá: "
                    + platformData.reviewCount + " | Đánh giá trung bình: " + formattedRating + "/5");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi xử lý thông tin sản phẩm: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Tải hình ảnh từ URL và co giãn nó
     * @param imageUrl URL của hình ảnh
     * @param width Chiều rộng mong muốn
     * @param height Chiều cao mong muốn
     * @return ImageIcon đã được co giãn hoặc null nếu không thể tải
     */
    private ImageIcon loadAndScaleImage(String imageUrl, int width, int height) {
        try {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // Tải hình ảnh từ URL
                URL url = new URL(imageUrl);
                Image image = ImageIO.read(url);

                if (image != null) {
                    // Co giãn hình ảnh để vừa với kích thước chỉ định
                    Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaledImage);
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi tải hình ảnh từ URL " + imageUrl + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Cập nhật bảng với danh sách các đánh giá
     */
    private void updateTableWithReviews(String[] reviews) {
        // Lấy model của platform hiện tại
        PlatformData platformData = platformDataMap.get(currentPlatform);
        DefaultTableModel model = platformData.tableModel;
        model.setRowCount(0); // Xóa dữ liệu cũ

        // Gán model này cho bảng hiển thị
        table.setModel(model);

        System.out.println("Tổng số đánh giá nhận được: " + reviews.length);

        for (String review : reviews) {
            if (review.trim().isEmpty()) continue;

            try {
                System.out.println("Đánh giá thô: " + review);

                // Kiểm tra xem chuỗi có đúng định dạng không
                String[] parts = null;

                // Nếu chuỗi chứa ít nhất 2 dấu "|", có thể phân tách theo định dạng mong muốn
                if (review.indexOf(" | ") != review.lastIndexOf(" | ")) {
                    // Phân tách thành các phần: tên, rating, content, image, video
                    parts = new String[5];
                    int firstDivider = review.indexOf(" | ");
                    int secondDivider = review.indexOf(" | ", firstDivider + 3);

                    // Tên reviewer
                    parts[0] = review.substring(0, firstDivider).trim();

                    // Rating
                    parts[1] = review.substring(firstDivider + 3, secondDivider).trim();

                    // Content - phần giữa second và third divider, hoặc đến cuối nếu không có ảnh/video
                    int thirdDivider = review.indexOf(" | ", secondDivider + 3);
                    if (thirdDivider != -1) {
                        parts[2] = review.substring(secondDivider + 3, thirdDivider).trim();

                        // Image URL
                        int fourthDivider = review.indexOf(" | ", thirdDivider + 3);
                        if (fourthDivider != -1) {
                            parts[3] = review.substring(thirdDivider + 3, fourthDivider).trim();

                            // Video URL
                            parts[4] = review.substring(fourthDivider + 3).trim();
                        } else {
                            parts[3] = review.substring(thirdDivider + 3).trim();
                            parts[4] = "";
                        }
                    } else {
                        parts[2] = review.substring(secondDivider + 3).trim();
                        parts[3] = "";
                        parts[4] = "";
                    }
                }

                // Nếu phân tách thành công
                if (parts != null && parts[0] != null && parts[1] != null && parts[2] != null) {
                    String reviewerName = parts[0];

                    // Xử lý rating để hiển thị 1 chữ số thập phân nếu là số thập phân
                    String rating = parts[1];
                    try {
                        double ratingValue = Double.parseDouble(rating);
                        rating = String.format("%.1f", ratingValue);
                    } catch (NumberFormatException e) {
                        // Nếu không phải số, giữ nguyên giá trị
                        System.err.println("Không thể chuyển đổi rating '" + rating + "' thành số: " + e.getMessage());
                    }

                    String content = parts[2];

                    // Xử lý nội dung quá dài
                    if (content.length() > 200) {
                        content = content.substring(0, 197) + "...";
                    }

                    // Xử lý ảnh và video (nếu có)
                    String imageUrl = parts[3] != null ? parts[3] : "";
                    String videoUrl = parts[4] != null ? parts[4] : "";

                    System.out.println(">>> Thêm vào bảng: Name=" + reviewerName + ", Rating=" + rating + ", Content=" + content);
                    System.out.println(">>> Image URL: " + imageUrl);

                    // Tải và co giãn hình ảnh nếu có URL ảnh
                    ImageIcon imageIcon = null;
                    if (!imageUrl.isEmpty()) {
                        imageIcon = loadAndScaleImage(imageUrl, 80, 80);
                    }

                    // Xử lý video (có thể mở trong trình duyệt nếu được nhấp vào)
                    JButton videoButton = null;
                    if (!videoUrl.isEmpty()) {
                        videoButton = new JButton("Xem Video");
                        videoButton.addActionListener(e -> {
                            try {
                                Desktop.getDesktop().browse(new URI(videoUrl));
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(null,
                                        "Không thể mở video: " + ex.getMessage(),
                                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }

                    // Thêm vào bảng với dữ liệu đã được xử lý
                    model.addRow(new Object[]{
                            reviewerName,                   // Cột 0: Name Reviewer
                            rating,                         // Cột 1: Rate
                            content,                        // Cột 2: Comments
                            imageIcon != null ? imageIcon : "Không có ảnh",  // Cột 3: Ảnh
                            videoButton != null ? "Có Video" : "Không có video"  // Cột 4: Video
                    });
                } else {
                    System.out.println("Không thể tách đánh giá theo định dạng mong muốn");
                    model.addRow(new Object[]{"Lỗi định dạng", "", "Không thể hiển thị đánh giá này", "", ""});
                }
            } catch (Exception e) {
                System.out.println("Exception khi xử lý đánh giá: " + e.getMessage());
                e.printStackTrace();
                model.addRow(new Object[]{"Lỗi", "", "Lỗi khi phân tích đánh giá: " + e.getMessage(), "", ""});
            }
        }

        System.out.println("Đã thêm " + model.getRowCount() + " đánh giá vào bảng");

        // Cấu hình hiển thị và căn lề cho các cột
        // Cột Name Reviewer
        TableColumn nameColumn = table.getColumnModel().getColumn(0);
        nameColumn.setPreferredWidth(150);
        DefaultTableCellRenderer nameRenderer = new DefaultTableCellRenderer();
        nameRenderer.setHorizontalAlignment(JLabel.CENTER);
        nameColumn.setCellRenderer(nameRenderer);

        // Cột Rate
        TableColumn rateColumn = table.getColumnModel().getColumn(1);
        rateColumn.setPreferredWidth(50);
        DefaultTableCellRenderer rateRenderer = new DefaultTableCellRenderer();
        rateRenderer.setHorizontalAlignment(JLabel.CENTER);
        rateColumn.setCellRenderer(rateRenderer);

        // Cột Comments
        TableColumn commentColumn = table.getColumnModel().getColumn(2);
        commentColumn.setPreferredWidth(450);
        DefaultTableCellRenderer commentRenderer = new DefaultTableCellRenderer();
        commentRenderer.setHorizontalAlignment(JLabel.LEFT);
        commentColumn.setCellRenderer(commentRenderer);

        // Thêm renderer cho cột ảnh và video
        table.getColumnModel().getColumn(3).setCellRenderer(new ImageCellRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(new ButtonCellRenderer());

        // Đảm bảo bảng được cập nhật đúng
        model.fireTableDataChanged();
        table.repaint();
    }

    /**
     * Renderer cho cột ảnh
     */
    private static class ImageCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setHorizontalAlignment(JLabel.CENTER);

            if (value instanceof ImageIcon) {
                // Nếu giá trị là ImageIcon, hiển thị nó
                setIcon((ImageIcon) value);
                setText("");
            } else {
                // Nếu không, hiển thị text
                setIcon(null);
                setText(value.toString());
            }

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }

            return this;
        }
    }

    /**
     * Renderer cho cột video (hiển thị button hoặc text)
     */
    private static class ButtonCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value.toString());
            setHorizontalAlignment(JLabel.CENTER);

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }

            return this;
        }
    }

    /**
     * Hiển thị tổng quan sản phẩm
     */
    private void showOverview() {
        // Lấy dữ liệu của platform hiện tại
        PlatformData platformData = platformDataMap.get(currentPlatform);

        if (platformData.productName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng tìm kiếm sản phẩm trước", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Hiển thị thông tin tổng quan với định dạng số thập phân cho đánh giá
        String formattedRating = String.format("%.1f", platformData.avgRating); // Hiển thị 1 chữ số thập phân
        lblOverView.setText("Sản phẩm: " + platformData.productName + " | Giá: " + platformData.productPrice + " VNĐ | Số đánh giá: "
                + platformData.reviewCount + " | Đánh giá trung bình: " + formattedRating + "/5");
    }

    /**
     * Hiển thị đánh giá từ nền tảng chỉ định (TIKI, SENDO, AMAZON)
     * @param platform Tên nền tảng
     */
    private void showPlatform(String platform) {
        if (platform.equals(currentPlatform)) {
            // Đã ở nền tảng này rồi, không cần làm gì thêm
            return;
        }

        // Lưu nền tảng trước đó
        String previousPlatform = currentPlatform;

        // Cập nhật UI để hiển thị nền tảng được chọn
        btnTiki.setBackground(platform.equals("TIKI") ? new Color(0, 150, 136) : new Color(70, 130, 180));
        btnSendo.setBackground(platform.equals("SENDO") ? new Color(0, 150, 136) : new Color(70, 130, 180));
        btnAmazon.setBackground(platform.equals("AMAZON") ? new Color(0, 150, 136) : new Color(70, 130, 180));

        try {
            // Thay đổi nền tảng trên server
            String response = client.changePlatform(platform);

            // Kiểm tra nếu phản hồi chứa thông báo lỗi
            if (response.startsWith("Lỗi:")) {
                // Hiển thị thông báo lỗi từ server
                JOptionPane.showMessageDialog(this, response, "Lỗi từ server", JOptionPane.ERROR_MESSAGE);

                // Khôi phục UI về nền tảng trước đó
                btnTiki.setBackground(previousPlatform.equals("TIKI") ? new Color(0, 150, 136) : new Color(70, 130, 180));
                btnSendo.setBackground(previousPlatform.equals("SENDO") ? new Color(0, 150, 136) : new Color(70, 130, 180));
                btnAmazon.setBackground(previousPlatform.equals("AMAZON") ? new Color(0, 150, 136) : new Color(70, 130, 180));
                return;
            }

            // Cập nhật nền tảng hiện tại
            currentPlatform = platform;

            // Lấy dữ liệu của nền tảng mới được chọn
            PlatformData platformData = platformDataMap.get(currentPlatform);

            // Kiểm tra xem đã có dữ liệu cached cho nền tảng này chưa
            if (!platformData.productName.isEmpty()) {
                // Đã có dữ liệu, hiển thị lại từ cache
                table.setModel(platformData.tableModel);
                displayProductImage(platformData.productImage);

                // Cập nhật thông tin tổng quan
                String formattedRating = String.format("%.1f", platformData.avgRating);
                lblOverView.setText("Sản phẩm: " + platformData.productName + " | Giá: " + platformData.productPrice +
                        " VNĐ | Số đánh giá: " + platformData.reviewCount + " | Đánh giá trung bình: " + formattedRating + "/5");

                // Đặt lại ô tìm kiếm với từ khóa đã tìm kiếm trước đó
                if (!platformData.searchKeyword.isEmpty()) {
                    textInfor.setText(platformData.searchKeyword);
                }

                // Thêm renderer cho cột ảnh và video
                table.getColumnModel().getColumn(3).setCellRenderer(new ImageCellRenderer());
                table.getColumnModel().getColumn(4).setCellRenderer(new ButtonCellRenderer());
            } else {
                // Chưa có dữ liệu, hiển thị thông báo nền tảng đã đổi
                lblOverView.setText("Đã chuyển sang nền tảng: " + platform);

                // Xóa dữ liệu hiển thị
                DefaultTableModel emptyModel = platformData.tableModel;
                table.setModel(emptyModel);
                lblProductImage.setIcon(null);

                // Nếu có từ khóa tìm kiếm từ nền tảng trước, tìm kiếm lại
                String previousKeyword = platformDataMap.get(previousPlatform).searchKeyword;
                if (!previousKeyword.isEmpty()) {
                    textInfor.setText(previousKeyword);
                    searchProduct();
                } else {
                    // Xóa ô tìm kiếm nếu không có từ khóa trước đó
                    textInfor.setText("");
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi chuyển đổi nền tảng: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ProductReviewGUI::new);
    }
}