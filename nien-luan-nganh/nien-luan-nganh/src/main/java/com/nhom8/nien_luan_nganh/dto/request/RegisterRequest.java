package com.nhom8.nien_luan_nganh.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "USERNAME_REQUIRED")
    @Size(min = 2, max = 50, message = "USERNAME_INVALID")
    private String username;

    // 1 ô duy nhất nhận Email HOẶC Số điện thoại
    @NotBlank(message = "IDENTIFIER_REQUIRED")
    private String identifier;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 6, message = "PASSWORD_TOO_SHORT")
    private String password;

    @NotBlank(message = "CONFIRM_PASSWORD_REQUIRED")
    private String confirmPassword;

    private String gender;

    private String preferredLanguage;
}
