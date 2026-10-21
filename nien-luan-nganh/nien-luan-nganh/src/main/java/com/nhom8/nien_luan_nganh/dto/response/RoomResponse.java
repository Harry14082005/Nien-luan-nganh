package com.nhom8.nien_luan_nganh.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponse {
    private UUID roomId;
    private String roomName;
    private String roomAvatarURL;
    private String theme;
    private String createdBy;
    private boolean requireApproval;
    private LocalDateTime createdAt;
}
