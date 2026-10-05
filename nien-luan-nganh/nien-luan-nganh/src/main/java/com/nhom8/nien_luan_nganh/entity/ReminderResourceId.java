package com.nhom8.nien_luan_nganh.entity;

import java.io.Serializable;
import java.util.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReminderResourceId implements Serializable {
    @Column(name = "reminder_id")
    private UUID reminderId;

    @Column(name = "message_id")
    private UUID messageId;

}
