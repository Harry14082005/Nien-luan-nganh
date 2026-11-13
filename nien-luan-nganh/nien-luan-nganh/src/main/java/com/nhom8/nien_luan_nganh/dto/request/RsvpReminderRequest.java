package com.nhom8.nien_luan_nganh.dto.request;

import com.nhom8.nien_luan_nganh.enums.RsvpResponse;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RsvpReminderRequest {

    @NotNull(message = "Phản hồi không được để trống (ACCEPTED hoặc DECLINED)")
    private RsvpResponse response;
}