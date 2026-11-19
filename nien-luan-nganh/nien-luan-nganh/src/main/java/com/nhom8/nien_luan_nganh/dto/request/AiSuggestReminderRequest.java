package com.nhom8.nien_luan_nganh.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSuggestReminderRequest {
    @NotNull(message = "Message ID không được để trống")
    private UUID messageId;
}

