package com.nhom8.nien_luan_nganh.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnreadNotificationCountResponse {
    private long unreadCount;
}
