package com.nhom8.nien_luan_nganh.entity;

import java.io.Serializable;
import java.util.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ReminderParticipantId implements Serializable {

    @Column(name = "reminder_id")
    private UUID reminderId;

    @Column(name = "user_id")
    private UUID userId;

}
