package Test;

import com.microsoft.playwright.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Test {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Nhập từ khóa sản phẩm bạn muốn tìm: ");
        String keyword = scanner.nextLine().trim();

        if (!keyword.isEmpty()) {
            searchProductAndExtractReview(keyword);
        } else {
            System.out.println("Từ khóa không hợp lệ.");
        }
    }

    private static void searchProductAndExtractReview(String keyword){
        String url = "https://www.dienmayxanh.com/search?key=" + keyword;
        List<String> productLinks = new ArrayList<>();
        List<String> productNames = new ArrayList<>();  // Lưu tên sản phẩm

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.webkit().launch();
            Page page = browser.newPage();
            page.navigate(url);
            System.out.println("Đã tải trang tìm kiếm...");

            Locator products = page.locator(".item a.main-contain");

            if (products.count() > 0) {
                products.first().waitFor(new Locator.WaitForOptions().setTimeout(5000));
                Object evalResult = products.evaluateAll("list => list.map(el => el.getAttribute('href'))");

                if (evalResult instanceof List<?>) {
                    for (Object obj : (List<?>) evalResult) {
                        if (obj instanceof String href && !href.trim().isEmpty()) {
                            productLinks.add(href.trim());
                        }
                    }
                }

                System.out.println("Tìm thấy " + productLinks.size() + " sản phẩm:");
                // Lấy tên sản phẩm gần đúng
                for (String link : productLinks) {
                    String productName = extractProductNameFromLink(link);
                    productNames.add(productName);
                    System.out.println("- " + productName);  // Hiển thị tên sản phẩm
                }

                // Kiểm tra nếu người dùng nhập đúng tên sản phẩm
                suggestProductName(keyword, productNames, productLinks);

            } else {
                System.out.println("Không tìm thấy sản phẩm.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Trích xuất tên sản phẩm từ link
    private static String extractProductNameFromLink(String link) {
        // Giả sử bạn lấy tên sản phẩm từ link
        return link.substring(link.lastIndexOf("/") + 1).replaceAll("-", " ");
    }

    // Hàm gợi ý tên sản phẩm gần đúng
    private static void suggestProductName(String userInput, List<String> productNames, List<String> productLinks) {
        String bestMatch = null;
        int maxScore = 0;
        String bestMatchLink = null;

        // So sánh tên sản phẩm người dùng nhập với tên sản phẩm lấy được từ các link
        for (int i = 0; i < productNames.size(); i++) {
            String productName = productNames.get(i);
            int score = getMatchScore(userInput, productName);
            if (score > maxScore) {
                maxScore = score;
                bestMatch = productName;
                bestMatchLink = productLinks.get(i);  // Lưu lại link sản phẩm
            }
        }

        if (bestMatch != null && maxScore > 0) {
            System.out.println("Tên sản phẩm gần đúng với bạn là: " + bestMatch);
            // Nếu tìm thấy tên sản phẩm gần đúng, gọi hàm lấy review cho sản phẩm đó
            getReviewsFromProduct(bestMatchLink);
        } else {
            System.out.println("Không tìm thấy tên sản phẩm gần đúng.");
        }
    }

    // Tính điểm tương đồng giữa tên người dùng nhập và tên sản phẩm
    private static int getMatchScore(String userInput, String suggestion) {
        // Sử dụng phương pháp đơn giản để tính độ dài của sự trùng khớp
        userInput = userInput.toLowerCase();
        suggestion = suggestion.toLowerCase();

        // Tính số ký tự trùng khớp
        int matchCount = 0;
        for (int i = 0; i < Math.min(userInput.length(), suggestion.length()); i++) {
            if (userInput.charAt(i) == suggestion.charAt(i)) {
                matchCount++;
            }
        }

        return matchCount;
    }

    // Lấy review theo trang
    // Lấy tất cả review từ nhiều trang
    private static void getReviewsFromProduct(String productPath){
        String baseUrl = "https://www.dienmayxanh.com" + productPath + "/danh-gia";
        int pageNo = 1;
        int totalReviewCount = 0;

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.webkit().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();

            while (true) {
                String reviewUrl = baseUrl + "?page=" + pageNo;
                page.navigate(reviewUrl);
                System.out.println("Trang đánh giá #" + pageNo);

                Locator reviews = page.locator(".cmt-txt");
                List<String> reviewList = reviews.allTextContents();

                if (reviewList.isEmpty()) {
                    if (pageNo == 1) {
                        System.out.println("Chưa có đánh giá cho sản phẩm này.");
                    } else {
                        System.out.println("Đã lấy hết tất cả đánh giá (" + totalReviewCount + " đánh giá).");
                    }
                    break;
                }

                for (String review : reviewList) {
                    System.out.println(" - " + review);
                    totalReviewCount++;
                }

                pageNo++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
