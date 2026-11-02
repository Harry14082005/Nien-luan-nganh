package com.nhom8.nien_luan_nganh.enums;

/**
 * Loại xác nhận biên nhận tin nhắn (Message Receipt):
 * - DELIVERED: Tin nhắn đã được gửi tới thiết bị của người nhận (2 tích xám).
 * - READ: Người nhận đã mở phòng và xem tin nhắn (2 tích xanh hoặc hiển thị avatar).
 */
public enum MessageReceiptType {
    DELIVERED,
    READ
}

