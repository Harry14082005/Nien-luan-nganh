package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.Reminder;
import com.nhom8.nien_luan_nganh.enums.ReminderStatus;
import com.nhom8.nien_luan_nganh.enums.ReminderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

        // 1. Lấy chi tiết lịch hẹn trong phòng
        Optional<Reminder> findByReminderIdAndRoom_RoomId(UUID reminderId, UUID roomId);

        // 2. Lấy danh sách lịch hẹn trong phòng sắp xếp mới nhất
        List<Reminder> findByRoom_RoomIdOrderByCreatedAtDesc(UUID roomId);

        // 3. Dành cho Scheduler quét nhắc hẹn trước 15 phút (Chặn 2 đầu: từ now đến now
        // + 15 phút)
        List<Reminder> findByIsNotifiedFalseAndReminderStatusAndStartAtBetween(
                        ReminderStatus status,
                        LocalDateTime now,
                        LocalDateTime threshold);

        // 4. Tự động tìm các lịch hẹn đã THỰC SỰ kết thúc để chuyển sang DONE
        @Query("SELECT r FROM Reminder r WHERE r.reminderStatus = :status " +
                        "AND COALESCE(r.endAt, r.startAt) < :now")
        List<Reminder> findExpiredReminders(
                        @Param("status") ReminderStatus status,
                        @Param("now") LocalDateTime now);

        // 5. Đếm số lịch hẹn nhóm mới tạo chưa đọc theo từng phòng chat
        @Query("SELECT r.room.roomId, COUNT(r) " +
                        "FROM Reminder r, RoomMember rm " +
                        "WHERE rm.room.roomId = r.room.roomId " +
                        "AND rm.roomMemberId.userId = :userId " +
                        "AND r.room.roomId IN :roomIds " +
                        "AND r.reminderType = com.nhom8.nien_luan_nganh.enums.ReminderType.GROUP " +
                        "AND (r.createdBy IS NULL OR r.createdBy.userId <> :userId) " +
                        "AND r.reminderStatus <> com.nhom8.nien_luan_nganh.enums.ReminderStatus.CANCELLED " +
                        "AND (rm.lastReadMessageId IS NULL OR r.createdAt > COALESCE(" +
                        "    (SELECT mr.createdAt FROM Message mr WHERE mr.messageId = rm.lastReadMessageId)," +
                        "    (SELECT pr.createdAt FROM Poll pr WHERE pr.pollId = rm.lastReadMessageId)," +
                        "    (SELECT rr.createdAt FROM Reminder rr WHERE rr.reminderId = rm.lastReadMessageId)" +
                        ")) " +
                        "GROUP BY r.room.roomId")
        List<Object[]> countUnreadRemindersByRoomIds(
                        @Param("roomIds") List<UUID> roomIds,
                        @Param("userId") UUID userId);
}