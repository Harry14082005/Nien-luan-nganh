package com.nhom8.nien_luan_nganh.repository;

import com.nhom8.nien_luan_nganh.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.actor WHERE n.user.userId = :userId ORDER BY n.createdAt DESC")
    Page<Notification> findByUserIdWithActor(@Param("userId") UUID userId, Pageable pageable);

    long countByUser_UserIdAndIsReadFalse(UUID userId);

    Optional<Notification> findByNotificationIdAndUser_UserId(UUID notificationId, UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") UUID userId);
}

