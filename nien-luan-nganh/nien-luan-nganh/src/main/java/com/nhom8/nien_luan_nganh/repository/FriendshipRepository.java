package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.Friendship;
import com.nhom8.nien_luan_nganh.enums.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    // 1. Dành cho danh sách chặn: 1 chiều (chỉ lấy người do chính mình chặn)
    @Query("SELECT f FROM Friendship f " +
            "JOIN FETCH f.user " +
            "JOIN FETCH f.friend " +
            "WHERE f.user.userId = :userId AND f.friendshipStatus = :status")
    List<Friendship> findBlockedList(@Param("userId") UUID userId,
                                     @Param("status") FriendshipStatus status);

    // 2. Dành cho bạn bè hoặc lời mời: 2 chiều (cả gửi đi và nhận về)
    @Query("SELECT f FROM Friendship f " +
            "JOIN FETCH f.user " +
            "JOIN FETCH f.friend " +
            "WHERE (f.user.userId = :userId OR f.friend.userId = :userId) " +
            "AND f.friendshipStatus = :status")
    List<Friendship> findTwoWayFriendships(@Param("userId") UUID userId,
                                           @Param("status") FriendshipStatus status);

    // 3. Kiểm tra trạng thái quan hệ giữa 2 người dùng cụ thể (có fetch thông tin user)
    @Query("SELECT f FROM Friendship f " +
            "JOIN FETCH f.user " +
            "JOIN FETCH f.friend " +
            "WHERE (f.user.userId = :userId1 AND f.friend.userId = :userId2) " +
            "OR (f.user.userId = :userId2 AND f.friend.userId = :userId1)")
    Optional<Friendship> findBetweenUsers(@Param("userId1") UUID userId1,
                                          @Param("userId2") UUID userId2);
}
