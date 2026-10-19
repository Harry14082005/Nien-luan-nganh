# Danh sách API – Ứng dụng Chat tích hợp AI

- **Base URL (REST):** `http://localhost:8080/api/v1`
- **WebSocket:** `ws://localhost:8080/ws?token=<accessToken>`
- **Ghi chú chung:** Cột *Quyền* ghi theo spec hiện có; các dòng *Cần bổ sung* và *Nên có thêm* là gợi ý, cần nhóm chốt.
- **Chú thích cột:** `STT` = số thứ tự API; `STT CN` = số thứ tự chức năng trong file xlsx chức năng (`—` = không thuộc chức năng nào). Dòng có STT `—` là API mới thêm, chưa được đánh số.
- **Nguồn:** sheet `Danh sách API (Official)`.

## Tổng quan

| Nhóm | Loại | Số API |
|---|---|---|
| Auth | REST | 4 |
| Users | REST | 7 |
| Friendship | REST | 7 |
| Rooms | REST | 15 |
| Messages | REST | 11 |
| Uploads | REST | 1 |
| Polls | REST | 6 |
| Reminders | REST | 6 |
| Notifications | REST | 3 |
| AI | REST | 2 |
| Admin | REST | 10 |
| WebSocket gửi lên | WebSocket | 8 |
| WebSocket nhận về | WebSocket | 20 |
| **Tổng** | | **100** |

**Theo tình trạng:**

| Tình trạng | Số API |
|---|---|
| Đã có trong spec | 79 |
| Cần bổ sung | 11 |
| — | 7 |
| Nên có thêm | 3 |

## Auth

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 1 | `POST` | `/auth/register` | Đăng ký tài khoản | 1 | Công khai | Đã có trong spec | Chốt SĐT có bắt buộc không |
| 2 | `POST` | `/auth/login` | Đăng nhập, trả accessToken và refreshToken | 2 | Công khai | Đã có trong spec | Đổi email thành identifier (email hoặc SĐT); thêm 403 khi tài khoản bị khóa |
| 3 | `POST` | `/auth/refresh` | Làm mới access token | 2 | Công khai | Đã có trong spec |  |
| 4 | `POST` | `/auth/logout` | Đăng xuất, thu hồi refresh token | 3 | Đăng nhập | Đã có trong spec | Thêm body { refreshToken } |

## Users

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 5 | `GET` | `/users/me` | Lấy thông tin bản thân | 4 | Đăng nhập | Đã có trong spec | Schema User thiếu lastSeenAt, role, isBanned |
| 6 | `PUT` | `/users/me` | Cập nhật hồ sơ, avatar, ngôn ngữ ưa thích | 4 | Đăng nhập | Đã có trong spec | Avatar nên upload qua POST /uploads |
| — | `PUT` | `/user/me/email` | Đổi email | — | Đăng nhập | Đã có trong spec |  |
| — | `PUT` | `/user/me/phone` | Đổi SĐT | — | Đăng nhập | Đã có trong spec |  |
| — | `PUT` | `/user/me/password` | Đổi mật khẩu | — | Đăng nhập | Đã có trong spec |  |
| 7 | `GET` | `/users/search` | Tìm người dùng theo email hoặc SĐT (tham số q) ?q=... | 17 | Đăng nhập | Đã có trong spec |  |
| 8 | `GET` | `/users/{userId}` | Xem profile người dùng | — | Đăng nhập | Đã có trong spec |  |

## Friendship

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 9 | `GET` | `/friendships` | Danh sách bạn bè, lọc theo status (accepted, pending, blocked) | 22, 23 | Đăng nhập | Đã có trong spec | Trả thêm friendshipId và direction (lời mời đến hay đi) |
| 10 | `POST` | `/friendships/request` | Gửi lời mời kết bạn | 22 | Đăng nhập | Đã có trong spec | Thêm 403 BLOCKED |
| 11 | `PUT` | `/friendships/{friendshipId}/accept` | Chấp nhận lời mời kết bạn | 22 | Đăng nhập | Đã có trong spec |  |
| 12 | `PUT` | `/friendships/{friendshipId}/decline` | Từ chối lời mời kết bạn | — | Đăng nhập | Đã có trong spec |  |
| 13 | `DELETE` | `/friendships/{friendshipId}` | Hủy kết bạn hoặc rút lời mời | — | Đăng nhập | Đã có trong spec |  |
| 14 | `POST` | `/friendships/block` | Chặn người dùng | 23 | Đăng nhập | Đã có trong spec |  |
| 15 | `DELETE` | `/friendships/block/{userId}` | Bỏ chặn người dùng | 23 | Đăng nhập | Cần bổ sung |  |

