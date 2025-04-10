package GUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

/**
 * Class để xử lý gợi ý sản phẩm khi gõ từ khóa vào ô tìm kiếm
 */
public class ProductSuggestionProvider {
    private JTextField searchField;
    private JWindow popupWindow;
    private JList<String> suggestionList;
    private DefaultListModel<String> listModel;
    private boolean isSelecting = false;

    // ThreadPool để thực hiện truy vấn API bất đồng bộ
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Thời gian delay để tránh gọi API quá nhiều
    private static final int SUGGESTION_DELAY = 300; // milliseconds
    private Timer suggestionsTimer;

    // API URL cho gợi ý từ khóa tìm kiếm
    private static final String TIKI_SUGGESTION_API = "https://tiki.vn/api/v2/search/suggestion?&q=";

    // Danh sách các gợi ý mặc định sử dụng khi không có kết nối hoặc API lỗi
    private final List<String> defaultSuggestions = List.of(
            "điện thoại samsung",
            "điện thoại iphone",
            "laptop asus",
            "laptop dell",
            "laptop macbook",
            "tai nghe bluetooth",
            "tai nghe airpods",
            "bàn phím cơ",
            "chuột gaming",
            "tivi samsung"
    );

    /**
     * Constructor
     * @param searchField TextField để gõ từ khóa tìm kiếm
     */
    public ProductSuggestionProvider(JTextField searchField) {
        this.searchField = searchField;
        this.listModel = new DefaultListModel<>();
        this.suggestionList = new JList<>(listModel);

        // Khởi tạo timer với delay
        this.suggestionsTimer = new Timer(SUGGESTION_DELAY, e -> fetchSuggestionsFromAPI());
        this.suggestionsTimer.setRepeats(false);

        setupSuggestionList();
        setupSearchField();
    }

