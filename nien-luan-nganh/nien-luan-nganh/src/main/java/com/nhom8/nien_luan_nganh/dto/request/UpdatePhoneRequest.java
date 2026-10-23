package com.nhom8.nien_luan_nganh.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * DTO nhận dữ liệu đổi số điện thoại từ người dùng.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePhoneRequest {

    @NotBlank(message = "PHONE_REQUIRED")
    @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "PHONE_INVALID")
    private String phoneNumber;
}
