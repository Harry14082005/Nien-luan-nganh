package com.nhom8.nien_luan_nganh.dto.response;

import com.nhom8.nien_luan_nganh.enums.ReminderEventType;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderEventResponse {

    private ReminderEventType type;
    private UUID roomId;
    private ReminderResponse reminder;

}