    /**
     * Phương thức để lấy các từ khóa gợi ý từ API Tiki
     */
    private void fetchSuggestionsFromAPI() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            return;
        }

        // Thực hiện gọi API trong một thread riêng biệt
        executor.submit(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                String apiUrl = TIKI_SUGGESTION_API + encodedQuery;

                System.out.println("Gọi API gợi ý: " + apiUrl);

                // Gọi API và lấy kết quả
                String jsonResponse = Jsoup.connect(apiUrl)
                        .ignoreContentType(true)
                        .userAgent("Mozilla/5.0")
                        .timeout(3000)
                        .execute()
                        .body();

                // In ra để debug
                System.out.println("Phản hồi API: " + jsonResponse);

                // Phân tích JSON
                JSONObject json = new JSONObject(jsonResponse);
                if (json.has("data")) {
                    JSONArray data = json.getJSONArray("data");

                    // Danh sách từ khóa được gợi ý
                    List<String> suggestions = new ArrayList<>();
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject item = data.getJSONObject(i);
                        if (item.has("keyword")) {
                            String keyword = item.getString("keyword");
                            suggestions.add(keyword);
                            System.out.println("Đã thêm gợi ý: " + keyword);
                        } else if (item.has("type") && item.getString("type").equals("keyword")) {
                            // Cấu trúc mới của API suggestion
                            String keyword = item.getString("keyword");
                            suggestions.add(keyword);
                            System.out.println("Đã thêm gợi ý (cấu trúc mới): " + keyword);
                        }
                    }

                    if (suggestions.isEmpty()) {
                        // Nếu không có gợi ý nào từ API, thêm text hiện tại vào
                        suggestions.add(query);
                    }

                    // Cập nhật UI trong EDT
                    SwingUtilities.invokeLater(() -> updateSuggestionList(suggestions));
                } else {
                    // Nếu không có data, vẫn hiển thị text hiện tại như một gợi ý
                    SwingUtilities.invokeLater(() -> {
                        List<String> defaultList = new ArrayList<>();
                        defaultList.add(query);
                        updateSuggestionList(defaultList);
                    });
                }
            } catch (Exception e) {
                // Nếu có lỗi, sử dụng gợi ý mặc định
                System.err.println("Lỗi khi lấy gợi ý từ API: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    // Lọc gợi ý mặc định dựa trên text
                    String text = searchField.getText().toLowerCase().trim();
                    List<String> filteredSuggestions = new ArrayList<>();

                    // Thêm text hiện tại như một gợi ý
                    filteredSuggestions.add(text);

                    // Thêm các gợi ý mặc định phù hợp
                    for (String suggestion : defaultSuggestions) {
                        if (suggestion.toLowerCase().contains(text) && !filteredSuggestions.contains(suggestion)) {
                            filteredSuggestions.add(suggestion);
                        }
                    }

                    updateSuggestionList(filteredSuggestions);
                });
            }
        });
    }

    /**
     * Cập nhật danh sách gợi ý vào UI
     */
    private void updateSuggestionList(List<String> suggestions) {
        if (suggestions.isEmpty()) {
            hidePopup();
            return;
        }

        // Cập nhật model
        listModel.clear();
        for (String suggestion : suggestions) {
            listModel.addElement(suggestion);
        }

        // Hiển thị popup
        if (!popupWindow.isVisible()) {
            showPopup();
        } else {
            updatePopupSize();
        }

        // Chọn mục đầu tiên mặc định
        suggestionList.setSelectedIndex(0);
    }

    /**
     * Thiết lập danh sách gợi ý
     */
    private void setupSuggestionList() {
        // Cấu hình danh sách gợi ý
        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setFixedCellHeight(25);
        suggestionList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        suggestionList.setFont(new Font("Arial", Font.PLAIN, 14));
        suggestionList.setBackground(Color.WHITE);

        // Tạo window popup thay vì JPopupMenu để tăng khả năng kiểm soát
        popupWindow = new JWindow((Window)SwingUtilities.getRoot(searchField));
        JScrollPane scrollPane = new JScrollPane(suggestionList);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        popupWindow.getContentPane().add(scrollPane);
        popupWindow.setFocusableWindowState(false);

        // Xử lý khi click vào một gợi ý
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    selectSuggestion();
                }
            }
        });

        // Cho phép chọn bằng Enter
        suggestionList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    selectSuggestion();
                }
            }
        });

        // Xử lý focus
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (!e.isTemporary()) {
                    SwingUtilities.invokeLater(() -> {
                        if (!suggestionList.hasFocus()) {
                            hidePopup();
                        }
                    });
                }
            }
        });

        // Ẩn popup khi click ra ngoài
        Component rootComponent = SwingUtilities.getRoot(searchField);
        if (rootComponent instanceof JFrame) {
            JFrame rootFrame = (JFrame) rootComponent;
            rootFrame.getContentPane().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Component source = e.getComponent();
                    if (source != suggestionList && source != searchField) {
                        hidePopup();
                    }
                }
            });
        }
    }

    /**
     * Chọn gợi ý từ danh sách và cập nhật ô tìm kiếm
     */
    private void selectSuggestion() {
        isSelecting = true;
        int selectedIndex = suggestionList.getSelectedIndex();
        if (selectedIndex >= 0) {
            String selectedValue = suggestionList.getModel().getElementAt(selectedIndex);
            searchField.setText(selectedValue);
            searchField.setCaretPosition(selectedValue.length());
            hidePopup();
            searchField.requestFocus();

            // Tự động thực hiện tìm kiếm bằng cách giả lập ấn Enter
            ActionEvent event = new ActionEvent(
                    searchField,
                    ActionEvent.ACTION_PERFORMED,
                    selectedValue
            );

            // Kích hoạt ActionListeners của searchField
            for (ActionListener listener : searchField.getActionListeners()) {
                listener.actionPerformed(event);
            }
        }
        isSelecting = false;
    }

    /**
     * Thiết lập xử lý sự kiện cho ô tìm kiếm
     */
    private void setupSearchField() {
        // Lắng nghe thay đổi nội dung
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!isSelecting) updateSuggestions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!isSelecting) updateSuggestions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (!isSelecting) updateSuggestions();
            }
        });

        // Xử lý phím
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (popupWindow.isVisible()) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_DOWN:
                            int currentIndex = suggestionList.getSelectedIndex();
                            int size = suggestionList.getModel().getSize();
                            if (size > 0) {
                                if (currentIndex < 0 || currentIndex >= size - 1) {
                                    suggestionList.setSelectedIndex(0);
                                } else {
                                    suggestionList.setSelectedIndex(currentIndex + 1);
                                }
                                suggestionList.ensureIndexIsVisible(suggestionList.getSelectedIndex());
                            }
                            e.consume(); // Ngăn không cho sự kiện truyền xuống text field
                            break;
                        case KeyEvent.VK_UP:
                            currentIndex = suggestionList.getSelectedIndex();
                            size = suggestionList.getModel().getSize();
                            if (size > 0) {
                                if (currentIndex <= 0) {
                                    suggestionList.setSelectedIndex(size - 1);
                                } else {
                                    suggestionList.setSelectedIndex(currentIndex - 1);
                                }
                                suggestionList.ensureIndexIsVisible(suggestionList.getSelectedIndex());
                            }
                            e.consume(); // Ngăn không cho sự kiện truyền xuống text field
                            break;
                        case KeyEvent.VK_ENTER:
                            if (suggestionList.getSelectedIndex() >= 0) {
                                selectSuggestion();
                                e.consume(); // Ngăn không cho sự kiện truyền xuống text field
                            } else {
                                hidePopup();
                            }
                            break;
                        case KeyEvent.VK_ESCAPE:
                            hidePopup();
                            e.consume(); // Ngăn không cho sự kiện truyền xuống text field
                            break;
                    }
                }
            }
        });
    }

    /**
     * Cập nhật danh sách gợi ý dựa trên văn bản hiện tại
     */
    private void updateSuggestions() {
        String text = searchField.getText().trim();

        // Chỉ hiển thị gợi ý khi có văn bản
        if (text.isEmpty()) {
            hidePopup();
            return;
        }

        // Dừng timer hiện tại nếu đang chạy
        if (suggestionsTimer.isRunning()) {
            suggestionsTimer.stop();
        }

        // Khởi động timer mới - sẽ gọi fetchSuggestionsFromAPI() sau khoảng thời gian delay
        suggestionsTimer.start();
    }

    /**
     * Hiển thị popup gợi ý
     */
    private void showPopup() {
        if (!popupWindow.isVisible()) {
            Point p = new Point();
            SwingUtilities.convertPointToScreen(p, searchField);

            // Đặt vị trí và kích thước cho popup
            p.y += searchField.getHeight();
            updatePopupSize();
            popupWindow.setLocation(p);
            popupWindow.setVisible(true);
        }
    }

    /**
     * Ẩn popup gợi ý
     */
    private void hidePopup() {
        popupWindow.setVisible(false);
    }

    /**
     * Cập nhật kích thước popup theo danh sách gợi ý
     */
    private void updatePopupSize() {
        int width = searchField.getWidth();
        int height = Math.min(200, suggestionList.getModel().getSize() * 25 + 10);
        if (height > 0) {
            popupWindow.setSize(width, height);
        }
    }
}