# Ứng Dụng Web Nhắn Tin & Kết Nối Cộng Đồng Tích Hợp Trợ Lý AI

> **ĐỀ TÀI NIÊN LUẬN NGÀNH KỸ THUẬT PHẦN MỀM**
> **Nhóm thực hiện:** Nhóm 8
> **Tên đề tài:** *Xây dựng ứng dụng Web nhắn tin và kết nối cộng đồng tích hợp trợ lý AI thông minh (Web Chat AI Assistant)*

---

## 📖 1. Giới Thiệu & Mô Tả Đề Tài

Ứng dụng cung cấp giải pháp liên lạc hiện đại, cho phép người dùng nhắn tin cá nhân (1:1) hoặc theo nhóm, kết nối bạn bè trong cộng đồng, đồng thời tích hợp **Trợ lý Trí tuệ Nhân tạo (AI)** để thông minh hóa quá trình giao tiếp:

- **Dịch tin nhắn tự động:** Hỗ trợ giao tiếp đa ngôn ngữ mượt mà ngay trong khung chat.
- **Tự động nhận diện lịch hẹn:** AI phân tích nội dung trò chuyện, phát hiện thời gian/địa điểm cuộc hẹn và hỗ trợ tạo sự kiện/lịch nhắc nhở (Reminder).
- **Tự động nhận diện biểu quyết/bình chọn:** AI phát hiện các nội dung thảo luận cần lấy ý kiến nhóm để đề xuất tạo bình chọn (Poll).
- **Trợ lý AI nhóm:** Tham gia tương tác, đưa ra gợi ý, lời khuyên và hỗ trợ đưa ra quyết định dựa trên ngữ cảnh hội thoại giữa các thành viên.

---

## 👥 2. Phân Chia Chức Năng Nhóm

<!-- | Thành viên | Trách nhiệm & Module chức năng đảm nhiệm |
| :--- | :--- |
| **PT** | • Đăng ký, đăng nhập, quản lý tài khoản & thông tin cá nhân.<br>• Tìm kiếm người dùng, tìm kiếm cuộc trò chuyện.<br>• Ghim tin nhắn, quản lý lịch sử trò chuyện.<br>• Tích hợp AI dịch nội dung tin nhắn. |
| **CNT** | • Nhắn tin cá nhân (1:1) và nhắn tin nhóm (Group Chat).<br>• Tạo và quản lý nhóm chat, phân quyền thành viên (OWNER, ADMIN, MEMBER).<br>• Gửi các loại nội dung: văn bản, hình ảnh, file đính kèm.<br>• Tích hợp AI phát hiện nội dung cần biểu quyết → hỗ trợ tạo Poll. |
| **Kiet** | • Tạo và quản lý lịch hẹn (Reminder) & bình chọn (Poll).<br>• Hệ thống thông báo thời gian thực cho người dùng (Notification).<br>• Tích hợp AI phát hiện nội dung lịch hẹn → hỗ trợ tạo sự kiện/lịch. | -->

---

## 🛠️ 3. Công Nghệ Sử Dụng (Tech Stack)

### Backend
- **Ngôn ngữ & Nền tảng:** Java 21, Spring Boot (Spring Framework 7)
- **Truy cập dữ liệu:** Spring Data JPA, Hibernate ORM
- **Bảo mật & Xác thực:** Spring Security, JWT (JSON Web Token), OAuth2 Resource Server
- **Giao tiếp thời gian thực:** Spring WebSocket (STOMP over SockJS)
- **Công cụ build & quản lý thư viện:** Apache Maven

### Frontend
- **Ngôn ngữ & Framework:** JavaScript / HTML5 / CSS3, Vue.js 3
- **Quản lý trạng thái & Định tuyến:** Pinia, Vue Router
- **Build tool:** Vite

### Cơ sở dữ liệu & Tích hợp AI
- **Database:** MySQL Server 8.0 (kết nối qua HikariCP, tự động sinh schema với Hibernate DDL)
- **AI Engine:** Google Gemini API (Model: `gemini-1.5-flash`) / gpt-oss-120b

---

