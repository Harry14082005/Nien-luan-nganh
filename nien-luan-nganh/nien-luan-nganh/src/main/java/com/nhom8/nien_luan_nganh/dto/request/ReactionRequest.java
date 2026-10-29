package com.nhom8.nien_luan_nganh.dto.request;

import com.nhom8.nien_luan_nganh.enums.ReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO yêu cầu thả hoặc cập nhật cảm xúc cho tin nhắn.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionRequest {

    @NotNull(message = "Loại cảm xúc không được để trống")
    private ReactionType reactionType;
}
