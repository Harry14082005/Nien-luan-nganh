package com.nhom8.nien_luan_nganh.service.ai;

import java.util.Locale;

public class AiPromptBuilder {

    public static String translatePrompt(String content, String targetLang) {
        // Locale tự tra tên đầy đủ từ mã ngôn ngữ
        String langName = Locale.forLanguageTag(targetLang)
                .getDisplayLanguage(Locale.ENGLISH);

        // Nếu không tìm được tên thì dùng luôn mã ngôn ngữ
        if (langName == null || langName.isBlank() || langName.equals(targetLang)) {
            langName = targetLang;
        }

        return """
            Translate the following message to %s.
            Return ONLY the translated text, no explanation, no quotes, no extra text.
            If the message is already in %s, return it as-is.
            
            Message: %s
            """.formatted(langName, langName, content);
    }

    //phát hiện ngôn ngữ từ tin nhắn
    public static String detectLanguagePrompt(String content) {
        return """
                Detect the language of this text.
                Return ONLY the ISO 639-1 language code (e.g. vi, en, ja).
                No explanation, no punctuation, just the code.
                
                Text: %s
                """.formatted(content);
    }
}