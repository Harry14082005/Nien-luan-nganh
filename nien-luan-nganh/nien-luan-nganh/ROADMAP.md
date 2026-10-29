# 🗺️ Lộ Trình Phát Triển Dự Án (Roadmap) - Nhóm 8

> **Đề tài:** Xây dựng ứng dụng Web nhắn tin và kết nối cộng đồng tích hợp trợ lý AI thông minh (*Web Chat AI Assistant*)  
> **Tech Stack:** Spring Boot (Java 21), MySQL 8.0, Vue.js 3, Pinia, Spring Security, JJWT 0.12.6, WebSocket STOMP, Google Gemini AI.

---

## 🟢 PHẦN 1: CÁC CHẶNG ĐÃ HOÀN THÀNH (100%)

### ✅ Chặng 0: Khởi tạo Cơ sở hạ tầng & Mô hình Dữ liệu (Database)
- [x] Khởi tạo dự án Spring Boot (Java 21, Spring Boot 4 / Spring 7, Maven Wrapper).
- [x] Tạo file `Dockerfile` và `docker-compose.yml` (chạy MySQL 8.0, cấu hình kết nối tự động qua HikariCP).
- [x] Xây dựng đầy đủ **16 JPA Entities** khớp 100% với Class Diagram (`prompt_entity.md`):
  - `User`, `Room`, `RoomMember`, `Friendship`, `Message`, `MessageHistory`, `MessageReaction`, `Translation`, `Poll`, `PollOption`, `PollVote`, `PollResource`, `Reminder`, `ReminderParticipant`, `ReminderResource`, `Notification`.
- [x] Xây dựng **5 Composite Key Classes**:
  - `RoomMemberId`, `MessageReactionId`, `PollResourceId`, `ReminderParticipantId`, `ReminderResourceId`.
- [x] Xây dựng **9 Enums**:
  - `UserRole`, `RoomType`, `RoomRole`, `FriendshipStatus`, `MessageType`, `ReminderType`, `ReminderStatus`, `RsvpResponse`, `NotificationType`.
- [x] Xây dựng **16 Spring Data JPA Repositories** với các câu truy vấn tối ưu và cursor pagination.

---

### ✅ Chặng 1: Chuẩn bị DTOs, Bắt lỗi & Định dạng Response
- [x] Xây dựng chuẩn Response chung `ApiResponse.java` (`status`, `message`, `data`).
- [x] Xây dựng bộ bắt lỗi tập trung:
  - `ErrorCode.java`: Danh mục mã lỗi hệ thống (1000 - 1120).
  - `AppException.java`: Ngoại lệ runtime tùy biến gói `ErrorCode`.
  - `GlobalExceptionHandler.java`: Bắt lỗi `@RestControllerAdvice`, tự động map validation lỗi sang tiếng Việt.
- [x] Xây dựng bộ Request DTOs cho Xác thực:
  - `RegisterRequest.java`: Hỗ trợ 1 ô `identifier` (Email hoặc SĐT), username, password, confirmPassword, gender, preferredLanguage.
  - `LoginRequest.java`: `identifier` + `password`.
  - `RefreshTokenRequest.java`: `refreshToken`.
- [x] Xây dựng bộ Response DTOs & Mapper:
  - `UserResponse.java`: Thông tin an toàn của người dùng (có `createdAt`, `updatedAt`, không chứa `password`).
  - `UserMapper.java`: Ánh xạ tự động giữa `User` và `UserResponse` thông qua thư viện `MapStruct` (chuẩn doanh nghiệp).
  - `AuthResponse.java`: Trả về `accessToken`, `refreshToken`, `tokenType` ("Bearer"), `user`.

---

### ✅ Chặng 2: Cấu hình Bảo mật Spring Security & JWT Service
- [x] `UserDetailsImpl.java`: Lớp bọc entity `User` cho Spring Security, kiểm tra khóa tài khoản (`isBanned`), phân quyền `ROLE_`.
- [x] `UserDetailsServiceImpl.java`: Nạp user linh hoạt từ database theo chuỗi UUID (từ JWT) HOẶC Email/SĐT (khi đăng nhập form).
- [x] `JwtService.java`: Tạo Access Token & Refresh Token bằng thư viện JJWT 0.12.6, thuật toán HMAC-SHA256, kiểm tra hạn dùng và chữ ký.
- [x] `JwtAuthenticationFilter.java`: Bộ lọc chặn trước mọi request, đọc Header `Authorization: Bearer <token>`, nạp Authentication vào SecurityContextHolder.
- [x] `SecurityConfig.java`:
  - Cấu hình CORS mở cổng cho Vue.js (`http://localhost:5173`).
  - Cung cấp Bean `BCryptPasswordEncoder` mã hóa mật khẩu 1 chiều.
  - Phân quyền các route công khai (`/api/v1/auth/**`, `/ws/**`, `/uploads/**`) và route bảo mật.
  - Tùy biến trả lỗi JSON 401 (Chưa đăng nhập) & 403 (Không có quyền) theo chuẩn `ApiResponse`.


