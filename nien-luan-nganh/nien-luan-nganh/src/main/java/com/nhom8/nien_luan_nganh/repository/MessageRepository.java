package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.Message;
import com.nhom8.nien_luan_nganh.enums.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

        // Lay tin nhan moi nhat (lan dau load)
        @Query(value = "SELECT m FROM Message m " +
                        "LEFT JOIN FETCH m.sender " +
                        "LEFT JOIN FETCH m.repliedMessage rm " +
                        "LEFT JOIN FETCH rm.sender " +
                        "WHERE m.room.roomId = :roomId " +
                        "ORDER BY m.createdAt DESC", countQuery = "SELECT COUNT(m) FROM Message m WHERE m.room.roomId = :roomId")
        Page<Message> findByRoom_RoomIdOrderByCreatedAtDesc(@Param("roomId") UUID roomId, Pageable pageable);

        // Cuon de xem tin nhan cu - Cursor-based pagination - infinite scroll
        @Query(value = "SELECT m FROM Message m " +
                        "LEFT JOIN FETCH m.sender " +
                        "LEFT JOIN FETCH m.repliedMessage rm " +
                        "LEFT JOIN FETCH rm.sender " +
                        "WHERE m.room.roomId = :roomId " +
                        "AND m.createdAt < (SELECT m2.createdAt FROM Message m2 WHERE m2.messageId = :before) " +
                        "ORDER BY m.createdAt DESC", countQuery = "SELECT COUNT(m) FROM Message m WHERE m.room.roomId = :roomId "
                                        +
                                        "AND m.createdAt < (SELECT m2.createdAt FROM Message m2 WHERE m2.messageId = :before)")
        Page<Message> findByRoomIdBefore(@Param("roomId") UUID roomId, @Param("before") UUID before, Pageable pageable);

        @Query("SELECT m FROM Message m " +
                        "LEFT JOIN FETCH m.sender " +
                        "LEFT JOIN FETCH m.repliedMessage rm " +
                        "LEFT JOIN FETCH rm.sender " +
                        "WHERE m.room.roomId = :roomId " +
                        "AND m.createdAt < :createdAt " +
                        "ORDER BY m.createdAt DESC")
        List<Message> findOlderAround(@Param("roomId") UUID roomId, @Param("createdAt") LocalDateTime createdAt,
                        Pageable pageable);

        @Query("SELECT m FROM Message m " +
                        "LEFT JOIN FETCH m.sender " +
                        "LEFT JOIN FETCH m.repliedMessage rm " +
                        "LEFT JOIN FETCH rm.sender " +
                        "WHERE m.room.roomId = :roomId " +
                        "AND m.createdAt > :createdAt " +
                        "ORDER BY m.createdAt ASC")
        List<Message> findNewerAround(@Param("roomId") UUID roomId, @Param("createdAt") LocalDateTime createdAt,
                        Pageable pageable);

        @Query("SELECT COUNT(m) > 0 FROM Message m WHERE m.room.roomId = :roomId AND m.createdAt < :createdAt")
        boolean existsOlderThan(@Param("roomId") UUID roomId, @Param("createdAt") LocalDateTime createdAt);

        @Query("SELECT m FROM Message m " +
                        "LEFT JOIN FETCH m.sender " +
                        "LEFT JOIN FETCH m.repliedMessage rm " +
                        "LEFT JOIN FETCH rm.sender " +
                        "WHERE m.room.roomId = :roomId " +
                        "AND m.createdAt <= :targetTime " +
                        "ORDER BY m.createdAt DESC")
        List<Message> findOlderContext(@Param("roomId") UUID roomId, @Param("targetTime") LocalDateTime targetTime,
                        Pageable pageable);

        @Query("SELECT m FROM Message m " +
                        "LEFT JOIN FETCH m.sender " +
                        "LEFT JOIN FETCH m.repliedMessage rm " +
                        "LEFT JOIN FETCH rm.sender " +
                        "WHERE m.room.roomId = :roomId " +
                        "AND m.createdAt > :targetTime " +
                        "ORDER BY m.createdAt ASC")
        List<Message> findNewerContext(@Param("roomId") UUID roomId, @Param("targetTime") LocalDateTime targetTime,
                        Pageable pageable);

        // Lấy danh sách tin nhắn MỚI HƠN con trỏ cursorId (sắp xếp tăng dần theo thời
        // gian)
        @Query("SELECT m FROM Message m " +
                        "LEFT JOIN FETCH m.sender " +
                        "LEFT JOIN FETCH m.repliedMessage rm " +
                        "LEFT JOIN FETCH rm.sender " +
                        "WHERE m.room.roomId = :roomId " +
                        "AND m.createdAt > (SELECT c.createdAt FROM Message c WHERE c.messageId = :cursorId) " +
                        "ORDER BY m.createdAt ASC")
        List<Message> findMessagesAfterCursor(@Param("roomId") UUID roomId,
                        @Param("cursorId") UUID cursorId,
                        Pageable pageable);

        // Tim danh sach tin nhan kem thong tin nguoi gui (phuc vu Search Sidebar bên phải)
        @Query("SELECT m FROM Message m " +
                        "LEFT JOIN FETCH m.sender " +
                        "WHERE m.room.roomId = :roomId " +
                        "AND LOWER(m.content) LIKE :pattern " +
                        "AND (m.isRevoked = false OR m.isRevoked IS NULL) " +
                        "ORDER BY m.createdAt DESC")
        List<Message> searchMessagesWithDetails(@Param("roomId") UUID roomId,
                        @Param("pattern") String pattern,
                        Pageable pageable);

        // Lay danh sach tin nhan da ghim trong phong chat
        @Query("SELECT m FROM Message m " +
                        "LEFT JOIN FETCH m.sender " +
                        "LEFT JOIN FETCH m.repliedMessage rm " +
                        "LEFT JOIN FETCH rm.sender " +
                        "WHERE m.room.roomId = :roomId " +
                        "AND m.isPinned = true " +
                        "AND (m.isRevoked = false OR m.isRevoked IS NULL) " +
                        "ORDER BY m.createdAt DESC")
        List<Message> findPinnedMessagesByRoomId(@Param("roomId") UUID roomId);

        @Query("SELECT COUNT(m) FROM Message m " +
                        "WHERE m.room.roomId = :roomId " +
                        "AND m.isPinned = true " +
                        "AND (m.isRevoked = false OR m.isRevoked IS NULL)")
        long countPinnedMessagesByRoomId(@Param("roomId") UUID roomId);

        // Lay danh sach file phuong tien (Anh, Video, Tai lieu) trong phong chat
        @Query("SELECT m FROM Message m " +
                        "LEFT JOIN FETCH m.sender " +
                        "WHERE m.room.roomId = :roomId " +
                        "AND m.messageType IN (com.nhom8.nien_luan_nganh.enums.MessageType.IMAGE, com.nhom8.nien_luan_nganh.enums.MessageType.FILE) " +
                        "AND (m.isRevoked = false OR m.isRevoked IS NULL) " +
                        "ORDER BY m.createdAt DESC")
        List<Message> findMediaMessagesByRoomId(@Param("roomId") UUID roomId);

        // Admin
        // 1. Thống kê số tin nhắn theo khoảng thời gian
        @Query("SELECT COUNT(m) FROM Message m WHERE m.createdAt >= :start AND m.createdAt < :end")
        long countByCreatedAtRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        // 2. Thống kê theo loại tin nhắn (TEXT, IMAGE, FILE...)
        long countByMessageType(MessageType messageType);

        // Đếm số tin nhắn chưa đọc theo từng phòng chat trong 1 câu query gộp.
        // Hỗ trợ trường hợp lastReadMessageId là ID của Message, Poll hoặc Reminder.
        @Query("SELECT m.room.roomId, COUNT(m) " +
                        "FROM Message m, RoomMember rm " +
                        "WHERE rm.room.roomId = m.room.roomId " +
                        "AND rm.roomMemberId.userId = :userId " +
                        "AND m.room.roomId IN :roomIds " +
                        "AND (m.sender IS NULL OR m.sender.userId <> :userId) " +
                        "AND (m.isRevoked = false OR m.isRevoked IS NULL) " +
                        "AND (rm.lastReadMessageId IS NULL OR m.createdAt > COALESCE(" +
                        "    (SELECT mr.createdAt FROM Message mr WHERE mr.messageId = rm.lastReadMessageId)," +
                        "    (SELECT pr.createdAt FROM Poll pr WHERE pr.pollId = rm.lastReadMessageId)," +
                        "    (SELECT rr.createdAt FROM Reminder rr WHERE rr.reminderId = rm.lastReadMessageId)" +
                        ")) " +
                        "GROUP BY m.room.roomId")
        List<Object[]> countUnreadMessagesByRoomIds(@Param("roomIds") List<UUID> roomIds,
                        @Param("userId") UUID userId);

        // xoá tất cả rìa rìa liên quan tới tin nhắn khi 1 nhóm bị xoá
        @Modifying
        @Query("delete from Message m where m.room.roomId = :roomId")
        void deleteAllByRoomId(@Param("roomId") UUID roomId);
}
