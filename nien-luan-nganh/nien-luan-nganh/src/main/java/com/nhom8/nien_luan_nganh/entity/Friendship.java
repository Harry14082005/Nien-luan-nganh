package com.nhom8.nien_luan_nganh.entity;

import com.nhom8.nien_luan_nganh.enums.FriendshipStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "friendship", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id", "friend_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "friendship_id")
    private UUID friendshipId;

    // User gui loi moi ket ban
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // User nhan loi moi ket ban
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id", nullable = false)
    private User friend;

    // Trang thai ket ban
    @Enumerated(EnumType.STRING)
    @Column(name = "friendship_status", nullable = false)
    private FriendshipStatus friendshipStatus;

    // Thoi gian tao
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