## 🗄️ 4. Mô Hình Dữ Liệu (Database Schema)

Cơ sở dữ liệu gồm **16 bảng** được liên kết chặt chẽ, chia thành 5 nhóm chức năng:

### 👤 Người dùng & Quan hệ

| Bảng | Mô tả | Các cột chính |
| :--- | :--- | :--- |
| `users` | Tài khoản người dùng | `user_id` (PK), `email` (UQ), `phone_number` (UQ), `role` (USER/ADMIN), `is_banned`, `preferred_language` |
| `friendships` | Quan hệ bạn bè | `friendship_id` (PK), `user_id` (FK), `friend_id` (FK), `friendship_status` (PENDING/ACCEPTED/DECLINED/BLOCKED) |

### 🏠 Phòng chat & Thành viên

| Bảng | Mô tả | Các cột chính |
| :--- | :--- | :--- |
| `rooms` | Phòng chat cá nhân hoặc nhóm | `room_id` (PK), `room_type` (DIRECT/GROUP), `room_name`, `theme` |
| `user_room` | Thành viên trong phòng kèm quyền hạn | `user_id` + `room_id` (Composite PK), `role` (OWNER/ADMIN/MEMBER), `joined_at` |
| `room_members` | Theo dõi trạng thái đọc tin nhắn | `room_id` + `user_id` (Composite PK), `last_read_message_id`, `last_delivered_message_id` |

### 💬 Tin nhắn

| Bảng | Mô tả | Các cột chính |
| :--- | :--- | :--- |
| `messages` | Tin nhắn trong phòng chat | `message_id` (PK), `type` (TEXT/IMAGE/FILE), `content`, `is_revoked`, `is_edited`, `is_pinned`, `message_id_replied` (self FK) |
| `message_histories` | Lịch sử chỉnh sửa tin nhắn | `history_id` (PK), `message_id` (FK), `content`, `edited_at` |
| `message_reactions` | Reaction (cảm xúc) trên tin nhắn | `message_id` + `user_id` (Composite PK), `reaction_type` |
| `translations` | Cache bản dịch AI đa ngôn ngữ | `trans_id` (PK), `message_id` (FK), `source_language`, `target_language`, `translated_text` |

### 🗳️ Bình chọn (Poll)

| Bảng | Mô tả | Các cột chính |
| :--- | :--- | :--- |
| `polls` | Cuộc bình chọn trong nhóm | `poll_id` (PK), `room_id` (FK), `created_by` (FK), `question`, `is_active` |
| `poll_options` | Các lựa chọn trong poll | `poll_option_id` (PK), `poll_id` (FK), `option` |
| `poll_votes` | Lượt vote của người dùng | `poll_vote_id` (PK), `poll_id` (FK), `poll_option_id` (FK), `user_id` (FK), UNIQUE(poll_id, user_id) |
| `poll_resource` | Liên kết tin nhắn gốc → Poll do AI trích xuất | `message_id` + `poll_id` (Composite PK) |

### 📅 Lịch hẹn (Reminder)

| Bảng | Mô tả | Các cột chính |
| :--- | :--- | :--- |
| `reminders` | Lịch hẹn cá nhân hoặc nhóm | `reminder_id` (PK), `room_id` (FK), `created_by` (FK), `reminder_type` (MEETING/DEADLINE/EVENT), `reminder_status` (PENDING/DONE/CANCELLED), `start_at`, `end_at` |
| `reminder_participants` | Người tham gia & phản hồi lịch hẹn | `user_id` + `reminder_id` (Composite PK), `response` (PENDING/ACCEPTED/DECLINED) |
| `reminder_resource` | Liên kết tin nhắn gốc → Reminder do AI trích xuất | `message_id` + `reminder_id` (Composite PK) |

### 🔔 Thông báo

| Bảng | Mô tả | Các cột chính |
| :--- | :--- | :--- |
| `notifications` | Thông báo đến người dùng | `notification_id` (PK), `user_id` (FK), `type` (NEW_MESSAGE/NEW_POLL/NEW_REMINDER/FRIEND_REQUEST/RSVP_REMINDER), `reference_id`, `is_read` |

