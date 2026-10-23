package com.nhom8.nien_luan_nganh.dto.request;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGroupRequest {
    private String roomName;
    //private String roomType;
    private String roomAvatarURL;
    private List<UUID> memberIds; // danh sách các thành viên sẽ được thêm
}
