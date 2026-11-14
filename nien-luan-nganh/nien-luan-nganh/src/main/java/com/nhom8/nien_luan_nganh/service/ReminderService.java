
package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.event.BroadcastEvent;
import com.nhom8.nien_luan_nganh.dto.request.CreateReminderRequest;
import com.nhom8.nien_luan_nganh.dto.request.RsvpReminderRequest;
import com.nhom8.nien_luan_nganh.dto.response.MessageResponse;
import com.nhom8.nien_luan_nganh.dto.response.ReminderEventResponse;
import com.nhom8.nien_luan_nganh.dto.response.ReminderParticipantResponse;
import com.nhom8.nien_luan_nganh.dto.response.ReminderResponse;
import com.nhom8.nien_luan_nganh.entity.*;
import com.nhom8.nien_luan_nganh.enums.*;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.mapper.UserMapper;
import com.nhom8.nien_luan_nganh.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final ReminderParticipantRepository reminderParticipantRepository;
    private final ReminderResourceRepository reminderResourceRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageRepository messageRepository;
    private final UserMapper userMapper;
    private final RoomService roomService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;

    /**
     * Tạo lịch hẹn cá nhân (PERSONAL) hoặc lịch hẹn nhóm (GROUP)
     */
    @Transactional
    public ReminderResponse createReminder(UUID roomId, CreateReminderRequest request, UUID currentUserId) {
        Room room = getRoomOrThrow(roomId);
        RoomMember actor = getActiveMemberOrThrow(roomId, currentUserId);

        // 1. Kiểm tra thời gian bắt đầu & ket thuc
        if (request.getStartAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.START_TIME_IN_PAST);
        }
        if (request.getEndAt() != null && !request.getEndAt().isAfter(request.getStartAt())) {
            throw new AppException(ErrorCode.INVALID_REMINDER_TIME);
        }

        // 2. Bước 1: Lưu Reminder trước để sinh ra reminderId chuẩn xác (Tránh lỗi null
        // ID)
        Reminder reminder = Reminder.builder()
                .room(room)
                .createdBy(actor.getUser())
                .reminderType(request.getReminderType())
                .reminderStatus(ReminderStatus.PENDING)
                .isNotified(false)
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .build();

        reminder = reminderRepository.save(reminder);

        // 3. Bước 2: Tạo danh sách người tham gia với reminderId đã có
        List<ReminderParticipant> participantsToSave = new ArrayList<>();

        if (request.getReminderType() == ReminderType.PERSONAL) {
            // Lịch cá nhân: chỉ người tạo tham gia (ACCEPTED)
            ReminderParticipant creatorParticipant = ReminderParticipant.builder()
                    .id(new ReminderParticipantId(reminder.getReminderId(), actor.getUser().getUserId()))
                    .reminder(reminder)
                    .user(actor.getUser())
                    .response(RsvpResponse.ACCEPTED)
                    .build();
            participantsToSave.add(creatorParticipant);
        } else {
            // Lịch nhóm: tập hợp danh sách nhung nguoi duoc moi Reminder
            Map<UUID, User> targetUsers = new HashMap<>();

            if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
                List<RoomMember> activeMembers = roomMemberRepository.findMembersByRoomId(roomId);

                Set<UUID> activeMemberIds = new HashSet<>();
                Map<UUID, User> memberUserMap = new HashMap<>();
                for (RoomMember rm : activeMembers) {
                    if (rm.getStatus() == RoomMemberStatus.ACTIVE) {
                        activeMemberIds.add(rm.getUser().getUserId());
                        memberUserMap.put(rm.getUser().getUserId(), rm.getUser());
                    }
                }
                // Moi dich danh user nao
                for (UUID targetId : request.getParticipantIds()) {
                    if (activeMemberIds.contains(targetId)) {
                        targetUsers.put(targetId, memberUserMap.get(targetId));
                    }
                }
            } else {
                List<RoomMember> activeMembers = roomMemberRepository.findMembersByRoomId(roomId);
                for (RoomMember rm : activeMembers) {
                    if (rm.getStatus() == RoomMemberStatus.ACTIVE) {
                        targetUsers.put(rm.getUser().getUserId(), rm.getUser());
                    }
                }
            }

            // Đảm bảo người tạo luôn có mặt trong reminder
            targetUsers.put(actor.getUser().getUserId(), actor.getUser());

            for (User user : targetUsers.values()) {
                boolean isCreator = user.getUserId().equals(actor.getUser().getUserId());
                ReminderParticipant p = ReminderParticipant.builder()
                        .id(new ReminderParticipantId(reminder.getReminderId(), user.getUserId()))
                        .reminder(reminder)
                        .user(user)
                        .response(isCreator ? RsvpResponse.ACCEPTED : RsvpResponse.PENDING)
                        .build();
                participantsToSave.add(p);
            }
        }

        List<ReminderParticipant> savedParticipants = reminderParticipantRepository.saveAll(participantsToSave);

        // 4. Tạo tin nhắn đại diện trên dòng chat
        Message message = Message.builder()
                .room(room)
                .sender(actor.getUser())
                .messageType(MessageType.REMINDER)
                .content(reminder.getTitle())
                .build();
        message = messageRepository.save(message);

        // 5. Lưu liên kết ReminderResource
        ReminderResource resource = ReminderResource.builder()
                .reminderResourceId(new ReminderResourceId(reminder.getReminderId(), message.getMessageId()))
                .reminder(reminder)
                .message(message)
                .build();
        reminderResourceRepository.save(resource);

        // 6. Cập nhật hoạt động mới nhất của phòng chat
        String previewContent = (reminder.getReminderType() == ReminderType.PERSONAL ? "[Lịch cá nhân] "
                : "[Lịch hẹn] ")
                + preview(reminder.getTitle());
        roomService.updateLastActivity(room, message.getMessageId(), previewContent, actor.getUser(),
                message.getCreatedAt());

        // 7. Dựng Response trực tiếp từ dữ liệu vừa lưu (Tránh xung đột Persistence
        // Context & đỡ tốn thêm câu query SQL)
        ReminderResponse reminderResponse = buildResponseFromData(reminder, savedParticipants, message.getMessageId(),
                currentUserId);

        // 8. Broadcast Realtime qua WebSocket STOMP
        // Kênh dòng chat
        MessageResponse messageResponse = buildReminderMessageResponse(message, reminder.getReminderId());
        eventPublisher.publishEvent(new BroadcastEvent("/topic/room." + roomId, messageResponse));

        // Kênh sự kiện Lịch hẹn (cho Widget / Drawer)
        eventPublisher.publishEvent(new BroadcastEvent(
                "/topic/room." + roomId + ".reminder",
                ReminderEventResponse.builder()
                        .type(ReminderEventType.REMINDER_CREATED)
                        .roomId(roomId)
                        .reminder(reminderResponse)
                        .build()));

        return reminderResponse;
    }

    /**
     * Phản hồi tham gia lịch hẹn (RSVP: ACCEPTED / DECLINED)
     */
    @Transactional
    public ReminderResponse respondRsvp(UUID roomId, UUID reminderId, RsvpReminderRequest request, UUID currentUserId) {
        getRoomOrThrow(roomId);
        getActiveMemberOrThrow(roomId, currentUserId);

        Reminder reminder = reminderRepository.findByReminderIdAndRoom_RoomId(reminderId, roomId)
                .orElseThrow(() -> new AppException(ErrorCode.REMINDER_NOT_FOUND));

        // Kiểm tra mốc hết hạn của lịch hẹn (ưu tiên endAt, nếu không có lấy startAt)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = reminder.getEndAt() != null ? reminder.getEndAt() : reminder.getStartAt();

        if (reminder.getReminderStatus() == ReminderStatus.CANCELLED) {
            throw new AppException(ErrorCode.REMINDER_ALREADY_CANCELLED);
        }
        // Nếu lịch đã DONE hoặc đã trôi qua deadline -> Tự động chuyển sang DONE, phát
        // WebSocket và chặn không cho RSVP
        if (reminder.getReminderStatus() == ReminderStatus.DONE
                || (reminder.getReminderStatus() == ReminderStatus.PENDING && deadline.isBefore(now))) {
            if (reminder.getReminderStatus() == ReminderStatus.PENDING) {
                reminder.setReminderStatus(ReminderStatus.DONE);
                reminderRepository.save(reminder);

                // Broadcast sự kiện cập nhật trạng thái DONE tới phòng chat qua WebSocket
                eventPublisher.publishEvent(new BroadcastEvent(
                        "/topic/room." + roomId + ".reminder",
                        ReminderEventResponse.builder()
                                .type(ReminderEventType.REMINDER_UPDATED)
                                .roomId(roomId)
                                .reminder(toResponse(reminder, null, currentUserId))
                                .build()));
            }
            throw new AppException(ErrorCode.REMINDER_ALREADY_COMPLETED);
        }

        // Chặn không cho người khác phản hồi lịch cá nhân
        if (reminder.getReminderType() == ReminderType.PERSONAL
                && !reminder.getCreatedBy().getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Lấy hoặc thêm mới người tham gia nếu mới vào nhóm
        ReminderParticipant participant = reminderParticipantRepository
                .findById_ReminderIdAndId_UserId(reminderId, currentUserId)
                .orElseGet(() -> {
                    User currentUser = userRepository.findById(currentUserId)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
                    return ReminderParticipant.builder()
                            .id(new ReminderParticipantId(reminderId, currentUserId))
                            .reminder(reminder)
                            .user(currentUser)
                            .build();
                });

        participant.setResponse(request.getResponse());
        reminderParticipantRepository.save(participant);

        ReminderResponse response = toResponse(reminder, null, currentUserId);

        // Broadcast realtime cập nhật RSVP tới phòng
        eventPublisher.publishEvent(new BroadcastEvent(
                "/topic/room." + roomId + ".reminder",
                ReminderEventResponse.builder()
                        .type(ReminderEventType.REMINDER_RSVP)
                        .roomId(roomId)
                        .reminder(response)
                        .build()));

        return response;
    }

    /**
     * Lấy danh sách tất cả lịch hẹn trong phòng chat (cho Drawer/Widget) - Tránh
     * lỗi N+1
     */
    @Transactional
    public List<ReminderResponse> getRoomReminders(UUID roomId, UUID currentUserId) {
        getRoomOrThrow(roomId);
        getActiveMemberOrThrow(roomId, currentUserId);

        List<Reminder> reminders = reminderRepository.findByRoom_RoomIdOrderByCreatedAtDesc(roomId);
        if (reminders.isEmpty()) {
            return Collections.emptyList();
        }

        // Lazy check - Tự động cập nhật DONE cho các lịch hẹn đã quá hạn ngay khi truy
        // vấn danh sách
        LocalDateTime now = LocalDateTime.now();
        for (Reminder r : reminders) {
            if (r.getReminderStatus() == ReminderStatus.PENDING) {
                LocalDateTime deadline = r.getEndAt() != null ? r.getEndAt() : r.getStartAt();
                if (deadline.isBefore(now)) {
                    r.setReminderStatus(ReminderStatus.DONE);
                    reminderRepository.save(r);
                }
            }
        }

        List<UUID> reminderIds = reminders.stream().map(Reminder::getReminderId).toList();

        // Gom toàn bộ Participants trong 1 câu SQL
        List<ReminderParticipant> allParticipants = reminderParticipantRepository
                .findByReminderIdInWithUser(reminderIds);
        // Tao Map tren RAM de phan loai
        Map<UUID, List<ReminderParticipant>> participantsByReminderId = new HashMap<>();

        for (ReminderParticipant p : allParticipants) {
            // computeIfAbsent: Neu trong Map chua cos gio danh sach cho reminder nay thi
            // tao moi 1 ArrayList rong sau do bo nguoi nay vao o tuong ung
            participantsByReminderId.computeIfAbsent(p.getId().getReminderId(), k -> new ArrayList<>()).add(p);
        }

        // Gom toàn bộ MessageId liên kết trong 1 câu SQL (tranh N+1)
        List<ReminderResource> allResources = reminderResourceRepository.findByReminder_ReminderIdIn(reminderIds);
        Map<UUID, UUID> messageIdByReminderId = new HashMap<>();

        for (ReminderResource res : allResources) {
            // đề phòng reminder hong gắn với message nào
            if (res.getMessage() != null) {
                messageIdByReminderId.put(res.getReminder().getReminderId(), res.getMessage().getMessageId());
            }
        }

        // Ghép dữ liệu trên RAM
        // Map tren RAM giup cai thien truy xuat O(1), lay nhanh
        return reminders.stream().map(r -> {
            List<ReminderParticipant> participants = participantsByReminderId.getOrDefault(r.getReminderId(),
                    Collections.emptyList());
            UUID msgId = messageIdByReminderId.get(r.getReminderId());
            return buildResponseFromData(r, participants, msgId, currentUserId);
        }).toList();
    }

    /**
     * Xem chi tiết 1 lịch hẹn cụ thể
     */
    @Transactional(readOnly = true)
    public ReminderResponse getReminderDetail(UUID roomId, UUID reminderId, UUID currentUserId) {
        getRoomOrThrow(roomId);
        getActiveMemberOrThrow(roomId, currentUserId);

        Reminder reminder = reminderRepository.findByReminderIdAndRoom_RoomId(reminderId, roomId)
                .orElseThrow(() -> new AppException(ErrorCode.REMINDER_NOT_FOUND));

        return toResponse(reminder, null, currentUserId);
    }

    /**
     * Hủy lịch hẹn (Chỉ Người tạo hoặc Trưởng nhóm/Phó nhóm mới có quyền)
     */
    @Transactional
    public ReminderResponse cancelReminder(UUID roomId, UUID reminderId, UUID currentUserId) {
        getRoomOrThrow(roomId);
        RoomMember actor = getActiveMemberOrThrow(roomId, currentUserId);

        Reminder reminder = reminderRepository.findByReminderIdAndRoom_RoomId(reminderId, roomId)
                .orElseThrow(() -> new AppException(ErrorCode.REMINDER_NOT_FOUND));

        // Kiểm tra mốc hết hạn của lịch hẹn (ưu tiên endAt, nếu không có lấy startAt)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = reminder.getEndAt() != null ? reminder.getEndAt() : reminder.getStartAt();

        if (reminder.getReminderStatus() == ReminderStatus.CANCELLED) {
            throw new AppException(ErrorCode.REMINDER_ALREADY_CANCELLED);
        }
        // Nếu lịch đã DONE hoặc đã trôi qua deadline -> Tự động chuyển sang DONE, phát
        // WebSocket và chặn không cho RSVP
        if (reminder.getReminderStatus() == ReminderStatus.DONE
                || (reminder.getReminderStatus() == ReminderStatus.PENDING && deadline.isBefore(now))) {
            if (reminder.getReminderStatus() == ReminderStatus.PENDING) {
                reminder.setReminderStatus(ReminderStatus.DONE);
                reminderRepository.save(reminder);

                // Broadcast sự kiện cập nhật trạng thái DONE tới phòng chat qua WebSocket
                eventPublisher.publishEvent(new BroadcastEvent(
                        "/topic/room." + roomId + ".reminder",
                        ReminderEventResponse.builder()
                                .type(ReminderEventType.REMINDER_UPDATED)
                                .roomId(roomId)
                                .reminder(toResponse(reminder, null, currentUserId))
                                .build()));
            }
            throw new AppException(ErrorCode.REMINDER_ALREADY_COMPLETED);
        }

        boolean isCreator = reminder.getCreatedBy().getUserId().equals(currentUserId);
        boolean isAdmin = actor.getRoomMemberRole() == RoomRole.ADMIN;

        if (!isCreator && !isAdmin) {
            throw new AppException(ErrorCode.CANNOT_CANCEL_REMINDER);
        }

        reminder.setReminderStatus(ReminderStatus.CANCELLED);
        reminderRepository.save(reminder);

        ReminderResponse response = toResponse(reminder, null, currentUserId);

        // Broadcast sự kiện hủy lịch hẹn
        eventPublisher.publishEvent(new BroadcastEvent(
                "/topic/room." + roomId + ".reminder",
                ReminderEventResponse.builder()
                        .type(ReminderEventType.REMINDER_CANCELLED)
                        .roomId(roomId)
                        .reminder(response)
                        .build()));

        return response;
    }

    private ReminderResponse buildResponseFromData(Reminder reminder, List<ReminderParticipant> participants,
            UUID messageId, UUID currentUserId) {
        long acceptedCount = 0;
        long declinedCount = 0;
        long pendingCount = 0;
        RsvpResponse myRsvp = null;

        List<ReminderParticipantResponse> participantResponses = new ArrayList<>();
        for (ReminderParticipant p : participants) {
            User u = p.getUser();
            if (p.getResponse() == RsvpResponse.ACCEPTED)
                acceptedCount++;
            else if (p.getResponse() == RsvpResponse.DECLINED)
                declinedCount++;
            else
                pendingCount++;

            if (u.getUserId().equals(currentUserId)) {
                myRsvp = p.getResponse();
            }

            participantResponses.add(ReminderParticipantResponse.builder()
                    .userId(u.getUserId())
                    .username(u.getUsername())
                    .avatarUrl(u.getAvatarUrl())
                    .response(p.getResponse())
                    .build());
        }

        return ReminderResponse.builder()
                .reminderId(reminder.getReminderId())
                .roomId(reminder.getRoom().getRoomId())
                .messageId(messageId)
                .title(reminder.getTitle())
                .description(reminder.getDescription())
                .startAt(reminder.getStartAt())
                .endAt(reminder.getEndAt())
                .reminderType(reminder.getReminderType())
                .reminderStatus(reminder.getReminderStatus())
                .createdById(reminder.getCreatedBy().getUserId())
                .createdByName(reminder.getCreatedBy().getUsername())
                .createdByAvatar(reminder.getCreatedBy().getAvatarUrl())
                .createdAt(reminder.getCreatedAt())
                .myRsvp(myRsvp)
                .totalParticipants(participants.size())
                .acceptedCount(acceptedCount)
                .declinedCount(declinedCount)
                .pendingCount(pendingCount)
                .participants(participantResponses)
                .build();
    }

    /**
     * Chuyển đổi Entity sang Response DTO kèm thống kê RSVP
     */
    public ReminderResponse toResponse(Reminder reminder, UUID messageId, UUID currentUserId) {
        List<ReminderParticipant> participants = reminderParticipantRepository
                .findByReminderIdWithUser(reminder.getReminderId());

        long acceptedCount = 0;
        long declinedCount = 0;
        long pendingCount = 0;
        RsvpResponse myRsvp = null;

        List<ReminderParticipantResponse> participantResponses = new ArrayList<>();
        for (ReminderParticipant p : participants) {
            User u = p.getUser();
            if (p.getResponse() == RsvpResponse.ACCEPTED)
                acceptedCount++;
            else if (p.getResponse() == RsvpResponse.DECLINED)
                declinedCount++;
            else
                pendingCount++;

            if (u.getUserId().equals(currentUserId)) {
                myRsvp = p.getResponse();
            }

            participantResponses.add(ReminderParticipantResponse.builder()
                    .userId(u.getUserId())
                    .username(u.getUsername())
                    .avatarUrl(u.getAvatarUrl())
                    .response(p.getResponse())
                    .build());
        }

        if (messageId == null) {
            messageId = reminderResourceRepository.findByReminder_ReminderId(reminder.getReminderId())
                    .map(rr -> rr.getMessage().getMessageId())
                    .orElse(null);
        }

        return ReminderResponse.builder()
                .reminderId(reminder.getReminderId())
                .roomId(reminder.getRoom().getRoomId())
                .messageId(messageId)
                .title(reminder.getTitle())
                .description(reminder.getDescription())
                .startAt(reminder.getStartAt())
                .endAt(reminder.getEndAt())
                .reminderType(reminder.getReminderType())
                .reminderStatus(reminder.getReminderStatus())
                .createdById(reminder.getCreatedBy().getUserId())
                .createdByName(reminder.getCreatedBy().getUsername())
                .createdByAvatar(reminder.getCreatedBy().getAvatarUrl())
                .createdAt(reminder.getCreatedAt())
                .myRsvp(myRsvp)
                .totalParticipants(participants.size())
                .acceptedCount(acceptedCount)
                .declinedCount(declinedCount)
                .pendingCount(pendingCount)
                .participants(participantResponses)
                .build();
    }

    /**
     * Dành cho ReminderScheduler quét định kỳ (mỗi 45 giây).
     * Tự động tìm các lịch hẹn PENDING mà COALESCE(endAt, startAt) < NOW(),
     * chuyển sang trạng thái DONE và phát WebSocket báo cho toàn bộ thành viên
     * trong phòng.
     */
    @Transactional
    public int scanAndExpireReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Reminder> expired = reminderRepository.findExpiredReminders(ReminderStatus.PENDING, now);
        if (expired.isEmpty()) {
            return 0;
        }

        for (Reminder r : expired) {
            r.setReminderStatus(ReminderStatus.DONE);
            reminderRepository.save(r);

            UUID roomId = r.getRoom().getRoomId();
            ReminderResponse response = toResponse(r, null, null);

            eventPublisher.publishEvent(new BroadcastEvent(
                    "/topic/room." + roomId + ".reminder",
                    ReminderEventResponse.builder()
                            .type(ReminderEventType.REMINDER_UPDATED)
                            .roomId(roomId)
                            .reminder(response)
                            .build()));
        }
        return expired.size();
    }

    private MessageResponse buildReminderMessageResponse(Message message, UUID reminderId) {
        return MessageResponse.builder()
                .messageId(message.getMessageId())
                .roomId(message.getRoom().getRoomId())
                .messageType(MessageType.REMINDER)
                .content(message.getContent())
                .reminderId(reminderId)
                .createdAt(message.getCreatedAt())
                .isEdited(false)
                .isRevoked(false)
                .isPinned(false)
                .sender(userMapper.toUserResponse(message.getSender()))
                .build();
    }

    private Room getRoomOrThrow(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    private RoomMember getActiveMemberOrThrow(UUID roomId, UUID userId) {
        RoomMember member = roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ROOM_MEMBER));
        if (member.getStatus() != RoomMemberStatus.ACTIVE) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }
        return member;
    }

    private String preview(String text) {
        if (text == null)
            return "";
        return text.length() > 80 ? text.substring(0, 80) + "..." : text;
    }
}