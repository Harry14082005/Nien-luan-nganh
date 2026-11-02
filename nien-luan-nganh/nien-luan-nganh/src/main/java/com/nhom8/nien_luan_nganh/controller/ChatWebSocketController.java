package com.nhom8.nien_luan_nganh.controller;

import com.nhom8.nien_luan_nganh.config.UserDetailsImpl;
import com.nhom8.nien_luan_nganh.dto.request.ChatMessagePayload;
import com.nhom8.nien_luan_nganh.dto.request.MarkMessageReceiptPayload;
import com.nhom8.nien_luan_nganh.dto.request.SendMessageRequest;
import com.nhom8.nien_luan_nganh.dto.request.TypingPayload;
import com.nhom8.nien_luan_nganh.dto.response.ApiResponse;
import com.nhom8.nien_luan_nganh.dto.response.MessageReceiptResponse;
import com.nhom8.nien_luan_nganh.dto.response.MessageResponse;
import com.nhom8.nien_luan_nganh.dto.response.TypingEventResponse;
import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.service.MessageService;
import com.nhom8.nien_luan_nganh.service.RoomMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.FieldError;

import java.security.Principal;
import java.util.UUID;

/**
 * Controller xử lý các tin nhắn và sự kiện thời gian thực qua giao thức WebSocket STOMP.
 * - Tuân thủ kiến trúc phân tầng: chỉ phụ thuộc vào Service và MessagingTemplate.
 * - Lắng nghe các event gửi từ Client lên các destination có tiền tố /app/...
 * - Phân phối và broadcast dữ liệu xuống Topic chung /topic/room.{roomId}
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final MessageService messageService;
    private final RoomMemberService roomMemberService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gửi tin nhắn tức thời vào phòng chat theo chuẩn duy nhất (Payload-based).
     * Destination gửi lên: /app/chat.sendMessage
     * Destination nhận về (Broadcast): /topic/room.{roomId}
     *
     * @param payload   Chứa roomId, content, messageType, repliedMessageId
     * @param principal Danh tính người gửi đã được xác thực từ JWT
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(
            @Valid @Payload ChatMessagePayload payload,
            Principal principal) {
        UUID currentUserId = getAuthenticatedUserId(principal);
        if (currentUserId == null) {
            return;
        }

        log.info("WebSocket nhận tin nhắn: senderId = {}, roomId = {}", currentUserId, payload.getRoomId());

        SendMessageRequest request = SendMessageRequest.builder()
                .content(payload.getContent())
                .messageType(payload.getMessageType())
                .repliedMessageId(payload.getRepliedMessageId())
                .build();

        // 1. Lưu tin nhắn vào CSDL và cập nhật phòng chat thông qua MessageService
        MessageResponse response = messageService.sendMessage(currentUserId, payload.getRoomId(), request);

        // 2. Broadcast tức thì tới Topic của phòng chat cho tất cả thành viên đang lắng nghe
        messagingTemplate.convertAndSend("/topic/room." + payload.getRoomId(), response);
    }

    /**
     * Phát sóng trạng thái Đang gõ chữ (Typing indicator: "Harry đang nhập tin nhắn...").
     * Destination gửi lên: /app/chat.typing
     * Destination nhận về (Broadcast): /topic/room.{roomId}.typing
     *
     * @param payload   Chứa roomId và cờ isTyping (true/false)
     * @param principal Danh tính người đang gõ chữ từ JWT
     */
    @MessageMapping("/chat.typing")
    public void sendTypingStatus(
            @Valid @Payload TypingPayload payload,
            Principal principal) {
        UUID currentUserId = getAuthenticatedUserId(principal);
        if (currentUserId == null) {
            return;
        }

        // Trích xuất sẵn username & avatarUrl từ Principal trên RAM (nếu có) để tầng Service không phải query DB
        String cachedUsername = null;
        String cachedAvatarUrl = null;
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            User user = userDetails.getUser();
            if (user != null) {
                cachedUsername = user.getUsername();
                cachedAvatarUrl = user.getAvatarUrl();
            }
        }

        // Ủy quyền kiểm tra quyền thành viên (có cache TTL) và tạo event cho MessageService
        TypingEventResponse event = messageService.buildTypingEvent(
                currentUserId, payload, cachedUsername, cachedAvatarUrl);

        // Broadcast tới topic typing của phòng: /topic/room.{roomId}.typing
        messagingTemplate.convertAndSend("/topic/room." + payload.getRoomId() + ".typing", event);
    }

    /**
     * Đánh dấu đã đọc (Read / Seen) tin nhắn qua WebSocket STOMP.
     * Destination gửi lên: /app/chat.markRead
     * Destination nhận về (Broadcast): /topic/room.{roomId}.receipts (và /topic/room.{roomId}.read)
     */
    @MessageMapping("/chat.markRead")
    public void markAsRead(
            @Valid @Payload MarkMessageReceiptPayload payload,
            Principal principal) {
        UUID currentUserId = getAuthenticatedUserId(principal);
        if (currentUserId == null) {
            return;
        }

        log.debug("WebSocket đánh dấu đã đọc: userId = {}, roomId = {}, messageId = {}",
                currentUserId, payload.getRoomId(), payload.getMessageId());

        MessageReceiptResponse receipt = roomMemberService.markAsRead(
                payload.getRoomId(), payload.getMessageId(), currentUserId);

        if (receipt != null) {
            try {
                messagingTemplate.convertAndSend("/topic/room." + payload.getRoomId() + ".receipts", receipt);
            } catch (Exception ex) {
                log.warn("Không thể broadcast read receipt qua WebSocket cho room {}: {}",
                        payload.getRoomId(), ex.getMessage());
            }
        }
    }

    /**
     * Đánh dấu đã nhận (Delivered) tin nhắn qua WebSocket STOMP.
     * Destination gửi lên: /app/chat.markDelivered
     * Destination nhận về (Broadcast): /topic/room.{roomId}.receipts
     */
    @MessageMapping("/chat.markDelivered")
    public void markAsDelivered(
            @Valid @Payload MarkMessageReceiptPayload payload,
            Principal principal) {
        UUID currentUserId = getAuthenticatedUserId(principal);
        if (currentUserId == null) {
            return;
        }

        log.debug("WebSocket đánh dấu đã nhận: userId = {}, roomId = {}, messageId = {}",
                currentUserId, payload.getRoomId(), payload.getMessageId());

        MessageReceiptResponse receipt = roomMemberService.markAsDelivered(
                payload.getRoomId(), payload.getMessageId(), currentUserId);

        if (receipt != null) {
            try {
                messagingTemplate.convertAndSend("/topic/room." + payload.getRoomId() + ".receipts", receipt);
            } catch (Exception ex) {
                log.warn("Không thể broadcast delivered receipt qua WebSocket cho room {}: {}",
                        payload.getRoomId(), ex.getMessage());
            }
        }
    }

    /**
     * Helper trích xuất UUID người dùng từ Principal một cách an toàn.
     */
    private UUID getAuthenticatedUserId(Principal principal) {
        if (principal == null) {
            log.warn("Từ chối truy cập WebSocket: Thiếu thông tin Principal");
            return null;
        }
        try {
            return UUID.fromString(principal.getName());
        } catch (IllegalArgumentException e) {
            log.error("Principal name không phải là định dạng UUID hợp lệ: {}", principal.getName());
            return null;
        }
    }

    /**
     * Bắt và xử lý lỗi Validation (@Valid) khi gửi tin nhắn qua WebSocket:
     * Ví dụ: Thiếu roomId, nội dung tin nhắn để trống (@NotBlank), sai kích thước...
     * Gửi thông báo lỗi chi tiết về Hộp thư riêng (/user/queue/errors) của người vừa gửi.
     */
    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    public void handleValidationException(MethodArgumentNotValidException ex, Principal principal) {
        String errorMsg = "Dữ liệu gửi lên không hợp lệ";
        int errorCodeValue = ErrorCode.INVALID_INPUT.getCode();

        if (ex.getBindingResult() != null && ex.getBindingResult().getFieldError() != null) {
            FieldError fieldError = ex.getBindingResult().getFieldError();
            String defaultMessage = fieldError.getDefaultMessage();
            if (defaultMessage != null && !defaultMessage.isBlank()) {
                try {
                    ErrorCode errorCode = ErrorCode.valueOf(defaultMessage);
                    errorCodeValue = errorCode.getCode();
                    errorMsg = errorCode.getMessage();
                } catch (IllegalArgumentException e) {
                    errorMsg = defaultMessage;
                }
            }
        }
        log.warn("Lỗi validation khi gửi tin nhắn WebSocket: code = {}, message = {}", errorCodeValue, errorMsg);

        if (principal != null) {
            ApiResponse<Void> errorResponse = ApiResponse.error(errorCodeValue, errorMsg);
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", errorResponse);
        }
    }

    /**
     * Xử lý ngoại lệ nghiệp vụ (AppException) khi gửi tin nhắn qua WebSocket:
     * Ví dụ: Không phải thành viên trong phòng, bị chặn (Block), tin nhắn cha đã bị thu hồi...
     * Gửi thông báo lỗi trực tiếp vào Hộp thư riêng (/user/queue/errors) của người vừa gửi.
     */
    @MessageExceptionHandler(AppException.class)
    public void handleAppException(AppException ex, Principal principal) {
        log.warn("Lỗi nghiệp vụ khi gửi tin nhắn qua WebSocket: code = {}, message = {}",
                ex.getErrorCode().getCode(), ex.getMessage());

        if (principal != null) {
            ApiResponse<Void> errorResponse = ApiResponse.error(ex.getErrorCode().getCode(), ex.getMessage());
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", errorResponse);
        }
    }

    /**
     * Bắt các lỗi hệ thống không mong muốn khác trong quá trình gửi tin nhắn WebSocket.
     */
    @MessageExceptionHandler(Exception.class)
    public void handleGeneralException(Exception ex, Principal principal) {
        log.error("Lỗi không mong muốn trong WebSocket Chat: {}", ex.getMessage(), ex);

        if (principal != null) {
            ApiResponse<Void> errorResponse = ApiResponse.error(
                    ErrorCode.UNCATEGORIZED_EXCEPTION.getCode(),
                    ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", errorResponse);
        }
    }
}
