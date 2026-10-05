package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.Room;
import com.nhom8.nien_luan_nganh.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    // Lay tat ca room cua user
    @Query("SELECT r FROM Room r JOIN r.userRooms ur WHERE ur.id.userId = :userId")
    List<Room> findByUserId(@Param("userId") UUID userId);

    // Tranh tao trung phong 1:1
    @Query("SELECT r FROM Room r JOIN r.userRooms ur1 JOIN r.userRooms ur2 "
            + "WHERE r.roomType = :roomType "
            + "AND ur1.user.userId = :userId1 AND ur2.user.userId = :userId2")
    Optional<Room> findDirectRoom(@Param("userId1") UUID userId1,
            @Param("userId2") UUID userId2,
            @Param("roomType") RoomType roomType);

    // Admin
    List<Room> findByRoomType(RoomType roomType);

    Long countByRoomType(RoomType roomType);
}
