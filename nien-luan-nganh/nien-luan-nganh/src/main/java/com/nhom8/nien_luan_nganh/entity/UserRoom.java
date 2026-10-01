package com.nhom8.nien_luan_nganh.entity;

import com.nhom8.nien_luan_nganh.enums.RoomRole;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_room", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "room_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoom {

    @EmbeddedId
    private UserRoomId userRoomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RoomRole role = RoomRole.MEMBER;

    @Column(name = "joined_at", updatable = false)
    private java.time.LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = java.time.LocalDateTime.now();
    }
}