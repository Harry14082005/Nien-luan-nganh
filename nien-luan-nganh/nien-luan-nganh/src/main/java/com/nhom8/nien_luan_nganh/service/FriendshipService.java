package com.nhom8.nien_luan_nganh.service;

import com.nhom8.nien_luan_nganh.dto.request.FriendshipRequest;
import com.nhom8.nien_luan_nganh.dto.response.FriendshipResponse;
import com.nhom8.nien_luan_nganh.entity.Friendship;
import com.nhom8.nien_luan_nganh.entity.User;
import com.nhom8.nien_luan_nganh.enums.FriendshipStatus;
import com.nhom8.nien_luan_nganh.exception.AppException;
import com.nhom8.nien_luan_nganh.exception.ErrorCode;
import com.nhom8.nien_luan_nganh.mapper.FriendshipMapper;
import com.nhom8.nien_luan_nganh.repository.FriendshipRepository;
import com.nhom8.nien_luan_nganh.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service xử lý toàn bộ nghiệp vụ Phân hệ Kết nối Bạn bè (Friendship Module):
 * - Gửi lời mời kết bạn (POST /friendships/request).
 * - Chấp nhận lời mời kết bạn (PUT /friendships/{friendshipId}/accept).
 * - Từ chối lời mời kết bạn (PUT /friendships/{friendshipId}/decline).
 * - Hủy kết bạn hoặc thu hồi lời mời kết bạn (DELETE /friendships/{friendshipId}).
 * - Chặn người dùng (POST /friendships/block) và Bỏ chặn (DELETE /friendships/block/{userId}).
 * - Lấy danh sách quan hệ theo trạng thái: Bạn bè (ACCEPTED), Lời mời (PENDING), Đã chặn (BLOCKED).
 */
@Service
public class FriendshipService {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipMapper friendshipMapper;

    /**
     * Nghiệp vụ 1: Gửi lời mời kết bạn
     * @param currentUserId Khóa chính UUID của người gửi lời mời
     * @param request       Chứa friendId (người nhận lời mời)
     * @return FriendshipResponse Thông tin lời mời kết bạn vừa tạo
     */
    @Transactional
    public FriendshipResponse sendFriendRequest(UUID currentUserId, FriendshipRequest request) {
        UUID targetUserId = request.getFriendId();

        // 1. Không tự kết bạn với chính mình
        if (currentUserId.equals(targetUserId)) {
            throw new AppException(ErrorCode.CANNOT_ADD_YOURSELF);
        }

        // 2. Lấy thông tin 2 người dùng
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 3. Kiểm tra xem giữa 2 người đã từng có mối quan hệ nào chưa
        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(currentUserId, targetUserId);

        if (existing.isPresent()) {
            Friendship friendship = existing.get();

            // Nếu đã là bạn bè
            if (friendship.getFriendshipStatus() == FriendshipStatus.ACCEPTED) {
                throw new AppException(ErrorCode.ALREADY_FRIENDS);
            }

            // Nếu đang bị chặn
            if (friendship.getFriendshipStatus() == FriendshipStatus.BLOCKED) {
                throw new AppException(ErrorCode.USER_BLOCKED);
            }

            // Nếu đang có lời mời kết bạn chờ xử lý giữa 2 người (bất kể ai gửi trước)
            if (friendship.getFriendshipStatus() == FriendshipStatus.PENDING) {
                throw new AppException(ErrorCode.FRIEND_REQUEST_EXISTED);
            }

            // Nếu trước đó từng bị từ chối (DECLINED) -> Cho phép gửi lại lời mời
            if (friendship.getFriendshipStatus() == FriendshipStatus.DECLINED) {
                friendship.setUser(currentUser);
                friendship.setFriend(targetUser);
                friendship.setFriendshipStatus(FriendshipStatus.PENDING);
                Friendship saved = friendshipRepository.save(friendship);
                return friendshipMapper.toFriendshipResponse(saved, currentUserId);
            }
        }

        // 4. Nếu chưa từng có quan hệ -> Tạo mới lời mời kết bạn
        Friendship newFriendship = Friendship.builder()
                .user(currentUser)
                .friend(targetUser)
                .friendshipStatus(FriendshipStatus.PENDING)
                .build();

        Friendship saved = friendshipRepository.save(newFriendship);
        return friendshipMapper.toFriendshipResponse(saved, currentUserId);
    }
        

