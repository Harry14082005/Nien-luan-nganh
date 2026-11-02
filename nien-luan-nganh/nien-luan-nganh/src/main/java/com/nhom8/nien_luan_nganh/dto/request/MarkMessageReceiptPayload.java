package com.nhom8.nien_luan_nganh.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * Payload yêu cầu cập nhật trạng thái đã nhận (Delivered) hoặc đã đọc (Read) của tin nhắn.
 * Hỗ trợ cho cả WebSocket STOMP (/app/chat.markDelivered, /app/chat.markRead) và REST API.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkMessageReceiptPayload {

    @NotNull(message = "ID phòng không được để trống")
    private UUID roomId;

    @NotNull(message = "ID tin nhắn không được để trống")
    private UUID messageId;
}

