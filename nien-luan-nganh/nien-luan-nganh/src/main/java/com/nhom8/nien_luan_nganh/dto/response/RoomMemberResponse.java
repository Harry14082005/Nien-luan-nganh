package com.nhom8.nien_luan_nganh.dto.response;

import com.nhom8.nien_luan_nganh.enums.RoomRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomMemberResponse {
    private UUID roomId;
    private UUID userId;
    private RoomRole roomMemberRole;
    private String status;
    private LocalDateTime joinedAt;
    private UUID lastDeliveredMessagedId;
    private UUID lastReadMessageId;
}
