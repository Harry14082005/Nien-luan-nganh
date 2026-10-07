# API Documentation

> Danh sách REST API và WebSocket API của hệ thống.

### **Auth (`/auth`)**

- `POST /auth/register` : Đăng ký tài khoản (Quyền: Công khai; Chốt SĐT có bắt buộc không).
- `POST /auth/login` : Đăng nhập, trả accessToken và refreshToken (Quyền: Công khai; Đổi email thành identifier (email hoặc SĐT); thêm 403 khi tài khoản bị khóa).
- `POST /auth/refresh` : Làm mới access token (Quyền: Công khai).
- `POST /auth/logout` : Đăng xuất, thu hồi refresh token (Quyền: Đăng nhập; Thêm body { refreshToken }).

### **Users (`/users`)**

- `GET /users/me` : Lấy thông tin bản thân (Quyền: Đăng nhập; Schema User thiếu lastSeenAt, role, isBanned).
- `PATCH /users/me` : Cập nhật hồ sơ, avatar, ngôn ngữ ưa thích (Quyền: Đăng nhập; Avatar nên upload qua POST /uploads).
- `GET /users/search` : Tìm người dùng theo email hoặc SĐT (tham số q) (Quyền: Đăng nhập).
- `GET /users/{userId}` : Xem profile người dùng (Quyền: Đăng nhập).

### **Friendship (`/friendships`)**

- `GET /friendships` : Danh sách bạn bè, lọc theo status (accepted, pending, blocked) (Quyền: Đăng nhập; Trả thêm friendshipId và direction (lời mời đến hay đi)).
- `POST /friendships/request` : Gửi lời mời kết bạn (Quyền: Đăng nhập; Thêm 403 BLOCKED).
- `PATCH /friendships/{friendshipId}/accept` : Chấp nhận lời mời kết bạn (Quyền: Đăng nhập).
- `PATCH /friendships/{friendshipId}/decline` : Từ chối lời mời kết bạn (Quyền: Đăng nhập; xlsx chưa ghi).
- `DELETE /friendships/{friendshipId}` : Hủy kết bạn hoặc rút lời mời (Quyền: Đăng nhập; xlsx chưa ghi).
- `POST /friendships/block` : Chặn người dùng (Quyền: Đăng nhập).
- `DELETE /friendships/block/{userId}` : Bỏ chặn người dùng (Quyền: Đăng nhập; Cần bổ sung).

### **Rooms (`/rooms`)**

- `GET /rooms` : Danh sách phòng của tôi (Quyền: Đăng nhập; Tùy chọn: thêm lastMessage, unreadCount).
- `POST /rooms` : Tạo phòng nhóm (Quyền: Đăng nhập).
- `POST /rooms/direct` : Tạo hoặc lấy phòng chat 1:1 (Quyền: Đăng nhập; Thêm 403 BLOCKED).
- `GET /rooms/search` : Tìm cuộc trò chuyện theo tên phòng hoặc nội dung (Quyền: Đăng nhập; Cần bổ sung).
- `GET /rooms/{roomId}` : Lấy thông tin phòng (Quyền: Đăng nhập).
- `PATCH /rooms/{roomId}` : Đổi tên, avatar, theme phòng (Quyền: Admin phòng; xlsx chưa ghi).
- `DELETE /rooms/{roomId}` : Xóa phòng (Quyền: Owner phòng).
- `GET /rooms/{roomId}/members` : Danh sách thành viên và role (Quyền: Đăng nhập).
- `POST /rooms/{roomId}/members` : Thêm thành viên vào phòng (Quyền: Đăng nhập).
- `DELETE /rooms/{roomId}/members/{userId}` : Xóa thành viên khỏi phòng (Quyền: Admin phòng).
- `PATCH /rooms/{roomId}/members/{userId}/role` : Đổi quyền thành viên (Owner, Admin, Member) (Quyền: Owner/Admin phòng (gợi ý); Cần bổ sung).
- `POST /rooms/{roomId}/leave` : Rời phòng (Quyền: Đăng nhập; xlsx chưa ghi).

### **Messages (`/rooms`)**