    /**
     * Nghiệp vụ 2: Chấp nhận lời mời kết bạn.
     * Chỉ người nhận lời mời (friend) mới có quyền bấm chấp nhận.
     *
     * @param currentUserId Khóa chính UUID của người đang đăng nhập (phải là người nhận)
     * @param friendshipId  Khóa chính UUID của mối quan hệ
     * @return FriendshipResponse Thông tin mối quan hệ sau khi đã chuyển sang ACCEPTED
     */
    @Transactional
    public FriendshipResponse acceptFriendRequest(UUID currentUserId, UUID friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new AppException(ErrorCode.FRIENDSHIP_NOT_FOUND));

        // Kiểm tra quyền: chỉ người nhận (friend) mới có quyền bấm chấp nhận
        if (!friendship.getFriend().getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.NOT_FRIEND_REQUEST_RECEIVER);
        }

        // Kiem tra xem co dang duoc gui loi moi hong
        if (friendship.getFriendshipStatus() != FriendshipStatus.PENDING) {
            if (friendship.getFriendshipStatus() == FriendshipStatus.BLOCKED) {
                throw new AppException(ErrorCode.USER_BLOCKED);
            }
            if (friendship.getFriendshipStatus() == FriendshipStatus.ACCEPTED) {
                throw new AppException(ErrorCode.ALREADY_FRIENDS);
            }
            throw new AppException(ErrorCode.INVALID_REQUEST_STATUS);
        }

        friendship.setFriendshipStatus(FriendshipStatus.ACCEPTED);
        Friendship updated = friendshipRepository.save(friendship);
        return friendshipMapper.toFriendshipResponse(updated, currentUserId);
    }

    /**
     * Nghiệp vụ 3: Từ chối lời mời kết bạn.
     *
     * @param currentUserId Khóa chính UUID của người nhận lời mời
     * @param friendshipId  Khóa chính UUID của mối quan hệ
     * @return FriendshipResponse Thông tin sau khi chuyển sang DECLINED
     */
    @Transactional
    public FriendshipResponse declineFriendRequest(UUID currentUserId, UUID friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new AppException(ErrorCode.FRIENDSHIP_NOT_FOUND));

        if (!friendship.getFriend().getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.NOT_FRIEND_REQUEST_RECEIVER);
        }

        //Phai dang trong trang thai cho duyet moi duoc tu choi
        if(friendship.getFriendshipStatus() != FriendshipStatus.PENDING){
            throw new AppException(ErrorCode.INVALID_REQUEST_STATUS);
        }

        friendship.setFriendshipStatus(FriendshipStatus.DECLINED);
        Friendship updated = friendshipRepository.save(friendship);
        return friendshipMapper.toFriendshipResponse(updated, currentUserId);
    }

    /**
     * Nghiệp vụ 4: Hủy kết bạn hoặc Thu hồi lời mời đã gửi.
     * Kiểm tra bảo mật chặt chẽ:
     * - Không cho phép thao tác nếu quan hệ đang là BLOCKED (chặn người bị block tự giải thoát).
     * - Trạng thái PENDING: Chỉ người gửi (user) mới có quyền thu hồi lời mời.
     * - Trạng thái ACCEPTED: Cả hai bên đều có quyền hủy kết bạn.
     *
     * @param currentUserId Khóa chính UUID của người đang thực hiện
     * @param friendshipId  Khóa chính UUID của mối quan hệ
     */
    @Transactional
    public void cancelOrUnfriend(UUID currentUserId, UUID friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new AppException(ErrorCode.FRIENDSHIP_NOT_FOUND));

        // 1. Chặn tuyệt đối nếu mối quan hệ đang bị chặn (Block)
        // Tuyệt đối không cho người bị chặn tự ý xóa để tự gỡ block!
        if (friendship.getFriendshipStatus() == FriendshipStatus.BLOCKED) {
            throw new AppException(ErrorCode.USER_BLOCKED);
        }

        // 2. Kiểm tra quyền hạn theo từng trạng thái cụ thể
        switch (friendship.getFriendshipStatus()) {
            case PENDING -> {
                // Khi đang chờ duyệt: Chỉ người gửi lời mời mới có quyền thu hồi
                if (!friendship.getUser().getUserId().equals(currentUserId)) {
                    throw new AppException(ErrorCode.UNAUTHORIZED);
                }
            }
            case ACCEPTED -> {
                // Khi đã là bạn bè: Bất kỳ ai trong 2 người đều có quyền bấm Hủy kết bạn
                boolean isParticipant = friendship.getUser().getUserId().equals(currentUserId)
                        || friendship.getFriend().getUserId().equals(currentUserId);
                if (!isParticipant) {
                    throw new AppException(ErrorCode.UNAUTHORIZED);
                }
            }
            default -> throw new AppException(ErrorCode.INVALID_REQUEST_STATUS);
        }

        // 3. Tiến hành xóa khỏi CSDL đưa về trạng thái không quan hệ
        friendshipRepository.delete(friendship);
    }

    /**
     * Nghiệp vụ 5: Chặn người dùng.
     *
     * @param currentUserId Khóa chính UUID của người chặn
     * @param targetUserId  Khóa chính UUID của người bị chặn
     * @return FriendshipResponse Thông tin sau khi chuyển sang BLOCKED
     */
    @Transactional
    public FriendshipResponse blockUser(UUID currentUserId, UUID targetUserId) {
        // 1. Không cho phép tự chặn chính bản thân
        if (currentUserId.equals(targetUserId)) {
            throw new AppException(ErrorCode.CANNOT_BLOCK_YOURSELF);
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(currentUserId, targetUserId);

        Friendship friendship;
        if (existing.isPresent()) {
            friendship = existing.get();

            // 2. Chặn lỗi ghi đè: Nếu đối phương ĐÃ CHẶN MÌNH trước đó
            if (friendship.getFriendshipStatus() == FriendshipStatus.BLOCKED) {
                // Đối phương là người chặn (user) -> Mình (friend) không được phép ghi đè
                if (friendship.getUser().getUserId().equals(targetUserId)) {
                    throw new AppException(ErrorCode.BLOCKED_BY_USER);
                }
                // Nếu chính mình đã chặn họ từ trước đó rồi
                throw new AppException(ErrorCode.ALREADY_BLOCKED);
            }

            // 3. Đảo chiều quan hệ: Đảm bảo currentUser luôn giữ vai trò người chặn (user)
            friendship.setUser(currentUser);
            friendship.setFriend(targetUser);
            friendship.setFriendshipStatus(FriendshipStatus.BLOCKED);
        } else {
            // Chưa có tương tác nào -> Tạo mới bản ghi BLOCKED
            friendship = Friendship.builder()
                    .user(currentUser)
                    .friend(targetUser)
                    .friendshipStatus(FriendshipStatus.BLOCKED)
                    .build();
        }

        Friendship saved = friendshipRepository.save(friendship);
        return friendshipMapper.toFriendshipResponse(saved, currentUserId);
    }

    /**
     * Nghiệp vụ 6: Bỏ chặn người dùng.
     * Chỉ người đã thực hiện hành động chặn mới có quyền bỏ chặn.
     *
     * @param currentUserId Khóa chính UUID của người bỏ chặn
     * @param targetUserId  Khóa chính UUID của người đang bị chặn
     */
    @Transactional
    public void unblockUser(UUID currentUserId, UUID targetUserId) {
        // Kiem tra xem nguoi bo chan co phai la nguoi chan hong
        if (currentUserId.equals(targetUserId)) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // 1. Tìm quan hệ 2 chiều, nếu không có thì báo lỗi ngay
        Friendship friendship = friendshipRepository.findBetweenUsers(currentUserId, targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.FRIENDSHIP_NOT_FOUND));

        // 2. Kiểm tra trạng thái: Phải đang bị BLOCKED mới được gỡ
        if (friendship.getFriendshipStatus() != FriendshipStatus.BLOCKED) {
            throw new AppException(ErrorCode.INVALID_REQUEST_STATUS);
        }

        // 3. Kiểm tra quyền hạn: Chỉ người chủ động chặn (user) mới có quyền gỡ chặn
        if (!friendship.getUser().getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 4. Xóa bản ghi đưa quan hệ về NONE (người lạ)
        friendshipRepository.delete(friendship);
    }

    /**
     * Nghiệp vụ 7: Lấy danh sách mối quan hệ theo trạng thái.
     * - status = ACCEPTED: Danh sách bạn bè hiện tại.
     * - status = PENDING: Danh sách lời mời (cả gửi đi và nhận về, có trường direction phân biệt).
     * - status = BLOCKED: Danh sách người dùng mà tôi đã chặn.
     *
     * @param currentUserId Khóa chính UUID của người đang đăng nhập
     * @param status        Trạng thái cần lọc (mặc định ACCEPTED nếu null)
     * @return Danh sách FriendshipResponse
     */
    @Transactional(readOnly = true)
    public List<FriendshipResponse> getFriendships(UUID currentUserId, FriendshipStatus status) {
        FriendshipStatus targetStatus = (status != null) ? status : FriendshipStatus.ACCEPTED;

        List<Friendship> friendships;
        if (targetStatus == FriendshipStatus.BLOCKED) {
            // Chỉ lấy những người mà chính tôi đã chặn
            friendships = friendshipRepository.findBlockedList(currentUserId, FriendshipStatus.BLOCKED);
        } else {
            // Với ACCEPTED hoặc PENDING: Lấy 2 chiều (cả gửi và nhận)
            friendships = friendshipRepository.findTwoWayFriendships(currentUserId, targetStatus);
        }

        return friendships.stream()
                .map(f -> friendshipMapper.toFriendshipResponse(f, currentUserId))
                .toList();
    }

    /**
     * Nghiệp vụ 8: Kiểm tra trạng thái quan hệ cụ thể giữa người đang đăng nhập và một người khác.
     * Dùng để Frontend biết chính xác cần hiển thị nút gì trên trang cá nhân của đối phương:
     * - Chưa có quan hệ: Trả về null -> Frontend hiện nút [Thêm bạn bè].
     * - PENDING + OUTGOING: Frontend hiện nút [Đã gửi lời mời (Bấm để Hủy)].
     * - PENDING + INCOMING: Frontend chỉ hiện 2 nút [Xác nhận] và [Xóa yêu cầu] (KHÔNG hiện nút Thêm bạn bè).
     * - ACCEPTED: Frontend hiện nút [Bạn bè / Nhắn tin].
     * - BLOCKED: Frontend hiện nút [Đã chặn].
     *
     * @param currentUserId Khóa chính UUID của người đang đăng nhập
     * @param targetUserId  Khóa chính UUID của người cần kiểm tra
     * @return FriendshipResponse Thông tin quan hệ hiện tại (hoặc null nếu chưa có quan hệ)
     */
    @Transactional(readOnly = true)
    public FriendshipResponse getFriendshipStatusWithUser(UUID currentUserId, UUID targetUserId) {
        // 1. Nếu tự kiểm tra chính mình -> không có quan hệ bạn bè, return null ngay (tiết kiệm 1 query DB)
        if (currentUserId.equals(targetUserId)) {
            return null;
        }

        // 2. Kiểm tra xem người dùng cần xem trạng thái có tồn tại không
        if (!userRepository.existsById(targetUserId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        // 3. Tìm quan hệ 2 chiều và ánh xạ sang FriendshipResponse (kèm direction)
        return friendshipRepository.findBetweenUsers(currentUserId, targetUserId)
                .map(f -> friendshipMapper.toFriendshipResponse(f, currentUserId))
                .orElse(null);
    }
}
