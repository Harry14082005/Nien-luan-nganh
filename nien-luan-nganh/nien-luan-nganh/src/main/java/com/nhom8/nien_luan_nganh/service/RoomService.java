package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.request.CreateDirectRoomRequest;
import com.nhom8.nien_luan_nganh.dto.request.CreateGroupRequest;
import com.nhom8.nien_luan_nganh.dto.response.PageResponse;
import com.nhom8.nien_luan_nganh.dto.response.RoomDetailResponse;
import com.nhom8.nien_luan_nganh.dto.response.RoomListResponse;
import com.nhom8.nien_luan_nganh.entity.Message;
import com.nhom8.nien_luan_nganh.entity.Room;
import com.nhom8.nien_luan_nganh.entity.RoomMember;
import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.enums.RoomRole;
import com.nhom8.nien_luan_nganh.enums.RoomType;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.mapper.RoomMapper;
import com.nhom8.nien_luan_nganh.repository.MessageRepository;
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

    // tạo group
    @Transactional
    public RoomDetailResponse createGroup(CreateGroupRequest request, UUID currentUserId) {
        User creator = userRepository.findById(currentUserId).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Room room = roomMapper.toRoom(request);
        room.setRoomType(RoomType.GROUP);
        room.setCreatedBy(creator);

        roomRepository.save(room);

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

        // 2. Tạo thông tin phân trang (sắp xếp theo updatedAt DESC để phòng vừa có tin
        // nhắn mới nổi lên đầu)
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

        // 4.3. Lấy tin nhắn mới nhất của các phòng trong trang hiện tại bằng 1 query
        Map<UUID, Message> latestMessageByRoomId = new HashMap<>();
        List<Message> latestMessages = messageRepository.findLatestMessagesByRoomIds(roomIds);
        for (Message msg : latestMessages) {
            // putIfAbsent: nếu có rồi thì thôi, không replace (2 phong co tin nhan cung
            // luc)
            latestMessageByRoomId.putIfAbsent(msg.getRoom().getRoomId(), msg);
        }

        // 4.4. Đếm số tin nhắn chưa đọc của từng phòng bằng 1 query gộp GROUP BY
        Map<UUID, Long> unreadCountByRoomId = new HashMap<>();
        List<Object[]> unreadRows = messageRepository.countUnreadMessagesByRoomIds(roomIds, currentUserId);
        for (Object[] row : unreadRows) {
            UUID rId = (UUID) row[0];
            Long count = (Long) row[1];
            unreadCountByRoomId.put(rId, count);
        }

        // 5. Chuyển sang DTO và làm giàu dữ liệu hiển thị bên ngoài (Sidebar)
        Page<RoomListResponse> responses = rooms.map(room -> {
            RoomListResponse item = roomMapper.toRoomListResponse(room);
            UUID roomId = room.getRoomId();

            // Nếu là phòng DIRECT (1-1) và chưa có tên/avatar riêng thì hiển thị tên/avatar
            // của đối phương
            if (room.getRoomType() == RoomType.DIRECT) {
                User peer = peerUserByRoomId.get(roomId);
                if (peer != null) {
                    if (item.getRoomName() == null || item.getRoomName().isBlank()) {
                        item.setRoomName(peer.getUsername());
                    }
                    if (item.getRoomAvatarURL() == null || item.getRoomAvatarURL().isBlank()) {
                        item.setRoomAvatarURL(peer.getAvatarUrl());
                    }
                }
            }

            // Gán tin nhắn mới nhất bên ngoài
            Message lastMsg = latestMessageByRoomId.get(roomId);
            if (lastMsg != null) {
                item.setLastMessageId(lastMsg.getMessageId());
                item.setLastMessageContent(Boolean.TRUE.equals(lastMsg.getIsRevoked())
                        ? "Tin nhắn đã bị thu hồi"
                        : lastMsg.getContent());
                item.setLastMessageSenderName(lastMsg.getSender() != null
                        ? lastMsg.getSender().getUsername()
                        : null);
                item.setLastMessageAt(lastMsg.getCreatedAt());
            }

            // Gán mốc đã nhận / đã đọc của thành viên hiện tại
            RoomMember myMember = myMemberByRoomId.get(roomId);
            if (myMember != null) {
                item.setLastDeliveredMessageId(myMember.getLastDeliveredMessageId());
                item.setLastReadMessageId(myMember.getLastReadMessageId());
            }

            // Gán số tin chưa đọc và cờ in đậm (hasUnread)
            long unreadCount = unreadCountByRoomId.getOrDefault(roomId, 0L);
            item.setUnreadCount(unreadCount);
            item.setHasUnread(unreadCount > 0);

            return item;
        });

        // 6. Trả kết quả phân trang
        return PageResponse.from(responses);
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
