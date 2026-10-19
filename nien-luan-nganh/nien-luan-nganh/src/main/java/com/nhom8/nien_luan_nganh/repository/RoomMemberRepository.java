package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.RoomMember;
import com.nhom8.nien_luan_nganh.entity.RoomMemberId;
import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.enums.RoomRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, RoomMemberId> {

    // Kiem tra user co trong room khong
    boolean existsById_UserIdAndId_RoomId(UUID userId, UUID roomId);

    // Lay thong tin member
    Optional<RoomMember> findById_UserIdAndId_RoomId(UUID userId, UUID roomId);

    // Lay tat ca member trong room
    @Query("SELECT rm FROM RoomMember rm JOIN FETCH rm.user WHERE rm.id.roomId = :roomId")
    List<RoomMember> findMembersByRoomId(@Param("roomId") UUID roomId);

    // Lay tat ca room cua user
    @Query("SELECT rm FROM RoomMember rm JOIN FETCH rm.room WHERE rm.id.userId = :userId")
    List<RoomMember> findRoomsByUserId(@Param("userId") UUID userId);

    // Lay member theo role
    List<RoomMember> findById_RoomIdAndRoomMemberRole(UUID roomId, RoomRole roomMemberRole);

    // Dem so member trong room
    long countById_RoomId(UUID roomId);

    // Lay user trong room
    @Query("SELECT rm.user FROM RoomMember rm WHERE rm.id.roomId = :roomId")
    List<User> findUsersByRoomId(@Param("roomId") UUID roomId);
}
