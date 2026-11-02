package com.nhom8.nien_luan_nganh.dto.response;


import com.nhom8.nien_luan_nganh.enums.PollEventType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollEventResponse {
    private PollEventType type;     // "POLL_CREATED", "POLL_VOTED", "POLL_CLOSED", "POLL_OPTION_ADDED"
    private UUID roomId;
    private PollResponse poll;
}
