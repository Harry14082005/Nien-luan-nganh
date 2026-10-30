package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.request.AddPollOptionRequest;
import com.nhom8.nien_luan_nganh.dto.request.CreatePollRequest;
import com.nhom8.nien_luan_nganh.dto.request.VotePollRequest;
import com.nhom8.nien_luan_nganh.dto.response.PollOptionResponse;
import com.nhom8.nien_luan_nganh.dto.response.PollResponse;
import com.nhom8.nien_luan_nganh.entity.*;
import com.nhom8.nien_luan_nganh.enums.MessageType;
import com.nhom8.nien_luan_nganh.enums.RoomMemberStatus;
import com.nhom8.nien_luan_nganh.enums.RoomType;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.mapper.PollMapper;
import com.nhom8.nien_luan_nganh.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PollService {
    private static final int MAX_OPTIONS = 10;

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final PollRepository pollRepository;
    private final MessageRepository messageRepository;
    private final PollMapper pollMapper;
    private final PollVoteRepository pollVoteRepository;
    //Tạo poll
    @Transactional
    public PollResponse createPoll(UUID roomId, CreatePollRequest request, UUID currentUserId) {
        Room room = getRoomOrThrow(roomId);

        //nếu là phòng DIRECT thì không có poll
        if (room.getRoomType() == RoomType.DIRECT) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        RoomMember actor = getActiveMemberOrThrow(roomId, currentUserId);

        //kiểm tra đầu vào
        List<String> texts = normalizeOptions(request.getOptionIds());
        if(texts.size() < 2){
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        if(texts.size() > MAX_OPTIONS){
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        //tạo poll
        Poll poll = Poll.builder()
                .room(room)
                .createdBy(actor.getUser())
                .question(request.getQuestion().trim())
                .build();
        //thêm lựa chọn vô
        for (String text : texts){
            PollOption option = PollOption.builder()
                    .poll(poll)
                    .optionText(text)
                    .build();
            poll.getOptions().add(option);
        }

        pollRepository.save(poll);

        //tạo thành dạng tin nhắn
        Message message = Message.builder()
                .room(room)
                .sender(actor.getUser())
                .messageType(MessageType.POLL)
                .content(poll.getQuestion())
                .build();
        messageRepository.save(message);

        poll.setMessage(message);

        ///xử lý Websocket
        ////

        //xử lý cập nhật tin nhắn mới nhất

        return toResponse(poll, currentUserId);
    }

    // Thêm lựa chọn sau khi tạo
    @Transactional
    public PollResponse addOption(UUID roomId, UUID pollId, AddPollOptionRequest request, UUID currentUserId) {
        return null;
    }

    // Bỏ phiếu / đổi phiếu
    // Client gửi toàn bộ danh sách option đang chọn. Danh sách rỗng = rút phiếu.
    @Transactional
    public PollResponse vote(UUID roomId, UUID pollId, VotePollRequest request, UUID currentUserId) {
        return null;
    }

    // Đóng poll: người tạo hoặc admin
    @Transactional
    public PollResponse closePoll(UUID roomId, UUID pollId, UUID currentUserId){
        return null;
    }

    // Xem
    @Transactional(readOnly = true)
    public PollResponse getPoll(UUID roomId, UUID pollId, UUID currentUserId) {
        return null;
    }

    @Transactional(readOnly = true)
    public List<PollResponse> getPolls(UUID roomId, UUID currentUserId) {
        return null;
    }

    /// các hàm phụ
    private Room getRoomOrThrow(UUID roomId){
        return roomRepository.findById(roomId).orElseThrow(
                () -> new AppException(ErrorCode.ROOM_NOT_FOUND)
        );
    }

    private RoomMember getRoomMemberOrThrow(UUID roomId, UUID userId) {
        return roomMemberRepository.findByRoomMemberId_UserIdAndRoomMemberId_RoomId(userId, roomId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_ROOM_MEMBER));
    }
    private RoomMember getActiveMemberOrThrow(UUID roomId, UUID userId) {
        RoomMember member = getRoomMemberOrThrow(roomId, userId);
        if (member.getStatus() != RoomMemberStatus.ACTIVE) {
            throw new AppException(ErrorCode.NOT_ROOM_MEMBER);
        }
        return member;
    }

    // trim, bỏ chuỗi rỗng, bỏ trùng (không phân biệt hoa thường)
    private List<String> normalizeOptions(List<String> raw) {
        Map<String, String> unique = new LinkedHashMap<>();

        for (String item : raw) {
            if (item == null) {
                continue;
            }
            String text = item.trim();
            if (text.isEmpty()) {
                continue;
            }
            String key = text.toLowerCase();
            if (!unique.containsKey(key)) {
                unique.put(key, text);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private Poll getPollInRoomOrThrow(UUID roomId, UUID pollId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new AppException(ErrorCode.POLL_NOT_FOUND));

        if (!poll.getRoom().getRoomId().equals(roomId)) {
            throw new AppException(ErrorCode.POLL_NOT_FOUND);
        }
        return poll;
    }

    private PollResponse toResponse(Poll poll, UUID currentUserId) {
        UUID pollId = poll.getPollId();

        PollResponse response = pollMapper.toPollResponse(poll);

        // số phiếu của từng option (1 query)
        Map<UUID, Long> voteCounts = new HashMap<>();
        List<Object[]> rows = pollVoteRepository.countVotesByOption(pollId);
        for (Object[] row : rows) {
            UUID optionId = (UUID) row[0];
            Long count = (Long) row[1];
            voteCounts.put(optionId, count);
        }

        // các option tôi đã chọn
        Set<UUID> myOptionIds = new HashSet<>();
        List<PollVote> myVotes = pollVoteRepository
                .findByPollOption_Poll_PollIdAndUser_UserId(pollId, currentUserId);
        for (PollVote vote : myVotes) {
            myOptionIds.add(vote.getPollOption().getPollOptionId());
        }

        // điền vào từng option của response
        for (PollOptionResponse optionResponse : response.getOptions()) {
            UUID optionId = optionResponse.getPollOptionId();

            Long count = voteCounts.get(optionId);
            if (count == null) {
                optionResponse.setVoteCount(0);
            } else {
                optionResponse.setVoteCount(count);
            }

            optionResponse.setVotedByMe(myOptionIds.contains(optionId));
        }

        response.setTotalVoters(pollVoteRepository.countVoters(pollId));
        return response;
    }

}
