package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.request.AddMemberRequest;
import com.nhom8.nien_luan_nganh.dto.request.LeaveRoomRequest;
import com.nhom8.nien_luan_nganh.dto.response.RoomMemberResponse;
import com.nhom8.nien_luan_nganh.entity.Room;
import com.nhom8.nien_luan_nganh.entity.RoomMember;
import com.nhom8.nien_luan_nganh.entity.RoomMemberId;
import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.enums.RoomMemberStatus;
import com.nhom8.nien_luan_nganh.enums.RoomRole;
import com.nhom8.nien_luan_nganh.enums.RoomType;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.mapper.RoomMemberMapper;
import com.nhom8.nien_luan_nganh.repository.RoomMemberRepository;
import com.nhom8.nien_luan_nganh.repository.RoomRepository;
import com.nhom8.nien_luan_nganh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomMemberService {
    private final UserRepository userRepository;

    private final RoomMemberRepository roomMemberRepository;

    private final RoomMemberMapper roomMemberMapper;
    private final RoomRepository roomRepository;

    //1. Thêm thành viên lúc tạo
    //2. Thêm thành viên vô nhóm đã tạo
    //3. Phê duyệt thành viên
    //4. Xoá thành viên/ Rời khỏi phòng
    //4.  1. Nếu nhóm duy nhất 1 admin, mà admin rời phòng thì phải chỉ định
    //5. Chỉnh bật/tắt phê duyệt thành viên
    //6. Xem danh sách thành viên

    //hàm cơ bản thêm một thành viên
    @Transactional
    public RoomMemberResponse addMember(Room room, UUID userId, RoomRole role, RoomMemberStatus status){
        User user = userRepository.findById(userId).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));

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
    public RoomMemberResponse approveMember(UUID roomId, UUID targetUserId, UUID currentUserId){
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new AppException(ErrorCode.ROOM_NOT_FOUND)
        );

        RoomMember actor = roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId, roomId).orElseThrow(
                () -> new AppException(ErrorCode.NOT_ROOM_MEMBER)
        );

        //phải là admin thì mới được duyệt
        if(actor.getRoomMemberRole() != RoomRole.ADMIN){
            throw new AppException(ErrorCode.NOT_ROOM_ADMIN);
        }

        //kiểm tra trường hợp coi còn đang được cần phải duyệt hông
        RoomMember target = roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(targetUserId, roomId).orElseThrow(
                () -> new AppException(ErrorCode.ROOM_MEMBER_NOT_FOUND)
        );

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
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new AppException(ErrorCode.ROOM_NOT_FOUND)
        );

        if(room.getRoomType() == RoomType.DIRECT){
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        RoomMember actor = roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId, roomId).orElseThrow(
                () -> new AppException(ErrorCode.NOT_ROOM_MEMBER)
        );

        //Nếu là admin thì kiểm tra có phải là admin duy nhất hay không
        if(actor.getRoomMemberRole() == RoomRole.ADMIN){
            long adminCount = roomMemberRepository.countByRoomMemberId_RoomIdAndRoomMemberRole(roomId, RoomRole.ADMIN);

            if(adminCount == 1){
                long memberCount = roomMemberRepository.countByRoomMemberId_RoomId(roomId);

                if(memberCount == 1){
                    //còn mình ên thì xoá luôn phòng
                    roomRepository.delete(room);
                    return;
                }


                if(request.getNewAdminId() == null){
                    throw new AppException(ErrorCode.INVALID_INPUT);
                }

                RoomMember newAdmin = roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(request.getNewAdminId(), roomId)
                        .orElseThrow(
                                () -> new AppException(ErrorCode.ROOM_MEMBER_NOT_FOUND)
                        );

                newAdmin.setRoomMemberRole(RoomRole.ADMIN);
                roomMemberRepository.save(newAdmin);
            }
        }

        roomMemberRepository.delete(actor);
    }

    //xem 1 thành viên
    @Transactional
    public RoomMemberResponse getMember(UUID roomId, UUID targetUserId, UUID currentUserId){
        //phải là thành viên thì moiws xemdđược
        roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId, roomId).orElseThrow(
                () -> new AppException(ErrorCode.NOT_ROOM_MEMBER)
        );

        return roomMemberMapper.toRoomMemberResponse(roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(targetUserId, roomId).orElseThrow(
                () -> new AppException(ErrorCode.ROOM_MEMBER_NOT_FOUND)
        ));
    }

    //xem một đống thành viên trong phòng
    @Transactional
    public List<RoomMemberResponse> getMembers(UUID roomId, RoomMemberStatus status, UUID currentUserId){
        //phải là thành viên thì moiws xemdđược
        roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId, roomId).orElseThrow(
                () -> new AppException(ErrorCode.NOT_ROOM_MEMBER)
        );

        List<RoomMember> members;
        if(status != null) {
             members = roomMemberRepository.findByRoomMemberId_RoomIdAndStatus(roomId, status);
        }
        else{
            members = roomMemberRepository.findByRoomMemberId_RoomId(roomId);
        }

        return members.stream().map(roomMemberMapper::toRoomMemberResponse).toList();
    }

    private List<RoomMemberResponse> saveMember(Room room, List<UUID> userIds, RoomRole role, RoomMemberStatus status){
        //loại bỏ trùng nhau
        List<UUID> distinctIds = userIds.stream().distinct().toList();

        //lấy danh sách người dùng từ id
        List<User> users = userRepository.findAllById(distinctIds);

        //kiểm tra người dùng
        if(users.size() != distinctIds.size()){
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        //kiểm tra người dùng có thuộc phòng hay không
        for(User user : users){
            if(roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(
                    user.getUserId(), room.getRoomId()
            )){
                throw new AppException(ErrorCode.ALREADY_IN_ROOM);
            }
        }

        //tạo danh sách roommember
        List<RoomMember> roomMembers = users.stream()
                .map(user -> createRoomMember(room, user, role, status))
                .toList();
        return roomMemberRepository.saveAll(roomMembers)
                .stream()
                .map(roomMemberMapper::toRoomMemberResponse)
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
}
