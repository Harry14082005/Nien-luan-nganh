package com.nhom8.nien_luan_nganh.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.UUID;

/**
 * DTO phát sóng trạng thái đang gõ chữ (Typing indicator) tới các thành viên trong phòng
 * qua Topic: /topic/room.{roomId}.typing
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TypingEventResponse {

    private UUID roomId;
    private UUID userId;
    private String username;
    private String avatarUrl;

    @JsonProperty("isTyping")
    private boolean typing;
}

