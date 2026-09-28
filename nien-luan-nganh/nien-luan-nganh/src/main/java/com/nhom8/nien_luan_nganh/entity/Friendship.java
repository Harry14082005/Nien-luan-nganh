package com.nhom8.nien_luan_nganh.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name="friendships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Friendship{

    public enum FriendshipStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        BLOCKED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="friendship_id")
    private UUID friendshipID;

    //User gui loi moi ket ban
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // User nhan loi moi ket ban
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id", nullable = false)
    private User friend;

    // Trang thai ket ban
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FriendshipStatus status;

    // Thoi gian tao
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    //Gan thoi gian tao truoc khi luu vao CSDL
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}

