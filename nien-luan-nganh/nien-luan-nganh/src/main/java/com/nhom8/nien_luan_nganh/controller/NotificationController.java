package com.nhom8.nien_luan_nganh.controller;

import com.nhom8.nien_luan_nganh.dto.response.ApiResponse;
import com.nhom8.nien_luan_nganh.dto.response.NotificationResponse;
import com.nhom8.nien_luan_nganh.dto.response.UnreadNotificationCountResponse;
import com.nhom8.nien_luan_nganh.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Lấy danh sách thông báo của người dùng đăng nhập (phân trang).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID currentUserId = UUID.fromString(auth.getName());
        Page<NotificationResponse> result = notificationService.getNotifications(currentUserId, page, size);
        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách thông báo thành công"));
    }

    /**
     * Lấy số lượng thông báo chưa đọc để hiển thị badge quả chuông.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadNotificationCountResponse>> getUnreadCount(Authentication auth) {
        UUID currentUserId = UUID.fromString(auth.getName());
        long count = notificationService.getUnreadCount(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(new UnreadNotificationCountResponse(count), "Lấy số lượng thông báo chưa đọc thành công"));
    }

    /**
     * Đánh dấu 1 thông báo là đã đọc khi click vào.
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            Authentication auth,
            @PathVariable UUID notificationId) {
        UUID currentUserId = UUID.fromString(auth.getName());
        NotificationResponse response = notificationService.markAsRead(notificationId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã đánh dấu thông báo là đã đọc"));
    }

    /**
     * Đánh dấu tất cả thông báo là đã đọc.
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication auth) {
        UUID currentUserId = UUID.fromString(auth.getName());
        notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã đánh dấu tất cả thông báo là đã đọc"));
    }
}
