package com.nhom8.nien_luan_nganh.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO yêu cầu chỉnh sửa nội dung tin nhắn.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMessageRequest {

    @NotBlank(message = "MESSAGE_CONTENT_REQUIRED")
    private String content;
}
