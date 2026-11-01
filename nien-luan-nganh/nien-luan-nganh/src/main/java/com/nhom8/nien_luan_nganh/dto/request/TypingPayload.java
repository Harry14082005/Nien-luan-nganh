package com.nhom8.nien_luan_nganh.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * DTO nhận tín hiệu trạng thái đang gõ chữ (Typing indicator) qua WebSocket STOMP (/app/chat.typing).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypingPayload {

    @NotNull(message = "ROOM_NOT_FOUND")
    private UUID roomId;

    /**
     * true: bắt đầu gõ chữ ("Harry đang nhập tin nhắn...")
     * false: ngừng gõ chữ hoặc vừa bấm gửi tin nhắn
     */
    @Builder.Default
    @JsonProperty("isTyping")
    private boolean typing = true;
}