- `GET /rooms/{roomId}/messages` : Lịch sử tin nhắn, phân trang cursor (Quyền: Đăng nhập).
- `POST /rooms/{roomId}/messages` : Gửi tin nhắn (text, image, file, reply) (Quyền: Đăng nhập; Thêm 403 BLOCKED; schema Message thiếu isRevoked, fileName, fileSize, mimeType, isPinned).
- `PATCH /rooms/{roomId}/messages/{messageId}` : Chỉnh sửa tin nhắn (Quyền: Đăng nhập; Ghi rõ ai được phép, thêm 403).
- `DELETE /rooms/{roomId}/messages/{messageId}` : Thu hồi tin nhắn (Quyền: Đăng nhập; Ghi rõ ai được phép, thêm 403).
- `GET /rooms/{roomId}/messages/{messageId}/translate` : Dịch tin nhắn (cache hoặc gọi AI) (Quyền: Đăng nhập).
- `GET /rooms/{roomId}/messages/search` : Tìm kiếm tin nhắn trong phòng (Quyền: Đăng nhập).
- `GET /rooms/{roomId}/messages/{messageId}/history` : Xem lịch sử chỉnh sửa của tin nhắn (Quyền: Đăng nhập; Cần bổ sung; Dựa trên bảng MessageHistory).
- `DELETE /rooms/{roomId}/messages` : Xóa lịch sử chat (phía mình) (Quyền: Đăng nhập; Cần bổ sung).
- `POST /rooms/{roomId}/messages/{messageId}/pin` : Ghim tin nhắn (Quyền: Cần chốt; Cần bổ sung; Cần bảng PinnedMessage).
- `DELETE /rooms/{roomId}/messages/{messageId}/pin` : Bỏ ghim tin nhắn (Quyền: Cần chốt; Cần bổ sung).
- `GET /rooms/{roomId}/pins` : Danh sách tin nhắn đã ghim (Quyền: Đăng nhập; Cần bổ sung).

### **Uploads (`/uploads`)**

- `POST /uploads` : Upload ảnh, file, avatar (multipart); trả url, fileName, size, mimeType (Quyền: Đăng nhập; Cần bổ sung).

### **Polls (`/rooms`)**

- `GET /rooms/{roomId}/polls` : Danh sách poll trong phòng (Quyền: Đăng nhập).
- `POST /rooms/{roomId}/polls` : Tạo poll thủ công hoặc xác nhận poll do AI gợi ý (Quyền: Đăng nhập).
- `GET /rooms/{roomId}/polls/{pollId}` : Xem chi tiết 1 poll (Quyền: Đăng nhập; Nên có thêm).
- `POST /rooms/{roomId}/polls/{pollId}/vote` : Bình chọn (Quyền: Đăng nhập).
- `PATCH /rooms/{roomId}/polls/{pollId}/close` : Đóng poll (Quyền: Người tạo hoặc Admin phòng; xlsx chưa ghi).

### **Reminders (`/rooms`)**

- `GET /rooms/{roomId}/reminders` : Danh sách lịch hẹn trong phòng (Quyền: Đăng nhập).
- `POST /rooms/{roomId}/reminders` : Tạo lịch hẹn thủ công hoặc xác nhận từ AI (Quyền: Đăng nhập).
- `GET /rooms/{roomId}/reminders/{reminderId}` : Xem chi tiết 1 lịch hẹn (Quyền: Đăng nhập; Nên có thêm).
- `PATCH /rooms/{roomId}/reminders/{reminderId}` : Sửa hoặc hủy lịch hẹn (status pending, done, cancelled) (Quyền: Đăng nhập; Nên có thêm).
- `PATCH /rooms/{roomId}/reminders/{reminderId}/rsvp` : Phản hồi lịch hẹn (accepted, declined) (Quyền: Đăng nhập).

### **Notifications (`/notifications`)**

- `GET /notifications` : Danh sách thông báo, có phân trang (Quyền: Đăng nhập).
- `PATCH /notifications/read-all` : Đánh dấu tất cả đã đọc (Quyền: Đăng nhập).
- `PATCH /notifications/{notificationId}/read` : Đánh dấu 1 thông báo đã đọc (Quyền: Đăng nhập).

### **AI (`/ai`)**

- `POST /ai/parse-schedule` : AI parse câu nhắn thành lịch hẹn (chưa lưu DB) (Quyền: Đăng nhập).
- `POST /ai/suggest-poll` : AI gợi ý poll từ đoạn hội thoại (chưa lưu DB) (Quyền: Đăng nhập).

### **Admin (`/admin`)**

