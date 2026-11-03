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
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
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
    private final UserRepository userRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageRepository messageRepository;
    private final TranslationRepository translationRepository;
    private final TranslationMapper translationMapper;
    private final AiService aiService;

    public TranslationResponse translate(UUID messageId, UUID currentUserId){
        //1. kiểm tra người dùng
        User user = getUserOrThrow(currentUserId);

        //2. lấy ngôn ngữ ưu tiên
        String targetLanguage = normalize(user.getPreferredLanguage());

        //3. lấy tin nhắn
        Message message = messageRepository.findById(messageId).orElseThrow(
                () -> new AppException(ErrorCode.MESSAGE_NOT_FOUND)
        );

        //4. kiểm tra thành viên có Active hông
        getActiveMemberOrThrow(message.getRoom().getRoomId(), currentUserId);

        //5. không dịch tin nhắn đã thu ồi
        if(message.getIsRevoked()){
            throw new AppException(ErrorCode.MESSAGE_REVOKED);
        }
        //6. không dịch poll với reminder
        if(message.getMessageType() != MessageType.TEXT){
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        //7. kiểm tra nội dung content
        String content = message.getContent();
        if(content == null || content.isBlank()){
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        //8. kieemr tra bản dịch có lưu rồi hay chưa
        Optional<Translation> cached = translationRepository.findByMessage_MessageIdAndTargetLanguage(messageId, targetLanguage);

        if(cached.isPresent()){
            log.info("Get translation from cache: messageId={}, lang={}", messageId, targetLanguage);
            return toResponse(cached.get(), true);
        }

        //xử lý gọi AI
        //9. kiểm tra AI có khả dụng hay không
        if(!aiService.isAvailable()){
            throw new AppException(ErrorCode.TRANSLATION_FAILED);
        }

        //10. xử lý lấy ngôn ngữ gốc của người nhắn
        String sourceLanguage;
        try{
            sourceLanguage = aiService.generateText(AiPromptBuilder.detectLanguagePrompt(content))
                    .trim().toLowerCase();
            log.info("Detect: {} -> {}", messageId, sourceLanguage);
        } catch (Exception e){
            log.warn("Detect failed, use default: {}", e.getMessage());
            sourceLanguage = "unknown";
        }

        //11. xử lý nếu source = target thì thôi
        if(sourceLanguage.equals(targetLanguage)){
            Translation t = Translation.builder()
                    .message(message)
                    .sourceLanguage(sourceLanguage)
                    .targetLanguage(targetLanguage)
                    .translatedText(content)
                    .build();
            return toResponse(t, false);
        }

        //12. gọi AI
        String translatedText = translatedWithRetry(content, targetLanguage);

        //13. lưu cache
        Translation t = Translation.builder()
                .message(message)
                .sourceLanguage(sourceLanguage)
                .targetLanguage(targetLanguage)
                .translatedText(translatedText)
                .build();
        try {
            translationRepository.save(t);
        } catch (DataIntegrityViolationException e){
            //request khác đã lưu lúc mình đang gọi
            return translationRepository
                    .findByMessage_MessageIdAndTargetLanguage(messageId, targetLanguage)
                    .map(existing -> toResponse(existing, true))
                    .orElseThrow(() -> new AppException(ErrorCode.TRANSLATION_FAILED));
        }
        log.info("Translation success: messageId={}, {} -> {}", messageId, sourceLanguage, targetLanguage);
        return toResponse(t, false);
    }

    private String normalize(String lang){
        if (lang == null || lang.isBlank()) return "vi";
        String l = lang.trim().toLowerCase();
        return l.contains("-") ? l.substring(0, l.indexOf('-')) : l; // vi-VN -> vi
    }

    private String translatedWithRetry(String content, String targetLanguage){
        String prompt = AiPromptBuilder.translatePrompt(content, targetLanguage);

        try{
            return aiService.generateText(prompt).trim();
        } catch (Exception e){
            log.warn("First try failed, retry: {}", e.getMessage());
        }

        try{
            return aiService.generateText(prompt).trim();
        } catch (Exception e){
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

    private TranslationResponse toResponse(Translation t, boolean fromCache){
        TranslationResponse response = translationMapper.toTranslationResponse(t);
        response.setFromCache(fromCache);
        return response;
    }
}
