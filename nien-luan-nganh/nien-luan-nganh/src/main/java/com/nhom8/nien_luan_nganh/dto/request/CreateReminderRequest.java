package com.nhom8.nien_luan_nganh.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nhom8.nien_luan_nganh.enums.ReminderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReminderRequest {

    @NotBlank(message = "Tiêu đề lịch hẹn không được để trống")
    @Size(max = 255, message = "Tiêu đề lịch hẹn tối đa 255 ký tự")
    private String title;

    private String description;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endAt;

    @NotNull(message = "Loại lịch hẹn không được để trống (PERSONAL hoặc GROUP)")
    private ReminderType reminderType;

    /**
     * Danh sách userId được mời tham gia (chỉ dùng cho lịch nhóm GROUP).
     * Nếu để trống (null hoặc rỗng), hệ thống sẽ tự động thêm toàn bộ thành viên
     * đang hoạt động trong phòng chat.
     */
    private List<UUID> participantIds;

}
