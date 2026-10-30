package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.request.AddMemberRequest;
import com.nhom8.nien_luan_nganh.dto.request.ApproveMemberRequest;
import com.nhom8.nien_luan_nganh.dto.request.ChangeMemberRoleRequest;
import com.nhom8.nien_luan_nganh.dto.request.LeaveRoomRequest;
import com.nhom8.nien_luan_nganh.dto.response.RoomMemberResponse;
import com.nhom8.nien_luan_nganh.dto.response.UserResponse;
import com.nhom8.nien_luan_nganh.entity.*;
import com.nhom8.nien_luan_nganh.enums.FriendshipStatus;
import com.nhom8.nien_luan_nganh.enums.RoomMemberStatus;
import com.nhom8.nien_luan_nganh.enums.RoomRole;
import com.nhom8.nien_luan_nganh.enums.RoomType;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.mapper.RoomMemberMapper;
import com.nhom8.nien_luan_nganh.mapper.UserMapper;
import com.nhom8.nien_luan_nganh.repository.*;
import lombok.RequiredArgsConstructor;
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

    //1. Thêm thành viên lúc tạo
    //2. Thêm thành viên vô nhóm đã tạo
    //3. Phê duyệt thành viên
    //4. Xoá thành viên/ Rời khỏi phòng
    //4.  1. Nếu nhóm duy nhất 1 admin, mà admin rời phòng thì phải chỉ định
    //5. Chỉnh bật/tắt phê duyệt thành viên
    //6. Xem danh sách thành viên

    //checkRoo

    //hàm cơ bản thêm một thành viên
    @Transactional
    public RoomMemberResponse addMember(Room room, UUID userId, RoomRole role, RoomMemberStatus status){
        User user = getUserOrThrow(userId);

        if(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, room.getRoomId())){
            throw new AppException(ErrorCode.ALREADY_IN_ROOM);
        }

        RoomMember roomMember = createRoomMember(room, user, role, status);
        roomMemberRepository.save(roomMember);

        return roomMemberMapper.toRoomMemberResponse(roomMember);
    }

    //thêm thành viên với trạng thái mặc định là ACTIVE
    @Transactional
    public RoomMemberResponse addMember(Room room, UUID userId, RoomRole role){
        return addMember(room, userId, role, RoomMemberStatus.ACTIVE);
    }

    //thêm thành viên khi tạo phòng (cơ sở 1 thành viên)
    @Transactional
    public List<RoomMemberResponse> addMembersOnCreate(Room room, List<UUID> userIds, RoomRole role){
        return saveMember(room, userIds, role, RoomMemberStatus.ACTIVE);
    }

    //thêm nhiều thành viên khi tạo phòng cùng lúc
    @Transactional
    public List<RoomMemberResponse> addMembers(Room room, List<UUID> userIds, RoomRole role){
        return saveMember(room, userIds, role, RoomMemberStatus.ACTIVE);
    }

    //thêm nhiều (1+) thành viên vào phòng đã có
    @Transactional
    public List<RoomMemberResponse> addMembers(UUID roomId, AddMemberRequest request, UUID currentUserId){
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new AppException(ErrorCode.ROOM_NOT_FOUND)
        );

        //nếu là phòng trực tiếp thì hông thêm đươc
        if(room.getRoomType() == RoomType.DIRECT)
            throw new AppException(ErrorCode.INVALID_INPUT);

        //kiểm tra người thực thi
        RoomMember actor = roomMemberRepository
                .findByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId, roomId).orElseThrow(
                        () -> new AppException(ErrorCode.NOT_ROOM_MEMBER)
                );

        RoomMemberStatus status;

        if(actor.getRoomMemberRole() == RoomRole.ADMIN){
            status = RoomMemberStatus.ACTIVE;
        } else {
            if(room.isRequireApproval()) { // nếu phải duyệt thì thêm vô là pending
                status = RoomMemberStatus.PENDING;
            } else {
                status = RoomMemberStatus.ACTIVE;
            }
        }

        return saveMember(room, request.getMemberIds(), RoomRole.MEMBER, status);
    }

    //phê duyệt thành viên
    @Transactional
    public RoomMemberResponse approveMember(UUID roomId, ApproveMemberRequest request, UUID currentUserId){
        getRoomOrThrow(roomId);

        RoomMember actor = getRoomMemberOrThrow(roomId, currentUserId);

        //phải là admin thì mới được duyệt
        if(actor.getRoomMemberRole() != RoomRole.ADMIN){
            throw new AppException(ErrorCode.NOT_ROOM_ADMIN);
        }

        //kiểm tra trường hợp coi còn đang được cần phải duyệt hông
        RoomMember target = getRoomMemberOrThrow(roomId, request.getTargetUserId());

        //không phải trạng thái được duyệt thì phát sinh lỗi
        if(target.getStatus() != RoomMemberStatus.PENDING){
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        target.setStatus(RoomMemberStatus.ACTIVE);

        roomMemberRepository.save(target);

        return roomMemberMapper.toRoomMemberResponse(target);
    }

    //xoá thành viên ra khỏi phòng
    @Transactional
    public void removeMember(UUID roomId, UUID targetUserId, UUID currentUserId){
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new AppException(ErrorCode.ROOM_NOT_FOUND)
        );

        if(room.getRoomType() == RoomType.DIRECT){
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        RoomMember actor = roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId, roomId).orElseThrow(
                () -> new AppException(ErrorCode.NOT_ROOM_MEMBER)
        );

        //chưa xử lý tự kick bản thân (hàm tự rời ở dưới)
        boolean isSelf = currentUserId.equals(targetUserId);
        if(isSelf)
            throw new AppException(ErrorCode.INVALID_INPUT);

        /// ADMIN MỚI ĐƯỢC QUYỀN XOÁ THÀNH VIÊN ///
        if(actor.getRoomMemberRole() != RoomRole.ADMIN)
            throw new AppException(ErrorCode.NOT_ROOM_ADMIN);

        RoomMember target = roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(targetUserId, roomId).orElseThrow(
                () -> new AppException(ErrorCode.NOT_ROOM_MEMBER)
        );

        roomMemberRepository.delete(target);
    }

    //tự rời
    @Transactional
    public void leaveRoom(UUID roomId, LeaveRoomRequest request, UUID currentUserId){
        Room room = getRoomOrThrow(roomId);

        if(room.getRoomType() == RoomType.DIRECT){
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        RoomMember actor = getRoomMemberOrThrow(roomId, currentUserId);

        //Nếu là admin thì kiểm tra có phải là admin duy nhất hay không
        if(actor.getRoomMemberRole() == RoomRole.ADMIN){
            long adminCount = roomMemberRepository.countByRoomMemberId_RoomIdAndRoomMemberRole(roomId, RoomRole.ADMIN);

            if(adminCount == 1){
                long memberCount = roomMemberRepository.countByRoomMemberId_RoomId(roomId);

                if(memberCount == 1){
                    //còn mình ên thì xoá luôn phòng
                    //roomRepository.delete(room);
                    deleteRoomCompletely(room);
                    return;
                }

                UUID newAdminId;
                if (request == null) {
                    newAdminId = null;
                } else {
                    newAdminId = request.getNewAdminId();
                }

                if(newAdminId == null || newAdminId.equals(currentUserId)){
                    throw new AppException(ErrorCode.INVALID_INPUT);
                }

                //người được chỉ định phải là thành veien Active
                RoomMember newAdmin = getActiveMemberOrThrow(roomId, newAdminId);
                newAdmin.setRoomMemberRole(RoomRole.ADMIN);
            }
        }

        roomMemberRepository.delete(actor);
    }

    //xem 1 thành viên
    @Transactional(readOnly = true)
    public RoomMemberResponse getMember(UUID roomId, UUID targetUserId, UUID currentUserId){
        //phải là thành viên thì moiws xemdđược
        getActiveMemberOrThrow(roomId, currentUserId);

        return roomMemberMapper.toRoomMemberResponse(getRoomMemberOrThrow(roomId, targetUserId));
    }

    //xem một đống thành viên trong phòng
    @Transactional(readOnly = true)
    public List<RoomMemberResponse> getMembers(UUID roomId, RoomMemberStatus status, UUID currentUserId){
        //phải là thành viên thì moiws xemdđược
        getActiveMemberOrThrow(roomId, currentUserId);

        List<RoomMember> members;
        if(status != null) {
             members = roomMemberRepository.findByRoomMemberId_RoomIdAndStatus(roomId, status);
        }
        else{
            members = roomMemberRepository.findByRoomMemberId_RoomId(roomId);
        }

        return members.stream().map(roomMemberMapper::toRoomMemberResponse).toList();
    }

    //đổi quyền thành viên
    @Transactional
    public RoomMemberResponse changeMemberRole(UUID roomId, UUID userId, ChangeMemberRoleRequest request, UUID currentUserId){
        //kiểm tra phòng chat tồn tại
        getRoomOrThrow(roomId);

        //phải là thành viên thì moiws xemdđược
        RoomMember actor = getActiveMemberOrThrow(roomId, currentUserId);

        //kiểm tra thành viên thay đổi có tồn tại trong group hay không
        RoomMember target = getRoomMemberOrThrow(roomId, userId);

        //kiểm tra có quyền admin hay không
        if(actor.getRoomMemberRole() != RoomRole.ADMIN){
            throw new AppException(ErrorCode.NOT_ROOM_ADMIN);
        }

        //khong cho đổi role người mà đang hông được duyệt (duyệt thì sẽ là approve)
        if(target.getStatus() != RoomMemberStatus.ACTIVE){
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        RoomRole newRole = request.getRole();

        //không làm gì nếu hông đổi quyền
        if(target.getRoomMemberRole() == newRole){
            return roomMemberMapper.toRoomMemberResponse(target);
        }

        //admin duy nhất không thể đổi quyền
        if(target.getRoomMemberRole() == RoomRole.ADMIN && newRole == RoomRole.MEMBER){
            long adminCount = roomMemberRepository.countByRoomMemberId_RoomIdAndRoomMemberRole(roomId, RoomRole.ADMIN);

            if(adminCount <= 1){
                throw new AppException(ErrorCode.INVALID_INPUT);
            }
        }

        //cập nhật vai trò
        target.setRoomMemberRole(newRole);

        return roomMemberMapper.toRoomMemberResponse(target);
    }

    //đánh dấu đã nhận tin nhắn (client nhận được qua Socket thì gọi)
    @Transactional
    public void markAsDelivered(UUID roomId, UUID messageId, UUID currentUserId){
        RoomMember member = getActiveMemberOrThrow(roomId, currentUserId);
        Message message = getMessageInRoomOrThrow(roomId, messageId);

        if(isNewer(message, currentUserId)){
            member.setLastDeliveredMessageId(messageId);
        }
    }

    //đánh dấu đã đọc khi nhấn vào phòng
    @Transactional
    public void markAsRead(UUID roomId, UUID messageId, UUID currentUserId){
        RoomMember member = getActiveMemberOrThrow(roomId, currentUserId);
        Message message = getMessageInRoomOrThrow(roomId, messageId);

        if(isNewer(message, member.getLastReadMessageId())){
            member.setLastReadMessageId(messageId);
        }

        //nếu đọc ròi thì đã nhận rồi
        if(isNewer(message, member.getLastDeliveredMessageId())){
            member.setLastDeliveredMessageId(messageId);
        }
    }


    private List<RoomMemberResponse> saveMember(Room room, List<UUID> userIds, RoomRole role, RoomMemberStatus status){
        //kiểm tra userIds
        if (userIds == null || userIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        //loại bỏ trùng nhau
        List<UUID> distinctIds = userIds.stream().distinct().toList();

        //lấy danh sách người dùng từ id
        List<User> users = userRepository.findAllById(distinctIds);

        //kiểm tra người dùng
        if(users.size() != distinctIds.size()){
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }
        //Vì mỗi ID hợp lệ tương ứng với một user tồn tại.
        //Nếu số user tìm được ít hơn số ID cần tìm thì có ít nhất một ID không tồn tại.

        //kiểm tra người dùng có thuộc phòng hay không
        if(!roomMemberRepository.findExistingUserIds(room.getRoomId(), distinctIds).isEmpty()){
            throw new AppException(ErrorCode.ALREADY_IN_ROOM);
        }
        //nếu có 1 người là thành viên thì ném lỗi

//        for(User user : users){
//            if(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(
//                    user.getUserId(), room.getRoomId()
//            )){
//                throw new AppException(ErrorCode.ALREADY_IN_ROOM);
//            }
//        }

        //tạo danh sách roommember
        List<RoomMember> roomMembers = users.stream()
                .map(user -> createRoomMember(room, user, role, status))
                .toList();
        return roomMemberRepository.saveAll(roomMembers)
                .stream()
                .map(roomMemberMapper::toRoomMemberResponse)
                .toList();
    }

    //lấy danh sách thành viên có thể thêm (tất cả bạn bè trừ những người có trong nhóm cả Active với Pending)
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
                        FriendshipStatus.ACCEPTED
        );

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
                roomMemberRepository.findMemberIdsByRoomId(roomId)
        );

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


    private RoomMember createRoomMember(Room room, User user, RoomRole role, RoomMemberStatus status){
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

    private void deleteRoomCompletely(Room room){
        messageRepository.deleteAllByRoomId(room.getRoomId());
        roomRepository.delete(room);  // cascade xoá luôn room_members
    }


    private Room getRoomOrThrow(UUID roomId){
        return roomRepository.findById(roomId).orElseThrow(
                () -> new AppException(ErrorCode.ROOM_NOT_FOUND)
        );
    }

    private RoomMember getRoomMemberOrThrow(UUID roomId, UUID userId) {
        return roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ROOM_MEMBER));
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    //chỉ thành viên ACTIVE mới được cập nhật trạng thái
    private RoomMember getActiveMemberOrThrow(UUID roomId, UUID userId) {
        RoomMember member = getRoomMemberOrThrow(roomId, userId);
        if (member.getStatus() != RoomMemberStatus.ACTIVE) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }
        return member;
    }

    //tin nhắn phải tồn tại và thuộc đúng phòng
    private Message getMessageInRoomOrThrow(UUID roomId, UUID messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!message.getRoom().getRoomId().equals(roomId)) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        return message;
    }

    //tin nhắn mới có mới hơn mốc hiện tại không
    private boolean isNewer(Message newMessage, UUID currentMarkId) {
        if (currentMarkId == null) return true;

        return messageRepository.findById(currentMarkId)
                .map(old -> newMessage.getCreatedAt().isAfter(old.getCreatedAt()))
                .orElse(true); // mốc cũ đã bị xoá thì cho ghi đè
    }
}
