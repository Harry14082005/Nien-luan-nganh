package com.nhom8.nien_luan_nganh.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    @NotBlank(message = "PASSWORD_REQUIRED")
    private String oldPassword;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 6, message = "PASSWORD_TOO_SHORT")
    private String newPassword;

    @NotBlank(message = "CONFIRM_PASSWORD_REQUIRED")
    private String confirmNewPassword;
}
