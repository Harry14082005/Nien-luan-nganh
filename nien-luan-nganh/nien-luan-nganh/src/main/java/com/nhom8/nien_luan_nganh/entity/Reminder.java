package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;
import java.time.LocalDateTime;

@Entity
@Table(name="reminders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Reminder{
    public enum ReminderType {
        PERSONAL,
        GROUP,
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name= "reminder_id")
    private UUID reminderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "created_by",nullable = false)
    private User creator;

    @Column(name= "title",nullable = false)
    private String title;

    @Column(name= "description")
    private String description;

    @Column(name = "reminder_type")
    private ReminderType reminderType;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name ="created_at",nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
    }


}