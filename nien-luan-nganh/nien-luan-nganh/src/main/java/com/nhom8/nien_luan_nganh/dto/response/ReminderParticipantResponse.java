package com.nhom8.nien_luan_nganh.dto.response;

import lombok.*;
import java.util.UUID;

import com.nhom8.nien_luan_nganh.enums.RsvpResponse;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderParticipantResponse {
    private UUID userId;
    private String username;
    private String avatarUrl;
    private RsvpResponse response;

}
