package com.nhom8.nien_luan_nganh.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "USERNAME_INVALID")
    private String username;

    private String avatarUrl;

    private String gender;

    private String description; // Tiểu sử (bio)

    private String preferredLanguage; // vi, en,...
}
