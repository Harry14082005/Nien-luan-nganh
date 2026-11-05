package com.nhom8.nien_luan_nganh.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO chứa thông tin tóm tắt của tin nhắn tìm kiếm (Search Preview)
 * phục vụ hiển thị trên Sidebar Drawer bên phải giống Telegram / Slack.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSearchResultDTO {
    private UUID messageId;
    private String content;
    private LocalDateTime createdAt;
    private String senderUsername;
    private String senderAvatarUrl;
}

