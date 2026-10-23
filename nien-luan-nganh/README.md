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

| Thành viên | Trách nhiệm & Module chức năng đảm nhiệm |
| :--- | :--- |
| **PT** | • Đăng ký, đăng nhập, quản lý tài khoản & thông tin cá nhân.<br>• Tìm kiếm người dùng, tìm kiếm cuộc trò chuyện.<br>• Ghim tin nhắn, quản lý lịch sử trò chuyện.<br>• Tích hợp AI dịch nội dung tin nhắn. |
| **CNT** | • Nhắn tin cá nhân (1:1) và nhắn tin nhóm (Group Chat).<br>• Tạo và quản lý nhóm chat, phân quyền thành viên (OWNER, ADMIN, MEMBER).<br>• Gửi các loại nội dung: văn bản, hình ảnh, file đính kèm.<br>• Tích hợp AI phát hiện nội dung cần biểu quyết → hỗ trợ tạo Poll. |
| **Kiet** | • Tạo và quản lý lịch hẹn (Reminder) & bình chọn (Poll).<br>• Hệ thống thông báo thời gian thực cho người dùng (Notification).<br>• Tích hợp AI phát hiện nội dung lịch hẹn → hỗ trợ tạo sự kiện/lịch. |

---

## 🛠️ 3. Công Nghệ Sử Dụng (Tech Stack)

### **Backend**
- **Ngôn ngữ & Nền tảng:** Java 21, Spring Boot (Spring Framework 7)
- **Truy cập dữ liệu:** Spring Data JPA, Hibernate ORM
- **Bảo mật & Xác thực:** Spring Security, JWT (JSON Web Token), OAuth2 Resource Server
- **Giao tiếp thời gian thực:** Spring WebSocket (STOMP over SockJS)
- **Công cụ build & quản lý thư viện:** Apache Maven

### **Frontend**
- **Ngôn ngữ & Framework:** JavaScript / HTML5 / CSS3, Vue.js 3
- **Quản lý trạng thái & Định tuyến:** Pinia, Vue Router
- **Build tool:** Vite

### **Cơ sở dữ liệu & Tích hợp AI**
- **Database:** MySQL Server 8.0 (kết nối qua HikariCP, tự động sinh schema với Hibernate DDL)
- **AI Engine:** Google Gemini API (Model: `gemini-1.5-flash`) / gpt-oss-120b

---

## 🗄️ 4. Mô Hình Dữ Liệu (Database Schema)

Cơ sở dữ liệu bao gồm 15 bảng liên kết chặt chẽ:

1. **`users`**: Lưu trữ thông tin tài khoản, mật khẩu băm, email, số điện thoại, avatar, vai trò (`USER`, `ADMIN`), trạng thái khóa.
2. **`friendships`**: Quản lý mối quan hệ bạn bè (`PENDING`, `ACCEPTED`, `DECLINED`, `BLOCKED`).
3. **`rooms`**: Phòng chat cá nhân (`DIRECT`) hoặc nhóm (`GROUP`).
4. **`user_room`**: Bảng trung gian quản lý thành viên trong phòng chat kèm vai trò (`OWNER`, `ADMIN`, `MEMBER`).
5. **`messages`**: Tin nhắn trong phòng chat, hỗ trợ loại tin (`TEXT`, `IMAGE`, `FILE`), trả lời tin nhắn (Reply), thu hồi (`is_revoked`) và sửa tin nhắn (`is_edited`).
6. **`message_histories`**: Lưu lại lịch sử các lần chỉnh sửa nội dung tin nhắn.
7. **`translations`**: Lưu bản dịch AI đa ngôn ngữ cho tin nhắn chat.
8. **`polls`**: Quản lý các cuộc bình chọn trong nhóm chat.
9. **`poll_options`**: Các phương án lựa chọn trong cuộc bình chọn.
10. **`poll_votes`**: Lượt vote của từng người dùng cho từng lựa chọn.
11. **`poll_resources`**: Liên kết giữa tin nhắn chat gốc và cuộc bình chọn AI trích xuất.
12. **`reminders`**: Lịch hẹn, sự kiện nhắc nhở cá nhân hoặc nhóm (`PERSONAL`, `GROUP`).
13. **`reminder_participants`**: Danh sách người tham gia lịch hẹn và phản hồi (`PENDING`, `ACCEPTED`, `DECLINED`).
14. **`reminder_resource`**: Liên kết giữa tin nhắn chat gốc và lịch hẹn AI trích xuất.
15. **`notifications`**: Thông báo cho người dùng (`INVITATION_TO_GROUP`, `NEW_MESSAGE`, `NEW_REMINDER`, `NEW_POLL`).

