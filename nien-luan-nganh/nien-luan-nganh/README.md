# Backend - Web Nhắn Tin & Kết Nối Cộng Đồng Tích Hợp Trợ Lý AI

> **ĐỀ TÀI NIÊN LUẬN NGÀNH KỸ THUẬT PHẦN MỀM - NHÓM 8**  
> Dự án: Xây dựng ứng dụng Web nhắn tin và kết nối cộng đồng tích hợp trợ lý AI thông minh (*Web Chat AI Assistant*)  
> Server: Spring Boot REST API & WebSocket Real-time

---

## 📌 1. Giới Thiệu Dự Án

Dự án Backend cung cấp toàn bộ hệ thống API phục vụ xác thực người dùng, quản trị mối quan hệ bạn bè, nhắn tin thời gian thực (1:1 và Nhóm), bình chọn biểu quyết (Poll), nhắc hẹn lịch (Reminder), thông báo (Notification) và tích hợp Trí tuệ Nhân tạo Gemini AI hỗ trợ dịch thuật đa ngôn ngữ và trích xuất sự kiện tự động.

---

## 👥 2. Phân Chia Trách Nhiệm Thành Viên

| Thành viên | Trách nhiệm chính trong Backend |
| :--- | :--- |
| **PT** | • Module Xác thực: Đăng ký, đăng nhập, JWT Token, User Details, Phân quyền.<br>• Quản lý thông tin cá nhân & tìm kiếm người dùng / cuộc trò chuyện.<br>• Lịch sử chỉnh sửa tin nhắn, ghim tin nhắn.<br>• Tích hợp AI dịch nội dung tin nhắn đa ngôn ngữ. |
| **CNT** | • Module Chat: Nhắn tin 1:1 và Nhắn tin nhóm (Group Chat).<br>• Quản lý phòng chat, vai trò thành viên (`OWNER`, `ADMIN`, `MEMBER`).<br>• Xử lý đa phương tiện: Tin nhắn văn bản, hình ảnh, file đính kèm.<br>• Tích hợp AI phát hiện nội dung cần biểu quyết → đề xuất tạo Poll. |
| **Kiet** | • Module Bình chọn (Poll) & Phương án bình chọn, biểu quyết thời gian thực.<br>• Module Lịch hẹn (Reminder) & Quản lý người tham gia.<br>• Hệ thống thông báo thời gian thực (Notification) qua WebSocket.<br>• Tích hợp AI phát hiện lịch hẹn trong hội thoại → đề xuất tạo Reminder. |

---

## 🛠️ 3. Công Nghệ & Thư Viện Sử Dụng

- **Ngôn ngữ:** Java 21 (LTS)
- **Framework:** Spring Boot 3 / Spring Framework
- **ORM & Data Access:** Spring Data JPA, Hibernate ORM
- **Cơ sở dữ liệu:** MySQL 8.0 (MySQL Connector/J)
- **Bảo mật & Xác thực:** Spring Security, JWT (jjwt 0.12.6)
- **Giao tiếp Real-time:** Spring WebSocket với giao thức STOMP
- **Trợ lý AI:** Google Gemini API (`gemini-1.5-flash`)
- **Quản lý dự án & Build tool:** Maven (Maven Wrapper `mvnw`)
- **Công cụ hỗ trợ:** Lombok

---

## 🗄️ 4. Cấu Trúc Cơ Sở Dữ Liệu (15 Bảng)

Cơ sở dữ liệu: `aichat_db` trên MySQL 8.0.

| STT | Tên bảng | Chức năng |
|:---:|:---|:---|
| 1 | `users` | Thông tin người dùng, tài khoản, mật khẩu băm BCrypt, trạng thái kích hoạt, quyền hạn. |
| 2 | `friendships` | Quan hệ bạn bè (`PENDING`, `ACCEPTED`, `DECLINED`, `BLOCKED`). |
| 3 | `rooms` | Phòng chat cá nhân (`DIRECT`) hoặc nhóm (`GROUP`). |
| 4 | `user_room` | Thành viên trong từng phòng chat kèm vai trò (`OWNER`, `ADMIN`, `MEMBER`). |
| 5 | `messages` | Tin nhắn văn bản, ảnh, tệp tin; liên kết trả lời tin nhắn (Reply), cờ thu hồi/chỉnh sửa. |
| 6 | `message_histories` | Lịch sử chỉnh sửa nội dung tin nhắn chat. |
| 7 | `translations` | Bản dịch tin nhắn chat qua AI theo các ngôn ngữ đích. |
| 8 | `polls` | Các cuộc bình chọn trong nhóm chat. |
| 9 | `poll_options` | Các lựa chọn đáp án cho cuộc bình chọn. |
| 10 | `poll_votes` | Lượt bình chọn của người dùng cho từng lựa chọn. |
| 11 | `poll_resources` | Liên kết giữa tin nhắn chat gốc và cuộc bình chọn do AI gợi ý. |
| 12 | `reminders` | Lịch hẹn, sự kiện nhắc nhở cá nhân hoặc nhóm (`PERSONAL`, `GROUP`). |
| 13 | `reminder_participants` | Danh sách người tham gia sự kiện và trạng thái phản hồi. |
| 14 | `reminder_resource` | Liên kết giữa tin nhắn chat gốc và lịch hẹn do AI gợi ý. |
| 15 | `notifications` | Thông báo sự kiện cho người dùng (mời vào nhóm, tin mới, lịch hẹn mới, poll mới). |

---

## 📁 5. Cấu Trúc Thư Mục Backend (`src/main/java/com/nhom8/nien_luan_nganh`)

