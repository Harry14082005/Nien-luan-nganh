package com.nhom8.nien_luan_nganh.dto.request;


import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LeaveRoomRequest {
    private UUID newAdminId; //có thể null, chỉ sử dụng khi trong nhóm chỉ 1 admin mà rời
}