---

### ✅ Chặng 3: Tầng Nghiệp vụ Xác thực (AuthService & AuthController)
- [x] `AuthService.java`:
  - Logic Đăng ký: tự động phân tích Email hoặc Số điện thoại qua Regex, kiểm tra trùng lặp (`existsByEmail`/`existsByPhoneNumber`), băm mật khẩu bằng `BCryptPasswordEncoder`, sinh bộ token JWT.
  - Logic Đăng nhập: tìm kiếm bằng Email HOẶC Số điện thoại (`findByEmailOrPhoneNumber`), kiểm tra tài khoản bị khóa (`isBanned`), đối soát mật khẩu hash, cập nhật `lastSeenAt`.
  - Logic Làm mới token: kiểm tra refresh token và cấp access token mới.
  - Logic Đăng xuất.
- [x] `AuthController.java`: Mở 4 Endpoint REST API:
  - `POST /api/v1/auth/register` (Đăng ký tài khoản mới - HTTP 201 Created)
  - `POST /api/v1/auth/login` (Đăng nhập hệ thống - HTTP 200 OK)
  - `POST /api/v1/auth/refresh` (Làm mới access token - HTTP 200 OK)
  - `POST /api/v1/auth/logout` (Đăng xuất - HTTP 200 OK)
- [x] `AuthServiceTest.java`: 11 kịch bản Unit Test (Mocking) kiểm thử toàn diện mọi luồng đăng ký, đăng nhập, khóa tài khoản, refresh token.

---

## 🟡 PHẦN 2: CÁC CHẶNG ĐANG VÀ SẼ LÀM

### ✅ Chặng 4: Phân hệ Hồ sơ Người dùng (Users Module) - *[HOÀN THÀNH]*
- [x] Lấy thông tin cá nhân hiện tại (`GET /api/v1/users/me`).
- [x] Cập nhật hồ sơ: bio, giới tính, avatar, ngôn ngữ ưa thích (`PUT /api/v1/users/me`).
- [x] Đổi mật khẩu (`PUT /api/v1/user/me/password` hoặc `/api/v1/users/me/password`).
- [x] Đổi Email (`PUT /api/v1/user/me/email`), Đổi Số điện thoại (`PUT /api/v1/user/me/phone`).
- [x] Tìm kiếm người dùng theo Email, SĐT hoặc Tên hiển thị (`GET /api/v1/users/search?q=...`).
- [x] Xem trang cá nhân công khai của người khác (`GET /api/v1/users/{userId}`).

---

### ✅ Chặng 5: Phân hệ Kết nối Bạn bè (Friendship Module) - *[HOÀN THÀNH]*
- [x] Gửi lời mời kết bạn (`POST /api/v1/friendships/request`).
- [x] Chấp nhận lời mời kết bạn (`PUT /api/v1/friendships/{friendshipId}/accept`).
- [x] Từ chối lời mời kết bạn (`PUT /api/v1/friendships/{friendshipId}/decline`).
- [x] Hủy kết bạn (`DELETE /api/v1/friendships/{friendshipId}`).
- [x] Chặn người dùng (`POST /api/v1/friendships/block`), Bỏ chặn (`DELETE /api/v1/friendships/block/{userId}`).
- [x] Lấy danh sách bạn bè, lời mời, chặn (`GET /api/v1/friendships?status=...`).
- [x] Kiểm tra trạng thái quan hệ 2 chiều (`GET /api/v1/friendships/status/{userId}`).

---

### 🔹 Chặng 6: Phân hệ Phòng chat & Thành viên (Rooms & Room Members Module)
- [ ] Tạo / lấy phòng chat cá nhân 1:1 (`POST /api/v1/rooms/direct`).
- [ ] Tạo phòng chat nhóm (`POST /api/v1/rooms/group`).
- [ ] Thêm thành viên mới vào nhóm chat.
- [ ] Xóa thành viên khỏi nhóm chat.
- [ ] Phân quyền vai trò: Trưởng nhóm (`OWNER`), Quản trị viên (`ADMIN`), Thành viên (`MEMBER`).
- [ ] Đổi tên nhóm, avatar nhóm, chuyển quyền trưởng nhóm (Transfer Owner), rời nhóm.

---

