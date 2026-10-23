package com.nhom8.nien_luan_nganh.mapper;

import com.nhom8.nien_luan_nganh.dto.response.FriendshipResponse;
import com.nhom8.nien_luan_nganh.entity.Friendship;
import com.nhom8.nien_luan_nganh.entity.User;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

/**
 * Mapper chuyển đổi từ Friendship Entity sang FriendshipResponse DTO.
 * Tự động tính toán đối phương (friend) và chiều của lời mời (INCOMING / OUTGOING)
 * dựa theo currentUserId của người đang đăng nhập.
 */
@Mapper(componentModel = "spring")
public abstract class FriendshipMapper {

    @Autowired
    protected UserMapper userMapper;

    public FriendshipResponse toFriendshipResponse(Friendship friendship, UUID currentUserId) {
        if (friendship == null) {
            return null;
        }

        // Xác định ai là người gửi và ai là đối phương
        boolean isSender = friendship.getUser().getUserId().equals(currentUserId);
        User otherPerson = isSender ? friendship.getFriend() : friendship.getUser();
        String direction = isSender ? "OUTGOING" : "INCOMING";

        return FriendshipResponse.builder()
                .friendshipId(friendship.getFriendshipId())
                .friend(userMapper.toUserResponse(otherPerson))
                .status(friendship.getFriendshipStatus())
                .direction(direction)
                .createdAt(friendship.getCreatedAt())
                .build();
    }
}
