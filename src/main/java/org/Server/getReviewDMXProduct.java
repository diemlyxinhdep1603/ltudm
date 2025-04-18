package org.Server;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class để lấy review sản phẩm từ Điện Máy Xanh sử dụng Playwright
 * Dựa trên cấu trúc từ Test.java
 */
public class getReviewDMXProduct {
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    
    // Biến lưu thông tin đánh giá để các phương thức khác sử dụng
    private double productRatingAverage = 0.0;
    private int productRatingCount = 0;
    
    /**
     * Lấy điểm đánh giá trung bình đã lưu
     * @return Điểm đánh giá trung bình
     */
    public double getProductRatingAverage() {
        return productRatingAverage;
    }
    
    /**
     * Lấy số lượng đánh giá đã lưu
     * @return Số lượng đánh giá
     */
    public int getProductRatingCount() {
        return productRatingCount;
    }
    
    /**
     * Constructor - khởi tạo Playwright và trình duyệt
     */
    public getReviewDMXProduct() {
        try {
            // Khởi tạo logger
            DMXSearchLogger.init();
            DMXSearchLogger.log("Bắt đầu khởi tạo Playwright...");
            
            // Khởi tạo Playwright
            playwright = Playwright.create();
            DMXSearchLogger.log("Đã tạo Playwright instance");
            
            // Khởi tạo trình duyệt, sử dụng webkit như trong Test.java (không thêm timeout)
            browser = playwright.webkit().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)); // Chạy ẩn, không mở UI
            DMXSearchLogger.log("Đã khởi tạo trình duyệt WebKit");
            
            // Tạo context mới giữ nguyên cấu hình mặc định giống với Test.java
            context = browser.newContext();
            DMXSearchLogger.log("Đã tạo browser context");
            
