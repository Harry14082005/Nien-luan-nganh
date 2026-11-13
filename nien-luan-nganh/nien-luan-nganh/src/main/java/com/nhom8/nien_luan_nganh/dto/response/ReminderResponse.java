package com.nhom8.nien_luan_nganh.dto.response;

import com.nhom8.nien_luan_nganh.enums.ReminderStatus;
import com.nhom8.nien_luan_nganh.enums.ReminderType;
import com.nhom8.nien_luan_nganh.enums.RsvpResponse;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderResponse {

    private UUID reminderId;
    private UUID roomId;
    private UUID messageId; // Tin nhắn gắn liền trên luồng chat
    private String title;
    private String description;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private ReminderType reminderType;
    private ReminderStatus reminderStatus;

    // Thông tin người tạo
    private UUID createdById;
    private String createdByName;
    private String createdByAvatar;
    private LocalDateTime createdAt;

    // Trạng thái phản hồi của người đang đăng nhập
    private RsvpResponse myRsvp;

    // Thống kê số lượng tham gia
    private int totalParticipants;
    private long acceptedCount;
    private long declinedCount;
    private long pendingCount;

    // Danh sách chi tiết người tham gia
    private List<ReminderParticipantResponse> participants;
}