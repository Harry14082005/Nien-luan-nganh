package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.request.CreateDirectRoomRequest;
import com.nhom8.nien_luan_nganh.dto.request.CreateGroupRequest;
import com.nhom8.nien_luan_nganh.dto.response.PageResponse;
import com.nhom8.nien_luan_nganh.dto.response.RoomDetailResponse;
import com.nhom8.nien_luan_nganh.dto.response.RoomListResponse;
import com.nhom8.nien_luan_nganh.entity.Room;
import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.enums.RoomRole;
import com.nhom8.nien_luan_nganh.enums.RoomType;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.mapper.RoomMapper;
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomMapper roomMapper;
    private final RoomMemberService roomMemberService;
    private final RoomMemberRepository roomMemberRepository;

    //tạo group
    @Transactional
    public RoomDetailResponse createGroup(CreateGroupRequest request, UUID currentUserId){
        User creator = userRepository.findById(currentUserId).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Room room = roomMapper.toRoom(request);
        room.setRoomType(RoomType.GROUP);
        room.setCreatedBy(creator);

        roomRepository.save(room);

        /// xử lý thêm danh sách thành viên
        //người tạo là Admin
        roomMemberService.addMember(room, currentUserId, RoomRole.ADMIN);
        //những người được thêm là Member
        if(request.getMemberIds() != null && !request.getMemberIds().isEmpty())
            roomMemberService.addMembersOnCreate(room, request.getMemberIds(), RoomRole.MEMBER);

        return roomMapper.toRoomDetailResponse(room);
    }

    //tạo nhóm 2 người trực tiếp
    @Transactional
    public RoomDetailResponse createDirectRoom(CreateDirectRoomRequest request, UUID currentUserId){
        if(request.getUserId() == null)
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        if(request.getUserId().equals(currentUserId))
            throw  new AppException(ErrorCode.CANNOT_ADD_YOURSELF);

        //kiểm tra người được thêm tồn tại
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
    public PageResponse<RoomListResponse> getRooms(UUID currentUserId, RoomType type, int pageNo, int pageSize){
        //1. kiểm tra người dùng
        if(!userRepository.existsById(currentUserId)){
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        //2. Tạo thông tin phân trang
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.Direction.DESC, "createdAt");

        //3. Lấy danh sách phòng
        Page<Room> rooms;

        if(type == null){
            rooms = roomRepository.findDistinctByRoomMembers_User_UserId(currentUserId, pageable);
        }
        else{
            rooms = roomRepository.findDistinctByRoomMembers_User_UserIdAndRoomType(currentUserId, type, pageable);
        }

        //4. Chuyển sang DTO
        Page<RoomListResponse> responses = rooms.map(roomMapper::toRoomListResponse);

        //5. Trả kết quả phân trang
        return PageResponse.from((responses));
    }

    //lấy thông
    @Transactional
    public RoomDetailResponse getRoom(UUID roomId, UUID currentUserId){
        //1. kiểm tra người dùng
        if(!userRepository.existsById(currentUserId)){
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        //2. kiểm tra người dùng có trong room hay không
        if(!roomMemberRepository.existsByRoomMemberId_UserIdAndRoomMemberId_RoomId(currentUserId, roomId)){
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }

        //3. lấy thông tin phòng
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new AppException(ErrorCode.ROOM_NOT_FOUND)
        );

        //4. Chuyển sang DTO
        return roomMapper.toRoomDetailResponse(room);
    }
}
