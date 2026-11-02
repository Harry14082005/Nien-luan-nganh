package com.nhom8.nien_luan_nganh.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO phản hồi trạng thái Online / Vừa mới truy cập (User Presence) của người dùng.
 * Phục vụ cả REST API và WebSocket STOMP topic (/topic/users.presence).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPresenceResponse {

    private UUID userId;

    @JsonProperty("isOnline")
    private boolean online;

    private LocalDateTime lastSeenAt;
}

