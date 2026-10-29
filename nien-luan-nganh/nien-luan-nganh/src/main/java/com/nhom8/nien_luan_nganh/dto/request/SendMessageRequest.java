package com.nhom8.nien_luan_nganh.dto.request;

import com.nhom8.nien_luan_nganh.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

/**
 * DTO yêu cầu gửi tin nhắn vào phòng chat.
 * Hỗ trợ các loại tin nhắn: TEXT, IMAGE, FILE và trả lời tin nhắn (reply).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {

    @NotBlank(message = "MESSAGE_CONTENT_REQUIRED")
    private String content;
    private MessageType messageType;
    /**
     * Khóa chính UUID của tin nhắn cha (nếu đây là hành động Reply/Trả lời tin nhắn).
     * Để trống (null) nếu là tin nhắn thông thường.
     */
    private UUID repliedMessageId;
}