- `GET /admin/stats/users` : Thống kê người dùng (Quyền: Admin hệ thống; Ngoài danh sách chức năng xlsx).
- `GET /admin/stats/messages` : Thống kê tin nhắn (Quyền: Admin hệ thống; Ngoài danh sách chức năng xlsx).
- `GET /admin/stats/groups` : Thống kê nhóm chat (Quyền: Admin hệ thống; Ngoài danh sách chức năng xlsx).
- `GET /admin/stats/ai` : Thống kê sử dụng AI và API (Quyền: Admin hệ thống; Cần bảng log token và số lần gọi API).
- `GET /admin/users` : Danh sách toàn bộ người dùng (Quyền: Admin hệ thống; Ngoài danh sách chức năng xlsx).
- `GET /admin/users/search` : Tìm người dùng theo username hoặc email (Quyền: Admin hệ thống; Ngoài danh sách chức năng xlsx).
- `PATCH /admin/users/{userId}/ban` : Khóa tài khoản (Quyền: Admin hệ thống; Cần thêm isBanned vào User).
- `PATCH /admin/users/{userId}/unban` : Mở khóa tài khoản (Quyền: Admin hệ thống).

### **WebSocket Real-time (`/ws`)**

- **Handshake Endpoint:** `ws://localhost:8080/ws`

**Client → Server:**

- `join_room` : Vào kênh realtime của phòng (kiểm tra user có trong phòng) (Quyền: Đã kết nối).
- `leave_room` : Ngừng nhận realtime của phòng, không rời phòng (Quyền: Đã kết nối).
- `send_message` : Gửi tin nhắn realtime (text, image, file, reply) (Quyền: Đã kết nối).
- `typing_start` : Báo đang gõ (Quyền: Đã kết nối).
- `typing_stop` : Báo ngừng gõ (Quyền: Đã kết nối).
- `mark_seen` : Đánh dấu đã xem tin nhắn (Quyền: Đã kết nối; Cần bảng MessageSeen).
- `poll_vote` : Bình chọn poll qua WebSocket (Quyền: Đã kết nối).
- `ping` : Kiểm tra kết nối còn sống (5 đến 10 phút một lần) (Quyền: Đã kết nối).

**Server → Client:**

- `new_message` : Có tin nhắn mới (kể cả tin do AI tạo) (Quyền: Đã kết nối).
- `message_updated` : Tin nhắn bị sửa (Quyền: Đã kết nối).
- `message_deleted` : Tin nhắn bị thu hồi (Quyền: Đã kết nối).
- `message_pinned` : Tin nhắn vừa được ghim (Quyền: Đã kết nối; Cần bổ sung).
- `message_unpinned` : Tin nhắn vừa bị bỏ ghim (Quyền: Đã kết nối; Cần bổ sung).
- `user_typing` : Có người đang gõ (Quyền: Đã kết nối).
- `user_stop_typing` : Người đó ngừng gõ (Quyền: Đã kết nối).
- `message_seen` : Có người đã xem tin (Quyền: Đã kết nối).
- `message_delivered` : Tin đã tới thiết bị người nhận (Quyền: Đã kết nối).
- `poll_updated` : Poll mới, có người vote hoặc poll bị đóng (Quyền: Đã kết nối).
- `reminder_created` : Lịch hẹn mới (Quyền: Đã kết nối).
- `reminder_updated` : Có người phản hồi RSVP hoặc lịch hẹn đổi trạng thái (Quyền: Đã kết nối).
- `ai_suggestion` : AI gợi ý poll hoặc lịch hẹn (chưa lưu DB) (Quyền: Đã kết nối).
- `notification` : Thông báo mới (Quyền: Đã kết nối).
- `user_online` : Bạn bè kết nối (Quyền: Đã kết nối).
- `user_offline` : Bạn bè ngắt kết nối (Quyền: Đã kết nối).
- `room_updated` : Phòng đổi tên, avatar hoặc theme (Quyền: Đã kết nối).
- `member_changed` : Thành viên được thêm, bị xóa hoặc rời phòng (Quyền: Đã kết nối).
- `error` : Báo lỗi cho event client vừa gửi (Quyền: Đã kết nối).
- `pong` : Trả lời ping (Quyền: Đã kết nối).

### **Tổng quan**

- Tổng cộng: **90 API/Event**.
- REST API: **62**.
- WebSocket Event: **28**.

### **Authentication Header**

Các REST API yêu cầu đăng nhập sử dụng:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

### **Base URL**

```text
http://localhost:8080
```
