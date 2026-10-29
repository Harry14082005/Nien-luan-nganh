package com.nhom8.nien_luan_nganh.dto.response;

import com.nhom8.nien_luan_nganh.enums.ReactionType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO trả về thông tin chi tiết một cảm xúc trên tin nhắn.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionResponse {

    private UUID messageId;
    private UUID userId;
    private String username;
    private String avatarUrl;
    private ReactionType reactionType;
    private LocalDateTime createdAt;
}
