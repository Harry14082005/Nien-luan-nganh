package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.*;
import lombok.*;
import com.nhom8.nien_luan_nganh.enums.ParticipantResponse;

@Entity
@Table(name = "reminder_participants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ReminderParticipant {

    @EmbeddedId
    private ReminerParticipantId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("reminderId")
    @JoinColumn(name = "reminder_id")
    private Reminder reminder;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "response")
    private ParticipantResponse response;

}
