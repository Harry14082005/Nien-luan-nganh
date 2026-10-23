package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.Room;
import com.nhom8.nien_luan_nganh.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    // Lay tat ca room cua user
    @Query("SELECT r FROM Room r JOIN r.roomMembers rm WHERE rm.id.userId = :userId")
    List<Room> findByUserId(@Param("userId") UUID userId);

    // Tranh tao trung phong 1:1
    @Query("SELECT r FROM Room r JOIN r.roomMembers rm1 JOIN r.roomMembers rm2 "
            + "WHERE r.roomType = :roomType "
            + "AND rm1.user.userId = :userId1 AND rm2.user.userId = :userId2")
    Optional<Room> findDirectRoom(@Param("userId1") UUID userId1,
                                  @Param("userId2") UUID userId2,
                                  @Param("roomType") RoomType roomType);

    // Admin
    List<Room> findByRoomType(RoomType roomType);

    Long countByRoomType(RoomType roomType);
}
