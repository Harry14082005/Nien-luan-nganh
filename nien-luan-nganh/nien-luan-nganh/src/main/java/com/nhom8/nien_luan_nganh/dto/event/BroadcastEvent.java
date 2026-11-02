package com.nhom8.nien_luan_nganh.dto.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event dùng chung cho mọi tính năng Realtime (Poll, Reminder,
 * Notification,...).
 * Mang theo destination (kênh /topic/...) và payload cần gửi tới client.
 */
@Getter
@AllArgsConstructor
public class BroadcastEvent {
    private final String destination;
    private final Object payload;

}