## Rooms

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 16 | `GET` | `/rooms` | Danh sách phòng của tôi | 5, 6 | Đăng nhập | Đã có trong spec | Tùy chọn: thêm lastMessage, unreadCount |
| 17 | `POST` | `/rooms` | Tạo phòng nhóm | 13 | Đăng nhập | Đã có trong spec |  |
| 18 | `POST` | `/rooms/direct` | Tạo hoặc lấy phòng chat 1:1 | 5 | Đăng nhập | Đã có trong spec | Thêm 403 BLOCKED |
| 19 | `GET` | `/rooms/search` | Tìm cuộc trò chuyện theo tên phòng hoặc nội dung | 18 | Đăng nhập | Cần bổ sung |  |
| 20 | `GET` | `/rooms/{roomId}` | Lấy thông tin phòng (mở phòng) | 5, 6 | Đăng nhập | Đã có trong spec |  |
| 21 | `PUT` | `/rooms/{roomId}` | Đổi tên, avatar, theme phòng | — | Admin phòng | Đã có trong spec |  |
| 22 | `DELETE` | `/rooms/{roomId}` | Xóa phòng | 13 | Owner phòng | Đã có trong spec |  |
| 23 | `GET` | `/rooms/{roomId}/members` | Danh sách thành viên và role | 14, 15 | Đăng nhập | Đã có trong spec |  |
| 24 | `POST` | `/rooms/{roomId}/members` | Thêm thành viên vào phòng (Mặc định ROLE = ? để thể hiện là cần duyệt) | 14 | Đăng nhập | Đã có trong spec |  |
| 25 | `DELETE` | `/rooms/{roomId}/members/{userId}` | Xóa thành viên khỏi phòng | 14 | Admin phòng | Đã có trong spec |  |
| 26 | `PUT` | `/rooms/{roomId}/members/{userId}/role` | Đổi quyền thành viên (Admin, Member) | 15 | Admin phòng (gợi ý) | Cần bổ sung |  |
| — | `GET` | `/rooms/{roomId}/pending` | Xem danh sách chờ duyệt | — | Admin | — |  |
| — | `PUT` | `/rooms/{roomId}/members/{userId}/approve` | Duyệt thành viên | — | Admin | — |  |
| — | `PUT` | `/rooms/{roomId}/members/{userId}/reject` | Từ chối thành viên | — | Admin | — |  |
| 27 | `DELETE` | `/rooms/{roomId}/leave` | Rời phòng | — | Đăng nhập | Đã có trong spec |  |

## Messages

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 28 | `GET` | `/rooms/{roomId}/messages` | Lịch sử tin nhắn, phân trang cursor | 21 | Đăng nhập | Đã có trong spec |  |
| 29 | `POST` | `/rooms/{roomId}/messages` | Gửi tin nhắn (text, image, file, reply) | 7, 8, 9, 10 | Đăng nhập | Đã có trong spec | Thêm 403 BLOCKED; schema Message thiếu isRevoked, fileName, fileSize, mimeType, isPinned |
| 30 | `PUT` | `/rooms/{roomId}/messages/{messageId}` | Chỉnh sửa tin nhắn | 11 | Đăng nhập | Đã có trong spec | Ghi rõ ai được phép, thêm 403 |
| 31 | `DELETE` | `/rooms/{roomId}/messages/{messageId}` | Thu hồi tin nhắn (xoá mềm, chỉ đổi nội dung thành NULL?) | 12 | Đăng nhập | Đã có trong spec | Ghi rõ ai được phép, thêm 403 |
| 32 | `GET` | `/rooms/{roomId}/messages/{messageId}/translate` | Dịch tin nhắn (cache hoặc gọi AI) | 29 | Đăng nhập | Đã có trong spec |  |
| 33 | `GET` | `/rooms/{roomId}/messages/search` | Tìm kiếm tin nhắn trong phòng | 19 | Đăng nhập | Đã có trong spec |  |
| 34 | `GET` | `/rooms/{roomId}/messages/{messageId}/history` | Xem lịch sử chỉnh sửa của tin nhắn | 11 | Đăng nhập | Cần bổ sung | Dựa trên bảng MessageHistory |
| 35 | `DELETE` | `/rooms/{roomId}/messages` | Xóa lịch sử chat (phía mình) | 21 | Đăng nhập | Cần bổ sung |  |
| 36 | `POST` | `/rooms/{roomId}/messages/{messageId}/pins` | Ghim tin nhắn | 20 | Cần chốt | Cần bổ sung | Cần bảng PinnedMessage |
| 37 | `DELETE` | `/rooms/{roomId}/messages/{messageId}/pins` | Bỏ ghim tin nhắn | 20 | Cần chốt | Cần bổ sung |  |
| 38 | `GET` | `/rooms/{roomId}/pins` | Danh sách tin nhắn đã ghim | 20 | Đăng nhập | Cần bổ sung |  |

