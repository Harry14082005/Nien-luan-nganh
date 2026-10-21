package com.nhom8.nien_luan_nganh.entity;

import com.nhom8.nien_luan_nganh.enums.RoomMemberStatus;
import com.nhom8.nien_luan_nganh.enums.RoomRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "room_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomMember {

    @EmbeddedId
    private RoomMemberId roomMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roomId")
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_member_role", nullable = false)
    @Builder.Default
    private RoomRole roomMemberRole = RoomRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RoomMemberStatus status;

    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "last_delivered_message_id")
    private UUID lastDeliveredMessageId;

    @Column(name = "last_read_message_id")
    private UUID lastReadMessageId;

}
