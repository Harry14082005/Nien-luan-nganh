package com.nhom8.nien_luan_nganh.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationResponse {
    private UUID translationId;
    private UUID messageId;
    private String sourceLanguage;
    private String targetLanguage;
    private String translatedText;
    private LocalDateTime createdAt;
    private boolean fromCache;
}