package com.nhom8.nien_luan_nganh.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollResponse {
    private UUID pollId;
    private UUID roomId;
    private UUID messageId;
    private String question;
    private UUID createdById;
    private String createdByName;
    private Boolean isActive;
    private LocalDateTime createdAt;

    private long totalVoters;
    private List<PollOptionResponse> options;
}