### ✅ Chặng 7: Phân hệ Tin nhắn & Cảm xúc (Messages & Reactions Module) - *[HOÀN THÀNH]*
- [x] Gửi tin nhắn Text, Image, File đính kèm (`POST /api/v1/rooms/{roomId}/messages`).
- [x] Trả lời tin nhắn (Reply message cha - tự tham chiếu self-reference).
- [x] Thu hồi tin nhắn (`isRevoked = true`), sửa tin nhắn (`isEdited = true`), lưu vào `message_histories` và xem lịch sử (`GET .../history`).
- [x] Ghim / Bỏ ghim tin nhắn quan trọng trong phòng chat (`POST/DELETE .../pins`, `GET .../pins`).
- [x] Thả cảm xúc tin nhắn (`message_reactions`: Like, Heart, Haha, Wow, Sad, Angry).
- [x] Lấy danh sách tin nhắn theo phân trang cuộn mượt mà (Cursor-based pagination: `GET /api/v1/rooms/{roomId}/messages?cursor=...&limit=...`).

---

### 🟡 Chặng 8: Giao tiếp Thời gian thực qua WebSocket (STOMP Real-time) - *[ĐANG THỰC HIỆN]*
- [x] Cấu hình WebSocket Handshake (`/ws`) tích hợp xác thực JWT Token (hỗ trợ cả SockJS và Native WS).
- [ ] Gửi / Nhận tin nhắn tức thời không cần reload qua Topic `/topic/room.{roomId}`.
- [ ] Trạng thái Đang gõ chữ (Typing indicator: "Harry đang nhập tin nhắn...").
- [ ] Trạng thái Đang online / Vừa mới truy cập (User Presence).
- [ ] Đánh dấu đã nhận (`lastDeliveredMessageId`) và đã đọc (`lastReadMessageId`).

---

### 🔹 Chặng 9: Phân hệ Bình chọn Biểu quyết (Polls Module)
- [ ] Tạo cuộc bình chọn trong nhóm chat (`POST /api/v1/rooms/{roomId}/polls`).
- [ ] Thêm các phương án lựa chọn (`poll_options`).
- [ ] Bỏ phiếu / Đổi phiếu bầu (`poll_votes`).
- [ ] Cập nhật kết quả số phiếu bầu real-time qua WebSocket.
- [ ] Đóng / Kết thúc cuộc bình chọn (`isActive = false`).

---

### 🔹 Chặng 10: Phân hệ Lịch hẹn & Nhắc nhở (Reminders Module)
- [ ] Tạo lịch hẹn cá nhân hoặc lịch hẹn nhóm (`reminders`).
- [ ] Mời thành viên tham gia lịch hẹn (`reminder_participants`).
- [ ] Phản hồi tham gia sự kiện RSVP (`ACCEPTED`, `DECLINED`, `PENDING`).
- [ ] Đổi trạng thái lịch hẹn (`PENDING` -> `DONE` -> `CANCELLED`).

---

### 🔹 Chặng 11: Phân hệ Thông báo (Notifications Module)
- [ ] Tạo thông báo tự động khi có: Lời mời kết bạn, được thêm vào nhóm, có lịch hẹn mới, có poll mới.
- [ ] Đẩy thông báo thời gian thực về kênh riêng của từng user qua WebSocket (`/user/queue/notifications`).
- [ ] Đánh dấu thông báo đã đọc (`is_read = true`).

---

### 🔹 Chặng 12: Tích hợp Trợ lý Trí tuệ Nhân tạo Gemini AI (AI Assistant)
- [ ] **Dịch tin nhắn tự động:** Tự động dịch nội dung tin nhắn sang ngôn ngữ ưa thích của người nhận và lưu cache vào `translations`.
- [ ] **Tự động nhận diện Lịch hẹn:** AI đọc nội dung chat (ví dụ: *"Tối mai 7h nhóm mình họp ở quán Highlands nhé"*), tự động bóc tách ngày/giờ/địa điểm và gợi ý nút "Tạo lịch hẹn" liên kết vào `reminder_resource`.
- [ ] **Tự động nhận diện Biểu quyết:** AI phát hiện thảo luận cần lấy ý kiến (ví dụ: *"Trưa nay ăn cơm hay ăn bún đậu mọi người?"*), tự gợi ý nút "Tạo bình chọn" liên kết vào `poll_resource`.

---

### 🔹 Chặng 13: Phân hệ Quản trị viên (Admin Module)
- [ ] Thống kê hệ thống: Tổng số user, số phòng chat, số tin nhắn trong ngày.
- [ ] Quản lý người dùng: Khóa tài khoản vi phạm (`isBanned = true`), mở khóa tài khoản.
- [ ] Quản lý danh sách phòng chat công cộng / nhóm.

---

### 🔹 Chặng 14: Kết nối Giao diện Frontend (Vue.js 3 + Pinia) & Kiểm thử Hệ thống
- [ ] Xây dựng màn hình Đăng ký / Đăng nhập bên Vue.js kết nối vào API Backend.
- [ ] Xây dựng giao diện Chat cá nhân, Chat nhóm, danh bạ bạn bè.
- [ ] Xây dựng Widget Bình chọn, Lịch hẹn và Trợ lý AI trên giao diện web.
- [ ] Kiểm thử toàn diện End-to-End (E2E) và hoàn tất báo cáo Niên luận.
