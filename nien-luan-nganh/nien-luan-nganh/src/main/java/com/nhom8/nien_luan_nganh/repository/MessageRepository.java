package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhom8.nien_luan_nganh.enums.MessageType;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

        // Lay tin nhan moi nhat(lan dau load)
        Page<Message> findByRoom_RoomIdOrderByCreatedAtDesc(UUID roomId, Pageable pageable);
        // hoac co the viet
        // @Query("SELECT m FROM Message m WHERE m.room.roomId = :roomId ORDER BY
        // m.createdAt DESC")
        // Page<Message> findLatestMessages(@Param("roomId") UUID roomId, Pageable
        // pageable);

        // Cuon de xem tin nhan cu - Cursor-based pagination - infinite scroll
        @Query("SELECT m FROM Message m WHERE m.room.roomId = :roomId " +
                        "AND m.createdAt < (SELECT m2.createdAt FROM Message m2 " +
                        "WHERE m2.messageId = :before) " +
                        "ORDER BY m.createdAt DESC")
        Page<Message> findByRoomIdBefore(@Param("roomId") UUID roomId, @Param("before") UUID before, Pageable pageable);

        // Tim tin nhan trong room (tin nhan thu hoi roi thi khong thay)
        @Query("SELECT m FROM Message m JOIN FETCH m.sender " +
                        "WHERE m.room.roomId = :roomId " +
                        "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :q, '%')) " +
                        "AND m.isRevoked = false " +
                        "ORDER BY m.createdAt DESC")
        List<Message> searchInRoom(@Param("roomId") UUID roomId,
                        @Param("q") String q,
                        Pageable pageable);

        // Admin
        // 1. Thống kê số tin nhắn theo khoảng thời gian (quét index cực nhanh, chuẩn
        // múi giờ)
        @Query("SELECT COUNT(m) FROM Message m WHERE m.createdAt >= :start AND m.createdAt < :end")
        long countByCreatedAtRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        // 2. Thống kê theo loại tin nhắn (TEXT, IMAGE, FILE...)
        long countByType(MessageType type);
}
