package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.service.ai.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

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
}