package com.nhom8.nien_luan_nganh.dto.response;

import com.nhom8.nien_luan_nganh.enums.MessageReceiptType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO phản hồi biên nhận tin nhắn (Message Receipt) thời gian thực qua WebSocket.
 * Broadcast tới Topic: /topic/room.{roomId}.receipts (hoặc .read, .delivered)
 * Giúp người gửi biết tin nhắn đã tới máy ai (DELIVERED) hoặc ai đã xem tin (READ).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageReceiptResponse {

    private UUID roomId;

    private UUID userId;

    private String username;

    private String avatarUrl;

    private UUID messageId;

    private MessageReceiptType receiptType;

    private LocalDateTime timestamp;
}

