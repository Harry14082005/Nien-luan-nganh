package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    // Lay danh sach ban be theo status
    @Query("SELECT f FROM Friendship f JOIN FETCH f.user JOIN FETCH f.friend" +
            "WHERE (f.user.userId = :userId OR f.friend.userId = :userId) AND f.status = :status")
    List<Friendship> findByUserIdAndStatus(@Param("userId") UUID userId,
            @Param("status") Friendship.FriendshipStatus status);

    // Check trang thai ban be
    @Query("SELECT f FROM Friendship f  WHERE (f.user.userId = :userId1 AND f.friend.userId = :userId2)" +
            "OR (f.user.userId = :userId2 AND f.friend.userId = :userId1)")
    Optional<Friendship> findBetweenUsers(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);

    // 2 truong hop khi gui va nhan loi moi ket ban
    // TH1: Nguoi nhan la user
    @Query("SELECT f FROM Friendship f JOIN FETCH f.user" +
            "WHERE f.friend.userId = :friendId AND f.status = :status")
    List<Friendship> findByFriend_UserIdAndStatus(@Param("friendId") UUID friendId,
            @Param("status") Friendship.FriendshipStatus status);

    // TH2 : User la nguoi gui loi moi ket ban
    @Query("SELECT f FROM Friendship f JOIN FETCH f.friend" +
            "WHERE f.user.userId = :userId AND f.status = :status")
    List<Friendship> findByUser_UserIdAndStatus(@Param("userId") UUID userId,
            @Param("status") Friendship.FriendshipStatus status);

}
