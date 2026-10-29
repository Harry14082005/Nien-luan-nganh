package com.nhom8.nien_luan_nganh.entity;

import com.nhom8.nien_luan_nganh.enums.RoomType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "room_name", length = 100)
    private String roomName;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    private RoomType roomType;

    @Column(name = "room_avatar_url")
    private String roomAvatarURL;

    @Column(length = 50)
    @Builder.Default
    private String theme = "default";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false, nullable = true)
    private User createdBy;

    @Column(name = "require_approval")
    private boolean requireApproval; // true = cần duyệt, false = không cần duyêt

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

//    // Tin nhắn mới nhất (hiển thị ở danh sách phòng)
//    @Column(name = "last_message_id")
//    private UUID lastMessageId;
//
//    @Column(name = "last_message_content", length = 500)
//    private String lastMessageContent;
//
//    @Column(name = "last_message_sender_id")
//    private UUID lastMessageSenderId;
//
//    @Column(name = "last_message_at")
//    private LocalDateTime lastMessageAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Relationships
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    @Builder.Default
    private List<RoomMember> roomMembers = new ArrayList<>();
}
