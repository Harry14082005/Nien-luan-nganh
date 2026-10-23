package com.nhom8.nien_luan_nganh.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "IDENTIFIER_REQUIRED")
    private String identifier;

    @NotBlank(message = "PASSWORD_REQUIRED")
    private String password;
}
