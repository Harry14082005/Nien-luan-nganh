package com.nhom8.nien_luan_nganh.controller;

import com.nhom8.nien_luan_nganh.dto.response.ApiResponse;
import com.nhom8.nien_luan_nganh.dto.response.TranslationResponse;
import com.nhom8.nien_luan_nganh.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages/{messageId}")
@RequiredArgsConstructor
public class TranslationController {
    private final TranslationService translationService;

    @PostMapping("/translate")
    public ResponseEntity<ApiResponse<TranslationResponse>> translate(
            @PathVariable UUID messageId,
            Authentication authentication
    ){
        UUID currentUserId = UUID.fromString(authentication.getName());
        TranslationResponse response = translationService.translate(messageId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Dịch tin nhắn thành công"));
    }
}
