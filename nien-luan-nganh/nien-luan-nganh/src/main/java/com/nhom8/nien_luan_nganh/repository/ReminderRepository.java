package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    // Đếm số lịch hẹn (Reminder) mới tạo chưa đọc theo từng phòng chat trong 1 query gộp
    @Query("SELECT r.room.roomId, COUNT(r) " +
            "FROM Reminder r, RoomMember rm " +
            "WHERE rm.room.roomId = r.room.roomId " +
            "AND rm.roomMemberId.userId = :userId " +
            "AND r.room.roomId IN :roomIds " +
            "AND (r.createdBy IS NULL OR r.createdBy.userId <> :userId) " +
            "AND (rm.lastReadMessageId IS NULL OR r.createdAt > COALESCE(" +
            "    (SELECT mr.createdAt FROM Message mr WHERE mr.messageId = rm.lastReadMessageId)," +
            "    (SELECT pr.createdAt FROM Poll pr WHERE pr.pollId = rm.lastReadMessageId)," +
            "    (SELECT rr.createdAt FROM Reminder rr WHERE rr.reminderId = rm.lastReadMessageId)" +
            ")) " +
            "GROUP BY r.room.roomId")
    List<Object[]> countUnreadRemindersByRoomIds(@Param("roomIds") List<UUID> roomIds,
            @Param("userId") UUID userId);
}
