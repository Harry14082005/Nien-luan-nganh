# WebSocket STOMP Test Tools

Thư mục này chứa các công cụ HTML/JS độc lập dùng để test và debug nhanh các tính năng WebSocket STOMP (Chat Realtime, Reaction, Typing, Poll) giữa 2 người dùng mà không cần chờ giao diện Frontend hoàn thiện.

---

## 1. Yêu cầu & Lưu ý quan trọng về CORS

Backend Spring Boot cấu hình CORS cho phép origin `http://localhost:5173`.  
- **Không nhấp đúp mở trực tiếp file HTML (dạng `file:///...`)** vì trình duyệt sẽ gửi `Origin: null` và bị server chặn mã lỗi `403 Forbidden`.  
- **Bắt buộc phải chạy qua một Web Server cục bộ ở cổng `5173`**.

---

## 2. Cách khởi động Web Server phục vụ file Test (Chọn 1 trong 3 cách)

Mở Terminal / PowerShell tại thư mục này (`tools/websocket-tester`):

### Cách 1: Dùng Node.js (`npx serve`) - Khuyên dùng nhất
```bash
npx serve -l 5173 .
```

### Cách 2: Dùng Python
```bash
python -m http.server 5173
```

### Cách 3: Dùng extension "Live Server" trong VS Code
1. Cài extension **Live Server** trong VS Code.
2. Vào Settings VS Code tìm `Live Server > Settings: Port` đổi thành `5173`.
3. Chuột phải vào `test-chat.html` hoặc `test-poll.html` chọn **Open with Live Server**.

---

## 3. Truy cập công cụ trên Trình duyệt

Sau khi bật server ở cổng 5173, mở trình duyệt vào địa chỉ:

- **Test Chat / Reaction / Typing**: [http://localhost:5173/test-chat.html](http://localhost:5173/test-chat.html)
- **Test Poll (Bình chọn)**: [http://localhost:5173/test-poll.html](http://localhost:5173/test-poll.html)

---

## 4. Hướng dẫn Test 2 Người Dùng (User A & User B)

### Bước 1: Chuẩn bị dữ liệu
1. Đăng nhập qua API lấy Access Token của **User A** và **User B**.
2. Cả 2 User phải thuộc cùng một nhóm chat (`RoomType.GROUP`), lấy `roomId` của nhóm này.

### Bước 2: Mở 2 Tab trình duyệt
- **Tab 1 (User A)**: Mở `http://localhost:5173/test-chat.html` -> Nhập `Room ID` và Token của User A -> Bấm **Kết Nối**.
- **Tab 2 (User B)**: Mở tab ẩn danh `http://localhost:5173/test-chat.html` -> Nhập `Room ID` và Token của User B -> Bấm **Kết Nối**.

### Bước 3: Thử nghiệm các kịch bản
- **Chat Realtime**: Gõ tin nhắn ở Tab 1 và bấm **Gửi** -> Tab 2 sẽ nhận được tin nhắn realtime trong khung `Live Logs`.
- **Typing Indicator**: Bấm **Bật Typing** ở Tab 1 -> Tab 2 sẽ nhận thông báo User A đang gõ.
- **Tạo / Vote Poll**: 
  - Mở `test-poll.html` để theo dõi các event `POLL_CREATED`, `POLL_VOTED` khi gọi REST API tạo hoặc bình chọn poll.

