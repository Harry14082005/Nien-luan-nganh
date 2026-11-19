package com.nhom8.nien_luan_nganh.dto.response;

import com.nhom8.nien_luan_nganh.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private UUID notificationId;
    private NotificationType notificationType;
    private String title;
    private String content;
    private UUID roomId;
    private UUID referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;

    // Thông tin người kích hoạt (actor)
    private UUID actorId;
    private String actorUsername;
    private String actorAvatarUrl;
}
