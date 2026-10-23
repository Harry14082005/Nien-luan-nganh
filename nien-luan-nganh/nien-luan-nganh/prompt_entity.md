# Prompt: Generate Java Spring Boot Entity từ Class Diagram

## Yêu cầu
Hãy generate toàn bộ Java Spring Boot Entity class theo class diagram bên dưới.

## Tech stack
- Java 17+
- Spring Boot 3.x
- Jakarta Persistence (JPA)
- Lombok
- MySQL database
- Package: `com.nhom8.nien_luan_nganh.entity`
- Enum classes đặt trong package: `com.nhom8.nien_luan_nganh.enums`

## Quy tắc bắt buộc

1. **Annotation bắt buộc trên mỗi class:**
   - `@Entity`, `@Table(name = "tên_bảng")`
   - `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`

2. **Primary Key:**
   - Dùng `UUID` cho tất cả PK đơn
   - Dùng `@GeneratedValue(strategy = GenerationType.UUID)`
   - Bảng có **composite key** (2 FK làm PK) thì tạo class `@Embeddable` riêng và dùng `@EmbeddedId` + `@MapsId`

3. **Kiểu dữ liệu:**
   - `UUID` → `java.util.UUID`
   - `String` → `String`
   - `Boolean` → `Boolean`, thêm `@Builder.Default private Boolean field = false`
   - `Date` (timestamp) → `java.time.LocalDateTime`
   - Các field có giá trị cố định (status, type, role) → dùng `enum` riêng

4. **Relationships:**
   - FK đơn: `@ManyToOne(fetch = FetchType.LAZY)` + `@JoinColumn(name = "tên_cột_fk")`
   - Nếu cần navigate ngược: `@OneToMany(mappedBy = "...", cascade = CascadeType.ALL)` + `@Builder.Default private List<X> list = new ArrayList<>()`
   - Self-reference (Message reply): `@ManyToOne` trỏ về chính class đó
   - Friendship có 2 FK cùng trỏ về User: đặt tên khác nhau (`user` và `friend`)

5. **Timestamps:**
   - `createdAt`: thêm `@Column(name = "created_at", updatable = false)` + `@PrePersist`
   - `updatedAt`: thêm `@Column(name = "updated_at")` + `@PreUpdate` với tên method `onUpdate()`
   - `editedAt`: thêm `@PrePersist` với tên method `onEdit()`

6. **Soft delete:**
   - `isRevoked = false` → khi thu hồi: set `content = null`, `isRevoked = true`

7. **Unique constraint:**
   - `email` và `phoneNumber` trong User: thêm `unique = true` trong `@Column`
   - `Translation`: thêm `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "target_language"}))`
   - `PollVote`: thêm `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"poll_id", "user_id"}))`
   - `Friendship`: thêm `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "friend_id"}))`

---

## Class Diagram

### 1. User
**Bảng:** `users`
| Field | Kiểu | Ghi chú |
|---|---|---|
| UserId | UUID | PK |
| Username | String | |
| Password | String | |
| PreferedLanguage | String | |
| UserRole | String | Enum: USER, ADMIN |
| Gender | String | |
| PhoneNumber | String | unique |
| AvatarURL | String | |
| Email | String | unique |
| LastSeenAt | LocalDateTime | |
| IsBanned | Boolean | default false |
| BannedAt | LocalDateTime | nullable |
| Description | String | |
| CreatedAt | LocalDateTime | @PrePersist |
| UpdatedAt | LocalDateTime | @PreUpdate |

---

### 2. Room
**Bảng:** `rooms`
| Field | Kiểu | Ghi chú |
|---|---|---|
| RoomId | UUID | PK |
| RoomName | String | |
| RoomType | String | Enum: GROUP, DIRECT |
| AvatarRoom | String | |
| Theme | String | default "default" |
| CreatedAt | LocalDateTime | @PrePersist |

---

