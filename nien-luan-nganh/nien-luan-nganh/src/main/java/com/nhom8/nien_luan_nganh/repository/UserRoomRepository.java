package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.entity.UserRoom;
import com.nhom8.nien_luan_nganh.entity.UserRoomId;
import com.nhom8.nien_luan_nganh.enums.RoomRole;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface UserRoomRepository extends JpaRepository<UserRoom, UserRoomId> {

    // Kiem tra user co trong room hong
    boolean existsById_UserIdAndId_RoomId(UUID userId, UUID roomId);

    // Lay thong tin member
    Optional<UserRoom> findById_UserIdAndId_RoomId(UUID userId, UUID roomId);

    // Lay tat ca member trong room
    @Query("SELECT ur FROM UserRoom ur JOIN FETCH ur.user WHERE ur.id.roomId = :roomId")
    List<UserRoom> findMembersByRoomId(@Param("roomId") UUID roomId);

    // Lay tat ca room cua user
    @Query("SELECT ur FROM UserRoom ur JOIN FETCH ur.room WHERE ur.id.userId = :userId")
    List<UserRoom> findRoomsByUserId(@Param("userId") UUID userId);

    // Lay member theo role
    List<UserRoom> findById_RoomIdAndRole(UUID roomId, RoomRole role);

    // Dem so member trong room
    long countById_RoomId(UUID roomId);

    // Lay user trong room
    @Query("SELECT ur.user FROM UserRoom ur WHERE ur.id.roomId = :roomId")
    List<User> findUsersByRoomId(@Param("roomId") UUID roomId);
}
