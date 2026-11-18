package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.Room;
import com.nhom8.nien_luan_nganh.enums.RoomMemberStatus;
import com.nhom8.nien_luan_nganh.enums.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    @Query("SELECT r FROM Room r JOIN r.roomMembers rm WHERE rm.roomMemberId.userId = :userId AND rm.status = :status")
    List<Room> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") RoomMemberStatus status);

    // Lay tat ca room cua user
    @Query("SELECT r FROM Room r JOIN r.roomMembers rm WHERE rm.roomMemberId.userId = :userId")
    List<Room> findByUserId(@Param("userId") UUID userId);

    // Tranh tao trung phong 1:1
    @Query("SELECT r FROM Room r JOIN r.roomMembers rm1 JOIN r.roomMembers rm2 "
            + "WHERE r.roomType = :roomType "
            + "AND rm1.user.userId = :userId1 AND rm2.user.userId = :userId2")
    Optional<Room> findDirectRoom(@Param("userId1") UUID userId1,
                                  @Param("userId2") UUID userId2,
                                  @Param("roomType") RoomType roomType);

    // Lấy danh sách phòng theo trạng thái thành viên (chỉ lấy ACTIVE)
    @Query(value = "SELECT DISTINCT r FROM Room r JOIN r.roomMembers rm WHERE rm.roomMemberId.userId = :userId AND rm.status = :status",
           countQuery = "SELECT COUNT(DISTINCT r) FROM Room r JOIN r.roomMembers rm WHERE rm.roomMemberId.userId = :userId AND rm.status = :status")
    Page<Room> findDistinctByRoomMembers_User_UserIdAndStatus(
            @Param("userId") UUID userId,
            @Param("status") RoomMemberStatus status,
            Pageable pageable
    );

    @Query(value = "SELECT DISTINCT r FROM Room r JOIN r.roomMembers rm WHERE rm.roomMemberId.userId = :userId AND r.roomType = :roomType AND rm.status = :status",
           countQuery = "SELECT COUNT(DISTINCT r) FROM Room r JOIN r.roomMembers rm WHERE rm.roomMemberId.userId = :userId AND r.roomType = :roomType AND rm.status = :status")
    Page<Room> findDistinctByRoomMembers_User_UserIdAndRoomTypeAndStatus(
            @Param("userId") UUID userId,
            @Param("roomType") RoomType roomType,
            @Param("status") RoomMemberStatus status,
            Pageable pageable
    );

    // Lấy danh sách (legacy / fallback)
    Page<Room> findDistinctByRoomMembers_User_UserId(
            UUID userId,
            Pageable pageable
    );

    Page<Room> findDistinctByRoomMembers_User_UserIdAndRoomType(
            UUID userId,
            RoomType roomType,
            Pageable pageable
    );

    // Admin
    List<Room> findByRoomType(RoomType roomType);

    Long countByRoomType(RoomType roomType);
}