### 3. RoomMember *(Composite Key: RoomId + UserId)*
**Bảng:** `room_members`  
**Composite Key class:** `RoomMemberId`
| Field | Kiểu | Ghi chú |
|---|---|---|
| RoomId | UUID | PK + FK → Room |
| UserId | UUID | PK + FK → User |
| RoomMemberRole | String | Enum: OWNER, ADMIN, MEMBER |
| Status | String | |
| JoinedAt | LocalDateTime | @PrePersist |
| LastDeliveredMessageId | UUID | nullable |
| LastReadMessageId | UUID | nullable |

---

### 4. Friendship
**Bảng:** `friendship`
| Field | Kiểu | Ghi chú |
|---|---|---|
| FriendshipId | UUID | PK |
| UserId | UUID | FK → User (người gửi) |
| FriendId | UUID | FK → User (người nhận) — 2 FK khác nhau cùng trỏ về User |
| FriendshipStatus | String | Enum: PENDING, ACCEPTED, DECLINED, BLOCKED |
| CreatedAt | LocalDateTime | @PrePersist |

---

### 5. Message
**Bảng:** `messages`
| Field | Kiểu | Ghi chú |
|---|---|---|
| MessageId | UUID | PK |
| RoomId | UUID | FK → Room |
| UserId | UUID | FK → User (sender) |
| MessageType | String | Enum: TEXT, IMAGE, FILE |
| Content | String | TEXT, nullable (null khi thu hồi) |
| MessageIdReplied | UUID | FK → Message (self-reference), nullable |
| isEdited | Boolean | default false |
| isRevoked | Boolean | default false |
| RevokedAt | LocalDateTime | nullable |
| isPinned | Boolean | default false |
| CreatedAt | LocalDateTime | @PrePersist |

---

### 6. MessageHistory
**Bảng:** `message_histories`
| Field | Kiểu | Ghi chú |
|---|---|---|
| MessageHistoryId | UUID | PK |
| MessageId | UUID | FK → Message |
| Content | String | TEXT |
| EditedAt | LocalDateTime | @PrePersist, method onEdit() |

---

### 7. MessageReaction *(Composite Key: MessageId + UserId)*
**Bảng:** `message_reactions`  
**Composite Key class:** `MessageReactionId`
| Field | Kiểu | Ghi chú |
|---|---|---|
| MessageId | UUID | PK + FK → Message |
| UserId | UUID | PK + FK → User |
| ReactionType | String | |
| CreatedAt | LocalDateTime | @PrePersist |

---

### 8. Translation
**Bảng:** `translations`  
**UniqueConstraint:** (message_id, target_language)
| Field | Kiểu | Ghi chú |
|---|---|---|
| TransId | UUID | PK |
| MessageId | UUID | FK → Message |
| SourceLanguage | String | |
| TargetLanguage | String | |
| TranslatedText | String | TEXT |
| CreatedAt | LocalDateTime | @PrePersist |

---

### 9. Poll
**Bảng:** `polls`
| Field | Kiểu | Ghi chú |
|---|---|---|
| PollId | UUID | PK |
| RoomId | UUID | FK → Room |
| CreatedBy | UUID | FK → User |
| IsActive | Boolean | default true |
| Question | String | TEXT |
| CreatedAt | LocalDateTime | @PrePersist |

---

### 10. PollOption
**Bảng:** `poll_options`
| Field | Kiểu | Ghi chú |
|---|---|---|
| PollOptionId | UUID | PK |
| PollId | UUID | FK → Poll |
| Option | String | |

---

### 11. PollVote
**Bảng:** `poll_votes`  
**UniqueConstraint:** (poll_id, user_id)
| Field | Kiểu | Ghi chú |
|---|---|---|
| PollVoteId | UUID | PK |
| PollId | UUID | FK → Poll |
| PollOptionId | UUID | FK → PollOption |
| UserId | UUID | FK → User |
| VotedAt | LocalDateTime | @PrePersist |

---

