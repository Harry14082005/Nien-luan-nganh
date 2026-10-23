package com.nhom8.nien_luan_nganh.dto.response;

import com.nhom8.nien_luan_nganh.enums.FriendshipStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO trả về thông tin mối quan hệ bạn bè cho Client.
 * Chứa thông tin đối phương (friend), trạng thái (ACCEPTED, PENDING, BLOCKED),
 * và chiều của lời mời (direction: INCOMING hoặc OUTGOING).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendshipResponse {

    private UUID friendshipId;

    // Thông tin người bạn (hoặc người gửi/nhận lời mời)
    private UserResponse friend;

    // Trạng thái quan hệ (ACCEPTED, PENDING, DECLINED, BLOCKED)
    private FriendshipStatus status;

    // Chiều của lời mời: "INCOMING" (lời mời gửi đến tôi) hoặc "OUTGOING" (lời mời tôi gửi đi)
    private String direction;

    private LocalDateTime createdAt;
}
