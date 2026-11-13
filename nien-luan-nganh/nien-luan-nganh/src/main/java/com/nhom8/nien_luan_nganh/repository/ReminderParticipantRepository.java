package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.ReminderParticipant;
import com.nhom8.nien_luan_nganh.entity.ReminderParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface ReminderParticipantRepository extends JpaRepository<ReminderParticipant, ReminderParticipantId> {

    // Lay tat ca user tham gia reminder kem thong tin
    @Query("SELECT rp FROM ReminderParticipant rp JOIN FETCH rp.user WHERE rp.id.reminderId = :reminderId")
    List<ReminderParticipant> findByReminderIdWithUser(@Param("reminderId") UUID reminderId);

    // Lấy toàn bộ người tham gia của nhiều lịch hẹn cùng lúc (Tránh N+1)
    @Query("SELECT rp FROM ReminderParticipant rp JOIN FETCH rp.user WHERE rp.id.reminderId IN :reminderIds")
    List<ReminderParticipant> findByReminderIdInWithUser(@Param("reminderIds") List<UUID> reminderIds);

    // 2. Tìm người tham gia theo reminderId và userId - Dòng 201 ReminderService
    Optional<ReminderParticipant> findById_ReminderIdAndId_UserId(UUID reminderId, UUID userId);

}
