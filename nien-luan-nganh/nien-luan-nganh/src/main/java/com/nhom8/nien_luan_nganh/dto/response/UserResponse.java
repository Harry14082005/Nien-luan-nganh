package com.nhom8.nien_luan_nganh.dto.response;

import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.enums.UserRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

  //@Data Transfer Object (DTO) chứa thông tin người dùng để trả về cho Client.
  //@Lưu ý bảo mật: Tuyệt đối không chứa trường 'password' nhằm bảo vệ mật khẩu người dùng.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private UUID userId;
    private String username;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    private String gender;
    private String preferredLanguage;
    private UserRole role;
    private String description;
    private Boolean isBanned;
    private LocalDateTime bannedAt;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Giup chuyen Entity User sang UserResponse nhanh
    public static UserResponse fromUser(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .preferredLanguage(user.getPreferredLanguage())
                .role(user.getRole())
                .description(user.getDescription())
                .isBanned(user.getIsBanned())
                .bannedAt(user.getBannedAt())
                .lastSeenAt(user.getLastSeenAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
