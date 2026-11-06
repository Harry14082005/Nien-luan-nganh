package com.nhom8.nien_luan_nganh.dto.response;

import com.nhom8.nien_luan_nganh.enums.RoomType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/// phản hồi danh sách phòng chat
public class RoomListResponse {
    private UUID roomId;
    private RoomType roomType;
    private String roomName;
    private String roomAvatarURL;

    // Thông tin đối phương (cho phòng DIRECT) và trạng thái Online
    private UUID peerId; // Friend dang chat trong phong
    private Boolean isOnline;
    private LocalDateTime lastSeenAt; 

    // private String theme;
    // private UUID createdBy;
    // private boolean requireApproval;
    // private LocalDateTime createdAt;

    // phần tin nhắn hiển thị bên ngoài mới nhất
    private UUID lastMessageId;
    private String lastMessageContent;
    private String lastMessageSenderName;
    private LocalDateTime lastMessageAt;

    // Thời điểm cập nhật hoạt động mới nhất của phòng
    private LocalDateTime updatedAt;

    // Trạng thái của thành viên hiện tại
    private UUID lastDeliveredMessageId;
    private UUID lastReadMessageId;

    // Trạng thái hiển thị tin nhắn chưa đọc
    private boolean hasUnread;
    private long unreadCount;
}
