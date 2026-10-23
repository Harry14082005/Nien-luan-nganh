package com.nhom8.nien_luan_nganh.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * DTO nhận dữ liệu gửi lời mời kết bạn từ Client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendshipRequest {

    @NotNull(message = "FRIEND_ID_REQUIRED")
    private UUID friendId;
}