## Uploads

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 39 | `POST` | `/uploads` | Upload ảnh, file, avatar (multipart); trả url, fileName, size, mimeType | 4, 8, 9 | Đăng nhập | Cần bổ sung |  |

## Polls

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| — | `GET` | `/polls` | Lấy tất cả danh sách poll trong các room mà người dùng tham gia | — | — | — |  |
| 40 | `GET` | `/rooms/{roomId}/polls` | Danh sách poll trong phòng | 27 | Đăng nhập | Đã có trong spec |  |
| 41 | `POST` | `/rooms/{roomId}/polls` | Tạo poll thủ công hoặc xác nhận poll do AI gợi ý | 27, 31 | Đăng nhập | Đã có trong spec |  |
| 42 | `GET` | `/rooms/{roomId}/polls/{pollId}` | Xem chi tiết 1 poll | 28 | Đăng nhập | Nên có thêm |  |
| 43 | `POST` | `/rooms/{roomId}/polls/{pollId}/vote` | Bình chọn | 28 | Đăng nhập | Đã có trong spec |  |
| 44 | `PUT` | `/rooms/{roomId}/polls/{pollId}/close` | Đóng poll | — | Người tạo hoặc Admin phòng | Đã có trong spec |  |

## Reminders

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| — | `GET` | `/reminders` | Lấy tất cả lời nhắc trong các room mà người dùng tham gia | — | — | — |  |
| 45 | `GET` | `/rooms/{roomId}/reminders` | Danh sách lịch hẹn trong phòng | 25 | Đăng nhập | Đã có trong spec |  |
| 46 | `POST` | `/rooms/{roomId}/reminders` | Tạo lịch hẹn thủ công hoặc xác nhận từ AI | 25, 30 | Đăng nhập | Đã có trong spec |  |
| 47 | `GET` | `/rooms/{roomId}/reminders/{reminderId}` | Xem chi tiết 1 lịch hẹn | 25 | Đăng nhập | Nên có thêm |  |
| 48 | `PUT` | `/rooms/{roomId}/reminders/{reminderId}` | Sửa hoặc hủy lịch hẹn (status pending, done, cancelled) | 25 | Đăng nhập | Nên có thêm |  |
| 49 | `PUT` | `/rooms/{roomId}/reminders/{reminderId}/rsvp` | Phản hồi lịch hẹn (accepted, declined) | 26 | Đăng nhập | Đã có trong spec |  |

## Notifications

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 50 | `GET` | `/notifications` | Danh sách thông báo, có phân trang | 24 | Đăng nhập | Đã có trong spec |  |
| 51 | `PUT` | `/notifications/read-all` | Đánh dấu tất cả đã đọc | 24 | Đăng nhập | Đã có trong spec |  |
| 52 | `PUT` | `/notifications/{notificationId}/read` | Đánh dấu 1 thông báo đã đọc | 24 | Đăng nhập | Đã có trong spec |  |

## AI

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 53 | `POST` | `/ai/parse-schedule` | AI parse câu nhắn thành lịch hẹn (chưa lưu DB) | 30 | Đăng nhập | Đã có trong spec |  |
| 54 | `POST` | `/ai/suggest-poll` | AI gợi ý poll từ đoạn hội thoại (chưa lưu DB) | 31 | Đăng nhập | Đã có trong spec |  |

## Admin

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 55 | `GET` | `/admin/stats/users` | Thống kê người dùng | — | Admin hệ thống | Đã có trong spec | Ngoài danh sách chức năng xlsx |
| 56 | `GET` | `/admin/stats/messages` | Thống kê tin nhắn | — | Admin hệ thống | Đã có trong spec | Ngoài danh sách chức năng xlsx |
| 57 | `GET` | `/admin/stats/groups` | Thống kê nhóm chat | — | Admin hệ thống | Đã có trong spec | Ngoài danh sách chức năng xlsx |
| 58 | `GET` | `/admin/stats/ai` | Thống kê sử dụng AI và API | — | Admin hệ thống | Đã có trong spec | Cần bảng log token và số lần gọi API |
| 59 | `GET` | `/admin/users` | Danh sách toàn bộ người dùng | — | Admin hệ thống | Đã có trong spec | Ngoài danh sách chức năng xlsx |
| 60 | `GET` | `/admin/users/search` | Tìm người dùng theo username hoặc email | — | Admin hệ thống | Đã có trong spec | Ngoài danh sách chức năng xlsx |
| 61 | `PUT` | `/admin/users/{userId}/ban` | Khóa tài khoản | — | Admin hệ thống | Đã có trong spec | Cần thêm isBanned vào User |
| 62 | `PUT` | `/admin/users/{userId}/unban` | Mở khóa tài khoản | — | Admin hệ thống | Đã có trong spec |  |
| — | `DELETE` | `/admin/users/{userId}/delete` | Xoá tài khoản | — | — | — |  |
| — | `POST` | `/admin/user/{userId}/reset-password` | Khôi phục lại mật khẩu cho tài khoản (gửi mail?) | — | — | — |  |

