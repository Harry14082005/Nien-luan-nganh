package com.nhom8.nien_luan_nganh.entity;

import com.nhom8.nien_luan_nganh.enums.UserRole;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", unique = true, length = 15)
    private String phoneNumber;

    @Column(length = 10)
    private String gender;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(name = "is_banned")
    @Builder.Default
    private Boolean isBanned = false;

    @Column(name = "banned_at")
    private java.time.LocalDateTime bannedAt;

    @Column(name = "created_at", updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }

    // Relationships
    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserRoom> userRooms = new ArrayList<>();

}
