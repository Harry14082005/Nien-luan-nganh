package com.nhom8.nien_luan_nganh.dto.request;

import com.nhom8.nien_luan_nganh.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * DTO nhận payload tin nhắn gửi qua WebSocket STOMP (/app/chat.sendMessage).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessagePayload {

    @NotNull(message = "ROOM_NOT_FOUND")
    private UUID roomId;

    @NotBlank(message = "MESSAGE_CONTENT_REQUIRED")
    private String content;

    private MessageType messageType;

    /**
     * Khóa chính UUID của tin nhắn cha (nếu Reply). Tùy chọn (null nếu là tin nhắn mới).
     */
    private UUID repliedMessageId;
}