```text
├── config/             # Cấu hình Spring Security, JWT, WebSocket STOMP, CORS, AI Client
├── controller/         # REST Controllers tiếp nhận & xử lý HTTP requests
├── dto/                # Data Transfer Objects (Request / Response payloads)
│   ├── request/        # LoginRequest, RegisterRequest, MessageRequest, ...
│   └── response/       # AuthResponse, UserResponse, RoomResponse, ...
├── entity/             # 19 Entity JPA ánh xạ các bảng CSDL
├── enums/              # Các Enum định nghĩa trạng thái, vai trò, loại tin nhắn
├── mapper/             # Map dữ liệu chuyển đổi giữa Entity và DTO
├── repository/         # 15 Spring Data JPA Repositories (tối ưu JPQL, phân trang Cursor)
├── service/            # Interface & Implementation xử lý logic nghiệp vụ
└── NienLuanNganhApplication.java # Lớp khởi chạy ứng dụng Spring Boot
```

---

## ⚙️ 6. Cấu Hình & Cài Đặt

### Yêu cầu hệ thống:
- **JDK 21** trở lên (`java -version`)
- **MySQL Server 8.0** đang chạy trên `localhost:3306`

### Cấu hình `src/main/resources/application.properties`:
```properties
# Server
server.port=8080

# MySQL DataSource
spring.datasource.url=jdbc:mysql://localhost:3306/aichat_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=kanekiken0
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# JWT Configuration
jwt.secret=9a785f7c32b5e1c4d9a8b7e6f5c4d3b2a10987654321fedcba0987654321abcd
jwt.expiration=86400000
jwt.refresh-expiration=604800000

# Gemini AI
gemini.api.key=YOUR_GEMINI_API_KEY_HERE
gemini.api.model=gemini-1.5-flash
```

---

## 🚀 7. Hướng Dẫn Khởi Chạy

### 1. Kiểm tra build và compile dự án:
```powershell
.\mvnw.cmd clean compile
```

### 2. Chạy ứng dụng Spring Boot:
```powershell
.\mvnw.cmd spring-boot:run
```

Khi khởi động thành công, server sẽ lắng nghe tại:
`http://localhost:8080`

### 3. Kiểm tra tự động sinh bảng CSDL:
Khi ứng dụng khởi chạy lần đầu tiên với `ddl-auto=update`, Hibernate sẽ tự động kết nối MySQL và sinh đầy đủ 15 bảng cùng các ràng buộc Foreign Key vào CSDL `aichat_db`.

---

## 🔌 8. Thiết Kế Endpoints API Dự Kiến

### **Authentication (`/api/auth`) - (PT)**
- `POST /api/auth/register` : Đăng ký tài khoản mới.
- `POST /api/auth/login` : Đăng nhập & cấp phát JWT Access Token / Refresh Token.
- `POST /api/auth/refresh` : Làm mới Access Token.
- `GET /api/auth/me` : Lấy thông tin tài khoản hiện tại.

### **Users & Friends (`/api/users`, `/api/friends`) - (PT)**
- `GET /api/users/search?keyword=...` : Tìm kiếm người dùng theo tên, email, sđt.
- `PUT /api/users/profile` : Cập nhật thông tin cá nhân / avatar.
- `GET /api/friends` : Lấy danh sách bạn bè.
- `POST /api/friends/request/{userId}` : Gửi lời mời kết bạn.
- `PUT /api/friends/accept/{requestId}` : Chấp nhận kết bạn.
- `DELETE /api/friends/{friendId}` : Hủy kết bạn / chặn.

### **Chat Rooms & Messages (`/api/rooms`, `/api/messages`) - (CNT)**
- `GET /api/rooms` : Lấy danh sách phòng chat của người dùng.
- `POST /api/rooms/direct` : Tạo hoặc lấy phòng chat 1:1.
- `POST /api/rooms/group` : Tạo phòng chat nhóm mới.
- `GET /api/rooms/{roomId}/messages` : Lấy tin nhắn theo phân trang (Cursor-based).
- `POST /api/rooms/{roomId}/messages` : Gửi tin nhắn (Text/Image/File).
- `DELETE /api/messages/{messageId}` : Thu hồi tin nhắn.
- `PUT /api/messages/{messageId}` : Chỉnh sửa tin nhắn.

### **Polls & Reminders (`/api/polls`, `/api/reminders`) - (Kiet)**
- `POST /api/rooms/{roomId}/polls` : Tạo cuộc bình chọn trong nhóm.
- `POST /api/polls/{pollId}/vote` : Bỏ phiếu cho phương án.
- `POST /api/reminders` : Tạo lịch hẹn / nhắc nhở.
- `GET /api/reminders` : Lấy danh sách lịch hẹn của người dùng.
- `PUT /api/reminders/{reminderId}/status` : Xác nhận tham gia sự kiện.

### **AI Assistant (`/api/ai`)**
- `POST /api/ai/translate` : Dịch tin nhắn sang ngôn ngữ khác (PT).
- `POST /api/ai/detect-poll` : Phân tích nội dung chat đề xuất bình chọn (CNT).
- `POST /api/ai/detect-reminder` : Phân tích nội dung chat đề xuất lịch hẹn (Kiet).

### **WebSocket Real-time (`/ws`)**
- **Handshake Endpoint:** `http://localhost:8080/ws`
- **Topic chat nhóm/cá nhân:** `/topic/room.{roomId}`
- **Queue thông báo cá nhân:** `/user/queue/notifications`
