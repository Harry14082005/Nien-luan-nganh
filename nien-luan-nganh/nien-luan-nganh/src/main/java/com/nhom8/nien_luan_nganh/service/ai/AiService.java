package com.nhom8.nien_luan_nganh.service.ai;

public interface AiService {
    // Sinh văn bản (cho dịch)
    String generateText(String prompt);

    String generateText(String prompt, double temperature);

    // Sinh JSON (cho lịch hẹn và poll)
    String generateJson(String prompt);

    // Kiểm tra AI có khả dụng hay hông
    boolean isAvailable();
}
