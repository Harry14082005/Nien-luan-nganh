package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.response.TranslationResponse;
import com.nhom8.nien_luan_nganh.entity.Message;
import com.nhom8.nien_luan_nganh.entity.Translation;
import com.nhom8.nien_luan_nganh.enums.MessageType;
import com.nhom8.nien_luan_nganh.mapper.TranslationMapper;
import com.nhom8.nien_luan_nganh.repository.MessageRepository;
import com.nhom8.nien_luan_nganh.repository.TranslationRepository;
import com.nhom8.nien_luan_nganh.service.ai.AiPromptBuilder;
import com.nhom8.nien_luan_nganh.service.ai.AiService;
import com.nhom8.nien_luan_nganh.service.ai.AiService.DetectTranslateResult;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.nhom8.nien_luan_nganh.entity.RoomMember;
import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.enums.RoomMemberStatus;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.repository.RoomMemberRepository;
import com.nhom8.nien_luan_nganh.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TranslationService {
    private final ObjectMapper objectMapper;

    private final UserRepository userRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageRepository messageRepository;
    private final TranslationRepository translationRepository;
    private final TranslationMapper translationMapper;
    private final AiService aiService;

    public DetectTranslateResult detectAndTranslate(String content, String targetLanguage) {
        // tạo câu prompt
        String prompt = AiPromptBuilder.detectAndTranslatePrompt(content, targetLanguage);

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                // gọi AI để trả về định dạng json
                String json = aiService.generateJson(prompt);
                DetectTranslateResult r = objectMapper.readValue(cleanJson(json), DetectTranslateResult.class);

                if (r.translation() == null || r.translation().isBlank()) {
                    throw new IllegalStateException("Empty translation");
                }

                String source = r.source() == null ? "unknown" : normalize(r.source());
                return new DetectTranslateResult(source, r.translation().trim());

            } catch (Exception e) {
                log.warn("detectAndTranslate attempt {} failed: {}", attempt, e.getMessage());
                if (attempt == 1)
                    sleepQuietly(700); // để không bị retry liên tục liền
            }
        }
        throw new AppException(ErrorCode.TRANSLATION_FAILED);
    }

    // phòng khi model bọc ```json ... ```
    private String cleanJson(String s) {
        if (s == null)
            return "";
        return s.trim().replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "");
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    @Transactional
    public TranslationResponse translate(UUID messageId, UUID currentUserId, String targetLangParam) {

        // 1. kiểm tra người dùng
        User user = getUserOrThrow(currentUserId);

        // 2. lấy ngôn ngữ: Ưu tiên ngôn ngữ client chọn, nếu không có thì lấy của User
        String targetLanguage;
        if (targetLangParam != null && !targetLangParam.isBlank()) {
            targetLanguage = normalize(targetLangParam);
        } else {
            targetLanguage = normalize(user.getPreferredLanguage());
        }

        // 3. lấy tin nhắn
        Message message = messageRepository.findById(messageId).orElseThrow(
                () -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        // 4. kiểm tra thành viên có Active hông
        getActiveMemberOrThrow(message.getRoom().getRoomId(), currentUserId);

        // 5. không dịch tin nhắn đã thu ồi
        if (message.getIsRevoked()) {
            throw new AppException(ErrorCode.MESSAGE_REVOKED);
        }
        // 6. không dịch poll với reminder
        if (message.getMessageType() != MessageType.TEXT) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        // 7. kiểm tra nội dung content
        String content = message.getContent();
        if (content == null || content.isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        // 8. kiểm tra bản dịch có lưu rồi hay chưa
        Optional<Translation> cached = translationRepository.findByMessage_MessageIdAndTargetLanguage(messageId,
                targetLanguage);

        if (cached.isPresent()) {
            Translation t = cached.get();
            boolean isStale = false;
            // Nếu tin nhắn đã từng bị chỉnh sửa, kiểm tra xem bản dịch có được tạo TRƯỚC
            // lần sửa tin nhắn không
            if (Boolean.TRUE.equals(message.getIsEdited())) {
                try {
                    java.util.List<com.nhom8.nien_luan_nganh.entity.MessageHistory> histories = message
                            .getMessageHistories();
                    if (histories != null && !histories.isEmpty()) {
                        LocalDateTime lastEditedAt = histories.stream()
                                .map(com.nhom8.nien_luan_nganh.entity.MessageHistory::getEditedAt)
                                .filter(java.util.Objects::nonNull)
                                .max(LocalDateTime::compareTo)
                                .orElse(null);

                        if (lastEditedAt != null && t.getCreatedAt() != null
                                && t.getCreatedAt().isBefore(lastEditedAt)) {
                            isStale = true;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Lỗi kiểm tra lịch sử sửa tin nhắn: {}", e.getMessage());
                }
            }

            if (!isStale) {
                log.info("Get translation from cache: messageId={}, lang={}", messageId, targetLanguage);
                return toResponse(t, true);
            }

            // Nếu bản dịch đã cũ do tin nhắn bị sửa: gọi AI dịch lại và cập nhật đè lên bản
            // ghi cũ
            log.info("Bản dịch đã cũ do tin nhắn đã được chỉnh sửa (messageId={}), dịch lại...", messageId);
            if (!aiService.isAvailable()) {
                throw new AppException(ErrorCode.TRANSLATION_FAILED);
            }
            DetectTranslateResult result = detectAndTranslate(content, targetLanguage);
            String sourceLanguage = result.source();
            String translatedText = sourceLanguage.equals(targetLanguage) ? content : result.translation();
            t.setSourceLanguage(sourceLanguage);
            t.setTranslatedText(translatedText);
            t.setCreatedAt(LocalDateTime.now());
            translationRepository.save(t);
            return toResponse(t, false);
        }

        // xử lý gọi AI
        // 9. kiểm tra AI có khả dụng hay không
        if (!aiService.isAvailable()) {
            throw new AppException(ErrorCode.TRANSLATION_FAILED);
        }

        // 10. detect + dịch
        DetectTranslateResult result = detectAndTranslate(content, targetLanguage);
        String sourceLanguage = result.source();

        // 11. nếu cùng ngôn ngữ t hì thôi
        String translatedText = sourceLanguage.equals(targetLanguage) ? content : result.translation();

        // 12. lưu cache
        Translation t = Translation.builder()
                .message(message)
                .sourceLanguage(sourceLanguage)
                .targetLanguage(targetLanguage)
                .translatedText(translatedText)
                .build();
        try {
            translationRepository.save(t);
        } catch (DataIntegrityViolationException e) {
            // request khác đã lưu lúc mình đang gọi
            return translationRepository
                    .findByMessage_MessageIdAndTargetLanguage(messageId, targetLanguage)
                    .map(existing -> toResponse(existing, true))
                    .orElseThrow(() -> new AppException(ErrorCode.TRANSLATION_FAILED));
        }
        log.info("Translation success: messageId={}, {} -> {}", messageId, sourceLanguage, targetLanguage);
        return toResponse(t, false);
    }

    private String normalize(String lang) {
        if (lang == null || lang.isBlank())
            return "vi";
        String l = lang.trim().toLowerCase();
        return l.contains("-") ? l.substring(0, l.indexOf('-')) : l; // vi-VN -> vi
    }

    private String translatedWithRetry(String content, String targetLanguage) {
        String prompt = AiPromptBuilder.translatePrompt(content, targetLanguage);

        try {
            return aiService.generateText(prompt).trim();
        } catch (Exception e) {
            log.warn("First try failed, retry: {}", e.getMessage());
        }

        try {
            return aiService.generateText(prompt).trim();
        } catch (Exception e) {
            log.warn("Second try failed, retry: {}", e.getMessage());
            throw new AppException(ErrorCode.TRANSLATION_FAILED);
        }
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private RoomMember getRoomMemberOrThrow(UUID roomId, UUID userId) {
        return roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ROOM_MEMBER));
    }

    private RoomMember getActiveMemberOrThrow(UUID roomId, UUID userId) {
        RoomMember member = getRoomMemberOrThrow(roomId, userId);
        if (member.getStatus() != RoomMemberStatus.ACTIVE) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }
        return member;
    }

    private TranslationResponse toResponse(Translation t, boolean fromCache) {
        TranslationResponse response = translationMapper.toTranslationResponse(t);
        response.setFromCache(fromCache);
        return response;
    }
}
