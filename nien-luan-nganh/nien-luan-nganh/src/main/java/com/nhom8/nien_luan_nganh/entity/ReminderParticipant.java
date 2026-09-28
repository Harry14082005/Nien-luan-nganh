package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;


@Entity
@Table(name ="reminder_participants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ReminderParticipant{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reminder_participant_id")
    private UUID reminderParticipantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reminder_id",nullable = false)
    private Reminder reminder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "response")
    private ParticipantResponse response;

    public enum ParticipantResponse {
        PENDING,
        ACCEPTED,
        DECLINED
    }


}

