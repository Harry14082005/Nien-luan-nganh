package com.nhom8.nien_luan_nganh.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * DTO nhận dữ liệu chặn người dùng từ Client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockUserRequest {

    @NotNull(message = "USER_ID_REQUIRED")
    private UUID userId;
}
