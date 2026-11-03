package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.request.CreateDirectRoomRequest;
import com.nhom8.nien_luan_nganh.dto.request.CreateGroupRequest;
import com.nhom8.nien_luan_nganh.dto.response.PageResponse;
import com.nhom8.nien_luan_nganh.dto.response.RoomDetailResponse;
import com.nhom8.nien_luan_nganh.dto.response.RoomListResponse;
import com.nhom8.nien_luan_nganh.entity.Room;
import com.nhom8.nien_luan_nganh.entity.RoomMember;
import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.enums.RoomRole;
import com.nhom8.nien_luan_nganh.enums.RoomType;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.mapper.RoomMapper;
import com.nhom8.nien_luan_nganh.repository.MessageRepository;
import com.nhom8.nien_luan_nganh.repository.PollRepository;
import com.nhom8.nien_luan_nganh.repository.ReminderRepository;
import com.nhom8.nien_luan_nganh.repository.RoomMemberRepository;
import com.nhom8.nien_luan_nganh.repository.RoomRepository;
import com.nhom8.nien_luan_nganh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
    private final RoomMemberService roomMemberService;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageRepository messageRepository;
    private final PollRepository pollRepository;
    private final ReminderRepository reminderRepository;
    private final UserPresenceService userPresenceService;

    // tạo group
    @Transactional
    public RoomDetailResponse createGroup(CreateGroupRequest request, UUID currentUserId) {
        User creator = userRepository.findById(currentUserId).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Room room = roomMapper.toRoom(request);
        room.setRoomType(RoomType.GROUP);
        room.setCreatedBy(creator);

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

        // 3. Lấy danh sách phòng
        Page<Room> rooms;

        if (type == null) {
            rooms = roomRepository.findDistinctByRoomMembers_User_UserId(currentUserId, pageable);
        } else {
            rooms = roomRepository.findDistinctByRoomMembers_User_UserIdAndRoomType(currentUserId, type, pageable);
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
                    //Tu dong kiem tra RAM cache xem Friend co dang mo tab/socket hay hong de gan online hong
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
            item.setUnreadCount(unreadCount);

            // Cờ in đậm: đối chiếu trực tiếp lastMessageId của phòng với lastReadMessageId
            // của mình (không tính hoạt động do chính mình tạo) hoặc còn mục chưa đọc
            UUID myLastReadId = myMember != null ? myMember.getLastReadMessageId() : null;
            boolean hasUnread = unreadCount > 0 || (room.getLastMessageId() != null
                    && !room.getLastMessageId().equals(myLastReadId) // Id new hoat dong != id cuoi user doc
                    && !currentUserId.equals(room.getLastMessageSenderId())); // new hoat dong khong phai do ban than
                                                                              // user tao ra
            item.setHasUnread(hasUnread);

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

    // lấy thông tin phòng
    @Transactional(readOnly = true)
    public RoomDetailResponse getRoom(UUID roomId, UUID currentUserId) {
        // 1. kiểm tra người dùng
        if (!userRepository.existsById(currentUserId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        // 2. kiểm tra người dùng có trong room hay không
        if (!roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId, roomId)) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }

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
}
