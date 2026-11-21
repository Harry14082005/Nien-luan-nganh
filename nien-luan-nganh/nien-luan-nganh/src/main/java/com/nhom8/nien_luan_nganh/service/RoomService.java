package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.event.BroadcastEvent;
import com.nhom8.nien_luan_nganh.dto.request.CreateDirectRoomRequest;
import com.nhom8.nien_luan_nganh.dto.request.CreateGroupRequest;
import com.nhom8.nien_luan_nganh.dto.response.PageResponse;
import com.nhom8.nien_luan_nganh.dto.response.RoomDetailResponse;
import com.nhom8.nien_luan_nganh.dto.response.RoomInviteInfoResponse;
import com.nhom8.nien_luan_nganh.dto.response.RoomListResponse;
import com.nhom8.nien_luan_nganh.dto.response.RoomMemberResponse;
import com.nhom8.nien_luan_nganh.entity.Room;
import com.nhom8.nien_luan_nganh.entity.RoomMember;
import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.enums.RoomMemberStatus;
import com.nhom8.nien_luan_nganh.enums.RoomRole;
import com.nhom8.nien_luan_nganh.enums.RoomType;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.entity.Message;
import com.nhom8.nien_luan_nganh.enums.MessageType;
import com.nhom8.nien_luan_nganh.enums.NotificationType;
import com.nhom8.nien_luan_nganh.mapper.MessageMapper;
import com.nhom8.nien_luan_nganh.mapper.RoomMapper;
import com.nhom8.nien_luan_nganh.repository.FriendshipRepository;
import com.nhom8.nien_luan_nganh.repository.MessageRepository;
import com.nhom8.nien_luan_nganh.repository.PollRepository;
import com.nhom8.nien_luan_nganh.repository.ReminderRepository;
import com.nhom8.nien_luan_nganh.repository.RoomMemberRepository;
import com.nhom8.nien_luan_nganh.repository.RoomRepository;
import com.nhom8.nien_luan_nganh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomMapper roomMapper;
    private final MessageMapper messageMapper;
    private final RoomMemberService roomMemberService;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageRepository messageRepository;
    private final PollRepository pollRepository;
    private final ReminderRepository reminderRepository;
    private final UserPresenceService userPresenceService;
    private final FriendshipRepository friendshipRepository;
    private final FileStorageService fileStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    // tạo group
    @Transactional
    public RoomDetailResponse createGroup(CreateGroupRequest request, UUID currentUserId) {
        User creator = userRepository.findById(currentUserId).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Room room = roomMapper.toRoom(request);
        room.setRoomType(RoomType.GROUP);
        room.setCreatedBy(creator);
        room.setInviteCode(generateInviteCode());

        roomRepository.save(room);
        updateLastActivity(room, room.getRoomId(), "Đã tạo nhóm", creator, room.getCreatedAt());

        /// xử lý thêm danh sách thành viên
        // người tạo là Admin
        roomMemberService.addMember(room, currentUserId, RoomRole.ADMIN);
        // những người được thêm là Member
        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty())
            roomMemberService.addMembersOnCreate(room, request.getMemberIds(), RoomRole.MEMBER);

        return roomMapper.toRoomDetailResponse(room);
    }

    // tạo nhóm 2 người trực tiếp
    @Transactional
    public RoomDetailResponse createDirectRoom(CreateDirectRoomRequest request, UUID currentUserId) {
        if (request.getUserId() == null)
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        if (request.getUserId().equals(currentUserId))
            throw new AppException(ErrorCode.CANNOT_ADD_YOURSELF);

        // kiểm tra người được thêm tồn tại
        userRepository.findById(request.getUserId()).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Kiểm tra quan hệ bị chặn (nếu đã bị chặn thì không cho tạo/mở phòng direct)
        friendshipRepository.findBetweenUsers(currentUserId, request.getUserId()).ifPresent(f -> {
            if (f.getFriendshipStatus() == com.nhom8.nien_luan_nganh.enums.FriendshipStatus.BLOCKED) {
                throw new AppException(ErrorCode.USER_BLOCKED);
            }
        });

        // Đã có phòng direct giữa 2 người thì trả về phòng cũ
        return roomRepository.findDirectRoom(currentUserId, request.getUserId(), RoomType.DIRECT)
                .map(roomMapper::toRoomDetailResponse)
                .orElseGet(() -> {
                    Room room = roomMapper.toRoom(request);
                    room.setRoomType(RoomType.DIRECT);
                    roomRepository.save(room);

                    // xử lý thêm 2 người vào phòng
                    roomMemberService.addMember(room, currentUserId, RoomRole.MEMBER);
                    roomMemberService.addMember(room, request.getUserId(), RoomRole.MEMBER);

                    return roomMapper.toRoomDetailResponse(room);
                });
    }

    // lấy danh sách phòng chat
    @Transactional(readOnly = true)
    public PageResponse<RoomListResponse> getRooms(UUID currentUserId, RoomType type, int pageNo, int pageSize) {
        // 1. kiểm tra người dùng
        if (!userRepository.existsById(currentUserId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        // 2. Tạo thông tin phân trang (sắp xếp theo updatedAt DESC để phòng vừa có hoạt
        // động mới nổi lên đầu)
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.Direction.DESC, "updatedAt");

        // 3. Lấy danh sách ID người bị chặn 2 chiều (nếu có) để lọc ngay từ database
        List<UUID> blockedUserIds = friendshipRepository.findBlockedUserIdsBothWays(currentUserId);

        Page<Room> rooms;
        if (blockedUserIds != null && !blockedUserIds.isEmpty()) {
            if (type == null) {
                rooms = roomRepository.findDistinctByRoomMembers_User_UserIdAndStatusExcludingBlocked(
                        currentUserId, RoomMemberStatus.ACTIVE, blockedUserIds, pageable);
            } else {
                rooms = roomRepository.findDistinctByRoomMembers_User_UserIdAndRoomTypeAndStatusExcludingBlocked(
                        currentUserId, type, RoomMemberStatus.ACTIVE, blockedUserIds, pageable);
            }
        } else {
            if (type == null) {
                rooms = roomRepository.findDistinctByRoomMembers_User_UserIdAndStatus(
                        currentUserId, RoomMemberStatus.ACTIVE, pageable);
            } else {
                rooms = roomRepository.findDistinctByRoomMembers_User_UserIdAndRoomTypeAndStatus(
                        currentUserId, type, RoomMemberStatus.ACTIVE, pageable);
            }
        }

        if (rooms.isEmpty()) {
            return PageResponse.empty(pageNo, pageSize);
        }

        List<UUID> roomIds = rooms.getContent().stream()
                .map(Room::getRoomId)
                .toList();

        List<UUID> directRoomIds = rooms.getContent().stream()
                .filter(r -> r.getRoomType() == RoomType.DIRECT)
                .map(Room::getRoomId)
                .toList();

        // 4.1. Lấy thông tin RoomMember của chính mình (lastReadMessageId,
        // lastDeliveredMessageId) trong 1 query
        Map<UUID, RoomMember> myMemberByRoomId = new HashMap<>();
        List<RoomMember> myMemberships = roomMemberRepository.findByRoomIdsAndUserId(roomIds, currentUserId);
        for (RoomMember rm : myMemberships) {
            myMemberByRoomId.put(rm.getRoomMemberId().getRoomId(), rm);
        }

        // 4.2. Lấy thông tin người đối diện cho phòng DIRECT (1-1) trong 1 query
        Map<UUID, User> peerUserByRoomId = new HashMap<>();
        if (!directRoomIds.isEmpty()) {
            List<RoomMember> otherMembers = roomMemberRepository.findOtherMembersInRooms(directRoomIds, currentUserId);
            for (RoomMember om : otherMembers) {
                peerUserByRoomId.put(om.getRoomMemberId().getRoomId(), om.getUser());
            }
        }

        // 4.3. Đếm tổng số hoạt động chưa đọc (Message + Poll + Reminder mới tạo)
        // của từng phòng bằng các query gộp GROUP BY
        Map<UUID, Long> unreadCountByRoomId = new HashMap<>();
        accumulateUnreadCounts(unreadCountByRoomId,
                messageRepository.countUnreadMessagesByRoomIds(roomIds, currentUserId));
        accumulateUnreadCounts(unreadCountByRoomId,
                pollRepository.countUnreadPollsByRoomIds(roomIds, currentUserId));
        accumulateUnreadCounts(unreadCountByRoomId,
                reminderRepository.countUnreadRemindersByRoomIds(roomIds, currentUserId));

        // 5. Chuyển sang DTO và làm giàu dữ liệu hiển thị bên ngoài (Sidebar)
        // lastMessageId/Content/SenderName/At được mapper copy trực tiếp từ Room
        Page<RoomListResponse> responses = rooms.map(room -> {
            RoomListResponse item = roomMapper.toRoomListResponse(room);
            UUID roomId = room.getRoomId();

            // Nếu là phòng DIRECT (1-1) và chưa có tên/avatar riêng thì hiển thị tên/avatar
            // của đối phương
            if (room.getRoomType() == RoomType.DIRECT) {
                User peer = peerUserByRoomId.get(roomId);
                if (peer != null) {
                    // Tu dong kiem tra RAM cache xem Friend co dang mo tab/socket hay hong de gan
                    // online hong
                    item.setPeerId(peer.getUserId());
                    boolean online = userPresenceService.isUserOnline(peer.getUserId());
                    item.setIsOnline(online);
                    item.setLastSeenAt(online ? null : peer.getLastSeenAt());
                    if (item.getRoomName() == null || item.getRoomName().isBlank()) {
                        item.setRoomName(peer.getUsername());
                    }
                    if (item.getRoomAvatarURL() == null || item.getRoomAvatarURL().isBlank()) {
                        item.setRoomAvatarURL(peer.getAvatarUrl());
                    }
                }
            }

            // Gán mốc đã nhận / đã đọc của thành viên hiện tại
            RoomMember myMember = myMemberByRoomId.get(roomId);
            if (myMember != null) {
                item.setLastDeliveredMessageId(myMember.getLastDeliveredMessageId());
                item.setLastReadMessageId(myMember.getLastReadMessageId());
            }

            // Gán tổng số hoạt động chưa đọc (tin nhắn + bình chọn + lịch hẹn mới)
            long unreadCount = unreadCountByRoomId.getOrDefault(roomId, 0L);

            // Cờ in đậm: đối chiếu trực tiếp lastMessageId của phòng với lastReadMessageId
            // của mình (không tính hoạt động do chính mình tạo) hoặc còn mục chưa đọc
            UUID myLastReadId = myMember != null ? myMember.getLastReadMessageId() : null;
            boolean hasUnread = unreadCount > 0 || (room.getLastMessageId() != null
                    && !room.getLastMessageId().equals(myLastReadId) // Id new hoat dong != id cuoi user doc
                    && !currentUserId.equals(room.getLastMessageSenderId())); // new hoat dong khong phai do ban than
                                                                              // user tao ra
            if (unreadCount == 0 && hasUnread) {
                unreadCount = 1L;
            }
            item.setHasUnread(hasUnread);
            item.setUnreadCount(unreadCount);

            return item;
        });

        // 6. Trả kết quả phân trang
        return PageResponse.from(responses);
    }

    private void accumulateUnreadCounts(Map<UUID, Long> targetMap, List<Object[]> rows) {
        if (rows == null) {
            return;
        }
        for (Object[] row : rows) {
            UUID rId = (UUID) row[0]; // roomId
            Long count = (Long) row[1]; // So luong dem duoc
            if (rId != null && count != null) {
                targetMap.merge(rId, count, Long::sum); // Cộng dồn vào Map
            }
        }
    }

    /**
     * Cập nhật hoạt động mới nhất của phòng (dùng chung cho MessageService,
     * PollService, ReminderService).
     * Tự động đẩy thời gian updatedAt của phòng lên đầu danh sách Sidebar.
     *
     * @param room           Phòng chat cần cập nhật
     * @param activityId     ID của hoạt động mới nhất (messageId, pollId hoặc
     *                       reminderId)
     * @param previewContent Nội dung hiển thị ngoài Sidebar (vd: "Alo", "📊 [Bình
     *                       chọn] ...", "⏰ [Lịch hẹn] ...")
     * @param sender         Người tạo hoạt động
     * @param activityAt     Thời điểm tạo hoạt động
     */
    @Transactional
    public void updateLastActivity(Room room, UUID activityId, String previewContent, User sender,
            LocalDateTime activityAt) {
        LocalDateTime timestamp = (activityAt != null) ? activityAt : LocalDateTime.now();
        room.setLastMessageId(activityId);
        room.setLastMessageContent(previewContent);
        if (sender != null) {
            room.setLastMessageSenderId(sender.getUserId());
            room.setLastMessageSenderName(sender.getUsername());
        }
        room.setLastMessageAt(timestamp);
        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.save(room);
    }

    /**
     * Cập nhật lại nội dung hiển thị bên ngoài (preview) nếu hoạt động bị chỉnh sửa
     * hoặc thu hồi
     * chính là hoạt động mới nhất hiện tại của phòng.
     */
    @Transactional
    public void updateLastActivityContentIfMatch(Room room, UUID activityId, String newPreviewContent) {
        if (room != null && activityId != null && activityId.equals(room.getLastMessageId())) {
            room.setLastMessageContent(newPreviewContent);
            roomRepository.save(room);
        }
    }

    private RoomMember ensureActiveMember(UUID currentUserId, UUID roomId) {
        RoomMember member = roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId, roomId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ROOM_MEMBER));
        if (member.getStatus() == RoomMemberStatus.PENDING) {
            throw new AppException(ErrorCode.ROOM_MEMBER_PENDING);
        }
        if (member.getStatus() != RoomMemberStatus.ACTIVE) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }
        return member;
    }

    // lấy thông tin phòng
    @Transactional(readOnly = true)
    public RoomDetailResponse getRoom(UUID roomId, UUID currentUserId) {
        // 1. kiểm tra người dùng
        if (!userRepository.existsById(currentUserId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        // 2. kiểm tra người dùng có trong room và đang ở trạng thái ACTIVE
        ensureActiveMember(currentUserId, roomId);

        // 3. lấy thông tin phòng
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // 4. Chuyển sang DTO
        RoomDetailResponse response = roomMapper.toRoomDetailResponse(room);
        if (room.getRoomType() == RoomType.DIRECT) {
            List<RoomMember> others = roomMemberRepository.findOtherMembersInRooms(List.of(roomId), currentUserId);
            if (!others.isEmpty() && others.get(0).getUser() != null) {
                User peer = others.get(0).getUser();
                if (response.getRoomName() == null || response.getRoomName().isBlank()) {
                    response.setRoomName(peer.getUsername());
                }
                if (response.getRoomAvatarURL() == null || response.getRoomAvatarURL().isBlank()) {
                    response.setRoomAvatarURL(peer.getAvatarUrl());
                }
            }
        }
        return response;
    }

    /**
     * Cập nhật ảnh đại diện của phòng / nhóm chat
     */
    @Transactional
    public RoomDetailResponse updateRoomAvatar(UUID roomId, UUID currentUserId,
            org.springframework.web.multipart.MultipartFile file) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // 1. Kiểm tra người dùng có phải thành viên Active của phòng không
        ensureActiveMember(currentUserId, roomId);

        // NOTE: tạm thời cho phép thành viên Active đổi avatar nhóm.
        // Khi cần quyền Admin, uncomment đoạn sau:
        /*
         * RoomMember member =
         * roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(
         * currentUserId, roomId)
         * .orElseThrow(() -> new AppException(ErrorCode.NOT_ROOM_MEMBER));
         * if (member.getRole() != RoomRole.ADMIN) {
         * throw new AppException(ErrorCode.UNAUTHORIZED);
         * }
         */

        // 2. Lưu ảnh avatar mới thông qua FileStorageService đã có
        String newAvatarKey = fileStorageService.saveAvatar(file);
        room.setRoomAvatarURL(newAvatarKey);
        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.save(room);

        // 3. Tạo tin nhắn hệ thống (SYSTEM Message) thông báo đổi ảnh nhóm
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        String systemContent = currentUser.getUsername() + " đã thay đổi ảnh đại diện nhóm";
        Message systemMessage = Message.builder()
                .room(room)
                .sender(currentUser)
                .messageType(MessageType.SYSTEM)
                .content(systemContent)
                .isEdited(false)
                .isRevoked(false)
                .isPinned(false)
                .createdAt(LocalDateTime.now())
                .build();
        Message savedSystemMessage = messageRepository.save(systemMessage);

        // Cập nhật hoạt động mới nhất của phòng
        updateLastActivity(room, savedSystemMessage.getMessageId(), systemContent, currentUser,
                savedSystemMessage.getCreatedAt());

        RoomDetailResponse response = roomMapper.toRoomDetailResponse(room);

        // 4. Phát sóng realtime tin nhắn hệ thống tới topic phòng
        eventPublisher.publishEvent(
                new com.nhom8.nien_luan_nganh.dto.event.BroadcastEvent("/topic/room." + roomId,
                        messageMapper.toMessageResponse(savedSystemMessage)));

        // 5. Phát sóng realtime cập nhật avatar phòng tới toàn bộ thành viên
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("type", "ROOM_AVATAR_UPDATED");
        wsPayload.put("roomId", roomId.toString());
        wsPayload.put("roomAvatarURL", newAvatarKey);
        eventPublisher.publishEvent(
                new com.nhom8.nien_luan_nganh.dto.event.BroadcastEvent("/topic/room." + roomId, wsPayload));

        return response;
    }

    /**
     * Cập nhật tên phòng / nhóm chat
     */
    @Transactional
    public RoomDetailResponse updateRoomName(UUID roomId, UUID currentUserId, String newRoomName) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // 1. Kiểm tra người dùng có phải thành viên Active của phòng không
        ensureActiveMember(currentUserId, roomId);

        // NOTE: Tạm thời cho phép thành viên Active đổi tên nhóm.
        // Khi cần kiểm tra quyền Admin, uncomment đoạn sau:
        /*
         * RoomMember member =
         * roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(
         * currentUserId, roomId)
         * .orElseThrow(() -> new AppException(ErrorCode.NOT_ROOM_MEMBER));
         * if (member.getRole() != RoomRole.ADMIN) {
         * throw new AppException(ErrorCode.UNAUTHORIZED);
         * }
         */

        String trimmedName = newRoomName != null ? newRoomName.trim() : "";
        if (trimmedName.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        room.setRoomName(trimmedName);
        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.save(room);

        // 2. Tạo tin nhắn hệ thống (SYSTEM Message) thông báo đổi tên nhóm
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        String systemContent = currentUser.getUsername() + " đã đổi tên nhóm thành \"" + trimmedName + "\"";
        Message systemMessage = Message.builder()
                .room(room)
                .sender(currentUser)
                .messageType(MessageType.SYSTEM)
                .content(systemContent)
                .isEdited(false)
                .isRevoked(false)
                .isPinned(false)
                .createdAt(LocalDateTime.now())
                .build();
        Message savedSystemMessage = messageRepository.save(systemMessage);

        // Cập nhật hoạt động mới nhất của phòng
        updateLastActivity(room, savedSystemMessage.getMessageId(), systemContent, currentUser,
                savedSystemMessage.getCreatedAt());

        RoomDetailResponse response = roomMapper.toRoomDetailResponse(room);

        // 3. Phát sóng realtime tin nhắn hệ thống tới topic phòng
        eventPublisher.publishEvent(
                new BroadcastEvent("/topic/room." + roomId,
                        messageMapper.toMessageResponse(savedSystemMessage)));

        // 4. Phát sóng realtime cập nhật tên phòng tới toàn bộ thành viên
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("type", "ROOM_NAME_UPDATED");
        wsPayload.put("roomId", roomId.toString());
        wsPayload.put("roomName", trimmedName);
        eventPublisher.publishEvent(
                new BroadcastEvent("/topic/room." + roomId, wsPayload));

        return response;
    }

    /**
     * Sinh mã mời ngắn gọn duy nhất 10 ký tự cho phòng nhóm
     */
    private String generateInviteCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    /**
     * Lấy link mời của phòng nhóm (chỉ dành cho Quản trị viên nhóm)
     */
    @Transactional
    public String getRoomInviteLink(UUID roomId, UUID currentUserId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (room.getRoomType() != RoomType.GROUP) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // Kiểm tra quyền Admin của người gọi
        RoomMember myMember = roomMemberRepository
                .findByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId, roomId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ROOM_MEMBER));

        if (myMember.getRoomMemberRole() != RoomRole.ADMIN
                && !currentUserId.equals(room.getCreatedBy() != null ? room.getCreatedBy().getUserId() : null)) {
            throw new AppException(ErrorCode.NOT_ROOM_ADMIN);
        }

        // Nếu nhóm chưa có inviteCode (nhóm cũ tạo trước đó) -> tự động tạo và lưu
        if (room.getInviteCode() == null || room.getInviteCode().isBlank()) {
            room.setInviteCode(generateInviteCode());
            roomRepository.save(room);
        }

        return room.getInviteCode();
    }

    /**
     * Lấy thông tin tóm tắt của nhóm qua inviteCode (để hiển thị màn hình
     * /join/:inviteCode)
     */
    @Transactional(readOnly = true)
    public RoomInviteInfoResponse getRoomInviteInfo(String inviteCode, UUID currentUserId) {
        Room room = roomRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_INVITE_LINK));

        // Đếm số lượng thành viên đang ACTIVE
        List<RoomMember> activeMembers = roomMemberRepository.findByRoomMemberId_RoomIdAndStatus(room.getRoomId(),
                RoomMemberStatus.ACTIVE);
        int memberCount = activeMembers != null ? activeMembers.size() : 0;

        // Kiểm tra người đang xem đã là thành viên trong nhóm chưa
        boolean isAlreadyMember = false;
        if (currentUserId != null) {
            isAlreadyMember = roomMemberRepository
                    .findByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId, room.getRoomId())
                    .map(m -> m.getStatus() == RoomMemberStatus.ACTIVE)
                    .orElse(false);
        }

        return RoomInviteInfoResponse.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .roomAvatarURL(room.getRoomAvatarURL())
                .memberCount(memberCount)
                .requireApproval(room.isRequireApproval())
                .isAlreadyMember(isAlreadyMember)
                .build();
    }

    /**
     * Tham gia nhóm qua link mời
     */
    @Transactional
    public RoomDetailResponse joinRoomByInviteCode(String inviteCode, UUID currentUserId) {
        Room room = roomRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_INVITE_LINK));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UUID roomId = room.getRoomId();

        // Kiểm tra xem đã là thành viên trong nhóm chưa
        Optional<RoomMember> existingMemberOpt = roomMemberRepository
                .findByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId, roomId);
        if (existingMemberOpt.isPresent()) {
            RoomMember existing = existingMemberOpt.get();
            if (existing.getStatus() == RoomMemberStatus.ACTIVE) {
                // Đã là thành viên rồi -> trả về thông tin phòng để mở chat ngay
                return roomMapper.toRoomDetailResponse(room);
            } else if (existing.getStatus() == RoomMemberStatus.PENDING) {
                // Đã gửi yêu cầu và đang chờ phê duyệt -> trả về response pending
                RoomDetailResponse response = roomMapper.toRoomDetailResponse(room);
                response.setPendingApproval(true);
                return response;
            }
        }

        // Kiểm tra nếu nhóm yêu cầu phê duyệt thành viên mới (requireApproval = true)
        if (room.isRequireApproval()) {
            RoomMemberResponse pendingMember = roomMemberService.addMember(room, currentUserId, RoomRole.MEMBER,
                    RoomMemberStatus.PENDING);

            // Gửi thông báo Notification (chuông thông báo) cho tất cả Admin của nhóm
            List<RoomMember> admins = roomMemberRepository
                    .findByRoomMemberId_RoomIdAndStatus(roomId, RoomMemberStatus.ACTIVE)
                    .stream()
                    .filter(m -> m.getRoomMemberRole() == RoomRole.ADMIN)
                    .toList();
            for (RoomMember admin : admins) {
                notificationService.createAndSendNotification(
                        admin.getUser(),
                        currentUser,
                        NotificationType.ROOM_JOIN_REQUEST,
                        "Yêu cầu tham gia nhóm",
                        currentUser.getUsername() + " đã gửi yêu cầu tham gia nhóm "
                                + (room.getRoomName() != null ? room.getRoomName() : "Chat") + ".",
                        roomId,
                        roomId);
            }

            // Phát sóng realtime sự kiện yêu cầu phê duyệt thành viên mới tới topic phòng
            // để Admin cập nhật ngay lập tức
            Map<String, Object> pendingEvent = new HashMap<>();
            pendingEvent.put("type", "ROOM_MEMBER_PENDING_ADDED");
            pendingEvent.put("roomId", roomId.toString());
            pendingEvent.put("pendingMembers", List.of(pendingMember));
            pendingEvent.put("invitedBy", currentUser.getUsername());
            eventPublisher.publishEvent(new BroadcastEvent("/topic/room." + roomId, pendingEvent));

            RoomDetailResponse response = roomMapper.toRoomDetailResponse(room);
            response.setPendingApproval(true);
            return response;
        }

        // Nhóm không cần phê duyệt -> thêm vào làm thành viên chính thức (ACTIVE)
        roomMemberService.addMember(room, currentUserId, RoomRole.MEMBER, RoomMemberStatus.ACTIVE);

        // Tạo tin nhắn hệ thống thông báo người mới vào nhóm
        String systemContent = currentUser.getUsername() + " đã tham gia nhóm qua liên kết mời";
        Message systemMessage = Message.builder()
                .room(room)
                .sender(currentUser)
                .messageType(MessageType.SYSTEM)
                .content(systemContent)
                .isEdited(false)
                .isRevoked(false)
                .isPinned(false)
                .createdAt(LocalDateTime.now())
                .build();
        Message savedSystemMessage = messageRepository.save(systemMessage);

        updateLastActivity(room, savedSystemMessage.getMessageId(), systemContent, currentUser,
                savedSystemMessage.getCreatedAt());

        // Phát sóng tin nhắn hệ thống realtime tới phòng chat
        eventPublisher.publishEvent(
                new BroadcastEvent("/topic/room." + roomId,
                        messageMapper.toMessageResponse(savedSystemMessage)));

        return roomMapper.toRoomDetailResponse(room);
    }
}
