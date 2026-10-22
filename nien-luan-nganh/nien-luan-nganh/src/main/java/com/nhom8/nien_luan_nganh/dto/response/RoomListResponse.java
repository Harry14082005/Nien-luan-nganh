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

    //private String theme;
    //private UUID createdBy;
    //private boolean requireApproval;
    //private LocalDateTime createdAt;

    //phần tin nhắn hiển thị bên ngoài mới nhất
    private UUID lastMessageId;
    private String lastMessageContent;
    private String lastMessageSenderName;
    private LocalDateTime lastMessageAt;

    // Trạng thái của thành viên hiện tại
    private UUID lastDeliveredMessageId;
    private UUID lastReadMessageId;
}
