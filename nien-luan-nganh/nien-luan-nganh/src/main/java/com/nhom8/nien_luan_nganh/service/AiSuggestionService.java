package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.request.AiSuggestRequest;
import com.nhom8.nien_luan_nganh.dto.response.AiSuggestionResponse;
import com.nhom8.nien_luan_nganh.entity.Message;
import com.nhom8.nien_luan_nganh.entity.Room;
import com.nhom8.nien_luan_nganh.enums.MessageType;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.repository.MessageRepository;
import com.nhom8.nien_luan_nganh.repository.RoomMemberRepository;
import com.nhom8.nien_luan_nganh.repository.RoomRepository;
import com.nhom8.nien_luan_nganh.service.ai.AiPromptBuilder;
import com.nhom8.nien_luan_nganh.service.ai.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiSuggestionService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageRepository messageRepository;
    private final ChatContextService chatContextService;
    private final RateLimiterService rateLimiterService;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    /**
     * Gợi ý nội dung tạo Poll hoặc Reminder từ tin nhắn anchor và ngữ cảnh hội
     * thoại.
     */
    @Transactional(readOnly = true)
    public AiSuggestionResponse suggest(UUID roomId, AiSuggestRequest request, UUID currentUserId) {
        // 1. Kiểm tra Rate limit (5 lần / phút / user)
        rateLimiterService.checkRateLimit(currentUserId);

        // 2. Kiểm tra quyền & room
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId, roomId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ROOM_MEMBER));

        // 3. Kiểm tra anchor message kèm sender (Eager fetch sender để tránh
        // LazyInitializationException)
        Message anchor = messageRepository.findByIdWithSender(request.getAnchorMessageId())
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!anchor.getRoom().getRoomId().equals(roomId)) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        if (Boolean.TRUE.equals(anchor.getIsRevoked()) || anchor.getMessageType() != MessageType.TEXT) {
            log.warn("Tin nhắn anchor không hợp lệ để gợi ý AI (revoked hoặc không phải TEXT)");
            return buildEmptyResponse(request.getType());
        }

        // 4. Lấy ngữ cảnh hội thoại (tối đa 10 tin, < 1500 ký tự)
        ChatContextService.ContextResult contextResult = chatContextService.extractRecentContext(roomId, anchor);

        String anchorSender = anchor.getSender() != null && anchor.getSender().getUsername() != null
                ? anchor.getSender().getUsername()
                : "User";
        String anchorContent = anchor.getContent() != null ? anchor.getContent().trim() : "";

        // 5. Chọn Prompt tương ứng theo Type
        String prompt;
        if (request.getType() == AiSuggestRequest.SuggestionType.POLL) {
            prompt = AiPromptBuilder.extractPollPrompt(
                    contextResult.getFormattedContextText(),
                    anchorSender,
                    anchorContent);

        } else { // reminder
            ZoneId hcmZone = ZoneId.of("Asia/Ho_Chi_Minh");
            LocalDateTime nowHcm = LocalDateTime.now(hcmZone);
            String nowIso = nowHcm.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String dayOfWeek = nowHcm.getDayOfWeek().toString();

            prompt = AiPromptBuilder.extractReminderPrompt(
                    contextResult.getFormattedContextText(),
                    anchorSender,
                    anchorContent,
                    nowIso,
                    dayOfWeek);
        }

        // 6. Gọi AI qua Json Mode
        String jsonResult;
        try {
            log.info("Bắt đầu gọi AI gợi ý {} cho tin nhắn [{}] với Prompt:\n{}", request.getType(),
                    anchor.getMessageId(), prompt);
            jsonResult = aiService.generateJson(prompt);
            log.info("AI Suggestion content parsed from response for {}: {}", request.getType(), jsonResult);
        } catch (Exception e) {
            log.error("Lỗi khi gọi AI gợi ý {}: {}", request.getType(), e.getMessage(), e);
            return buildEmptyResponse(request.getType());
        }

        // 7. Validate và map kết quả
        try {
            return parseAndValidate(request.getType(), jsonResult, contextResult.getCodeToMessageIdMap());
        } catch (Exception e) {
            log.warn("Lỗi parse kết quả JSON từ AI: {}, raw: {}", e.getMessage(), jsonResult);
            return buildEmptyResponse(request.getType());
        }
    }

    private AiSuggestionResponse parseAndValidate(
            AiSuggestRequest.SuggestionType type,
            String jsonResult,
            Map<String, UUID> codeMap) {

        if (jsonResult == null || jsonResult.isBlank()) {
            return buildEmptyResponse(type);
        }

        try {
            JsonNode root = objectMapper.readTree(jsonResult);
            boolean found = root.path("found").asBoolean(false);
            if (!found) {
                return buildEmptyResponse(type);
            }

            // Map source_codes sang UUID thực tế trong ngữ cảnh
            List<UUID> sourceMessageIds = new ArrayList<>();
            JsonNode sourceCodesNode = root.path("source_codes");
            if (sourceCodesNode.isArray()) {
                for (JsonNode cNode : sourceCodesNode) {
                    String code = cNode.asText().trim();
                    UUID realId = codeMap.get(code);
                    if (realId != null && !sourceMessageIds.contains(realId)) {
                        sourceMessageIds.add(realId);
                    }
                }
            }

            if (type == AiSuggestRequest.SuggestionType.POLL) {
                String question = root.path("question").asText("").trim();
                List<String> options = new ArrayList<>();
                JsonNode optionsNode = root.path("options");
                if (optionsNode.isArray()) {
                    for (JsonNode opt : optionsNode) {
                        String optText = opt.asText().trim();
                        if (!optText.isEmpty() && optText.length() <= 50) {
                            // Bỏ qua option trùng lặp không phân biệt hoa thường
                            boolean duplicate = options.stream().anyMatch(o -> o.equalsIgnoreCase(optText));
                            if (!duplicate) {
                                options.add(optText);
                            }
                        }
                    }
                }

                // Poll hợp lệ phải có ít nhất 2 phương án và câu hỏi
                if (question.isEmpty() || options.size() < 2) {
                    log.info("Kết quả Poll không đủ điều kiện (question: '{}', options: {})", question, options.size());
                    return buildEmptyResponse(type);
                }

                // Cắt tối đa 6 phương án (tối đq 10 nên phòng hờ)
                if (options.size() > 6) {
                    options = options.subList(0, 6);
                }

                return AiSuggestionResponse.builder()
                        .found(true)
                        .data(AiSuggestionResponse.PollSuggestionData.builder()
                                .question(question)
                                .options(options)
                                .build())
                        .sourceMessageIds(sourceMessageIds)
                        .build();

            } else {
                // Reminder
                String title = root.path("title").asText("").trim();
                String desc = root.path("description").asText("").trim();
                String startAt = root.path("startAt").asText("").trim();
                String endAt = root.path("endAt").asText("").trim();

                if (title.isEmpty()) {
                    return buildEmptyResponse(type);
                }

                return AiSuggestionResponse.builder()
                        .found(true)
                        .data(AiSuggestionResponse.ReminderSuggestionData.builder()
                                .title(title)
                                .description(desc.isEmpty() ? null : desc)
                                .startAt(startAt.isEmpty() ? null : startAt)
                                .endAt(endAt.isEmpty() ? null : endAt)
                                .build())
                        .sourceMessageIds(sourceMessageIds)
                        .build();
            }

        } catch (Exception e) {
            log.warn("Không thể parse JSON gợi ý AI: {}", e.getMessage());
            return buildEmptyResponse(type);
        }
    }

    private AiSuggestionResponse buildEmptyResponse(AiSuggestRequest.SuggestionType type) {
        return AiSuggestionResponse.builder()
                .found(false)
                .data(null)
                .sourceMessageIds(Collections.emptyList())
                .build();
    }
}