            System.out.println("Khởi tạo Playwright thành công cho Điện Máy Xanh");
        } catch (Exception e) {
            DMXSearchLogger.logError("Lỗi khởi tạo Playwright", e);
            System.err.println("Lỗi khởi tạo Playwright: " + e.getMessage());
            cleanup();
        }
    }
    
    /**
     * Tìm kiếm sản phẩm trên Điện Máy Xanh và trả về URL của sản phẩm đầu tiên
     * Đã cập nhật để phù hợp với cấu trúc HTML thực tế
     * 
     * @param productName Tên sản phẩm cần tìm
     * @return URL của sản phẩm đầu tiên hoặc null nếu không tìm thấy
     */
    public String findProductUrl(String productName) {
        if (context == null) {
            DMXSearchLogger.log("Browser context chưa được khởi tạo");
            System.err.println("Browser context chưa được khởi tạo");
            return null;
        }
        
        Page page = null;
        try {
            DMXSearchLogger.log("Bắt đầu tìm kiếm sản phẩm: " + productName);
            page = context.newPage();
            
            // Truy cập trang tìm kiếm của Điện Máy Xanh (giống Test.java)
            String searchUrl = "https://www.dienmayxanh.com/search?key=" + productName.replaceAll(" ", "+");
            DMXSearchLogger.log("Đang truy cập URL tìm kiếm: " + searchUrl);
            page.navigate(searchUrl);
            
            // Đợi trang load xong
            page.waitForLoadState(LoadState.NETWORKIDLE);
            DMXSearchLogger.log("Trang đã load xong");
            
            // Lưu screenshot để phân tích
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("dmx_search_page.png")));
            DMXSearchLogger.log("Đã lưu screenshot vào dmx_search_page.png");
            
            // Lưu HTML để debug nếu cần
            try {
                String pageContent = page.content();
                FileWriter htmlWriter = new FileWriter("dmx_search_html.html");
                htmlWriter.write(pageContent);
                htmlWriter.close();
                DMXSearchLogger.log("Đã lưu HTML vào dmx_search_html.html");
            } catch (IOException e) {
                DMXSearchLogger.log("Không thể lưu HTML: " + e.getMessage());
            }
            
            // Sử dụng selector giống hệt Test.java
            Locator products = page.locator(".item a.main-contain");
            DMXSearchLogger.log("Tìm thấy " + products.count() + " sản phẩm với selector '.item a.main-contain'");
            
            if (products.count() > 0) {
                System.out.println("Tìm thấy " + products.count() + " sản phẩm");
                
                // Lưu lại các link và tên sản phẩm
                List<String> productLinks = new ArrayList<>();
                List<String> productNames = new ArrayList<>();
                List<String> productTitles = new ArrayList<>(); // Titles từ attribute title
                
                // Lấy danh sách các link sản phẩm
                Object evalResult = products.evaluateAll("list => list.map(el => el.getAttribute('href'))");
                DMXSearchLogger.log("Đã chạy evaluateAll với href, kết quả: " + evalResult);
                
                // Lấy cả title để đối chiếu (từ attribute title)
                Object evalTitles = products.evaluateAll("list => list.map(el => el.getAttribute('title'))");
                DMXSearchLogger.log("Đã chạy evaluateAll với title, kết quả: " + evalTitles);
                
                if (evalResult instanceof List<?> && evalTitles instanceof List<?>) {
                    List<?> links = (List<?>) evalResult;
                    List<?> titles = (List<?>) evalTitles;
                    
                    for (int i = 0; i < links.size(); i++) {
                        Object link = links.get(i);
                        Object title = (i < titles.size()) ? titles.get(i) : null;
                        
                        if (link instanceof String href && !href.trim().isEmpty()) {
                            String productLink = href.trim();
                            productLinks.add(productLink);
                            
                            // Trích xuất tên từ link
                            String productName1 = extractProductNameFromLink(productLink);
                            productNames.add(productName1);
                            
                            // Lấy tên từ attribute title nếu có
                            String productTitle = (title instanceof String) ? ((String) title).trim() : "";
                            if (!productTitle.isEmpty()) {
                                productTitles.add(productTitle);
                                DMXSearchLogger.log("Sản phẩm: " + productTitle + " - Link: " + productLink);
                            } else {
                                productTitles.add(productName1); // Dùng tên từ link nếu không có title
                                DMXSearchLogger.log("Sản phẩm (từ link): " + productName1 + " - Link: " + productLink);
                            }
                        }
                    }
                } else {
                    // Lấy theo cách cũ nếu không lấy được cả href và title
                    if (evalResult instanceof List<?>) {
                        for (Object obj : (List<?>) evalResult) {
                            if (obj instanceof String href && !href.trim().isEmpty()) {
                                productLinks.add(href.trim());
                                String productTitle = extractProductNameFromLink(href);
                                productNames.add(productTitle);
                                productTitles.add(productTitle);
                                DMXSearchLogger.log("Sản phẩm: " + productTitle + " - Link: " + href);
                            }
                        }
                    }
                }
                
                // Nếu có sản phẩm nào được tìm thấy
                if (!productLinks.isEmpty()) {
                    // Tìm sản phẩm có độ tương đồng cao nhất với từ khóa tìm kiếm
                    String bestMatch = null;
                    int maxScore = 0;
                    String bestMatchLink = null;
                    
                    // So sánh cả tên từ link và title với từ khóa tìm kiếm
                    for (int i = 0; i < productLinks.size(); i++) {
                        String productTitle = (i < productTitles.size()) ? productTitles.get(i) : productNames.get(i);
                        int score = getMatchScore(productName, productTitle);
                        
                        // Kiểm tra thêm từ tên được trích xuất từ link
                        if (i < productNames.size()) {
                            int scoreFromLink = getMatchScore(productName, productNames.get(i));
                            if (scoreFromLink > score) {
                                score = scoreFromLink;
                            }
                        }
                        
                        DMXSearchLogger.log("So sánh: '" + productName + "' với '" + productTitle + "' - Điểm: " + score);
                        
                        if (score > maxScore) {
                            maxScore = score;
                            bestMatch = productTitle;
                            bestMatchLink = productLinks.get(i);
                        }
                    }
                    
                    if (bestMatch != null && maxScore > 0) {
                        DMXSearchLogger.log("Tên sản phẩm gần đúng nhất: " + bestMatch + " (điểm: " + maxScore + ")");
                        // Đảm bảo link đầy đủ
                        if (!bestMatchLink.startsWith("http")) {
                            bestMatchLink = "https://www.dienmayxanh.com" + bestMatchLink;
                        }
                        DMXSearchLogger.log("URL sản phẩm: " + bestMatchLink);
                        return bestMatchLink;
                    } else {
                        // Nếu không tìm được sản phẩm phù hợp nhất, trả về sản phẩm đầu tiên
                        String firstLink = productLinks.get(0);
                        if (!firstLink.startsWith("http")) {
                            firstLink = "https://www.dienmayxanh.com" + firstLink;
                        }
                        DMXSearchLogger.log("Không tìm thấy sản phẩm phù hợp nhất, trả về sản phẩm đầu tiên: " + 
                            (productTitles.isEmpty() ? productNames.get(0) : productTitles.get(0)));
                        return firstLink;
                    }
                } else {
                    DMXSearchLogger.log("Không trích xuất được link sản phẩm nào");
                }
            } else {
                DMXSearchLogger.log("Không tìm thấy sản phẩm nào với selector '.item a.main-contain'");
                
                // Thử tìm kiếm với selector khác để debug
                Locator allItems = page.locator(".item");
                DMXSearchLogger.log("Tìm thấy " + allItems.count() + " phần tử với selector '.item'");
                
                Locator allLinks = page.locator("a.main-contain");
                DMXSearchLogger.log("Tìm thấy " + allLinks.count() + " phần tử với selector 'a.main-contain'");
                
                // Thử tìm với selector khác của Điện Máy Xanh nếu cấu trúc trang đã thay đổi
                Locator alternativeProducts = page.locator("a[href*='/p/']");
                DMXSearchLogger.log("Thử với selector thay thế: Tìm thấy " + alternativeProducts.count() + " phần tử với selector 'a[href*=/p/]'");
                
                if (alternativeProducts.count() > 0) {
                    // Thử lấy link từ selector thay thế
                    String firstLink = alternativeProducts.first().getAttribute("href");
                    if (firstLink != null && !firstLink.isEmpty()) {
                        if (!firstLink.startsWith("http")) {
                            firstLink = "https://www.dienmayxanh.com" + firstLink;
                        }
                        DMXSearchLogger.log("Sử dụng selector thay thế và tìm thấy sản phẩm: " + firstLink);
                        return firstLink;
                    }
                }
            }
            
            DMXSearchLogger.log("Không tìm thấy sản phẩm nào cho từ khóa: " + productName);
            return null;
        } catch (Exception e) {
            DMXSearchLogger.logError("Lỗi khi tìm kiếm sản phẩm", e);
            System.err.println("Lỗi khi tìm kiếm sản phẩm trên Điện Máy Xanh: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (page != null) {
                page.close();
            }
        }
    }
    
    // Phương thức trích xuất tên sản phẩm từ link (từ Test.java) - Cải thiện để xử lý các trường hợp khác nhau
    private String extractProductNameFromLink(String link) {
        if (link == null || link.isEmpty()) {
            return "Sản phẩm không xác định";
        }
        
        // Xử lý URL với tham số
        String cleanLink = link;
        if (link.contains("?")) {
            cleanLink = link.substring(0, link.indexOf("?"));
        }
        
        // Lấy phần cuối của URL (tên sản phẩm)
        String nameFromUrl = cleanLink.substring(cleanLink.lastIndexOf("/") + 1).replaceAll("-", " ");
        
        // Nếu link không chứa dấu gạch ngang hoặc phần tách quá ngắn, trả về toàn bộ link
        if (nameFromUrl.isEmpty() || nameFromUrl.length() < 3) {
            return "Sản phẩm từ " + link;
        }
        
        return nameFromUrl;
    }
    
    // Phương thức tính điểm tương đồng (cải tiến từ Test.java)
    private int getMatchScore(String userInput, String suggestion) {
        if (userInput == null || suggestion == null) {
            return 0;
        }
        
        userInput = userInput.toLowerCase();
        suggestion = suggestion.toLowerCase();
        
        // Nếu chuỗi hoàn toàn giống nhau thì cho điểm cao nhất
        if (userInput.equals(suggestion)) {
            return Integer.MAX_VALUE;
        }
        
        // Nếu suggestion chứa userInput thì cũng cho điểm cao
        if (suggestion.contains(userInput)) {
            return userInput.length() * 10;
        }
        
        // Nếu userInput chứa suggestion
        if (userInput.contains(suggestion)) {
            return suggestion.length() * 8;
        }
        
        // Tính số ký tự trùng khớp theo vị trí
        int matchCount = 0;
        int consecutiveMatches = 0;
        int maxConsecutiveMatches = 0;
        
        for (int i = 0; i < Math.min(userInput.length(), suggestion.length()); i++) {
            if (userInput.charAt(i) == suggestion.charAt(i)) {
                matchCount++;
                consecutiveMatches++;
                if (consecutiveMatches > maxConsecutiveMatches) {
                    maxConsecutiveMatches = consecutiveMatches;
                }
            } else {
                consecutiveMatches = 0;
            }
        }
        
        // Tính toán điểm dựa trên số ký tự trùng khớp và số ký tự trùng khớp liên tiếp
        return matchCount + (maxConsecutiveMatches * 2);
    }
    
    /**
     * Lấy thông tin chi tiết sản phẩm từ URL
     * 
     * @param productUrl URL của sản phẩm
     * @return JSONObject chứa thông tin sản phẩm
     */
    public JSONObject getProductDetails(String productUrl) {
        if (context == null) {
            System.err.println("Browser context chưa được khởi tạo");
            return null;
        }
        
        Page page = null;
        try {
            page = context.newPage();
            
            // Truy cập trang sản phẩm
            System.out.println("Đang truy cập trang sản phẩm: " + productUrl);
            page.navigate(productUrl);
            
            // Đợi trang load xong
            page.waitForLoadState(LoadState.NETWORKIDLE);
            
            // Tạo đối tượng JSON để lưu thông tin sản phẩm
            JSONObject productInfo = new JSONObject();
            
            // Lấy tên sản phẩm
            String productName = page.querySelector("h1").textContent().trim();
            productInfo.put("name", productName);
            
            // Lấy giá sản phẩm
            String price = "0";
            ElementHandle priceElement = page.querySelector(".box-price-present");
            if (priceElement != null) {
                price = priceElement.textContent().replaceAll("[^0-9]", "").trim();
            }
            productInfo.put("price", price);
            
            // Lấy ảnh sản phẩm sử dụng nhiều cách để đảm bảo lấy được ảnh
            String imageUrl = "";
            
            // Cách 1: Thử lấy từ class "theImg" - ưu tiên cách này
            ElementHandle imageElement = page.querySelector(".theImg");
            if (imageElement != null) {
                imageUrl = imageElement.getAttribute("src");
                System.out.println("Đã lấy ảnh từ selector .theImg: " + imageUrl);
            }
            
            // Cách 2: Thử lấy từ trang chi tiết sản phẩm
            if (imageUrl == null || imageUrl.isEmpty()) {
                ElementHandle altImageElement = page.querySelector(".img-main img");
                if (altImageElement != null) {
                    imageUrl = altImageElement.getAttribute("src");
                    System.out.println("Đã lấy ảnh từ selector .img-main img: " + imageUrl);
                }
            }
            
            // Cách 3: Thử lấy từ các class khác
            if (imageUrl == null || imageUrl.isEmpty()) {
                ElementHandle galleryImageElement = page.querySelector("img.gallery-img");
                if (galleryImageElement != null) {
                    imageUrl = galleryImageElement.getAttribute("src");
                    System.out.println("Đã lấy ảnh từ selector img.gallery-img: " + imageUrl);
                }
            }
            
            // Xử lý URL ảnh để đảm bảo luôn hiển thị được
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // Thêm "https:" vào đầu URL nếu thiếu
                if (imageUrl.startsWith("//")) {
                    imageUrl = "https:" + imageUrl;
                    System.out.println("Đã thêm 'https:' vào URL ảnh: " + imageUrl);
                }
                
                // Loại bỏ phần xử lý ảnh từ TGDD trong URL (nếu có)
                if (imageUrl.contains("/imgt/") || imageUrl.contains("img.tgdd.vn")) {
                    // Tìm URL thực của ảnh sau phần xử lý
                    int cdnIndex = imageUrl.indexOf("https://cdn.tgdd.vn");
                    if (cdnIndex > 0) {
                        imageUrl = imageUrl.substring(cdnIndex);
                        System.out.println("Đã cắt bỏ phần xử lý ảnh, URL mới: " + imageUrl);
                    }
                }
                
                // Đảm bảo URL có đủ phần HTTP cho java.net.URL parse
                if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                    imageUrl = "https:" + imageUrl;
                    System.out.println("Đã thêm protocol HTTPS vào URL ảnh: " + imageUrl);
                }
                
                // Nếu URL chứa ký tự không hợp lệ, encode URL
                if (imageUrl.contains(" ")) {
                    imageUrl = imageUrl.replace(" ", "%20");
                    System.out.println("Đã encode ký tự space trong URL ảnh: " + imageUrl);
                }
            }
            
            // Đảm bảo luôn có URL ảnh, sử dụng ảnh dự phòng nếu cần
            if (imageUrl == null || imageUrl.isEmpty()) {
                // Tạo URL ảnh từ ID sản phẩm nếu biết
                Pattern patternID = Pattern.compile("/(p|pid)/(\\d+)");
                Matcher matcherID = patternID.matcher(productUrl);
                if (matcherID.find()) {
                    String productID = matcherID.group(2);
                    // Tạo URL ảnh dựa trên ID sản phẩm và đường dẫn có trong URL
                    if (productUrl.contains("/may-giat/")) {
                        imageUrl = "https://cdn.tgdd.vn/Products/Images/1944/" + productID + "/" + productID + "-thumbnail.jpg";
                        System.out.println("Đã tạo URL ảnh dự phòng từ ID: " + imageUrl);
                    }
                }
            }
            
            productInfo.put("image", imageUrl);
            
            // Lấy thông tin mô tả ngắn
            StringBuilder description = new StringBuilder();
            ElementHandle descriptionList = page.querySelector(".parameter");
            if (descriptionList != null) {
                for (ElementHandle li : descriptionList.querySelectorAll("li")) {
                    description.append("• ").append(li.textContent().trim()).append("\n");
                }
            }
            productInfo.put("description", description.toString());
            
            // Lấy URL review (đảm bảo đúng url là dienmayxanh.com, không phải diemmayxanh.com)
            String correctedUrl = productUrl;
            if (productUrl != null && productUrl.contains("diemmayxanh.com")) {
                correctedUrl = productUrl.replace("diemmayxanh.com", "dienmayxanh.com");
                System.out.println("Đã sửa URL từ " + productUrl + " thành " + correctedUrl);
            }
            productInfo.put("url", correctedUrl);
            
            // Trích xuất ID sản phẩm từ URL nếu có
            Pattern pattern = Pattern.compile("/(p|pid)/(\\d+)");
            Matcher matcher = pattern.matcher(productUrl);
            if (matcher.find()) {
                productInfo.put("id", matcher.group(2));
            } else {
                // Nếu không tìm thấy ID theo pattern, dùng toàn bộ URL làm ID
                productInfo.put("id", productUrl.hashCode());
            }
            
            return productInfo;
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy thông tin sản phẩm từ Điện Máy Xanh: " + e.getMessage());
            return null;
        } finally {
            if (page != null) {
                page.close();
            }
        }
    }
    
    /**
     * Lấy danh sách đánh giá cho một sản phẩm - Cập nhật dựa trên phân tích HTML thực tế
     * 
     * @param productUrl URL của sản phẩm
     * @param page Số trang cần lấy (bắt đầu từ 1)
     * @return JSONArray chứa danh sách đánh giá
     */
    public JSONArray getProductReviews(String productUrl, int page) {
        if (context == null) {
            System.err.println("Browser context chưa được khởi tạo");
            return new JSONArray();
        }
        
        Page browserPage = null;
        try {
            browserPage = context.newPage();
            
            // Xây dựng URL đánh giá giống hệt Test.java
            String reviewUrl;
            // Đảm bảo URL không chứa "diemmayxanh.com"
            String correctedProductUrl = productUrl;
            if (productUrl != null && productUrl.contains("diemmayxanh.com")) {
                correctedProductUrl = productUrl.replace("diemmayxanh.com", "dienmayxanh.com");
                System.out.println("Đã sửa URL từ " + productUrl + " thành " + correctedProductUrl);
            }
            
            if (correctedProductUrl.startsWith("http")) {
                // Lấy phần path từ URL đầy đủ
                String path = correctedProductUrl.replaceFirst("https?://[^/]+", "");
                reviewUrl = "https://www.dienmayxanh.com" + path + "/danh-gia?page=" + page;
            } else {
                // Nếu là đường dẫn tương đối
                reviewUrl = "https://www.dienmayxanh.com" + correctedProductUrl + "/danh-gia?page=" + page;
            }
            
            System.out.println("Đang truy cập trang đánh giá: " + reviewUrl);
            browserPage.navigate(reviewUrl);
            
            // Đợi trang load xong
            browserPage.waitForLoadState(LoadState.NETWORKIDLE);
            
            // Tạo mảng JSON chứa đánh giá
            JSONArray reviewsArray = new JSONArray();
            
            // Lưu HTML trang đánh giá để debug nếu cần
            try {
                String pageContent = browserPage.content();
                FileWriter htmlWriter = new FileWriter("dmx_review_html.html");
                htmlWriter.write(pageContent);
                htmlWriter.close();
                System.out.println("Đã lưu HTML trang đánh giá vào dmx_review_html.html");
            } catch (IOException e) {
                System.out.println("Không thể lưu HTML trang đánh giá: " + e.getMessage());
            }
            
            // Thử với nhiều selector khác nhau để phù hợp với cấu trúc trang khác nhau
            
            // Cách 1: Lấy tất cả các khối đánh giá bằng class cũ
            Locator reviewBlocks = browserPage.locator(".rate-view .crep");
            int count = reviewBlocks.count();
            
            /*
            // Cách 2: Nếu không tìm thấy với selector cũ, thử selector khác
            if (count == 0) {
                reviewBlocks = browserPage.locator(".comment-item");
                count = reviewBlocks.count();
                System.out.println("Thử lại với selector '.comment-item': Tìm thấy " + count + " đánh giá");
            }
            
            // Cách 3: Thử selector đơn giản hơn
            if (count == 0) {
                reviewBlocks = browserPage.locator(".rate-view li");
                count = reviewBlocks.count();
                System.out.println("Thử lại với selector '.rate-view li': Tìm thấy " + count + " đánh giá");
            }
            
            // Cách 4: Thử lấy tất cả các review từ trang
            if (count == 0) {
                reviewBlocks = browserPage.locator("li.comment"); 
                count = reviewBlocks.count();
                System.out.println("Thử lại với selector 'li.comment': Tìm thấy " + count + " đánh giá");
            }
            */
            
            // Vẫn không tìm thấy đánh giá, thử lấy nội dung đánh giá trực tiếp
            if (count == 0) {
                // Lấy trực tiếp nội dung đánh giá
                Locator directComments = browserPage.locator(".cmt-txt");
                count = directComments.count();
                System.out.println("Thử lấy trực tiếp nội dung với selector '.cmt-txt': Tìm thấy " + count + " đánh giá");
                
                // Nếu tìm thấy nội dung đánh giá, xử lý chúng trực tiếp
                if (count > 0) {
                    List<String> contents = directComments.allTextContents();
                    List<String> names = browserPage.locator(".cmt-top-name").allTextContents();
                    
                    for (int i = 0; i < contents.size(); i++) {
                        JSONObject review = new JSONObject();
                        String reviewerName = (i < names.size()) ? names.get(i) : "Người dùng ẩn danh";
                        review.put("reviewer_name", reviewerName);
                        review.put("content", contents.get(i));
                        review.put("rating", 5); // Mặc định là 5 sao
                        review.put("time", ""); // Không có thông tin thời gian
                        review.put("images", new JSONArray()); // Không có ảnh
                        
                        reviewsArray.put(review);
                    }
                    return reviewsArray;
                }
            }
            
            if (count == 0) {
                System.out.println("Không tìm thấy đánh giá nào ở trang " + page + " với tất cả các selector thử nghiệm");
                // Kiểm tra nếu có thông báo "Chưa có đánh giá nào cho sản phẩm"
                boolean noReviewMessage = browserPage.locator("div:has-text('Chưa có đánh giá nào cho sản phẩm')").count() > 0;
                if (noReviewMessage) {
                    System.out.println("Trang hiển thị thông báo: Chưa có đánh giá nào cho sản phẩm");
                }
                return new JSONArray();
            }
            
            System.out.println("Đã tìm thấy " + count + " khối đánh giá ở trang " + page);
            
            // Lặp qua từng khối đánh giá để lấy thông tin chi tiết
            for (int i = 0; i < count; i++) {
                JSONObject review = new JSONObject();
                Locator reviewBlock = reviewBlocks.nth(i);
                
                // Lấy tên người đánh giá
                String reviewerName = "Người dùng ẩn danh";
                Locator nameLocator = reviewBlock.locator(".cmt-top-name").first();
                if (nameLocator.count() > 0) {
                    reviewerName = nameLocator.textContent().trim();
                }
                review.put("reviewer_name", reviewerName);
                
                // Lấy nội dung đánh giá
                String content = "";
                Locator contentLocator = reviewBlock.locator(".cmt-txt").first();
                if (contentLocator.count() > 0) {
                    content = contentLocator.textContent().trim();
                }
                review.put("content", content);
                
                // Lấy điểm đánh giá (số sao) - Cập nhật để lấy chính xác điểm đánh giá
                int rating = 5; // Mặc định 5 sao
                
                // Lấy điểm số thực từ class point-average-score
                Locator ratingLocator = reviewBlock.locator(".point-average-score").first();
                if (ratingLocator.count() > 0) {
                    try {
                        String ratingText = ratingLocator.textContent().trim();
                        rating = (int) Math.round(Double.parseDouble(ratingText));
                        System.out.println("Đã lấy được rating: " + rating + " (từ giá trị: " + ratingText + ")");
                    } catch (NumberFormatException e) {
                        System.err.println("Không thể chuyển đổi rating thành số: " + e.getMessage());
                    }
                }
                review.put("rating", rating);
                
                // Lấy thời gian đánh giá
                String time = "";
                Locator timeLocator = reviewBlock.locator(".cmt-time").first();
                if (timeLocator.count() > 0) {
                    time = timeLocator.textContent().trim();
                }
                review.put("time", time);
                
                // Lấy ảnh người dùng đăng kèm theo đánh giá (class it-img)
                JSONArray images = new JSONArray();
                Locator imgContainers = reviewBlock.locator(".it-img");
                for (int j = 0; j < imgContainers.count(); j++) {
                    Locator imgLocator = imgContainers.nth(j).locator("img").first();
                    if (imgLocator.count() > 0) {
                        String imgSrc = imgLocator.getAttribute("src");
                        if (imgSrc != null && !imgSrc.isEmpty()) {
                            // Đảm bảo URL đầy đủ
                            if (imgSrc.startsWith("//")) {
                                imgSrc = "https:" + imgSrc;
                                System.out.println("Đã thêm 'https:' vào URL ảnh người dùng: " + imgSrc);
                            }
                            images.put(imgSrc);
                        }
                    }
                }
                review.put("images", images);
                
                // Thêm vào mảng kết quả
                reviewsArray.put(review);
            }
            
            return reviewsArray;
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy đánh giá sản phẩm từ Điện Máy Xanh: " + e.getMessage());
            e.printStackTrace();
            return new JSONArray();
        } finally {
            if (browserPage != null) {
                browserPage.close();
            }
        }
    }
    
    /**
     * Lấy tổng số trang đánh giá cho một sản phẩm - cập nhật từ cấu trúc HTML thực tế
     * 
     * @param productUrl URL của sản phẩm
     * @return Tổng số trang đánh giá
     */
    public int getTotalReviewPages(String productUrl) {
        if (context == null) {
            System.err.println("Browser context chưa được khởi tạo");
            return 1;
        }
        
        Page page = null;
        try {
            page = context.newPage();
            
            // Xây dựng URL trang đánh giá
            String reviewUrl;
            // Đảm bảo URL không chứa "diemmayxanh.com"
            String correctedProductUrl = productUrl;
            if (productUrl != null && productUrl.contains("diemmayxanh.com")) {
                correctedProductUrl = productUrl.replace("diemmayxanh.com", "dienmayxanh.com");
                System.out.println("Đã sửa URL từ " + productUrl + " thành " + correctedProductUrl);
            }
            
            if (correctedProductUrl.startsWith("http")) {
                // Lấy phần path từ URL đầy đủ
                String path = correctedProductUrl.replaceFirst("https?://[^/]+", "");
                reviewUrl = "https://www.dienmayxanh.com" + path + "/danh-gia";
            } else {
                // Nếu là đường dẫn tương đối
                reviewUrl = "https://www.dienmayxanh.com" + correctedProductUrl + "/danh-gia";
            }
            
            System.out.println("Đang truy cập trang đánh giá để đếm số trang: " + reviewUrl);
            page.navigate(reviewUrl);
            
            // Đợi trang load xong
            page.waitForLoadState(LoadState.NETWORKIDLE);
            
            int totalPages = 1; // Mặc định là 1 trang
            int totalReviews = 0; // Tổng số đánh giá
            double avgRating = 0.0; // Điểm đánh giá trung bình
            
            // Lưu HTML để debug
            String html = page.content();
            try (FileWriter writer = new FileWriter("dmx_review_html.html")) {
                writer.write(html);
                System.out.println("Đã lưu HTML trang đánh giá vào dmx_review_html.html");
            } catch (IOException e) {
                System.err.println("Lỗi khi lưu HTML: " + e.getMessage());
            }
            
            // Lấy thông tin đánh giá trung bình từ HTML
            try {
                // Phương pháp 1: Lấy từ ".point-average-score" - điểm đánh giá
                Locator avgRatingElement = page.locator(".point-average-score");
                if (avgRatingElement.count() > 0) {
                    String avgRatingText = avgRatingElement.first().textContent().trim();
                    avgRating = Double.parseDouble(avgRatingText.replace(",", "."));
                    System.out.println("Điểm đánh giá trung bình: " + avgRating);
                }
                
                // Phương pháp 2: Lấy từ ".point-alltimerate" - tổng số lượng đánh giá
                Locator totalRateElement = page.locator(".point-alltimerate");
                if (totalRateElement.count() > 0) {
                    String totalRateText = totalRateElement.first().textContent().trim()
                        .replaceAll("[^0-9]", ""); // Loại bỏ các ký tự không phải số
                    totalReviews = Integer.parseInt(totalRateText);
                    System.out.println("Số lượng đánh giá: " + totalReviews);
                    
                    // Tính toán số trang dựa trên tổng số đánh giá
                    // Mỗi trang hiển thị khoảng 20 đánh giá
                    totalPages = (int) Math.ceil(totalReviews / 20.0);
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi lấy thông tin điểm đánh giá: " + e.getMessage());
            }
            
            // Phương pháp 3: Lấy tổng số trang từ phân trang
            try {
                Locator paginationLinks = page.locator(".pagcomment a");
                if (paginationLinks.count() > 0) {
                    // Lấy số trang từ phần tử cuối cùng (thường là trang cuối)
                    String lastPageText = paginationLinks.last().textContent().trim();
                    try {
                        int lastPage = Integer.parseInt(lastPageText);
                        if (lastPage > totalPages) {
                            totalPages = lastPage;
                            System.out.println("Cập nhật số trang từ phân trang: " + totalPages);
                        }
                    } catch (NumberFormatException e) {
                        // Nếu không phải số, thử tìm số trang từ attribute
                        String lastPageHref = paginationLinks.last().getAttribute("href");
                        if (lastPageHref != null) {
                            Pattern pattern = Pattern.compile("page=(\\d+)");
                            Matcher matcher = pattern.matcher(lastPageHref);
                            if (matcher.find()) {
                                int lastPage = Integer.parseInt(matcher.group(1));
                                if (lastPage > totalPages) {
                                    totalPages = lastPage;
                                    System.out.println("Cập nhật số trang từ href: " + totalPages);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi lấy số trang từ phân trang: " + e.getMessage());
            }
            
            // Phương pháp 4: Thử lấy từ "Xem X đánh giá" nếu phương pháp khác thất bại
            if (totalPages <= 1 && totalReviews == 0) {
                try {
                    Locator totalReviewsElement = page.locator(".c-btn-rate.btn-view-all");
                    if (totalReviewsElement.count() > 0) {
                        String totalReviewsText = totalReviewsElement.first().textContent().trim();
                        Pattern pattern = Pattern.compile("Xem (\\d+) đánh giá");
                        Matcher matcher = pattern.matcher(totalReviewsText);
                        if (matcher.find()) {
                            totalReviews = Integer.parseInt(matcher.group(1));
                            // Ước tính số trang, mỗi trang hiển thị ~20 đánh giá
                            totalPages = (int) Math.ceil(totalReviews / 20.0);
                            System.out.println("Cập nhật số trang từ 'Xem X đánh giá': " + totalPages);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi khi lấy số trang từ 'Xem X đánh giá': " + e.getMessage());
                }
            }
            
            // Phương pháp 5: Giống Test.java, lấy trực tiếp số lượng đánh giá trên trang đầu tiên
            if (totalPages <= 1) {
                try {
                    Locator reviewItems = page.locator(".cmt-txt");
                    int reviewsPerPage = reviewItems.count();
                    System.out.println("Số lượng đánh giá trên trang đầu tiên: " + reviewsPerPage);
                    
                    if (reviewsPerPage > 0 && totalReviews > 0) {
                        // Có đánh giá trên trang đầu tiên và biết tổng số đánh giá
                        totalPages = (int) Math.ceil(totalReviews / (double)reviewsPerPage);
                        System.out.println("Ước tính số trang từ số lượng đánh giá/trang: " + totalPages);
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi khi đếm đánh giá trên trang: " + e.getMessage());
                }
            }
            
            // Phương pháp 6: Test thử các trang tiếp theo nếu vẫn không xác định được tổng trang
            if (totalPages <= 1 && totalReviews > 20) {
                // Nếu biết có nhiều hơn 20 đánh giá (1 trang) nhưng chưa xác định được số trang
                // Thử kiểm tra trang thứ 2, 3, 4, 5 xem có đánh giá không
                try {
                    // Chỉ kiểm tra tối đa 5 trang để tránh tốn thời gian
                    for (int testPage = 2; testPage <= 5; testPage++) {
                        String testUrl = reviewUrl + "?page=" + testPage;
                        Page testPageObj = context.newPage();
                        try {
                            testPageObj.navigate(testUrl);
                            testPageObj.waitForLoadState(LoadState.NETWORKIDLE);
                            
                            Locator reviewItems = testPageObj.locator(".cmt-txt");
                            int count = reviewItems.count();
                            System.out.println("Kiểm tra trang " + testPage + ": " + count + " đánh giá");
                            
                            if (count == 0) {
                                // Nếu không có đánh giá thì trang trước là trang cuối
                                totalPages = testPage - 1;
                                System.out.println("Phát hiện trang cuối: " + totalPages);
                                break;
                            } else if (testPage > totalPages) {
                                totalPages = testPage;
                            }
                        } finally {
                            testPageObj.close();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi khi kiểm tra các trang tiếp theo: " + e.getMessage());
                }
            }
            
            // Lưu thông tin đánh giá trung bình và tổng số đánh giá để trả về cho client
            this.productRatingAverage = avgRating;
            this.productRatingCount = totalReviews;
            
            // Đảm bảo có ít nhất 1 trang
            if (totalPages < 1) totalPages = 1;
            
            System.out.println("Tổng số trang đánh giá: " + totalPages);
            return totalPages;
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy tổng số trang đánh giá: " + e.getMessage());
            e.printStackTrace();
            return 1; // Mặc định là 1 trang nếu có lỗi
        } finally {
            if (page != null) {
                page.close();
            }
        }
    }
    
    /**
     * Tìm kiếm sản phẩm gợi ý khi không tìm thấy sản phẩm chính xác
     * 
     * @param productName Tên sản phẩm cần tìm
     * @return Map chứa các sản phẩm gợi ý (tên sản phẩm -> URL)
     */
    public Map<String, String> getSuggestedProducts(String productName) {
        if (context == null) {
            System.err.println("Browser context chưa được khởi tạo");
            return Collections.emptyMap();
        }
        
        Page page = null;
        try {
            page = context.newPage();
            
            // Truy cập trang tìm kiếm của Điện Máy Xanh (giống như Test.java)
            String searchUrl = "https://www.dienmayxanh.com/search?key=" + productName.replaceAll(" ", "+");
            page.navigate(searchUrl);
            
            // Đợi trang load xong
            page.waitForLoadState(LoadState.NETWORKIDLE);
            
            // Lấy danh sách sản phẩm (giống Test.java)
            Locator products = page.locator(".item a.main-contain");
            Map<String, String> suggestions = new HashMap<>();
            
            if (products.count() > 0) {
                // Lấy danh sách các link sản phẩm
                Object evalResult = products.evaluateAll("list => list.map(el => el.getAttribute('href'))");
                
                List<String> productLinks = new ArrayList<>();
                
                if (evalResult instanceof List<?>) {
                    for (Object obj : (List<?>) evalResult) {
                        if (obj instanceof String href && !href.trim().isEmpty()) {
                            productLinks.add(href.trim());
                        }
                    }
                }
                
                // Lấy tối đa 5 sản phẩm
                int limit = Math.min(5, productLinks.size());
                for (int i = 0; i < limit; i++) {
                    String link = productLinks.get(i);
                    String name = extractProductNameFromLink(link);
                    
                    if (!link.startsWith("http")) {
                        link = "https://www.dienmayxanh.com" + link;
                    }
                    
                    suggestions.put(name, link);
                }
            }
            
            return suggestions;
        } catch (Exception e) {
            System.err.println("Lỗi khi tìm kiếm sản phẩm gợi ý trên Điện Máy Xanh: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyMap();
        } finally {
            if (page != null) {
                page.close();
            }
        }
    }
    
    /**
     * Phương thức chính để lấy thông tin sản phẩm và đánh giá từ Điện Máy Xanh
     * 
     * @param productName Tên sản phẩm cần tìm
     * @return Chuỗi JSON chứa thông tin sản phẩm và đánh giá trang đầu tiên
     */
    public String getProductReviewsFirstPage(String productName) {
        try {
            // Tìm URL sản phẩm
            String productUrl = findProductUrl(productName);
            
            if (productUrl == null) {
                // Nếu không tìm thấy sản phẩm, trả về các gợi ý
                Map<String, String> suggestions = getSuggestedProducts(productName);
                if (suggestions.isEmpty()) {
                    return "Không tìm thấy sản phẩm nào cho từ khóa: " + productName;
                }
                
                // Xây dựng phản hồi với các gợi ý
                JSONObject response = new JSONObject();
                response.put("status", "suggestions");
                
                JSONArray suggestionsArray = new JSONArray();
                for (Map.Entry<String, String> entry : suggestions.entrySet()) {
                    JSONObject suggestion = new JSONObject();
                    suggestion.put("name", entry.getKey());
                    suggestion.put("url", entry.getValue());
                    suggestionsArray.put(suggestion);
                }
                
                response.put("suggestions", suggestionsArray);
                return response.toString();
            }
            
            // Lấy thông tin chi tiết sản phẩm
            JSONObject productDetails = getProductDetails(productUrl);
            if (productDetails == null) {
                return "Lỗi khi lấy thông tin sản phẩm.";
            }
            
            // Khởi tạo đối tượng lấy đánh giá đầy đủ
            getFullReviewsUtil fullReviewsUtil = new getFullReviewsUtil(this);
            
            // Lấy tất cả đánh giá
            String allReviewsJson = fullReviewsUtil.getAllReviewsForProduct(productUrl);
            
            // Nếu muốn vẫn giữ cấu trúc phản hồi cũ (chỉ trả về trang đầu tiên)
            // bạn có thể dùng đoạn code sau đây:
            
            // Lấy đánh giá trang đầu tiên
            JSONArray reviews = getProductReviews(productUrl, 1);
            
            // Lấy tổng số trang đánh giá
            int totalPages = getTotalReviewPages(productUrl);
            
            // Lấy thông tin rating trung bình và số lượng đánh giá từ class point-average-score và point-alltimerate
            Page page = context.newPage();
            JSONObject ratingInfo = new JSONObject();
            try {
                String reviewUrl;
                if (productUrl.startsWith("http")) {
                    String path = productUrl.replaceFirst("https?://[^/]+", "");
                    reviewUrl = "https://www.dienmayxanh.com" + path;
                } else {
                    reviewUrl = "https://www.dienmayxanh.com" + productUrl;
                }
                
                page.navigate(reviewUrl);
                page.waitForLoadState();
                
                // Lấy điểm đánh giá trung bình từ point-average-score
                double averageRating = 0;
                try {
                    String avgRatingText = page.locator(".point-average-score").first().textContent().trim();
                    averageRating = Double.parseDouble(avgRatingText);
                    System.out.println("Điểm đánh giá trung bình: " + averageRating);
                } catch (Exception e) {
                    System.out.println("Không lấy được điểm đánh giá trung bình: " + e.getMessage());
                }
                
                // Lấy số lượng đánh giá từ point-alltimerate
                int reviewCount = 0;
                try {
                    String countText = page.locator(".point-alltimerate").first().textContent().trim();
                    // Trích xuất số từ chuỗi vd: "123 đánh giá" → 123
                    countText = countText.replaceAll("[^0-9]", "");
                    reviewCount = Integer.parseInt(countText);
                    System.out.println("Số lượng đánh giá: " + reviewCount);
                } catch (Exception e) {
                    System.out.println("Không lấy được số lượng đánh giá: " + e.getMessage());
                }
                
                ratingInfo.put("average_rating", averageRating);
                ratingInfo.put("review_count", reviewCount);
                
            } catch (Exception e) {
                System.err.println("Lỗi khi lấy thông tin rating: " + e.getMessage());
            } finally {
                if (page != null) {
                    page.close();
                }
            }
            
            // Cập nhật thông tin rating từ biến instance
            // Ưu tiên dùng giá trị từ ratingInfo nếu có, nếu không dùng giá trị đã lưu
            if (ratingInfo.has("average_rating") && ratingInfo.getDouble("average_rating") > 0) {
                productRatingAverage = ratingInfo.getDouble("average_rating");
            }
            
            if (ratingInfo.has("review_count") && ratingInfo.getInt("review_count") > 0) {
                productRatingCount = ratingInfo.getInt("review_count");
            }
            
            // Xây dựng phản hồi
            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("product", productDetails);
            response.put("reviews", reviews);
            response.put("total_pages", totalPages);
            response.put("current_page", 1);
            
            // Cập nhật lại ratingInfo với dữ liệu chính xác nhất
            ratingInfo.put("average_rating", productRatingAverage);
            ratingInfo.put("review_count", productRatingCount);
            response.put("rating_info", ratingInfo);
            
            return response.toString();
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy thông tin sản phẩm và đánh giá: " + e.getMessage());
            e.printStackTrace();
            return "Lỗi: " + e.getMessage();
        }
    }
    
    /**
     * Phương thức lấy tất cả đánh giá của một sản phẩm
     * 
     * @param productName Tên sản phẩm cần tìm
     * @return Chuỗi JSON chứa tất cả đánh giá
     */
    public String getAllProductReviews(String productName) {
        try {
            // Tìm URL sản phẩm
            String productUrl = findProductUrl(productName);
            
            if (productUrl == null) {
                // Nếu không tìm thấy sản phẩm, trả về các gợi ý
                Map<String, String> suggestions = getSuggestedProducts(productName);
                if (suggestions.isEmpty()) {
                    return "Không tìm thấy sản phẩm nào cho từ khóa: " + productName;
                }
                
                // Xây dựng phản hồi với các gợi ý
                JSONObject response = new JSONObject();
                response.put("status", "suggestions");
                
                JSONArray suggestionsArray = new JSONArray();
                for (Map.Entry<String, String> entry : suggestions.entrySet()) {
                    JSONObject suggestion = new JSONObject();
                    suggestion.put("name", entry.getKey());
                    suggestion.put("url", entry.getValue());
                    suggestionsArray.put(suggestion);
                }
                
                response.put("suggestions", suggestionsArray);
                return response.toString();
            }
            
            // Khởi tạo đối tượng lấy đánh giá đầy đủ
            getFullReviewsUtil fullReviewsUtil = new getFullReviewsUtil(this);
            
            // Lấy tất cả đánh giá
            return fullReviewsUtil.getAllReviewsForProduct(productUrl);
            
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy tất cả đánh giá: " + e.getMessage());
            e.printStackTrace();
            return "Lỗi: " + e.getMessage();
        }
    }
    
    /**
     * Phương thức lấy đánh giá theo trang cụ thể
     * 
     * @param productUrl URL sản phẩm
     * @param page Số trang cần lấy
     * @return Chuỗi JSON chứa đánh giá ở trang được chỉ định
     */
    public String getProductReviewsByPage(String productUrl, int page) {
        try {
            // Lấy đánh giá ở trang được chỉ định
            JSONArray reviews = getProductReviews(productUrl, page);
            
            // Lấy tổng số trang đánh giá
            int totalPages = getTotalReviewPages(productUrl);
            
            // Xây dựng phản hồi
            JSONObject response = new JSONObject();
            response.put("status", "success");
            response.put("reviews", reviews);
            response.put("total_pages", totalPages);
            response.put("current_page", page);
            
            return response.toString();
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy đánh giá trang " + page + ": " + e.getMessage());
            return "Lỗi: " + e.getMessage();
        }
    }
    
    /**
     * Phương thức để lấy đánh giá với phân trang
     * (Tương tự getReviewTIKIProduct)
     * 
     * @param productUrl URL hoặc ID sản phẩm
     * @param initialPage Trang bắt đầu
     * @return Chuỗi JSON chứa thông tin sản phẩm và đánh giá
     */
    public String getProductReviewsWithPagination(String productUrl, int initialPage) {
        try {
            DMXSearchLogger.log("Bắt đầu lấy đánh giá với phân trang - URL: " + productUrl + ", Trang: " + initialPage);
            
            // Kiểm tra xem productUrl có phải là URL đầy đủ hay chỉ là ID/tên sản phẩm
            if (!productUrl.startsWith("http")) {
                // Nếu không phải URL, tìm URL sản phẩm từ tên
                DMXSearchLogger.log("Tìm kiếm URL sản phẩm từ tên: " + productUrl);
                String foundUrl = findProductUrl(productUrl);
                if (foundUrl == null) {
                    // Nếu không tìm thấy, trả về gợi ý
                    DMXSearchLogger.log("Không tìm thấy URL, lấy danh sách gợi ý");
                    Map<String, String> suggestions = getSuggestedProducts(productUrl);
                    if (suggestions.isEmpty()) {
                        DMXSearchLogger.log("Không có gợi ý nào, trả về thông báo lỗi");
                        return "Không tìm thấy sản phẩm nào cho từ khóa: " + productUrl;
                    }
                    
                    // Xây dựng phản hồi với các gợi ý
                    JSONObject response = new JSONObject();
                    response.put("status", "suggestions");
                    
                    JSONArray suggestionsArray = new JSONArray();
                    for (Map.Entry<String, String> entry : suggestions.entrySet()) {
                        JSONObject suggestion = new JSONObject();
                        suggestion.put("name", entry.getKey());
                        suggestion.put("url", entry.getValue());
                        suggestionsArray.put(suggestion);
                    }
                    
                    response.put("suggestions", suggestionsArray);
                    DMXSearchLogger.log("Trả về " + suggestions.size() + " gợi ý");
                    return response.toString();
                }
                
                productUrl = foundUrl;
                DMXSearchLogger.log("Đã tìm thấy URL sản phẩm: " + productUrl);
            }
            
            // Xây dựng kết quả trong định dạng như trong log ketqua.txt
            DMXSearchLogger.log("Bắt đầu xây dựng kết quả theo định dạng từ mẫu");
            StringBuilder result = new StringBuilder();
            
            // Thêm URL sản phẩm để client có thể sử dụng cho các yêu cầu tải thêm
            result.append("PRODUCT_URL:").append(productUrl).append("\n");
            
            // Lấy thông tin chi tiết sản phẩm
            DMXSearchLogger.log("Lấy thông tin chi tiết sản phẩm");
            JSONObject productDetails = getProductDetails(productUrl);
            if (productDetails == null) {
                DMXSearchLogger.log("Không lấy được thông tin sản phẩm");
                return "Lỗi khi lấy thông tin sản phẩm.";
            }
            
            // Thêm thông tin sản phẩm vào kết quả
            result.append("PRODUCT_NAME:").append(productDetails.getString("name")).append("\n");
            result.append("PRODUCT_PRICE:").append(productDetails.getString("price")).append("\n");
            result.append("PRODUCT_IMAGE:").append(productDetails.getString("image")).append("\n");
            result.append("PRODUCT_DESCRIPTION:").append(productDetails.getString("description")).append("\n");
            
            // Lấy đánh giá ở trang được chỉ định
            DMXSearchLogger.log("Lấy đánh giá ở trang " + initialPage);
            JSONArray reviews = getProductReviews(productUrl, initialPage);
            
            // Lấy tổng số trang đánh giá
            DMXSearchLogger.log("Lấy tổng số trang đánh giá");
            int totalPages = getTotalReviewPages(productUrl);
            DMXSearchLogger.log("Tổng số trang: " + totalPages);
            
            // Thêm thông tin phân trang
            result.append("TOTAL_PAGES:").append(totalPages).append("\n");
            result.append("CURRENT_PAGE:").append(initialPage).append("\n");
            
            // Thêm đánh giá vào kết quả
            result.append("REVIEWS_START\n");
            
            DMXSearchLogger.log("Đang thêm " + reviews.length() + " đánh giá vào kết quả");
            for (int i = 0; i < reviews.length(); i++) {
                JSONObject review = reviews.getJSONObject(i);
                
                result.append("REVIEW_START\n");
                result.append("REVIEWER_NAME:").append(review.getString("reviewer_name")).append("\n");
                result.append("REVIEW_CONTENT:").append(review.getString("content")).append("\n");
                result.append("REVIEW_RATING:").append(review.getInt("rating")).append("\n");
                result.append("REVIEW_TIME:").append(review.getString("time")).append("\n");
                
                // Thêm hình ảnh nếu có
                JSONArray images = review.getJSONArray("images");
                if (images.length() > 0) {
                    result.append("REVIEW_IMAGES:");
                    for (int j = 0; j < images.length(); j++) {
                        if (j > 0) {
                            result.append(",");
                        }
                        result.append(images.getString(j));
                    }
                    result.append("\n");
                }
                
                result.append("REVIEW_END\n");
            }
            
            result.append("REVIEWS_END\n");
            
            DMXSearchLogger.log("Đã hoàn thành việc xây dựng kết quả");
            return result.toString();
        } catch (Exception e) {
            DMXSearchLogger.logError("Lỗi khi lấy thông tin sản phẩm và đánh giá", e);
            System.err.println("Lỗi khi lấy thông tin sản phẩm và đánh giá: " + e.getMessage());
            e.printStackTrace();
            return "Lỗi: " + e.getMessage();
        }
    }
    
    /**
     * Đóng các tài nguyên của Playwright khi kết thúc
     * Xử lý tốt hơn các lỗi liên quan đến module không tìm thấy
     */
    public void cleanup() {
        try {
            DMXSearchLogger.log("Bắt đầu dọn dẹp tài nguyên Playwright...");
            
            if (context != null) {
                try {
                    DMXSearchLogger.log("Đóng browser context...");
                    context.close();
                } catch (Exception e) {
                    DMXSearchLogger.log("Bỏ qua lỗi khi đóng context: " + e.getMessage());
                    // Bỏ qua lỗi khi đóng context
                }
                context = null;
            }
            
            if (browser != null) {
                try {
                    DMXSearchLogger.log("Đóng browser...");
                    browser.close();
                } catch (Exception e) {
                    DMXSearchLogger.log("Bỏ qua lỗi khi đóng browser: " + e.getMessage());
                    // Bỏ qua lỗi khi đóng browser
                }
                browser = null;
            }
            
            if (playwright != null) {
                try {
                    DMXSearchLogger.log("Đóng playwright...");
                    playwright.close();
                } catch (Exception e) {
                    DMXSearchLogger.log("Bỏ qua lỗi khi đóng playwright: " + e.getMessage());
                    // Bỏ qua lỗi khi đóng playwright
                }
                playwright = null;
            }
            
            DMXSearchLogger.log("Đã hoàn tất việc dọn dẹp tài nguyên Playwright");
        } catch (Exception e) {
            DMXSearchLogger.logError("Lỗi tổng thể khi đóng Playwright", e);
            System.err.println("Lỗi khi đóng Playwright: " + e.getMessage());
        } finally {
            // Đóng logger
            DMXSearchLogger.close();
        }
    }
    
    // Thêm phương thức finalize để đảm bảo dọn dẹp tài nguyên
    @Override
    protected void finalize() throws Throwable {
        cleanup();
        super.finalize();
    }
    
    /**
     * Trích xuất thông tin đánh giá từ trang sản phẩm điện máy xanh
     * 
     * @param productUrl URL của trang sản phẩm
     * @return Chuỗi có định dạng "average_rating:X.X|total_reviews:Y"
     */
    public String getProductRatingInfo(String productUrl) {
        try {
            DMXSearchLogger.log("Bắt đầu lấy thông tin đánh giá từ URL: " + productUrl);
            
            // Tạo trang mới
            Page page = context.newPage();
            page.setDefaultTimeout(30000); // 30 giây timeout
            
            try {
                // Truy cập trang sản phẩm
                DMXSearchLogger.log("Truy cập trang: " + productUrl);
                page.navigate(productUrl);
                
                // Đợi cho trang tải xong
                page.waitForLoadState(LoadState.DOMCONTENTLOADED);
                
                // Lấy điểm đánh giá trung bình sử dụng CSS selector chính xác
                String avgRatingSelector = ".point-average-score";
                String ratingCountSelector = ".point-alltimerate";
                
                DMXSearchLogger.log("Đang tìm kiếm element đánh giá với selector: " + avgRatingSelector);
                
                // Thử lấy điểm đánh giá trung bình
                double avgRating = 0.0;
                try {
                    // Đợi để điểm đánh giá hiển thị
                    page.waitForSelector(avgRatingSelector, new Page.WaitForSelectorOptions().setTimeout(5000));
                    
                    // Lấy điểm đánh giá
                    String avgRatingText = page.textContent(avgRatingSelector);
                    DMXSearchLogger.log("Đã tìm thấy điểm đánh giá: " + avgRatingText);
                    
                    // Chuyển đổi thành số
                    if (avgRatingText != null && !avgRatingText.trim().isEmpty()) {
                        avgRatingText = avgRatingText.trim().replace(",", ".");
                        try {
                            avgRating = Double.parseDouble(avgRatingText);
                            DMXSearchLogger.log("Điểm đánh giá sau khi chuyển đổi: " + avgRating);
                        } catch (NumberFormatException e) {
                            DMXSearchLogger.log("Lỗi khi chuyển đổi điểm đánh giá: " + e.getMessage());
                        }
                    }
                } catch (TimeoutError e) {
                    DMXSearchLogger.log("Không tìm thấy element điểm đánh giá: " + e.getMessage());
                }
                
                // Thử lấy số lượng đánh giá
                int reviewCount = 0;
                try {
                    // Đợi để tìm số lượng đánh giá
                    page.waitForSelector(ratingCountSelector, new Page.WaitForSelectorOptions().setTimeout(5000));
                    
                    // Lấy số lượng đánh giá
                    String reviewCountText = page.textContent(ratingCountSelector);
                    DMXSearchLogger.log("Đã tìm thấy số lượng đánh giá: " + reviewCountText);
                    
                    // Chuyển đổi thành số
                    if (reviewCountText != null && !reviewCountText.trim().isEmpty()) {
                        // Loại bỏ các ký tự không phải số
                        reviewCountText = reviewCountText.replaceAll("[^0-9]", "").trim();
                        try {
                            reviewCount = Integer.parseInt(reviewCountText);
                            DMXSearchLogger.log("Số lượng đánh giá sau khi chuyển đổi: " + reviewCount);
                        } catch (NumberFormatException e) {
                            DMXSearchLogger.log("Lỗi khi chuyển đổi số lượng đánh giá: " + e.getMessage());
                        }
                    }
                } catch (TimeoutError e) {
                    DMXSearchLogger.log("Không tìm thấy element số lượng đánh giá: " + e.getMessage());
                }
                
                // Thử cách khác nếu không tìm thấy thông tin đánh giá
                if (avgRating == 0.0 || reviewCount == 0) {
                    DMXSearchLogger.log("Thử tìm kiếm thông tin đánh giá với cách khác");
                    
                    // Thử lấy thông tin từ đánh giá hiển thị ở phần khác
                    String altSelector = ".rating";
                    try {
                        page.waitForSelector(altSelector, new Page.WaitForSelectorOptions().setTimeout(5000));
                        String ratingText = page.textContent(altSelector);
                        DMXSearchLogger.log("Tìm thấy rating text: " + ratingText);
                        
                        // Trích xuất điểm đánh giá và số lượng từ chuỗi
                        if (ratingText != null && !ratingText.trim().isEmpty()) {
                            // Tìm số trong dạng "X.X/5"
                            Pattern pattern = Pattern.compile("([0-9]+(\\.[0-9]+)?)/5");
                            Matcher matcher = pattern.matcher(ratingText);
                            if (matcher.find()) {
                                String extractedRating = matcher.group(1);
                                DMXSearchLogger.log("Trích xuất điểm đánh giá: " + extractedRating);
                                avgRating = Double.parseDouble(extractedRating);
                            }
                            
                            // Tìm số lượng đánh giá dạng "(123 đánh giá)"
                            pattern = Pattern.compile("\\(([0-9]+)[^0-9]*\\)");
                            matcher = pattern.matcher(ratingText);
                            if (matcher.find()) {
                                String extractedCount = matcher.group(1);
                                DMXSearchLogger.log("Trích xuất số lượng đánh giá: " + extractedCount);
                                reviewCount = Integer.parseInt(extractedCount);
                            }
                        }
                    } catch (Exception e) {
                        DMXSearchLogger.log("Lỗi khi tìm kiếm với selector thay thế: " + e.getMessage());
                    }
                }
                
                // Trả về kết quả
                String result = "average_rating:" + avgRating + "|total_reviews:" + reviewCount;
                DMXSearchLogger.log("Trả về kết quả: " + result);
                return result;
                
            } finally {
                // Đóng trang web
                if (page != null) {
                    page.close();
                    DMXSearchLogger.log("Đã đóng trang");
                }
            }
        } catch (Exception e) {
            DMXSearchLogger.log("Lỗi khi lấy thông tin đánh giá: " + e.getMessage());
            return "average_rating:0.0|total_reviews:0";
        }
    }
}
