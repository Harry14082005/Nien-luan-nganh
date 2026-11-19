package com.nhom8.nien_luan_nganh.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollVoterResponse {
    private UUID userId;
    private String username;
    private String avatarUrl;
    private LocalDateTime votedAt;
}
