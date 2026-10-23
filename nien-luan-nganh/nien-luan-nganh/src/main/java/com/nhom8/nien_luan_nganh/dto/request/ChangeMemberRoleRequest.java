package com.nhom8.nien_luan_nganh.dto.request;

import com.nhom8.nien_luan_nganh.enums.RoomRole;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeMemberRoleRequest {
    @NonNull
    private RoomRole role;
}
