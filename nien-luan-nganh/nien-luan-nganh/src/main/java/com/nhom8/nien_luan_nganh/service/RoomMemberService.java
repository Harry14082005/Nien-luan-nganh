package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.event.BroadcastEvent;
import com.nhom8.nien_luan_nganh.dto.request.AddMemberRequest;
import com.nhom8.nien_luan_nganh.dto.request.ApproveMemberRequest;
import com.nhom8.nien_luan_nganh.dto.request.ChangeMemberRoleRequest;
import com.nhom8.nien_luan_nganh.dto.request.LeaveRoomRequest;
import com.nhom8.nien_luan_nganh.dto.response.MessageReceiptResponse;
import com.nhom8.nien_luan_nganh.dto.response.MessageResponse;
import com.nhom8.nien_luan_nganh.dto.response.RoomMemberResponse;
import com.nhom8.nien_luan_nganh.dto.response.UserResponse;
import com.nhom8.nien_luan_nganh.entity.*;
import com.nhom8.nien_luan_nganh.enums.FriendshipStatus;
import com.nhom8.nien_luan_nganh.enums.MessageReceiptType;
import com.nhom8.nien_luan_nganh.enums.MessageType;
import com.nhom8.nien_luan_nganh.enums.RoomMemberStatus;
import com.nhom8.nien_luan_nganh.enums.RoomRole;
import com.nhom8.nien_luan_nganh.enums.RoomType;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.mapper.MessageMapper;
import com.nhom8.nien_luan_nganh.mapper.RoomMemberMapper;
import com.nhom8.nien_luan_nganh.mapper.UserMapper;
import com.nhom8.nien_luan_nganh.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomMemberService {
    private final UserRepository userRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomMemberMapper roomMemberMapper;
    private final RoomRepository roomRepository;
    private final MessageRepository messageRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserMapper userMapper;
    private final MessageMapper messageMapper;
    private final ApplicationEventPublisher eventPublisher;

    // 1. Thêm thành viên lúc tạo
    // 2. Thêm thành viên vô nhóm đã tạo
    // 3. Phê duyệt thành viên
    // 4. Xoá thành viên/ Rời khỏi phòng
    // 4. 1. Nếu nhóm duy nhất 1 admin, mà admin rời phòng thì phải chỉ định
    // 5. Chỉnh bật/tắt phê duyệt thành viên
    // 6. Xem danh sách thành viên

    // hàm cơ bản thêm một thành viên
    @Transactional
    public RoomMemberResponse addMember(Room room, UUID userId, RoomRole role, RoomMemberStatus status) {
        User user = getUserOrThrow(userId);

        if (roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, room.getRoomId())) {
            throw new AppException(ErrorCode.ALREADY_IN_ROOM);
        }

        RoomMember roomMember = createRoomMember(room, user, role, status);
        roomMemberRepository.save(roomMember);

        return roomMemberMapper.toRoomMemberResponse(roomMember);
    }

    // thêm thành viên với trạng thái mặc định là ACTIVE
    @Transactional
    public RoomMemberResponse addMember(Room room, UUID userId, RoomRole role) {
        return addMember(room, userId, role, RoomMemberStatus.ACTIVE);
    }

    // thêm thành viên khi tạo phòng (cơ sở 1 thành viên)
    @Transactional
    public List<RoomMemberResponse> addMembersOnCreate(Room room, List<UUID> userIds, RoomRole role) {
        return saveMember(room, userIds, role, RoomMemberStatus.ACTIVE);
    }

    // thêm nhiều thành viên khi tạo phòng cùng lúc
    @Transactional
    public List<RoomMemberResponse> addMembers(Room room, List<UUID> userIds, RoomRole role) {
        return saveMember(room, userIds, role, RoomMemberStatus.ACTIVE);
    }

    // thêm nhiều (1+) thành viên vào phòng đã có
    @Transactional
    public List<RoomMemberResponse> addMembers(UUID roomId, AddMemberRequest request, UUID currentUserId) {
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // nếu là phòng trực tiếp thì hông thêm đươc
        if (room.getRoomType() == RoomType.DIRECT)
            throw new AppException(ErrorCode.INVALID_INPUT);

        // kiểm tra người thực thi
        RoomMember actor = roomMemberRepository
                .findByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId, roomId).orElseThrow(
                        () -> new AppException(ErrorCode.NOT_ROOM_MEMBER));

        RoomMemberStatus status;

        if (actor.getRoomMemberRole() == RoomRole.ADMIN) {
            status = RoomMemberStatus.ACTIVE;
        } else {
            if (room.isRequireApproval()) { // nếu phải duyệt thì thêm vô là pending
                status = RoomMemberStatus.PENDING;
            } else {
                status = RoomMemberStatus.ACTIVE;
            }
        }

        List<RoomMemberResponse> responses = saveMember(room, request.getMemberIds(), RoomRole.MEMBER, status);

        // Nếu người dùng được vào nhóm ngay (ACTIVE) -> Tạo thông báo hệ thống và phát realtime qua WebSocket
        if (status == RoomMemberStatus.ACTIVE && request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            User actorUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            List<User> addedUsers = userRepository.findAllById(request.getMemberIds());

            if (!addedUsers.isEmpty()) {
                String actorName = actorUser.getUsername();
                String systemContent;
                int count = addedUsers.size();

                // Định dạng nội dung thông báo hệ thống:
                // <= 2 người: liệt kê tên từng người
                // > 2 người: gom số lượng (ví dụ: "A đã thêm 3 thành viên vào nhóm")
                if (count == 1) {
                    systemContent = actorName + " đã thêm " + addedUsers.get(0).getUsername() + " vào nhóm";
                } else if (count == 2) {
                    systemContent = actorName + " đã thêm " + addedUsers.get(0).getUsername() + ", "
                            + addedUsers.get(1).getUsername() + " vào nhóm";
                } else {
                    systemContent = actorName + " đã thêm " + count + " thành viên vào nhóm";
                }

                // 1. Lưu tin nhắn hệ thống vào CSDL
                Message systemMessage = Message.builder()
                        .room(room)
                        .sender(actorUser)
                        .messageType(MessageType.SYSTEM)
                        .content(systemContent)
                        .isEdited(false)
                        .isRevoked(false)
                        .isPinned(false)
                        .createdAt(LocalDateTime.now())
                        .build();
                Message savedSystemMessage = messageRepository.save(systemMessage);

                // 2. Cập nhật hoạt động mới nhất của phòng
                LocalDateTime now = savedSystemMessage.getCreatedAt();
                room.setLastMessageId(savedSystemMessage.getMessageId());
                room.setLastMessageContent(systemContent);
                room.setLastMessageSenderId(actorUser.getUserId());
                room.setLastMessageSenderName(actorName);
                room.setLastMessageAt(now);
                room.setUpdatedAt(now);
                roomRepository.save(room);

                // 3. Reset trạng thái đã đọc cho các thành viên khác
                List<RoomMember> allMembers = roomMemberRepository.findByRoomMemberId_RoomId(roomId);
                for (RoomMember m : allMembers) {
                    if (!m.getRoomMemberId().getUserId().equals(currentUserId)) {
                        m.setLastReadMessageId(null);
                    }
                }
                roomMemberRepository.saveAll(allMembers);

                // 4. Phát sóng realtime tin nhắn hệ thống qua WebSocket tới /topic/room.{roomId}
                MessageResponse messageResponse = messageMapper.toMessageResponse(savedSystemMessage);
                eventPublisher.publishEvent(new BroadcastEvent("/topic/room." + roomId, messageResponse));
            }
        }

        return responses;
    }

    // phê duyệt thành viên
    @Transactional
    public RoomMemberResponse approveMember(UUID roomId, ApproveMemberRequest request, UUID currentUserId) {
        Room room = getRoomOrThrow(roomId);

        RoomMember actor = getRoomMemberOrThrow(roomId, currentUserId);

        // phải là admin thì mới được duyệt
        if (actor.getRoomMemberRole() != RoomRole.ADMIN) {
            throw new AppException(ErrorCode.NOT_ROOM_ADMIN);
        }

        // kiểm tra trường hợp coi còn đang được cần phải duyệt hông
        RoomMember target = getRoomMemberOrThrow(roomId, request.getTargetUserId());

        // không phải trạng thái được duyệt thì phát sinh lỗi
        if (target.getStatus() != RoomMemberStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        target.setStatus(RoomMemberStatus.ACTIVE);

        roomMemberRepository.save(target);

        // Tạo thông báo hệ thống khi admin phê duyệt thành viên vào nhóm
        User actorUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        User targetUser = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String systemContent = actorUser.getUsername() + " đã phê duyệt " + targetUser.getUsername() + " vào nhóm";

        Message systemMessage = Message.builder()
                .room(room)
                .sender(actorUser)
                .messageType(MessageType.SYSTEM)
                .content(systemContent)
                .isEdited(false)
                .isRevoked(false)
                .isPinned(false)
                .createdAt(LocalDateTime.now())
                .build();
        Message savedSystemMessage = messageRepository.save(systemMessage);

        LocalDateTime now = savedSystemMessage.getCreatedAt();
        room.setLastMessageId(savedSystemMessage.getMessageId());
        room.setLastMessageContent(systemContent);
        room.setLastMessageSenderId(actorUser.getUserId());
        room.setLastMessageSenderName(actorUser.getUsername());
        room.setLastMessageAt(now);
        room.setUpdatedAt(now);
        roomRepository.save(room);

        List<RoomMember> allMembers = roomMemberRepository.findByRoomMemberId_RoomId(roomId);
        for (RoomMember m : allMembers) {
            if (!m.getRoomMemberId().getUserId().equals(currentUserId)) {
                m.setLastReadMessageId(null);
            }
        }
        roomMemberRepository.saveAll(allMembers);

        MessageResponse messageResponse = messageMapper.toMessageResponse(savedSystemMessage);
        eventPublisher.publishEvent(new BroadcastEvent("/topic/room." + roomId, messageResponse));

        return roomMemberMapper.toRoomMemberResponse(target);
    }

    // xoá thành viên ra khỏi phòng (hoặc từ chối yêu cầu tham gia đang PENDING)
    @Transactional
    public void removeMember(UUID roomId, UUID targetUserId, UUID currentUserId) {
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (room.getRoomType() == RoomType.DIRECT) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        RoomMember actor = roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId, roomId)
                .orElseThrow(
                        () -> new AppException(ErrorCode.NOT_ROOM_MEMBER));

        // Không tự kick bản thân (dùng leaveRoom)
        boolean isSelf = currentUserId.equals(targetUserId);
        if (isSelf)
            throw new AppException(ErrorCode.INVALID_INPUT);

        // Chỉ ADMIN mới được quyền xoá / từ chối thành viên
        if (actor.getRoomMemberRole() != RoomRole.ADMIN)
            throw new AppException(ErrorCode.NOT_ROOM_ADMIN);

        RoomMember target = roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(targetUserId, roomId)
                .orElseThrow(
                        () -> new AppException(ErrorCode.NOT_ROOM_MEMBER));

        boolean wasActive = target.getStatus() == RoomMemberStatus.ACTIVE;
        User actorUser = getUserOrThrow(currentUserId);
        User targetUser = getUserOrThrow(targetUserId);

        roomMemberRepository.delete(target);
        roomMemberRepository.flush();

        // Nếu xoá một thành viên đang ACTIVE -> Tạo tin nhắn hệ thống thông báo trong nhóm
        if (wasActive) {
            String systemContent = actorUser.getUsername() + " đã xóa " + targetUser.getUsername() + " khỏi nhóm";
            createAndBroadcastSystemMessage(room, actorUser, systemContent);
        }
    }

    // tự rời phòng chat nhóm
    @Transactional
    public void leaveRoom(UUID roomId, LeaveRoomRequest request, UUID currentUserId) {
        Room room = getRoomOrThrow(roomId);

        if (room.getRoomType() == RoomType.DIRECT) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        RoomMember actor = getRoomMemberOrThrow(roomId, currentUserId);
        User actorUser = getUserOrThrow(currentUserId);

        // Đếm số thành viên ACTIVE hiện tại trong phòng
        List<RoomMember> activeMembers = roomMemberRepository.findByRoomMemberId_RoomIdAndStatus(
                roomId, RoomMemberStatus.ACTIVE);

        if (activeMembers.size() <= 1) {
            // Còn 1 mình trong nhóm thì xoá luôn phòng
            deleteRoomCompletely(room);
            return;
        }

        // Nếu là admin thì kiểm tra có phải là admin duy nhất hay không
        if (actor.getRoomMemberRole() == RoomRole.ADMIN) {
            long adminCount = activeMembers.stream()
                    .filter(m -> m.getRoomMemberRole() == RoomRole.ADMIN)
                    .count();

            if (adminCount <= 1) {
                UUID newAdminId = (request != null) ? request.getNewAdminId() : null;

                if (newAdminId == null || newAdminId.equals(currentUserId)) {
                    throw new AppException(ErrorCode.INVALID_INPUT);
                }

                // Người được chỉ định kế nhiệm phải là thành viên ACTIVE
                RoomMember newAdmin = getActiveMemberOrThrow(roomId, newAdminId);
                newAdmin.setRoomMemberRole(RoomRole.ADMIN);
                roomMemberRepository.save(newAdmin);
            }
        }

        roomMemberRepository.delete(actor);
        roomMemberRepository.flush();

        // Phát thông báo hệ thống "A đã rời khỏi nhóm"
        String systemContent = actorUser.getUsername() + " đã rời khỏi nhóm";
        createAndBroadcastSystemMessage(room, actorUser, systemContent);
    }

    // bật / tắt chế độ xét duyệt thành viên mới (chỉ Admin mới được phép)
    @Transactional
    public boolean toggleRequireApproval(UUID roomId, Boolean requireApproval, UUID currentUserId) {
        Room room = getRoomOrThrow(roomId);
        if (room.getRoomType() == RoomType.DIRECT) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        RoomMember actor = getActiveMemberOrThrow(roomId, currentUserId);
        if (actor.getRoomMemberRole() != RoomRole.ADMIN) {
            throw new AppException(ErrorCode.NOT_ROOM_ADMIN);
        }

        boolean nextValue = (requireApproval != null) ? requireApproval : !room.isRequireApproval();
        if (room.isRequireApproval() == nextValue) {
            return nextValue;
        }

        room.setRequireApproval(nextValue);
        roomRepository.save(room);

        User actorUser = getUserOrThrow(currentUserId);
        String systemContent = actorUser.getUsername()
                + (nextValue ? " đã bật phê duyệt thành viên mới" : " đã tắt phê duyệt thành viên mới");
        createAndBroadcastSystemMessage(room, actorUser, systemContent);

        return nextValue;
    }

    // xem 1 thành viên
    @Transactional(readOnly = true)
    public RoomMemberResponse getMember(UUID roomId, UUID targetUserId, UUID currentUserId) {
        // phải là thành viên thì mới xem được
        getActiveMemberOrThrow(roomId, currentUserId);

        return roomMemberMapper.toRoomMemberResponse(getRoomMemberOrThrow(roomId, targetUserId));
    }

    // xem một đống thành viên trong phòng
    @Transactional(readOnly = true)
    public List<RoomMemberResponse> getMembers(UUID roomId, RoomMemberStatus status, UUID currentUserId) {
        // phải là thành viên thì mới xem được
        getActiveMemberOrThrow(roomId, currentUserId);

        List<RoomMember> members;
        if (status != null) {
            members = roomMemberRepository.findByRoomMemberId_RoomIdAndStatus(roomId, status);
        } else {
            members = roomMemberRepository.findByRoomMemberId_RoomId(roomId);
        }

        return members.stream().map(roomMemberMapper::toRoomMemberResponse).toList();
    }

    // đổi quyền thành viên
    @Transactional
    public RoomMemberResponse changeMemberRole(UUID roomId, UUID userId, ChangeMemberRoleRequest request,
            UUID currentUserId) {
        // kiểm tra phòng chat tồn tại
        Room room = getRoomOrThrow(roomId);

        // phải là thành viên thì mới xem được
        RoomMember actor = getActiveMemberOrThrow(roomId, currentUserId);

        // kiểm tra thành viên thay đổi có tồn tại trong group hay không
        RoomMember target = getRoomMemberOrThrow(roomId, userId);

        // kiểm tra có quyền admin hay không
        if (actor.getRoomMemberRole() != RoomRole.ADMIN) {
            throw new AppException(ErrorCode.NOT_ROOM_ADMIN);
        }

        // không cho đổi role người mà đang không được duyệt (duyệt thì sẽ là approve)
        if (target.getStatus() != RoomMemberStatus.ACTIVE) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        RoomRole newRole = request.getRole();

        // không làm gì nếu không đổi quyền
        if (target.getRoomMemberRole() == newRole) {
            return roomMemberMapper.toRoomMemberResponse(target);
        }

        // admin duy nhất không thể đổi quyền xuống MEMBER
        if (target.getRoomMemberRole() == RoomRole.ADMIN && newRole == RoomRole.MEMBER) {
            long adminCount = roomMemberRepository.countByRoomMemberId_RoomIdAndRoomMemberRole(roomId, RoomRole.ADMIN);

            if (adminCount <= 1) {
                throw new AppException(ErrorCode.INVALID_INPUT);
            }
        }

        // cập nhật vai trò
        target.setRoomMemberRole(newRole);
        roomMemberRepository.save(target);

        // Phát thông báo hệ thống realtime khi phong hoặc gỡ quyền Quản trị viên
        User actorUser = getUserOrThrow(currentUserId);
        User targetUser = getUserOrThrow(userId);
        String systemContent = (newRole == RoomRole.ADMIN)
                ? actorUser.getUsername() + " đã bổ nhiệm " + targetUser.getUsername() + " làm quản trị viên"
                : actorUser.getUsername() + " đã gỡ quyền quản trị viên của " + targetUser.getUsername();
        createAndBroadcastSystemMessage(room, actorUser, systemContent);

        return roomMemberMapper.toRoomMemberResponse(target);
    }

    // đánh dấu đã nhận tin nhắn (client nhận được qua Socket thì gọi)
    @Transactional
    public MessageReceiptResponse markAsDelivered(UUID roomId, UUID messageId, UUID currentUserId) {
        RoomMember member = getActiveMemberOrThrow(roomId, currentUserId);

        if (messageId == null) {
            Room room = getRoomOrThrow(roomId);
            messageId = room.getLastMessageId();
        }

        Message message = null;
        if (messageId != null) {
            message = messageRepository.findById(messageId)
                    .filter(m -> m.getRoom() != null && m.getRoom().getRoomId().equals(roomId))
                    .orElse(null);
        }

        if (message == null) {
            Page<Message> latestPage = messageRepository.findByRoom_RoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(0, 1));
            if (latestPage.hasContent()) {
                message = latestPage.getContent().get(0);
                messageId = message.getMessageId();
            }
        }

        if (message == null) {
            return null;
        }

        if (isNewer(message, member.getLastDeliveredMessageId())) {
            member.setLastDeliveredMessageId(messageId);
            roomMemberRepository.save(member);

            return MessageReceiptResponse.builder()
                    .roomId(roomId)
                    .userId(currentUserId)
                    .username(member.getUser() != null ? member.getUser().getUsername() : null)
                    .avatarUrl(member.getUser() != null ? member.getUser().getAvatarUrl() : null)
                    .messageId(messageId)
                    .receiptType(MessageReceiptType.DELIVERED)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        return null;
    }

    // đánh dấu đã đọc khi nhấn vào phòng
    @Transactional
    public MessageReceiptResponse markAsRead(UUID roomId, UUID messageId, UUID currentUserId) {
        RoomMember member = getActiveMemberOrThrow(roomId, currentUserId);

        // Nếu client không truyền messageId cụ thể, thử lấy từ room.getLastMessageId()
        if (messageId == null) {
            Room room = getRoomOrThrow(roomId);
            messageId = room.getLastMessageId();
        }

        // Tìm tin nhắn tương ứng trong phòng
        Message message = null;
        if (messageId != null) {
            message = messageRepository.findById(messageId)
                    .filter(m -> m.getRoom() != null && m.getRoom().getRoomId().equals(roomId))
                    .orElse(null);
        }
        // Nếu messageId không phải tin nhắn (ví dụ: ID phòng, ID Poll, Reminder) hoặc chưa có,
        // tự động tìm tin nhắn mới nhất thật sự trong phòng
        if (message == null) {
            Page<Message> latestPage = messageRepository.findByRoom_RoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(0, 1));
            if (latestPage.hasContent()) {
                message = latestPage.getContent().get(0);
                messageId = message.getMessageId();
            }
        }

        if (message == null) {
            return null;
        }
        boolean updated = false;

        if (isNewer(message, member.getLastReadMessageId())) {
            member.setLastReadMessageId(messageId);
            updated = true;
        }

        // nếu đọc rồi thì chắc chắn đã nhận rồi
        if (isNewer(message, member.getLastDeliveredMessageId())) {
            member.setLastDeliveredMessageId(messageId);
            updated = true;
        }

        if (updated) {
            roomMemberRepository.save(member);

            return MessageReceiptResponse.builder()
                    .roomId(roomId)
                    .userId(currentUserId)
                    .username(member.getUser() != null ? member.getUser().getUsername() : null)
                    .avatarUrl(member.getUser() != null ? member.getUser().getAvatarUrl() : null)
                    .messageId(messageId)
                    .receiptType(MessageReceiptType.READ)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
        return null;
    }

    private List<RoomMemberResponse> saveMember(Room room, List<UUID> userIds, RoomRole role, RoomMemberStatus status) {
        // kiểm tra userIds
        if (userIds == null || userIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // loại bỏ trùng nhau
        List<UUID> distinctIds = userIds.stream().distinct().toList();

        // lấy danh sách người dùng từ id
        List<User> users = userRepository.findAllById(distinctIds);

        // kiểm tra người dùng
        if (users.size() != distinctIds.size()) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        // kiểm tra người dùng có thuộc phòng hay không
        if (!roomMemberRepository.findExistingUserIds(room.getRoomId(), distinctIds).isEmpty()) {
            throw new AppException(ErrorCode.ALREADY_IN_ROOM);
        }

        // tạo danh sách roommember
        List<RoomMember> roomMembers = users.stream()
                .map(user -> createRoomMember(room, user, role, status))
                .toList();
        return roomMemberRepository.saveAll(roomMembers)
                .stream()
                .map(roomMemberMapper::toRoomMemberResponse)
                .toList();
    }

    // lấy danh sách thành viên có thể thêm (tất cả bạn bè trừ những người có trong nhóm cả Active với Pending)
    @Transactional(readOnly = true)
    public List<UserResponse> getInvitableMembers(UUID roomId, UUID currentUserId) {
        Room room = getRoomOrThrow(roomId);

        if (room.getRoomType() == RoomType.DIRECT) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // Người gọi phải đang là thành viên ACTIVE
        getActiveMemberOrThrow(roomId, currentUserId);

        // 1. Lấy tất cả quan hệ bạn bè đã được chấp nhận
        List<Friendship> friendships = friendshipRepository.findTwoWayFriendships(
                currentUserId,
                FriendshipStatus.ACCEPTED);

        // 2. Lấy ID bạn bè, bất kể người đó gửi hay nhận lời mời
        List<UUID> friendIds = friendships.stream()
                .map(f -> f.getUser().getUserId().equals(currentUserId)
                        ? f.getFriend().getUserId()
                        : f.getUser().getUserId())
                .distinct()
                .toList();

        if (friendIds.isEmpty()) {
            return List.of();
        }

        // 3. Lấy ID những người đã có bản ghi trong phòng
        Set<UUID> memberIds = new HashSet<>(
                roomMemberRepository.findMemberIdsByRoomId(roomId));

        // 4. Trừ người đã ở trong phòng, chỉ giữ bạn bè có thể thêm
        List<UUID> invitableIds = friendIds.stream()
                .filter(id -> !memberIds.contains(id))
                .toList();

        // 5. Lấy thông tin người dùng để trả về frontend
        return userRepository.findAllById(invitableIds)
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    private RoomMember createRoomMember(Room room, User user, RoomRole role, RoomMemberStatus status) {
        RoomMemberId roomMemberId = new RoomMemberId();
        roomMemberId.setRoomId(room.getRoomId());
        roomMemberId.setUserId(user.getUserId());

        return RoomMember.builder()
                .room(room)
                .user(user)
                .roomMemberId(roomMemberId)
                .roomMemberRole(role)
                .status(status)
                .joinedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }

    private void deleteRoomCompletely(Room room) {
        messageRepository.deleteAllByRoomId(room.getRoomId());
        roomRepository.delete(room); // cascade xoá luôn room_members
    }

    private Room getRoomOrThrow(UUID roomId) {
        return roomRepository.findById(roomId).orElseThrow(
                () -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    private RoomMember getRoomMemberOrThrow(UUID roomId, UUID userId) {
        return roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ROOM_MEMBER));
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    // chỉ thành viên ACTIVE mới được cập nhật trạng thái
    private RoomMember getActiveMemberOrThrow(UUID roomId, UUID userId) {
        RoomMember member = getRoomMemberOrThrow(roomId, userId);
        if (member.getStatus() != RoomMemberStatus.ACTIVE) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }
        return member;
    }

    // tin nhắn phải tồn tại và thuộc đúng phòng
    private Message getMessageInRoomOrThrow(UUID roomId, UUID messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!message.getRoom().getRoomId().equals(roomId)) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        return message;
    }

    // tin nhắn mới có mới hơn mốc hiện tại không
    private boolean isNewer(Message newMessage, UUID currentMarkId) {
        if (newMessage == null || newMessage.getMessageId() == null) {
            return false;
        }
        if (currentMarkId == null) {
            return true;
        }
        // Nếu trùng đúng mốc hiện tại, không cần truy vấn DB so sánh thời gian
        if (currentMarkId.equals(newMessage.getMessageId())) {
            return false;
        }

        return messageRepository.findById(currentMarkId)
                .map(old -> newMessage.getCreatedAt().isAfter(old.getCreatedAt()))
                .orElse(true); // mốc cũ đã bị xoá thì cho ghi đè
    }

    /**
     * Tạo tin nhắn hệ thống (MessageType.SYSTEM), cập nhật hoạt động mới nhất của phòng,
     * reset trạng thái đã đọc cho các thành viên khác và phát sóng qua WebSocket.
     */
    private void createAndBroadcastSystemMessage(Room room, User actorUser, String systemContent) {
        UUID roomId = room.getRoomId();
        UUID currentUserId = actorUser.getUserId();

        Message systemMessage = Message.builder()
                .room(room)
                .sender(actorUser)
                .messageType(MessageType.SYSTEM)
                .content(systemContent)
                .isEdited(false)
                .isRevoked(false)
                .isPinned(false)
                .createdAt(LocalDateTime.now())
                .build();
        Message savedSystemMessage = messageRepository.save(systemMessage);

        LocalDateTime now = savedSystemMessage.getCreatedAt();
        room.setLastMessageId(savedSystemMessage.getMessageId());
        room.setLastMessageContent(systemContent);
        room.setLastMessageSenderId(currentUserId);
        room.setLastMessageSenderName(actorUser.getUsername());
        room.setLastMessageAt(now);
        room.setUpdatedAt(now);
        roomRepository.save(room);

        List<RoomMember> allMembers = roomMemberRepository.findByRoomMemberId_RoomId(roomId);
        for (RoomMember m : allMembers) {
            if (!m.getRoomMemberId().getUserId().equals(currentUserId)) {
                m.setLastReadMessageId(null);
            }
        }
        roomMemberRepository.saveAll(allMembers);

        MessageResponse messageResponse = messageMapper.toMessageResponse(savedSystemMessage);
        eventPublisher.publishEvent(new BroadcastEvent("/topic/room." + roomId, messageResponse));
    }
}
