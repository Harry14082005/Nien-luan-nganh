package com.nhom8.nien_luan_nganh.enums;

public enum NotificationType {
    // 1. Bạn bè (Friendship)
    FRIEND_REQUEST,
    FRIEND_ACCEPTED,

    // 2. Nhóm chat & Thành viên (Rooms & Members)
    ROOM_ADDED,
    ROOM_JOIN_REQUEST,
    ROOM_JOIN_APPROVED,
    ROOM_JOIN_REJECTED,
    ROOM_REMOVED,
    ROOM_ROLE_PROMOTED,

    // 3. Lịch hẹn & Nhắc nhở (Reminders)
    REMINDER_INVITE,
    REMINDER_DUE,
    REMINDER_UPDATED,
    REMINDER_RSVP,
    REMINDER_CANCELLED,

    // 4. Bình chọn (Polls)
    POLL_CREATED,
    POLL_CLOSED
}
