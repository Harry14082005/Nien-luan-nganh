package com.nhom8.nien_luan_nganh.dto.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PollBroadcastEvent {
    private final String destination;
    private final Object payload;

}