---

## 📁 5. Cấu Trúc Thư Mục Dự Án

```text
├── front-end/                      # Ứng dụng giao diện người dùng (Vue.js 3 + Vite)
│   ├── src/
│   │   ├── assets/
│   │   ├── components/
│   │   ├── router/
│   │   └── stores/
│   └── package.json
│
└── nien-luan-nganh/                 # Ứng dụng máy chủ (Spring Boot Backend)
    ├── src/
    │   ├── main/
    │   │   ├── java/com/nhom8/nien_luan_nganh/
    │   │   │   ├── config/         # Cấu hình Security, WebSocket, CORS, AI Client
    │   │   │   ├── controller/     # REST Controllers xử lý API Request
    │   │   │   ├── dto/            # Data Transfer Objects (Request / Response)
    │   │   │   ├── entity/         # 19 JPA Entity mô hình hóa CSDL
    │   │   │   ├── enums/          # Các Enum định nghĩa trạng thái & quyền hạn
    │   │   │   ├── mapper/         # Map dữ liệu giữa Entity và DTO
    │   │   │   ├── repository/     # 15 Spring Data JPA Repositories
    │   │   │   └── service/        # Tầng xử lý logic nghiệp vụ (Business Service)
    │   │   └── resources/
    │   │       └── application.properties # File cấu hình MySQL, JWT, Hibernate, AI
    │   └── test/                   # Unit test & Integration test
    └── pom.xml
```

---

## 🚀 6. Hướng Dẫn Cài Đặt & Khởi Chạy

### **Yêu cầu môi trường:**
- **Java Development Kit (JDK):** Phiên bản 21 trở lên
- **Node.js:** Phiên bản 20 trở lên & npm
- **MySQL Server:** Phiên bản 8.0 trở lên

---

### **Bước 1: Cấu hình Cơ sở dữ liệu**
1. Đảm bảo MySQL Server đang chạy trên cổng `3306`.
2. Mở file `nien-luan-nganh/src/main/resources/application.properties` và kiểm tra thông tin kết nối:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/aichat_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
   spring.datasource.username=root
   spring.datasource.password=kanekiken0
   ```
   *(Hibernate sẽ tự động tạo CSDL `aichat_db` và khởi tạo toàn bộ 15 bảng khi server chạy lần đầu).*
   > 💡 Xem thêm tài liệu chi tiết cho Backend tại: [`nien-luan-nganh/README.md`](file:///d:/Nien-luan-nganh/nien-luan-nganh/nien-luan-nganh/README.md)


---

### **Bước 2: Khởi chạy Backend (Spring Boot)**
Di chuyển vào thư mục backend và chạy bằng Maven Wrapper:
```bash
cd nien-luan-nganh
# Trên Windows PowerShell:
.\mvnw.cmd spring-boot:run

# Trên Linux / macOS:
./mvnw spring-boot:run
```
* Backend API sẽ chạy tại: `http://localhost:8080`

---

### **Bước 3: Khởi chạy Frontend (Vue.js)**
Mở một cửa sổ dòng lệnh mới:
```bash
cd front-end/front-end
npm install
npm run dev
```
* Frontend Web sẽ chạy tại: `http://localhost:5173`

---

## 📌 7. Tiến Độ Thực Hiện (Roadmap)

- [x] Phân tích yêu cầu và đặc tả bài toán.
- [x] Thiết kế mô hình dữ liệu (ERD) & 19 Entity quan hệ chặt chẽ.
- [x] Xây dựng tầng 15 Repository với Cursor-based pagination và tối ưu JPQL.
- [x] Cấu hình MySQL Server & tự động khởi tạo cấu trúc bảng với Hibernate.
- [ ] Xây dựng Module Authentication: Đăng ký, Đăng nhập, JWT Token, Phân quyền người dùng.
- [ ] Xây dựng RESTful API cho Bạn bè (Friendship) và Quản lý tài khoản (User).
- [ ] Xây dựng RESTful API cho Phòng chat (Room) và Tin nhắn (Message).
- [ ] Cấu hình WebSocket (STOMP) phục vụ gửi/nhận tin nhắn và thông báo Real-time.
- [ ] Xây dựng Module Bình chọn (Poll) và Lịch hẹn (Reminder).
- [ ] Tích hợp Trí tuệ Nhân tạo (Gemini AI Client): Dịch tin nhắn, bóc tách sự kiện/lịch hẹn, gợi ý biểu quyết.
- [ ] Xây dựng và hoàn thiện giao diện người dùng (Vue 3, Pinia, Vue Router).
- [ ] Kiểm thử toàn diện hệ thống và đóng gói sản phẩm.
