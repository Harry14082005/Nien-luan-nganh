package com.nhom8.nien_luan_nganh.dto.response;

import com.nhom8.nien_luan_nganh.enums.MessageType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO trả về thông tin chi tiết của một tin nhắn.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private UUID messageId;
    private UUID roomId;
    private UserResponse sender;
    private MessageType messageType;
    private String content;

    /**
     * Thông tin tin nhắn cha nếu tin nhắn này là lời phản hồi (Reply).
     */
    private RepliedMessageResponse repliedMessage;

    private Boolean isEdited;
    private Boolean isRevoked;
    private LocalDateTime revokedAt;
    private Boolean isPinned;
    private LocalDateTime createdAt;
    private java.util.List<ReactionResponse> reactions;
}
