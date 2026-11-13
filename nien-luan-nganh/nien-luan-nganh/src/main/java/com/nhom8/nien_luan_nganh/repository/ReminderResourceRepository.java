package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.ReminderResource;
import com.nhom8.nien_luan_nganh.entity.ReminderResourceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReminderResourceRepository extends JpaRepository<ReminderResource, ReminderResourceId> {

    // Tìm liên kết tin nhắn theo reminderId
    Optional<ReminderResource> findByReminder_ReminderId(UUID reminderId);

    // Lấy toàn bộ liên kết tin nhắn của nhiều lịch hẹn cùng lúc (Tránh N+1)
    @Query("SELECT rr FROM ReminderResource rr WHERE rr.reminder.reminderId IN :reminderIds")
    List<ReminderResource> findByReminder_ReminderIdIn(@Param("reminderIds") List<UUID> reminderIds);

    // Tìm liên kết lịch hẹn theo messageId
    Optional<ReminderResource> findByMessage_MessageId(UUID messageId);
}