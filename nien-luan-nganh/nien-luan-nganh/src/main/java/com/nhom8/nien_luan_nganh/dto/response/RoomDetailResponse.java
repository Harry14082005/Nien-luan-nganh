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
///trả về khi được mở một phòng
public class RoomDetailResponse {
    private UUID roomId;
    private RoomType roomType;
    private String theme;
    private String roomName;
    private String roomAvatarURL;
    private UUID createdBy;
    private boolean requireApproval;
    private LocalDateTime createdAt;
}
