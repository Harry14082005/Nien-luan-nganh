package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reminder_resource")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReminderResource {

    @EmbeddedId
    private ReminderResourceId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("messageId")
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("reminderId")
    @JoinColumn(name = "reminder_id", nullable = false)
    private Reminder reminder;

    @PrePersist
    protected void onCreate() {
        if (id == null && message != null && reminder != null) {
            id = new ReminderResourceId(reminder.getReminderId(), message.getMessageId());
        }
    }
}
