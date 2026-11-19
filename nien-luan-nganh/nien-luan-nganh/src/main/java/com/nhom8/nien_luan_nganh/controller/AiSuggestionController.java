package com.nhom8.nien_luan_nganh.controller;

import com.nhom8.nien_luan_nganh.dto.request.AiSuggestRequest;
import com.nhom8.nien_luan_nganh.dto.response.ApiResponse;
import com.nhom8.nien_luan_nganh.dto.response.AiSuggestionResponse;
import com.nhom8.nien_luan_nganh.service.AiSuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/rooms/{roomId}/ai", "/api/v1/room/{roomId}/ai"})
@RequiredArgsConstructor
public class AiSuggestionController {

    private final AiSuggestionService aiSuggestionService;

    @PostMapping("/suggestions")
    public ResponseEntity<ApiResponse<AiSuggestionResponse>> suggest(
            Authentication auth,
            @PathVariable UUID roomId,
            @Valid @RequestBody AiSuggestRequest request) {
        UUID currentUserId = UUID.fromString(auth.getName());
        AiSuggestionResponse response = aiSuggestionService.suggest(roomId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Gợi ý AI thành công"));
    }
}