### 12. PollResource *(Composite Key: MessageId + PollId)*
**Bảng:** `poll_resource`  
**Composite Key class:** `PollResourceId`
| Field | Kiểu | Ghi chú |
|---|---|---|
| MessageId | UUID | PK + FK → Message |
| PollId | UUID | PK + FK → Poll |

---

### 13. Reminder
**Bảng:** `reminders`
| Field | Kiểu | Ghi chú |
|---|---|---|
| ReminderId | UUID | PK |
| RoomId | UUID | FK → Room |
| CreatedBy | UUID | FK → User |
| ReminderType | String | Enum: MEETING, DEADLINE, EVENT |
| ReminderStatus | String | Enum: PENDING, DONE, CANCELLED |
| Title | String | |
| Description | String | TEXT, nullable |
| StartAt | LocalDateTime | |
| EndAt | LocalDateTime | nullable |
| CreatedAt | LocalDateTime | @PrePersist |

---

### 14. ReminderParticipant *(Composite Key: UserId + ReminderId)*
**Bảng:** `reminder_participants`  
**Composite Key class:** `ReminderParticipantId`
| Field | Kiểu | Ghi chú |
|---|---|---|
| UserId | UUID | PK + FK → User |
| ReminderId | UUID | PK + FK → Reminder |
| Response | String | Enum: PENDING, ACCEPTED, DECLINED |

---

### 15. ReminderResource *(Composite Key: MessageId + ReminderId)*
**Bảng:** `reminder_resource`  
**Composite Key class:** `ReminderResourceId`
| Field | Kiểu | Ghi chú |
|---|---|---|
| MessageId | UUID | PK + FK → Message |
| ReminderId | UUID | PK + FK → Reminder |

---

### 16. Notification
**Bảng:** `notifications`
| Field | Kiểu | Ghi chú |
|---|---|---|
| NotificationId | UUID | PK |
| UserId | UUID | FK → User |
| NotificationType | String | Enum: NEW_MESSAGE, POLL_CREATED, POLL_ENDED, REMINDER, FRIEND_REQUEST, RSVP_REMINDER |
| ReferenceId | UUID | nullable |
| IsRead | Boolean | default false |
| CreatedAt | LocalDateTime | @PrePersist |

---

## Enum cần tạo
Tạo các enum sau trong package `com.nhom8.nien_luan_nganh.enums`:

```
UserRole        { USER, ADMIN }
RoomType        { GROUP, DIRECT }
RoomRole        { OWNER, ADMIN, MEMBER }
FriendshipStatus { PENDING, ACCEPTED, DECLINED, BLOCKED }
MessageType     { TEXT, IMAGE, FILE }
ReminderType    { MEETING, DEADLINE, EVENT }
ReminderStatus  { PENDING, DONE, CANCELLED }
RsvpResponse    { PENDING, ACCEPTED, DECLINED }
NotificationType { NEW_MESSAGE, POLL_CREATED, POLL_ENDED, REMINDER, FRIEND_REQUEST, RSVP_REMINDER }
```

---

## Ví dụ mẫu để follow

```java
@Entity
@Table(name = "rooms")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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

    @Column(name = "avatar_room")
    private String avatarRoom;

    @Column(length = 50)
    @Builder.Default
    private String theme = "default";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
    @Builder.Default
    private List<RoomMember> roomMembers = new ArrayList<>();
}
```

```java
// Composite Key example
@Embeddable
@Data @NoArgsConstructor @AllArgsConstructor
public class RoomMemberId implements Serializable {
    @Column(name = "room_id")
    private UUID roomId;
    @Column(name = "user_id")
    private UUID userId;
}

@Entity
@Table(name = "room_members")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomMember {

    @EmbeddedId
    private RoomMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roomId")
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RoomRole roomMemberRole = RoomRole.MEMBER;
    ...
}
```

---

## Yêu cầu output
Hãy generate đầy đủ tất cả 16 entity class + các composite key class + 9 enum class.
Mỗi file đặt đúng package, đầy đủ import, không thiếu annotation nào.
