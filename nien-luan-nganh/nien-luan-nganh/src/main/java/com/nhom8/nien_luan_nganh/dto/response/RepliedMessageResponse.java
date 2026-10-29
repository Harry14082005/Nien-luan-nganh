package com.nhom8.nien_luan_nganh.dto.response;

import com.nhom8.nien_luan_nganh.enums.MessageType;
import lombok.*;

import java.util.UUID;

/**
 * DTO tóm tắt thông tin của tin nhắn cha được trả lời (Reply).
 * Giúp giao diện hiển thị khung trích dẫn ngắn gọn phía trên tin nhắn trả lời.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepliedMessageResponse {

    private UUID messageId;
    private UUID senderId;
    private String senderUsername;
    private String content;
    private MessageType messageType;
    private Boolean isRevoked;
}
