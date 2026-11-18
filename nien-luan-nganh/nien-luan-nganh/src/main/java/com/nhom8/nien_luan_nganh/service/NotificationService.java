package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.response.NotificationResponse;
import com.nhom8.nien_luan_nganh.entity.Notification;
import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.enums.NotificationType;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.mapper.NotificationMapper;
import com.nhom8.nien_luan_nganh.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Tạo và gửi thông báo cho một người dùng cụ thể.
     * Lưu vào Database đồng thời đẩy realtime qua WebSocket: /user/queue/notifications
     * Tách riêng giao dịch (REQUIRES_NEW) để lỗi thông báo không ảnh hưởng tới giao dịch nghiệp vụ chính.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationResponse createAndSendNotification(
            User recipient,
            User actor,
            NotificationType type,
            String title,
            String content,
            UUID roomId,
            UUID referenceId) {

        if (recipient == null) {
            return null;
        }

        // Chống tự gửi thông báo cho chính mình
        if (actor != null && recipient.getUserId().equals(actor.getUserId())) {
            return null;
        }

        Notification notification = Notification.builder()
                .user(recipient)
                .actor(actor)
                .notificationType(type)
                .title(title)
                .content(content)
                .roomId(roomId)
                .referenceId(referenceId)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = notificationMapper.toNotificationResponse(saved);

        // Đẩy realtime qua WebSocket riêng tư tới người nhận
        try {
            messagingTemplate.convertAndSendToUser(
                    recipient.getUserId().toString(),
                    "/queue/notifications",
                    response
            );
            log.info("🔔 [WebSocket Notification] Gửi thành công tới user {} (type: {})", recipient.getUserId(), type);
        } catch (Exception e) {
            log.error("❌ Lỗi gửi WebSocket Notification tới user {}: {}", recipient.getUserId(), e.getMessage());
        }

        return response;
    }

    /**
     * Lấy danh sách thông báo của người dùng theo phân trang (sắp xếp mới nhất trước).
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<Notification> notifPage = notificationRepository.findByUserIdWithActor(userId, pageable);
        return notifPage.map(notificationMapper::toNotificationResponse);
    }

    /**
     * Đếm số lượng thông báo chưa đọc.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUser_UserIdAndIsReadFalse(userId);
    }

    /**
     * Đánh dấu 1 thông báo là đã đọc.
     */
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository
                .findByNotificationIdAndUser_UserId(notificationId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (Boolean.FALSE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification = notificationRepository.save(notification);
        }

        return notificationMapper.toNotificationResponse(notification);
    }

    /**
     * Đánh dấu tất cả thông báo của người dùng là đã đọc.
     */
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}
