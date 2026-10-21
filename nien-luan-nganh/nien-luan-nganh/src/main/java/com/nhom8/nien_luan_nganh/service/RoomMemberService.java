package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.request.AddMemberRequest;
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
    UserRepository userRepository;

    RoomMemberRepository roomMemberRepository;

    RoomMemberMapper roomMemberMapper;
    RoomRepository roomRepository;

    //hàm cơ bản thêm một thành viên
    @Transactional
    public RoomMemberResponse addMember(Room room, UUID userId, RoomRole role, RoomMemberStatus status){
        User user = userRepository.findById(userId).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if(roomMemberRepository.existsById_UserIdAndId_RoomId(userId, room.getRoomId())){
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
    public RoomMemberResponse addMembersOnCreate(Room room, UUID userId, RoomRole role){
        return addMember(room, userId, role);
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
                .findById_UserIdAndId_RoomId(currentUserId, roomId).orElseThrow(
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
            if(roomMemberRepository.existsById_UserIdAndId_RoomId(
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
                .status(RoomMemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }
}
