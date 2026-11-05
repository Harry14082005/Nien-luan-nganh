package com.nhom8.nien_luan_nganh.service.ai;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqAiServiceImpl implements AiService {
    private final RestClient aiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.groq.api-key:}")
    private String apiKey;

    @Value("${ai.groq.model:openai/gpt-oss-20b}")
    private String model;

    @Value("${ai.groq.url:https://api.groq.com/openai/v1/chat/completions}")
    private String apiUrl;

    @Value("${ai.groq.max-tokens:1000}")
    private int maxTokens;

    // Sinh văn bản (cho dịch)
    @Override
    public String generateText(String prompt) {
        // Mặc định temperature 0.2: rất chính xác, cực kỳ phù hợp cho Dịch thuật
        return generate(prompt, 0.2, false);
    }

    @Override
    public String generateText(String prompt, double temperature) {
        return generate(prompt, temperature, false);
    }

    @Override
    // Sinh JSON (cho lịch hẹn và poll)
    public String generateJson(String prompt) {
        // Temperature 0.1 và bật response_format json_object
        return generate(prompt, 0.1, true);
    }

    @Override
    // Kiểm tra AI có khả dụng hay hông
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.contains("your_groq_api_key");
    }

    // Hàm nội bộ gửi HTTP Request tới Groq theo chuẩn OpenAI Chat Completion
    private String generate(String prompt, double temperature, boolean jsonMode) {
        // kiểm tra key có đúng chưa trước khi gửi
        if (!isAvailable()) {
            log.warn("Chưa cấu hình Groq API Key trong application.properties!");
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // Chuẩn bị payload chuẩn OpenAI:
        /*
         * {
         * "model": "openai/gpt-oss-20b",
         * "messages": [
         * {
         * "role": "user",
         * "content": "Dịch câu này sang tiếng Anh: Xin chào"
         * }
         * ],
         * "temperature": 0.2,
         * "max_tokens": 1000
         * }
         */
        Map<String, Object> userMessage = Map.of("role", "user", "content", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(userMessage));
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);

        // Bật chế độ ép trả về JSON nếu cần bóc tách dữ liệu
        if (jsonMode) {
            requestBody.put("response_format", Map.of("type", "json_object"));
        }
        try {
            log.debug("Đang gọi Groq AI (Model: {}, JSON mode: {})...", model, jsonMode);
            // gửi http post
            String responseBody = aiRestClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody) // biến cái Map<String, Object> thành Json
                    .retrieve()
                    .body(String.class);

            // Bóc tách text từ response OpenAI: choices[0].message.content
            /*
             * {
             * "id": "chatcmpl-12345",
             * "choices": [
             * {
             * "index": 0,
             * "message": {
             * "role": "assistant",
             * "content": "Hello"
             * },
             * "finish_reason": "stop"
             * }
             * ],
             * "usage": { "total_tokens": 15 }
             * }
             */
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText();
                return content != null ? content.trim() : "";
            }
            log.warn("Groq không trả về choices hợp lệ: {}", responseBody);
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Lỗi khi gọi Groq AI: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }
}
