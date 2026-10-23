package com.nhom8.nien_luan_nganh.entity;

import com.nhom8.nien_luan_nganh.enums.RsvpResponse;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reminder_participants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderParticipant {

    @EmbeddedId
    private ReminderParticipantId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("reminderId")
    @JoinColumn(name = "reminder_id", nullable = false)
    private Reminder reminder;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "response")
    @Builder.Default
    private RsvpResponse response = RsvpResponse.PENDING;

}
