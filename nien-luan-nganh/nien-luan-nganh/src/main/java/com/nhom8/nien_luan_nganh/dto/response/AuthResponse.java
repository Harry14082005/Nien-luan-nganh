package com.nhom8.nien_luan_nganh.dto.response;

import lombok.*;

//Data Transfer Object (DTO) trả về sau khi Đăng ký (/auth/register) hoặc Đăng nhập (/auth/login) thành công.
 //Cung cấp bộ token bảo mật (Access Token & Refresh Token) và thông tin cơ bản của người dùng.

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private UserResponse user;

    public static AuthResponse of(String accessToken, String refreshToken, UserResponse user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(user)
                .build();
    }
}