---

## 📁 5. Cấu Trúc Thư Mục Dự Án

```text
├── front-end/                          # Ứng dụng giao diện người dùng (Vue.js 3 + Vite)
│   ├── src/
│   │   ├── assets/
│   │   ├── components/
│   │   ├── router/
│   │   └── stores/
│   └── package.json
│
└── nien-luan-nganh/                    # Ứng dụng máy chủ (Spring Boot Backend)
    ├── src/
    │   ├── main/
    │   │   ├── java/com/nhom8/nien_luan_nganh/
    │   │   │   ├── config/             # Cấu hình Security, WebSocket, CORS, AI Client
    │   │   │   ├── controller/         # REST Controllers xử lý API Request
    │   │   │   ├── dto/                # Data Transfer Objects (Request / Response)
    │   │   │   ├── entity/             # 16 JPA Entity mô hình hóa CSDL
    │   │   │   ├── enums/              # Enum định nghĩa trạng thái & quyền hạn
    │   │   │   ├── mapper/             # Map dữ liệu giữa Entity và DTO
    │   │   │   ├── repository/         # 16 Spring Data JPA Repositories
    │   │   │   └── service/            # Tầng xử lý logic nghiệp vụ
    │   │   └── resources/
    │   │       └── application.properties  # Cấu hình MySQL, JWT, Hibernate, AI
    │   └── test/                       # Unit test & Integration test
    └── pom.xml
```

---

## 🚀 6. Hướng Dẫn Cài Đặt & Khởi Chạy

### Yêu cầu môi trường
- **Java Development Kit (JDK):** Phiên bản 21 trở lên
- **Node.js:** Phiên bản 20 trở lên & npm
- **MySQL Server:** Phiên bản 8.0 trở lên

---

### Bước 1: Cấu hình cơ sở dữ liệu

Đảm bảo MySQL Server đang chạy trên cổng `3306`. Mở file `nien-luan-nganh/src/main/resources/application.properties` và kiểm tra thông tin kết nối:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/aichat_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=your_password
```

> 💡 Hibernate sẽ tự động tạo CSDL `aichat_db` và khởi tạo toàn bộ 16 bảng khi server chạy lần đầu.

---

### Bước 2: Khởi chạy Backend (Spring Boot)

```bash
cd nien-luan-nganh

# Windows PowerShell
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Backend API sẽ chạy tại: `http://localhost:8080`

---

### Bước 3: Khởi chạy Frontend (Vue.js)

Mở cửa sổ terminal mới:

```bash
cd front-end/front-end
npm install
npm run dev
```

Frontend Web sẽ chạy tại: `http://localhost:5173`

---

## 📌 7. Tiến Độ Thực Hiện (Roadmap)

- [x] Phân tích yêu cầu và đặc tả bài toán.
- [x] Thiết kế mô hình dữ liệu (ERD) với 16 bảng liên kết chặt chẽ.
- [x] Xây dựng 16 JPA Entity và các Composite Key class.
- [x] Xây dựng tầng 16 Repository với Cursor-based pagination và tối ưu JPQL.
- [x] Cấu hình MySQL Server & tự động khởi tạo cấu trúc bảng với Hibernate DDL.
- [ ] Xây dựng Module Authentication: Đăng ký, Đăng nhập, JWT Token, Phân quyền.
- [ ] Xây dựng RESTful API cho Bạn bè (Friendship) và Quản lý tài khoản (User).
- [ ] Xây dựng RESTful API cho Phòng chat (Room) và Tin nhắn (Message).
- [ ] Cấu hình WebSocket (STOMP) phục vụ gửi/nhận tin nhắn và thông báo Real-time.
- [ ] Xây dựng Module Bình chọn (Poll) và Lịch hẹn (Reminder).
- [ ] Tích hợp Gemini AI: Dịch tin nhắn, bóc tách lịch hẹn, gợi ý biểu quyết.
- [ ] Xây dựng và hoàn thiện giao diện người dùng (Vue 3, Pinia, Vue Router).
- [ ] Kiểm thử toàn diện hệ thống và đóng gói sản phẩm.