## WebSocket gửi lên

| STT | Hướng | Event | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 63 | Client → Server | `join_room` | Vào kênh realtime của phòng (kiểm tra user có trong phòng) | 5, 6 | Đã kết nối | Đã có trong spec |  |
| 64 | Client → Server | `leave_room` | Ngừng nhận realtime của phòng, không rời phòng | — | Đã kết nối | Đã có trong spec |  |
| 65 | Client → Server | `send_message` | Gửi tin nhắn realtime (text, image, file, reply) | 5, 6, 7, 8, 9, 10 | Đã kết nối | Đã có trong spec |  |
| 66 | Client → Server | `typing_start` | Báo đang gõ | — | Đã kết nối | Đã có trong spec |  |
| 67 | Client → Server | `typing_stop` | Báo ngừng gõ | — | Đã kết nối | Đã có trong spec |  |
| 68 | Client → Server | `mark_seen` | Đánh dấu đã xem tin nhắn | — | Đã kết nối | Đã có trong spec | Cần bảng MessageSeen |
| 69 | Client → Server | `poll_vote` | Bình chọn poll qua WebSocket | 28 | Đã kết nối | Đã có trong spec |  |
| 70 | Client → Server | `ping` | Kiểm tra kết nối còn sống (5 đến 10 phút một lần) | — | Đã kết nối | Đã có trong spec |  |

## WebSocket nhận về

| STT | Hướng | Event | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 71 | Server → Client | `new_message` | Có tin nhắn mới (kể cả tin do AI tạo) | 5, 6 | Đã kết nối | Đã có trong spec |  |
| 72 | Server → Client | `message_updated` | Tin nhắn bị sửa | 11 | Đã kết nối | Đã có trong spec |  |
| 73 | Server → Client | `message_deleted` | Tin nhắn bị thu hồi | 12 | Đã kết nối | Đã có trong spec |  |
| 74 | Server → Client | `message_pinned` | Tin nhắn vừa được ghim | 20 | Đã kết nối | Cần bổ sung |  |
| 75 | Server → Client | `message_unpinned` | Tin nhắn vừa bị bỏ ghim | 20 | Đã kết nối | Cần bổ sung |  |
| 76 | Server → Client | `user_typing` | Có người đang gõ | — | Đã kết nối | Đã có trong spec |  |
| 77 | Server → Client | `user_stop_typing` | Người đó ngừng gõ | — | Đã kết nối | Đã có trong spec |  |
| 78 | Server → Client | `message_seen` | Có người đã xem tin | — | Đã kết nối | Đã có trong spec |  |
| 79 | Server → Client | `message_delivered` | Tin đã tới thiết bị người nhận | — | Đã kết nối | Đã có trong spec |  |
| 80 | Server → Client | `poll_updated` | Poll mới, có người vote hoặc poll bị đóng | 27, 28 | Đã kết nối | Đã có trong spec |  |
| 81 | Server → Client | `reminder_created` | Lịch hẹn mới | 25 | Đã kết nối | Đã có trong spec |  |
| 82 | Server → Client | `reminder_updated` | Có người phản hồi RSVP hoặc lịch hẹn đổi trạng thái | 26 | Đã kết nối | Đã có trong spec |  |
| 83 | Server → Client | `ai_suggestion` | AI gợi ý poll hoặc lịch hẹn (chưa lưu DB) | 30, 31 | Đã kết nối | Đã có trong spec |  |
| 84 | Server → Client | `notification` | Thông báo mới | 24 | Đã kết nối | Đã có trong spec |  |
| 85 | Server → Client | `user_online` | Bạn bè kết nối | 16 | Đã kết nối | Đã có trong spec |  |
| 86 | Server → Client | `user_offline` | Bạn bè ngắt kết nối | 16 | Đã kết nối | Đã có trong spec |  |
| 87 | Server → Client | `room_updated` | Phòng đổi tên, avatar hoặc theme | — | Đã kết nối | Đã có trong spec |  |
| 88 | Server → Client | `member_changed` | Thành viên được thêm, bị xóa hoặc rời phòng | 14 | Đã kết nối | Đã có trong spec |  |
| 89 | Server → Client | `error` | Báo lỗi cho event client vừa gửi | — | Đã kết nối | Đã có trong spec |  |
| 90 | Server → Client | `pong` | Trả lời ping | — | Đã kết nối | Đã có trong spec |  |
