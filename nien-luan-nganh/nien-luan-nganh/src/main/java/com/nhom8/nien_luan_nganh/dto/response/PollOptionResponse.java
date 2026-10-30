package com.nhom8.nien_luan_nganh.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollOptionResponse {
    private UUID pollOptionId;
    private String optionText;

    private long voteCount;
    private boolean votedByMe;
}
