package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.service.ai.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@SpringBootTest
public class AiServiceTest {

    @Autowired
    private AiService aiService;

    @Test
    void testGroqAiConnection() {
        // Kiểm tra xem đã nạp được API key chưa
        assertTrue(aiService.isAvailable(), "Chưa cấu hình Groq API Key trong application.properties!");

        System.out.println("========== BẮT ĐẦU TEST GROQ AI ==========");
        long start = System.currentTimeMillis();

        // Thử yêu cầu dịch 1 câu
        String prompt = "Translate this text to Vietnamese. Return only the translated text: 'Hello, how can I help you today?'";
        String response = aiService.generateText(prompt);

        long duration = System.currentTimeMillis() - start;

        System.out.println("👉 KẾT QUẢ TỪ GROQ AI: " + response);
        System.out.println("⚡ THỜI GIAN PHẢN HỒI: " + duration + " ms");
        System.out.println("==========================================");

        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    @Test
    void testGroqJsonMode() {
        System.out.println("========== BẮT ĐẦU TEST JSON MODE ==========");
        String prompt = "Trích xuất thông tin lịch hẹn từ câu sau và trả về đúng JSON dạng {\"title\": string, \"time\": string}: 'Tối mai 7h họp nhóm nhé'";
        String jsonResponse = aiService.generateJson(prompt);

        System.out.println("👉 KẾT QUẢ JSON TỪ AI: " + jsonResponse);
        System.out.println("============================================");

        assertNotNull(jsonResponse);
        assertTrue(jsonResponse.startsWith("{") && jsonResponse.endsWith("}"));
    }

    @Test
    void testSmartDateTimeExtraction() {
        // 1. Lấy mốc thời gian hiện tại của hệ thống
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String currentTimeStr = now.format(formatter);
        String dayOfWeek = now.getDayOfWeek().toString();

        // 2. Câu tin nhắn của người dùng
        String userMessage = "Tối mai 7h tụi mình đi cà phê Highlands nhé";

        // 3. Prompt thông minh có mốc thời gian và định dạng ISO-8601
        String prompt = String.format(
                """
                        Hôm nay là: %s (%s).
                        Hãy trích xuất thông tin lịch hẹn từ tin nhắn người dùng: "%s".
                        Yêu cầu trả về đúng định dạng JSON:
                        {
                          "title": "Tên sự kiện ngắn gọn",
                          "startAt": "yyyy-MM-dd'T'HH:mm:ss",
                          "location": "Địa điểm nếu có, không có thì null"
                        }
                        Lưu ý: Tính toán chính xác ngày giờ dựa vào thời gian hiện tại được cung cấp. Nếu không rõ phút thì để :00:00.
                        """,
                currentTimeStr, dayOfWeek, userMessage);

        String jsonResponse = aiService.generateJson(prompt);

        System.out.println("👉 KẾT QUẢ TỪ GROQ AI:\n" + jsonResponse);

        // 4. Kiểm tra xem Java có parse được thành LocalDateTime để lưu DB không
        // (Giả sử JSON trả về: {"title": "Đi cà phê Highlands", "startAt":
        // "2026-11-04T19:00:00", "location": "Highlands"})
    }

    @Test
    void testSmartDateTimeExtractionFromMultipleMessages() {

        // 1. Lấy thời gian hiện tại làm mốc
        LocalDateTime now = LocalDateTime.now(
                ZoneId.of("Asia/Ho_Chi_Minh"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String currentTimeStr = now.format(formatter);
        String dayOfWeek = now.getDayOfWeek().toString();

        // 2. Nội dung nhiều tin nhắn trong phòng chat
        String conversation = """
                [08:00] An: Mai tụi mình đi cà phê Highlands nha.
                [08:01] Bình: Được á, tối đi.
                [08:02] An: 7h tối nha mọi người.
                [08:03] Chi: Ok chốt.
                """;

        // 3. Prompt tổng hợp lịch hẹn từ nhiều tin nhắn
        String prompt = String.format(
                """
                        Bạn là trợ lý AI giúp trích xuất lịch hẹn từ hội thoại nhóm.

                        Thời gian hiện tại: %s (%s).
                        Múi giờ: Asia/Ho_Chi_Minh.

                        Nội dung hội thoại:
                        %s

                        Nhiệm vụ:
                        - Đọc toàn bộ hội thoại để xác định lịch hẹn.
                        - Tổng hợp thông tin từ nhiều tin nhắn khác nhau.
                        - Tin nhắn sau có thể bổ sung hoặc làm rõ thông tin của tin nhắn trước.
                        - Xác định tên sự kiện, thời gian và địa điểm nếu có.
                        - Không tạo lịch hẹn nếu hội thoại không có lịch hẹn thực sự.
                        - Không tự suy đoán thông tin không được đề cập.

                        Quy tắc thời gian:
                        - "mai" là ngày tiếp theo.
                        - "mốt" là sau 2 ngày.
                        - "tối 7h" là 19:00:00.
                        - Tính ngày giờ dựa trên thời gian hiện tại được cung cấp.
                        - Nếu không xác định được thời gian, trả về null.

                        Bắt buộc trả về đúng JSON:
                        {
                        "title": "Tên sự kiện ngắn gọn",
                        "startAt": "yyyy-MM-dd'T'HH:mm:ss",
                        "location": "Địa điểm hoặc null"
                        }

                        Chỉ trả về JSON, không giải thích thêm.
                        """,
                currentTimeStr,
                dayOfWeek,
                conversation);

        // 4. Gọi AI
        String jsonResponse = aiService.generateJson(prompt);

        System.out.println(
                "========== TEST TỔNG HỢP LỊCH HẸN ==========");

        System.out.println("Hội thoại:\n" + conversation);

        System.out.println("Kết quả AI:\n" + jsonResponse);

        System.out.println("============================================");

        assertNotNull(jsonResponse);
        assertFalse(jsonResponse.isBlank());
    }
}