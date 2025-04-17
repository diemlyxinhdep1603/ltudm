package GUI;
import Client.IPFetcher;
import Client.ProductReviewClient;
//import org.Server.AIReviewSummarizer;

import javax.swing.*;
import java.io.FileWriter;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

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
    private JButton btnDMX;
    private JButton btnSummarize; /// mới thêm

    private JTable table;
    private JScrollPane tableScrollPane;
    private JLabel lblOverView;
    private JLabel lblProductImage;
    private JPanel imagePanel;
    
    // Client để giao tiếp với server
    private ProductReviewClient client;
    // Đối tượng xử lý gợi ý tìm kiếm
    private ProductSuggestionProvider suggestionProvider;


    // Nền tảng hiện tại
    private String currentPlatform = "TIKI";
    
    // Thông tin hiện tại về sản phẩm, được lưu riêng cho mỗi nền tảng
    private static class PlatformData {
        String productId = "";    // Thêm ID sản phẩm cho lazy loading
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
    
    // Biến để theo dõi phân trang
    private int currentPage = 1;
    private int totalPages = 1;
    private JLabel lblPagination; // Label hiển thị thông tin phân trang

    public ProductReviewGUI() {
       //Gọi API để lấy IP server
        IPFetcher ipFetcher = new IPFetcher();
        String ip = ipFetcher.fetch_IP();

        client = new ProductReviewClient(ip, 1234);
        // Khởi tạo client kết nối đến server
    //    client = new ProductReviewClient("localhost", 1234);
        
        // Khởi tạo dữ liệu cho mỗi nền tảng
        platformDataMap.put("TIKI", new PlatformData());
        platformDataMap.put("ĐIỆN MÁY XANH", new PlatformData());

        
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
        btnTiki.setPreferredSize(new Dimension(180, 30));
        btnTiki.setBackground(new Color(0, 150, 136)); // Nền tảng mặc định
        btnTiki.addActionListener(e -> showPlatform("TIKI"));
        panelButton.add(btnTiki);
        
        // Nút ĐIỆN MÁY XANH
        btnDMX = new JButton("ĐIỆN MÁY XANH");
        formatButton(btnDMX);
        btnDMX.setPreferredSize(new Dimension(180, 30)); // Chiều rộng 180px, chiều cao 30px
        btnDMX.setBackground(new Color(0, 150, 136)); // Nền tảng mặc định
        btnDMX.addActionListener(e -> showPlatform("ĐIỆN MÁY XANH"));
        panelButton.add(btnDMX);

        //thêm *********
        // Khởi tạo nút "Tổng hợp đánh giá"
        btnSummarize = new JButton("TỔNG HỢP");
        btnSummarize.setFont(new Font("Arial", Font.BOLD, 14));
        btnSummarize.setBackground(Color.DARK_GRAY);
        btnSummarize.setForeground(Color.WHITE);
        btnSummarize.setFocusPainted(false);
        btnSummarize.setPreferredSize(new Dimension(180, 30));
        btnSummarize.addActionListener(e -> showSummarizedReview());

        // Thêm nút vào giao diện
        // Ví dụ: Thêm vào panelButton hoặc một panel phù hợp khác
        panelButton.add(btnSummarize);



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
        //
        tableScrollPane = new JScrollPane(table);
        mainContentPanel.add(tableScrollPane, BorderLayout.CENTER);
        
        // Thêm các nút phân trang
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        JButton btnPrevPage = new JButton("Trang trước");
        btnPrevPage.addActionListener(e -> loadPreviousPage());
        paginationPanel.add(btnPrevPage);
        
        // Label hiển thị thông tin trang hiện tại/tổng số trang
        lblPagination = new JLabel("Trang 1/1");
        lblPagination.setHorizontalAlignment(JLabel.CENTER);
        lblPagination.setPreferredSize(new Dimension(100, 20));
        lblPagination.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        paginationPanel.add(lblPagination);
        
        JButton btnNextPage = new JButton("Trang sau");
        btnNextPage.addActionListener(e -> loadNextPage());
        paginationPanel.add(btnNextPage);
        
        mainContentPanel.add(paginationPanel, BorderLayout.SOUTH);
        
        // Thêm panel chính vào panel sản phẩm
        panelSanPham.add(mainContentPanel, BorderLayout.CENTER);

        // Nút tổng quan
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
     * Cập nhật hiển thị thông tin phân trang
     */
    private void updatePaginationDisplay() {
        if (lblPagination != null) {
            lblPagination.setText("Trang " + currentPage + "/" + totalPages);
        }
    }
    
    /**
     * Hiển thị hình ảnh sản phẩm
     */
    private void displayProductImage(String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) {
                lblProductImage.setIcon(null);
                lblProductImage.setText("Không có hình ảnh");
                System.err.println("URL hình ảnh rỗng hoặc null");
                return;
            }

            // ===== Xử lý URL trước khi tải =====
            System.out.println("URL ảnh gốc: " + imageUrl);
            
            // Đảm bảo URL hình ảnh có giao thức
            if (imageUrl.startsWith("//")) {
                imageUrl = "https:" + imageUrl;
                System.out.println("Đã thêm https: " + imageUrl);
            }
            
            // Loại bỏ phần xử lý ảnh từ TGDD trong URL (nếu có)
            if (imageUrl.contains("/imgt/") || imageUrl.contains("img.tgdd.vn")) {
                int cdnIndex = imageUrl.indexOf("https://cdn.tgdd.vn");
                if (cdnIndex > 0) {
                    imageUrl = imageUrl.substring(cdnIndex);
                    System.out.println("Đã loại bỏ phần xử lý ảnh: " + imageUrl);
                }
            }
            
            // Đảm bảo URL có đủ phần HTTP cho java.net.URL parse
            if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                imageUrl = "https:" + imageUrl;
                System.out.println("Đã thêm protocol HTTPS: " + imageUrl);
            }
            
            // Nếu URL chứa ký tự không hợp lệ, encode URL
            if (imageUrl.contains(" ")) {
                imageUrl = imageUrl.replace(" ", "%20");
                System.out.println("Đã encode ký tự space: " + imageUrl);
            }
            
            System.out.println("URL ảnh đã xử lý: " + imageUrl);
            
            // ===== Tải ảnh với timeout =====
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Thiết lập User-Agent để tránh bị chặn
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            
            // Mở connection
            connection.connect();
            
            // Kiểm tra response code
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Lỗi HTTP khi tải ảnh: " + responseCode);
                lblProductImage.setIcon(null);
                lblProductImage.setText("Lỗi HTTP: " + responseCode);
                return;
            }
            
            // Đọc ảnh từ InputStream
            try (InputStream in = connection.getInputStream()) {
                Image image = ImageIO.read(in);
                
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
                    System.out.println("Đã tải thành công hình ảnh từ URL: " + imageUrl);
                    System.out.println("Kích thước panel: " + panelWidth + "x" + panelHeight);
                    System.out.println("Kích thước ảnh đã co giãn: " + width + "x" + height);
                } else {
                    lblProductImage.setIcon(null);
                    lblProductImage.setText("Không thể tải hình ảnh");
                    System.err.println("ImageIO.read trả về null cho URL: " + imageUrl);
                }
            }
        } catch (Exception e) {
            // Hiển thị thông báo lỗi chi tiết
            lblProductImage.setIcon(null);
            lblProductImage.setText("<html>Lỗi tải ảnh:<br>" + e.getMessage() + "</html>");
            System.err.println("Lỗi hiển thị hình ảnh: " + e.getMessage());
            e.printStackTrace();
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
        
        // Kiểm tra độ dài từ khóa tìm kiếm
    //    if (productName.length() > 100) {
    //        JOptionPane.showMessageDialog(this, "Từ khóa tìm kiếm quá dài (> 100 ký tự). Vui lòng rút ngắn từ khóa.", "Lỗi", JOptionPane.ERROR_MESSAGE);
    //        return;
    //    }
        
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
     * Tải trang đánh giá tiếp theo
     */
    private void loadNextPage() {
        // Lấy dữ liệu platform hiện tại
        PlatformData platformData = platformDataMap.get(currentPlatform);
        if (platformData.productId == null || platformData.productId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng tìm kiếm sản phẩm trước", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Kiểm tra nếu đang ở trang cuối
        if (currentPage >= totalPages) {
            JOptionPane.showMessageDialog(this, "Đã đến trang cuối cùng", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Tăng số trang hiện tại
        currentPage++;
        updatePaginationDisplay();
        
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
            
            // Gọi API lấy đánh giá trang tiếp theo
            List<String> results = client.getReviewsByPage(platformData.productId, currentPage);
            
            // Xử lý kết quả
            if (results != null && !results.isEmpty()) {
                String result = results.get(0);
                
                // Kiểm tra xem có phải là thông báo lỗi từ server không
                if (result.startsWith("Lỗi:")) {
                    JOptionPane.showMessageDialog(this, result, "Lỗi từ server", JOptionPane.ERROR_MESSAGE);
                } else {
                    // Phân tích và hiển thị kết quả
                    parseReviewPage(result);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Không nhận được dữ liệu từ server", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tải trang: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        } finally {
            // Đặt lại con trỏ chuột
            setCursor(Cursor.getDefaultCursor());
        }
    }
    
    /**
     * Tải trang đánh giá trước đó
     */
    private void loadPreviousPage() {
        // Kiểm tra nếu đang ở trang đầu tiên
        if (currentPage <= 1) {
            JOptionPane.showMessageDialog(this, "Đã ở trang đầu tiên", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Giảm số trang hiện tại
        currentPage--;
        updatePaginationDisplay();
        
        // Lấy dữ liệu platform hiện tại
        PlatformData platformData = platformDataMap.get(currentPlatform);
        if (platformData.productId == null || platformData.productId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng tìm kiếm sản phẩm trước", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
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
            
            // Gọi API lấy đánh giá trang trước đó
            List<String> results = client.getReviewsByPage(platformData.productId, currentPage);
            
            // Xử lý kết quả
            if (results != null && !results.isEmpty()) {
                String result = results.get(0);
                
                // Kiểm tra xem có phải là thông báo lỗi từ server không
                if (result.startsWith("Lỗi:")) {
                    JOptionPane.showMessageDialog(this, result, "Lỗi từ server", JOptionPane.ERROR_MESSAGE);
                } else {
                    // Phân tích và hiển thị kết quả
                    parseReviewPage(result);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Không nhận được dữ liệu từ server", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tải trang: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        } finally {
            // Đặt lại con trỏ chuột
            setCursor(Cursor.getDefaultCursor());
        }
    }
    
    /**
     * Phân tích nội dung trang đánh giá
     */
    private void parseReviewPage(String pageContent) {
        try {
            // Biến để lưu các đánh giá và thông tin liên quan
            List<String> formattedReviews = new ArrayList<>();
            
            // Phân tích các dòng
            String[] lines = pageContent.split("\n");
            
            // Biến để theo dõi quá trình đọc đánh giá
            boolean isReadingReviews = false;
            String currentReviewer = "";
            String currentRating = "";
            String currentContent = "";
            String currentImage = "";
            String currentVideo = "";
            
            // Kiểm tra thông tin phân trang
            for (String line : lines) {
                line = line.trim();
                
                if (line.startsWith("PAGINATION:")) {
                    // Format: PAGINATION:current_page:total_pages
                    String[] paginationParts = line.substring(11).split(":");
                    if (paginationParts.length == 2) {
                        try {
                            currentPage = Integer.parseInt(paginationParts[0]);
                            totalPages = Integer.parseInt(paginationParts[1]);
                            System.out.println("Thông tin phân trang: Trang " + currentPage + "/" + totalPages);
                            updatePaginationDisplay();
                        } catch (NumberFormatException e) {
                            System.err.println("Lỗi khi đọc thông tin phân trang: " + e.getMessage());
                        }
                    }
                } else if (line.startsWith("REVIEWER:")) {
                    // Nếu đã đọc một đánh giá trước đó, lưu lại
                    if (!currentReviewer.isEmpty() && !currentRating.isEmpty()) {
                        // Tạo đánh giá theo định dạng tương thích với updateTableWithReviews
                        String formattedReview = currentReviewer + " | " + currentRating + " | " + 
                                                currentContent + " | " + currentImage + " | " + currentVideo;
                        formattedReviews.add(formattedReview);
                        System.out.println("Đã thêm đánh giá: " + formattedReview);
                    }
                    
                    // Bắt đầu đánh giá mới - lấy tên người đánh giá
                    currentReviewer = line.substring(9).trim();
                    
                    // Reset các giá trị khác
                    currentRating = "";
                    currentContent = "";
                    currentImage = "";
                    currentVideo = "";
                } else if (line.startsWith("IMAGE:")) {
                    // Lấy URL hình ảnh từ review
                    currentImage = line.substring(6).trim();
                    System.out.println("Review Image URL: " + currentImage);
                } else if (line.startsWith("RATING:")) {
                    currentRating = line.substring(7).trim();
                } else if (line.startsWith("CONTENT:")) {
                    currentContent = line.substring(8).trim();
                } else if (line.startsWith("VIDEO:")) {
                    currentVideo = line.substring(6).trim();
                }
                // Nếu đang đọc đánh giá, và dòng tiếp theo không bắt đầu bằng keyword, có thể là phần nội dung tiếp theo
                else if (!line.isEmpty() && !line.contains(":")) {
                    // Nối với nội dung hiện tại
                    if (!currentContent.isEmpty()) {
                        currentContent += " " + line;
                    }
                }
            }
            
            // Đảm bảo đánh giá cuối cùng cũng được thêm vào
            if (!currentReviewer.isEmpty() && !currentRating.isEmpty()) {
                String formattedReview = currentReviewer + " | " + currentRating + " | " + 
                                        currentContent + " | " + currentImage + " | " + currentVideo;
                formattedReviews.add(formattedReview);
                System.out.println("Đã thêm đánh giá cuối cùng: " + formattedReview);
            }
            
            // Cập nhật bảng với danh sách đánh giá mới
            updateTableWithReviews(formattedReviews.toArray(new String[0]));
            
            // Cập nhật hiển thị thông tin phân trang
            updatePaginationDisplay();
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi xử lý trang đánh giá: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
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
            System.out.println("DEBUG - Kết quả từ server: " + result.substring(0, Math.min(100, result.length())) + "...");
            
            // Kiểm tra xem có phải là thông báo lỗi từ server không
            if (result.startsWith("Lỗi:")) {
                // Hiển thị thông báo lỗi chi tiết từ server
                JOptionPane.showMessageDialog(this, result, "Lỗi từ server", JOptionPane.ERROR_MESSAGE);
                return;
            } else if (result.contains("PRODUCT_ID:") || result.contains("TÊN SẢN PHẨM:") || result.contains("GIÁ:")) {
                // Định dạng mới - Lazy loading
                parseProductInfoNewFormat(result);
                return;
            } else if (result.startsWith("Product:")) {
                // Đây là một kết quả tìm kiếm thành công - định dạng cũ
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
        
        // Thêm ghi log để debug
        try {
            // Import lớp mới
            Client.ResponseLogger.init();
            Client.ResponseLogger.logError(currentPlatform, textInfor.getText().trim(), 
                String.join("\n", results), "Không thể phân tích kết quả từ server");
        } catch (Exception logEx) {
            System.err.println("Lỗi khi ghi log: " + logEx.getMessage());
        }
        
        // Kiểm tra xem phản hồi có phải là JSON không
        try {
            if (results.size() > 0 && results.get(0).trim().startsWith("{")) {
                // Log cho việc debug
                String jsonString = results.get(0).trim();
                System.out.println("Phân tích JSON response...");
                
                // Lưu response vào file để debug
                // Ghi log để debug 
                try {
                    java.io.FileWriter logWriter = new java.io.FileWriter("json_response_debug.json");
                    logWriter.write(jsonString);
                    logWriter.close();
                    System.out.println("Đã lưu JSON response vào file debug");
                } catch (Exception e) {
                    System.err.println("Lỗi lưu file debug: " + e.getMessage());
                }
                
                // Có vẻ là JSON, thử phân tích như JSON
                org.json.JSONObject jsonResponse = new org.json.JSONObject(jsonString);
                
                // Nếu có trạng thái thành công (áp dụng cho mọi nền tảng)
                if (jsonResponse.has("status") && jsonResponse.getString("status").equals("success")) {
                    System.out.println("JSON có status success");
                    
                    // Lấy thông tin sản phẩm từ JSON
                    org.json.JSONObject productJson = jsonResponse.getJSONObject("product");
                    String productName = productJson.getString("name");
                    String productPrice = productJson.getString("price");
                    String productImage = productJson.getString("image");
                    // Đảm bảo URL hình ảnh đầy đủ
                    if (productImage.startsWith("//")) {
                        productImage = "https:" + productImage;
                    }
                    // Lấy ID và URL nếu có, hoặc sử dụng giá trị mặc định
                    String productId = productJson.has("id") ? productJson.get("id").toString() : "-1";
                    String productUrl = productJson.has("url") ? productJson.getString("url") : "";
                    
                    // Lấy thông tin đánh giá từ server nếu có
                    org.json.JSONArray reviewsArray = jsonResponse.getJSONArray("reviews");
                    int reviewCount = reviewsArray.length();
                    double avgRating = 0.0;
                    
                    // Ưu tiên lấy thông tin rating từ rating_info nếu có
                    if (jsonResponse.has("rating_info")) {
                        org.json.JSONObject ratingInfo = jsonResponse.getJSONObject("rating_info");
                        if (ratingInfo.has("average_rating")) {
                            avgRating = ratingInfo.getDouble("average_rating");
                            System.out.println("Lấy điểm đánh giá từ server: " + avgRating);
                        }
                        if (ratingInfo.has("review_count")) {
                            reviewCount = ratingInfo.getInt("review_count");
                            System.out.println("Lấy số lượng đánh giá từ server: " + reviewCount);
                        }
                    } else {
                        // Nếu không có rating_info, tính toán từ reviews
                        System.out.println("Không có rating_info từ server, tính toán từ reviews");
                        double totalRating = 0.0;
                        if (reviewCount > 0) {
                            for (int i = 0; i < reviewCount; i++) {
                                org.json.JSONObject review = reviewsArray.getJSONObject(i);
                                totalRating += review.getInt("rating");
                            }
                            avgRating = totalRating / reviewCount;
                        }
                    }
                    
                    // Tạo thông tin sản phẩm định dạng mới để xử lý
                    StringBuilder formattedInfo = new StringBuilder();
                    formattedInfo.append("PRODUCT_ID:").append(productId).append("\n");
                    formattedInfo.append("PRODUCT_URL:").append(productUrl).append("\n");
                    formattedInfo.append("TÊN SẢN PHẨM:").append(productName).append("\n");
                    formattedInfo.append("GIÁ:").append(productPrice).append("\n");
                    formattedInfo.append("HÌNH ẢNH:").append(productImage).append("\n");
                    formattedInfo.append("SỐ LƯỢNG ĐÁNH GIÁ:").append(reviewCount).append("\n");
                    formattedInfo.append("ĐIỂM ĐÁNH GIÁ TRUNG BÌNH:").append(avgRating).append("\n");
                    // Xử lý thông tin phân trang, sử dụng optInt để tránh lỗi khi không tìm thấy
                    int totalPages = jsonResponse.optInt("total_pages", 1);
                    int currentPage = jsonResponse.optInt("current_page", 1);
                    
                    // Đảm bảo có ít nhất 1 trang
                    if (totalPages < 1) totalPages = 1;
                    if (currentPage < 1) currentPage = 1;
                    
                    // Tính lại số trang nếu có nhiều review nhưng chỉ có 1 trang (vấn đề với Điện Máy Xanh)
                    if (totalPages == 1 && reviewCount > 20) {
                        totalPages = (int) Math.ceil(reviewCount / 20.0);
                        System.out.println("Tính lại số trang từ reviewCount: " + totalPages);
                    }
                    
                    formattedInfo.append("TOTAL_PAGES:").append(totalPages).append("\n");
                    formattedInfo.append("CURRENT_PAGE:").append(currentPage).append("\n");
                    formattedInfo.append("--- ĐÁNH GIÁ SẢN PHẨM ---\n");
                    
                    // Thêm các đánh giá từ mảng JSON
                    for (int i = 0; i < reviewsArray.length(); i++) {
                        org.json.JSONObject review = reviewsArray.getJSONObject(i);
                        formattedInfo.append("REVIEWER:").append(review.getString("reviewer_name")).append("\n");
                        formattedInfo.append("CONTENT:").append(review.getString("content")).append("\n");
                        formattedInfo.append("RATING:").append(review.getInt("rating")).append("\n");
                        formattedInfo.append("TIME:").append(review.getString("time")).append("\n");
                        // Xử lý mảng hình ảnh nếu có
                        try {
                            if (review.has("images") && !review.isNull("images")) {
                                org.json.JSONArray images = review.getJSONArray("images");
                                if (images.length() > 0 && !images.isNull(0)) {
                                    // Kiểm tra cấu trúc của phần tử đầu tiên trong mảng
                                    Object firstImage = images.get(0);
                                    String imageUrl = "";
                                    
                                    if (firstImage instanceof String) {
                                        // Nếu là chuỗi, sử dụng trực tiếp
                                        imageUrl = (String) firstImage;
                                    } else if (firstImage instanceof org.json.JSONObject) {
                                        // Nếu là đối tượng JSON, tìm kiếm thuộc tính URL trong đó
                                        org.json.JSONObject imgObj = (org.json.JSONObject) firstImage;
                                        if (imgObj.has("full_path")) {
                                            imageUrl = imgObj.getString("full_path");
                                        } else if (imgObj.has("url")) {
                                            imageUrl = imgObj.getString("url");
                                        }
                                    }
                                    
                                    formattedInfo.append("IMAGE:").append(imageUrl).append("\n");
                                } else {
                                    formattedInfo.append("IMAGE:").append("\n");
                                }
                            } else {
                                formattedInfo.append("IMAGE:").append("\n");
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi khi xử lý hình ảnh đánh giá: " + e.getMessage());
                            formattedInfo.append("IMAGE:").append("\n");
                        }
                        formattedInfo.append("VIDEO:").append("\n");
                    }
                    
                    // Phân tích định dạng mới
                    parseProductInfoNewFormat(formattedInfo.toString());
                    return;
                }
            }
        } catch (Exception jsonEx) {
            System.err.println("Lỗi khi phân tích JSON: " + jsonEx.getMessage());
            jsonEx.printStackTrace();
        }
        
        // Nếu không phân tích được JSON, hiển thị thông báo lỗi
        JOptionPane.showMessageDialog(this, allMessages.toString(), "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Phân tích thông tin sản phẩm từ chuỗi phản hồi
     */
    /**
     * Phân tích thông tin sản phẩm từ định dạng mới (lazy loading)
     * @param productInfo Chuỗi thông tin sản phẩm từ server
     */
    private void parseProductInfoNewFormat(String productInfo) {
        try {
            System.out.println("Đang phân tích thông tin sản phẩm định dạng mới: " + productInfo.substring(0, Math.min(100, productInfo.length())) + "...");
            
            // Lấy dữ liệu platform hiện tại
            PlatformData platformData = platformDataMap.get(currentPlatform);
            
            // Phân tích các dòng khác nhau
            String[] lines = productInfo.split("\n");
            
            // Biến để lưu trữ thông tin sản phẩm
            String productId = "";
            String productName = "";
            String productPrice = "";
            String productImage = "";
            int reviewCount = 0;
            double avgRating = 0.0;
            
            // Danh sách để lưu các đánh giá và thông tin liên quan
            List<String> formattedReviews = new ArrayList<>();
            
            // Biến để theo dõi quá trình đọc đánh giá
            boolean isReadingReviews = false;
            StringBuilder currentReview = new StringBuilder();
            String currentReviewer = "";
            String currentRating = "";
            String currentContent = "";
            String currentImage = "";
            String currentVideo = "";
            
            // Reset thông tin phân trang
            currentPage = 1;
            totalPages = 1;
            
            // Biến để lưu trữ thông tin phân trang
            int parsedTotalPages = 0;
            int parsedCurrentPage = 0;
            
            // Đọc từng dòng dữ liệu
            for (String line : lines) {
                line = line.trim();
                
                // Xử lý thông tin sản phẩm
                if (line.startsWith("PRODUCT_ID:")) {
                    productId = line.substring(11).trim();
                } else if (line.startsWith("TÊN SẢN PHẨM:")) {
                    productName = line.substring(13).trim();
                } else if (line.startsWith("GIÁ:")) {
                    productPrice = line.substring(4).trim();
                } else if (line.startsWith("HÌNH ẢNH SẢN PHẨM:")) {
                    productImage = line.substring(18).trim();
                } else if (line.startsWith("HÌNH ẢNH:")) {
                    productImage = line.substring(9).trim();
                } else if (line.startsWith("SỐ LƯỢNG ĐÁNH GIÁ:")) {
                    try {
                        reviewCount = Integer.parseInt(line.substring(18).trim());
                    } catch (NumberFormatException e) {
                        System.err.println("Lỗi khi đọc số lượng đánh giá: " + e.getMessage());
                    }
                } else if (line.startsWith("ĐIỂM ĐÁNH GIÁ TRUNG BÌNH:")) {
                    try {
                        avgRating = Double.parseDouble(line.substring(25).trim());
                    } catch (NumberFormatException e) {
                        System.err.println("Lỗi khi đọc đánh giá trung bình: " + e.getMessage());
                    }
                } else if (line.startsWith("PAGINATION:")) {
                    // Format: PAGINATION:current_page:total_pages
                    String[] paginationParts = line.substring(11).split(":");
                    if (paginationParts.length == 2) {
                        try {
                            currentPage = Integer.parseInt(paginationParts[0]);
                            totalPages = Integer.parseInt(paginationParts[1]);
                            System.out.println("Thông tin phân trang: Trang " + currentPage + "/" + totalPages);
                            updatePaginationDisplay();
                        } catch (NumberFormatException e) {
                            System.err.println("Lỗi khi đọc thông tin phân trang: " + e.getMessage());
                        }
                    }
                } else if (line.equals("--- ĐÁNH GIÁ SẢN PHẨM ---")) {
                    // Bắt đầu đọc đánh giá
                    isReadingReviews = true;
                } else if (isReadingReviews) {
                    // Xử lý các đánh giá
                    if (line.startsWith("REVIEWER:")) {
                        // Nếu đã đọc một đánh giá trước đó, lưu lại
                        if (!currentReviewer.isEmpty() && !currentRating.isEmpty()) {
                            // Tạo đánh giá theo định dạng tương thích với updateTableWithReviews
                            String formattedReview = currentReviewer + " | " + currentRating + " | " + 
                                                     currentContent + " | " + currentImage + " | " + currentVideo;
                            formattedReviews.add(formattedReview);
                            System.out.println("Đã thêm đánh giá: " + formattedReview);
                        }
                        
                        // Bắt đầu đánh giá mới
                        String reviewerJson = line.substring(9).trim();
                        // Trích xuất full_name từ JSON
                        try {
                            if (reviewerJson.startsWith("{") && reviewerJson.contains("full_name")) {
                                int fullNameIndex = reviewerJson.indexOf("\"full_name\":\"");
                                if (fullNameIndex != -1) {
                                    int startIndex = fullNameIndex + 13; // độ dài của "full_name":"
                                    int endIndex = reviewerJson.indexOf("\"", startIndex);
                                    if (endIndex != -1) {
                                        currentReviewer = reviewerJson.substring(startIndex, endIndex);
                                    } else {
                                        currentReviewer = "Người dùng Tiki";
                                    }
                                } else {
                                    currentReviewer = "Người dùng Tiki";
                                }
                                
                                // Trích xuất avatar_url nếu có
                                int avatarIndex = reviewerJson.indexOf("\"avatar_url\":\"");
                                if (avatarIndex != -1) {
                                    int startIndex = avatarIndex + 13; // độ dài của "avatar_url":"
                                    int endIndex = reviewerJson.indexOf("\"", startIndex);
                                    if (endIndex != -1) {
                                        currentImage = reviewerJson.substring(startIndex, endIndex);
                                        // Đảm bảo URL avatar hợp lệ
                                        if (currentImage.startsWith("//")) {
                                            currentImage = "https:" + currentImage;
                                        }
                                    }
                                }
                            } else {
                                currentReviewer = reviewerJson;
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi khi trích xuất full_name: " + e.getMessage());
                            currentReviewer = reviewerJson;
                        }
                        
                        currentRating = "";
                        currentContent = "";
                        currentVideo = "";
                        currentImage = ""; // Reset hình ảnh cho đánh giá mới
                    } else if (line.startsWith("IMAGE:")) {
                        // Lấy URL hình ảnh từ review
                        currentImage = line.substring(6).trim();
                        System.out.println("Review Image URL (định dạng mới): " + currentImage);
                    } else if (line.startsWith("RATING:")) {
                        currentRating = line.substring(7).trim();
                    } else if (line.startsWith("CONTENT:")) {
                        currentContent = line.substring(8).trim();
                    } else if (line.startsWith("VIDEO:")) {
                        currentVideo = line.substring(6).trim();
                    }
                    // Nếu đang đọc đánh giá, và dòng tiếp theo không bắt đầu bằng keyword, có thể là phần nội dung tiếp theo
                    else if (!line.isEmpty() && !line.contains(":")) {
                        // Nối với nội dung hiện tại
                        if (!currentContent.isEmpty()) {
                            currentContent += " " + line;
                        }
                    }
                }
            }
            
            // Đảm bảo đánh giá cuối cùng cũng được thêm vào
            if (isReadingReviews && !currentReviewer.isEmpty() && !currentRating.isEmpty()) {
                String formattedReview = currentReviewer + " | " + currentRating + " | " + 
                                         currentContent + " | " + currentImage + " | " + currentVideo;
                formattedReviews.add(formattedReview);
                System.out.println("Đã thêm đánh giá cuối cùng: " + formattedReview);
            }
            
            // Cập nhật thông tin sản phẩm vào dữ liệu nền tảng
            platformData.productId = productId;
            platformData.productName = productName;
            platformData.productPrice = productPrice;
            platformData.productImage = productImage;
            platformData.reviewCount = reviewCount;
            platformData.avgRating = avgRating;
            platformData.searchKeyword = textInfor.getText().trim();
            
            // Cập nhật bảng và hiển thị thông tin
            updateTableWithReviews(formattedReviews.toArray(new String[0]));
            displayProductImage(platformData.productImage);
            
            // Cập nhật hiển thị thông tin phân trang
            updatePaginationDisplay();
            
            // Cập nhật thông tin tổng quan với định dạng số thập phân cho đánh giá
            String formattedRating = String.format("%.1f", platformData.avgRating); // Hiển thị 1 chữ số thập phân
            lblOverView.setText("Sản phẩm: " + platformData.productName + " | Giá: " + platformData.productPrice + " VNĐ | Số đánh giá: " 
                + platformData.reviewCount + " | Đánh giá trung bình: " + formattedRating + "/5");
                
            System.out.println("Đã hoàn tất phân tích thông tin sản phẩm định dạng mới!");
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi xử lý thông tin sản phẩm định dạng mới: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
    
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
            
            // Cập nhật hiển thị thông tin phân trang
            updatePaginationDisplay();
            
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
            if (imageUrl == null || imageUrl.isEmpty()) {
                System.err.println("URL hình ảnh rỗng hoặc null");
                return null;
            }

            // ===== Xử lý URL trước khi tải =====
            System.out.println("URL ảnh đánh giá gốc: " + imageUrl);
            
            // Đảm bảo URL hình ảnh có giao thức
            if (imageUrl.startsWith("//")) {
                imageUrl = "https:" + imageUrl;
                System.out.println("Đã thêm https: " + imageUrl);
            }
            
            // Loại bỏ phần xử lý ảnh từ TGDD trong URL (nếu có)
            if (imageUrl.contains("/imgt/") || imageUrl.contains("img.tgdd.vn")) {
                int cdnIndex = imageUrl.indexOf("https://cdn.tgdd.vn");
                if (cdnIndex > 0) {
                    imageUrl = imageUrl.substring(cdnIndex);
                    System.out.println("Đã loại bỏ phần xử lý ảnh: " + imageUrl);
                }
            }
            
            // Đảm bảo URL có đủ phần HTTP cho java.net.URL parse
            if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                imageUrl = "https:" + imageUrl;
                System.out.println("Đã thêm protocol HTTPS: " + imageUrl);
            }
            
            // Nếu URL chứa ký tự không hợp lệ, encode URL
            if (imageUrl.contains(" ")) {
                imageUrl = imageUrl.replace(" ", "%20");
                System.out.println("Đã encode ký tự space: " + imageUrl);
            }
            
            System.out.println("URL ảnh đánh giá đã xử lý: " + imageUrl);
            
            // ===== Tải ảnh với timeout =====
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Thiết lập User-Agent để tránh bị chặn
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            
            // Mở connection
            connection.connect();
            
            // Kiểm tra response code
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Lỗi HTTP khi tải ảnh đánh giá: " + responseCode);
                return null;
            }
            
            // Đọc ảnh từ InputStream
            try (InputStream in = connection.getInputStream()) {
                Image image = ImageIO.read(in);
                
                if (image != null) {
                    // Co giãn hình ảnh để vừa với kích thước chỉ định
                    Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    System.out.println("Đã tải và co giãn ảnh đánh giá thành công");
                    return new ImageIcon(scaledImage);
                } else {
                    System.err.println("ImageIO.read trả về null cho URL: " + imageUrl);
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi tải hình ảnh đánh giá từ URL " + imageUrl + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        // Trả về biểu tượng mặc định nếu không thể tải ảnh
        System.out.println("Sử dụng biểu tượng mặc định thay thế");
        return new ImageIcon(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
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
        
        // Nếu hiển thị Điện Máy Xanh và có URL sản phẩm, thì thử lấy thông tin rating chính xác từ trang web
        if (currentPlatform.equals("ĐIỆN MÁY XANH") && platformData.productId != null && !platformData.productId.isEmpty()) {
            try {
                // Tạo URL để truy cập trang sản phẩm
                String productUrl;
                if (platformData.productId.startsWith("http")) {
                    productUrl = platformData.productId;
                } else {
                    productUrl = "https://www.dienmayxanh.com" + platformData.productId;
                }
                
                // Gửi request để lấy thông tin chính xác về rating
                String response = client.getProductRatingInfo(productUrl);
                
                // Phân tích response
                if (response != null && !response.isEmpty() && !response.startsWith("Lỗi")) {
                    // Response có định dạng: "average_rating:4.5|total_reviews:123"
                    String[] parts = response.split("\\|");
                    if (parts.length >= 2) {
                        String avgRatingStr = parts[0].split(":")[1];
                        String reviewCountStr = parts[1].split(":")[1];
                        
                        try {
                            double newAvgRating = Double.parseDouble(avgRatingStr);
                            int newReviewCount = Integer.parseInt(reviewCountStr);
                            
                            // Cập nhật thông tin trong platformData
                            platformData.avgRating = newAvgRating;
                            platformData.reviewCount = newReviewCount;
                            
                            System.out.println("Đã cập nhật thông tin đánh giá từ server: Đánh giá trung bình=" + 
                                newAvgRating + ", Số đánh giá=" + newReviewCount);
                        } catch (NumberFormatException e) {
                            System.err.println("Không thể phân tích thông tin đánh giá: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi lấy thông tin đánh giá từ server: " + e.getMessage());
            }
        }
        
        // Hiển thị thông tin tổng quan với định dạng số thập phân cho đánh giá
        String formattedRating = String.format("%.1f", platformData.avgRating); // Hiển thị 1 chữ số thập phân
        String formattedPrice = platformData.productPrice;
        
        // Nếu giá không kết thúc bằng "VNĐ" hoặc "đ", thêm " VNĐ"
        if (!formattedPrice.endsWith("VNĐ") && !formattedPrice.endsWith("đ") && !formattedPrice.endsWith("Đ")) {
            formattedPrice += " VNĐ";
        }
        
        lblOverView.setText("Sản phẩm: " + platformData.productName + " | Giá: " + formattedPrice + 
            " | Số đánh giá: " + platformData.reviewCount + " | Đánh giá trung bình: " + formattedRating + "/5");
    }
    
    /**
     * Hiển thị đánh giá từ nền tảng chỉ định (TIKI, ĐIỆN MÁY XANH)
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
        btnDMX.setBackground(platform.equals("ĐIỆN MÁY XANH") ? new Color(0, 150, 136) : new Color(70, 130, 180));

        
        try {
            // Thay đổi nền tảng trên server
            String response = client.changePlatform(platform);
            
            // Kiểm tra nếu phản hồi chứa thông báo lỗi
            if (response.startsWith("Lỗi:")) {
                // Hiển thị thông báo lỗi từ server
                JOptionPane.showMessageDialog(this, response, "Lỗi từ server", JOptionPane.ERROR_MESSAGE);
                
                // Khôi phục UI về nền tảng trước đó
                btnTiki.setBackground(previousPlatform.equals("TIKI") ? new Color(0, 150, 136) : new Color(70, 130, 180));
                btnDMX.setBackground(previousPlatform.equals("ĐIỆN MÁY XANH") ? new Color(0, 150, 136) : new Color(70, 130, 180));

                return;
            }
            
            // Cập nhật nền tảng hiện tại
            currentPlatform = platform;
            
            // Lấy dữ liệu của nền tảng mới được chọn
            PlatformData platformData = platformDataMap.get(currentPlatform);
            
            // Reset dữ liệu của nền tảng mới để tránh ảnh hưởng từ dữ liệu cũ
            platformData.productId = "";
            platformData.productName = "";
            platformData.productPrice = "";
            platformData.productImage = "";
            platformData.reviewCount = 0;
            platformData.avgRating = 0.0;
            platformData.searchKeyword = "";
            platformData.tableModel.setRowCount(0); // Xóa tất cả dữ liệu trong bảng
            
            // Hiển thị thông báo nền tảng đã đổi
            lblOverView.setText("Đã chuyển sang nền tảng: " + platform);
            
            // Cập nhật giao diện
            table.setModel(platformData.tableModel);
            lblProductImage.setIcon(null);
            lblProductImage.setText("Không có hình ảnh");
            
            // Xóa ô tìm kiếm để người dùng tự nhập
            textInfor.setText("");
            
            // Reset thông tin phân trang
            currentPage = 1;
            totalPages = 1;
            updatePaginationDisplay();
            
            // Thêm renderer cho cột ảnh và video
            try {
                table.getColumnModel().getColumn(3).setCellRenderer(new ImageCellRenderer());
                table.getColumnModel().getColumn(4).setCellRenderer(new ButtonCellRenderer());
            } catch (Exception ex) {
                // Bỏ qua lỗi nếu bảng chưa sẵn sàng
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi chuyển đổi nền tảng: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
    //them
    private void showSummarizedReview() {
        // Lấy dữ liệu của nền tảng hiện tại
        PlatformData platformData = platformDataMap.get(currentPlatform);

        if (platformData.productName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng tìm kiếm sản phẩm trước", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Lưu trữ giá trị tìm kiếm hiện tại để khôi phục sau này
        final String currentSearchText = textInfor.getText();

        // Hiển thị thanh tiến trình
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        try {
            // Trích xuất nội dung đánh giá từ tableModel
            StringBuilder reviewText = new StringBuilder();
            DefaultTableModel model = platformData.tableModel;
            
            // Giới hạn độ dài chỉ lấy 10 đánh giá cho mỗi lần tổng hợp 
            // để tránh vượt quá giới hạn request và đảm bảo phản hồi nhanh
            int maxReviews = Math.min(model.getRowCount(), 10);
            
            for (int i = 0; i < maxReviews; i++) {
                Object content = model.getValueAt(i, 2); // Cột 2: nội dung đánh giá
                if (content != null && content.toString().trim().length() > 0) {
                    // Giới hạn độ dài mỗi đánh giá
                    String reviewContent = content.toString();
                    if (reviewContent.length() > 200) {
                        reviewContent = reviewContent.substring(0, 197) + "...";
                    }
                    reviewText.append("- ").append(reviewContent).append(" --------\n");
                }
            }

            // Kiểm tra nếu không có đánh giá
            if (reviewText.length() == 0) {
                JOptionPane.showMessageDialog(this, "Không có đánh giá nào để tổng hợp", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Tạo một kết nối mới đến server cho yêu cầu tổng hợp
            // để tránh ảnh hưởng đến kết nối hiện tại
            IPFetcher ipFetcher = new IPFetcher();
            String ip = ipFetcher.fetch_IP();
            ProductReviewClient summaryClient = new ProductReviewClient(ip, 1234);
            
            if (!summaryClient.connect()) {
                JOptionPane.showMessageDialog(this, "Không thể kết nối đến máy chủ", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                // Gửi yêu cầu tổng hợp đến server
                String productId = platformData.productId; // Sử dụng ID sản phẩm thực tế
                String summarizedReview = summaryClient.summarizeReviews(productId, reviewText.toString());
                
                // Kiểm tra kết quả
                if (summarizedReview == null || summarizedReview.isEmpty() || 
                    summarizedReview.contains("Lỗi") || 
                    summarizedReview.contains("Không thể tổng hợp")) {
                    JOptionPane.showMessageDialog(this, "Không thể tổng hợp đánh giá: " + summarizedReview, 
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
    
                // Định dạng lại kết quả để hiển thị
                summarizedReview = summarizedReview.replaceAll("(?m)^\\s*\\*", "-");
                summarizedReview = summarizedReview.replace("**", "");
    
                // Hiển thị kết quả trong một hộp thoại
                JTextArea textArea = new JTextArea(summarizedReview);
                textArea.setWrapStyleWord(true);
                textArea.setLineWrap(true);
                textArea.setEditable(false);
                textArea.setFocusable(false); // Ngăn chặn focus trên text area
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(600, 400));
                
                // Sử dụng JOptionPane modal để tránh tương tác với GUI chính khi dialog đang hiển thị
                JDialog dialog = new JDialog(this, "Tổng hợp đánh giá", true);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                JButton closeButton = new JButton("Đóng");
                closeButton.addActionListener(e -> {
                    dialog.dispose();
                });
                
                JPanel buttonPanel = new JPanel();
                buttonPanel.add(closeButton);
                
                dialog.setLayout(new BorderLayout());
                dialog.add(scrollPane, BorderLayout.CENTER);
                dialog.add(buttonPanel, BorderLayout.SOUTH);
                dialog.setSize(650, 450);
                dialog.setLocationRelativeTo(this);
                
                // Ngăn chặn các hành động trong khi dialog hiển thị
                dialog.setModal(true);
                dialog.setVisible(true);
                
                // Đóng và giải phóng tài nguyên dialog
                dialog.dispose();
            } finally {
                // Đóng kết nối riêng biệt sau khi sử dụng
                summaryClient.close();
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi khi tổng hợp đánh giá: " + e.getMessage(), 
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        } finally {
            // Đặt lại con trỏ chuột
            setCursor(Cursor.getDefaultCursor());
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(ProductReviewGUI::new);
    }
}