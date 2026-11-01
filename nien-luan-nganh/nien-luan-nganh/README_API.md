# Danh sách API – Ứng dụng Chat tích hợp AI

- **Base URL (REST):** `http://localhost:8080/api/v1`
- **WebSocket (SockJS):** `http://localhost:8080/ws` (STOMP Header `Authorization: Bearer <accessToken>` hoặc `?token=<accessToken>`)
- **WebSocket (Native WS / Postman):** `ws://localhost:8080/ws-raw` (STOMP Header `Authorization: Bearer <accessToken>` hoặc `?token=<accessToken>`)
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
| 1 | `POST` | `/auth/register` | Đăng ký tài khoản | 1 | Công khai | Đã hoàn thành | Hỗ trợ `identifier` (Email hoặc SĐT) |
| 2 | `POST` | `/auth/login` | Đăng nhập, trả accessToken và refreshToken | 2 | Công khai | Đã hoàn thành | Đổi email thành `identifier` (email hoặc SĐT); trả 403 khi tài khoản bị khóa |
| 3 | `POST` | `/auth/refresh` | Làm mới access token | 2 | Công khai | Đã hoàn thành |  |
| 4 | `POST` | `/auth/logout` | Đăng xuất, thu hồi refresh token | 3 | Đăng nhập | Đã hoàn thành |  |

## Users

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 5 | `GET` | `/users/me` | Lấy thông tin bản thân | 4 | Đăng nhập | Đã hoàn thành |  |
| 6 | `PUT` | `/users/me` | Cập nhật hồ sơ, avatar, ngôn ngữ ưa thích | 4 | Đăng nhập | Đã hoàn thành |  |
| — | `PUT` | `/user/me/email` | Đổi email | — | Đăng nhập | Đã hoàn thành | Hỗ trợ cả `/users/me/email` |
| — | `PUT` | `/user/me/phone` | Đổi SĐT | — | Đăng nhập | Đã hoàn thành | Hỗ trợ cả `/users/me/phone` |
| — | `PUT` | `/user/me/password` | Đổi mật khẩu | — | Đăng nhập | Đã hoàn thành | Hỗ trợ cả `/users/me/password` |
| 7 | `GET` | `/users/search` | Tìm người dùng theo email, SĐT hoặc username (`?q=...`) | 17 | Đăng nhập | Đã hoàn thành |  |
| 8 | `GET` | `/users/{userId}` | Xem profile người dùng | — | Đăng nhập | Đã hoàn thành |  |

## Friendship

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 9 | `GET` | `/friendships` | Danh sách bạn bè, lọc theo status (`ACCEPTED`, `PENDING`, `BLOCKED`) | 22, 23 | Đăng nhập | Đã hoàn thành | Trả thêm `friendshipId` và `direction` (`INCOMING` / `OUTGOING`), kèm kiểm tra trạng thái `/friendships/status/{userId}` |
| 10 | `POST` | `/friendships/request` | Gửi lời mời kết bạn | 22 | Đăng nhập | Đã hoàn thành | Kiểm tra 403 BLOCKED |
| 11 | `PUT` | `/friendships/{friendshipId}/accept` | Chấp nhận lời mời kết bạn | 22 | Đăng nhập | Đã hoàn thành |  |
| 12 | `PUT` | `/friendships/{friendshipId}/decline` | Từ chối lời mời kết bạn | — | Đăng nhập | Đã hoàn thành |  |
| 13 | `DELETE` | `/friendships/{friendshipId}` | Hủy kết bạn hoặc rút lời mời | — | Đăng nhập | Đã hoàn thành |  |
| 14 | `POST` | `/friendships/block` | Chặn người dùng | 23 | Đăng nhập | Đã hoàn thành |  |
| 15 | `DELETE` | `/friendships/block/{userId}` | Bỏ chặn người dùng | 23 | Đăng nhập | Đã hoàn thành |  |

## Rooms

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 16 | `GET` | `/rooms` | Danh sách phòng của tôi (`?type=DIRECT\|GROUP&pageNo=0&pageSize=20`) | 5, 6 | Đăng nhập | Đã hoàn thành | Đã tích hợp `lastMessage*`, `hasUnread`, `unreadCount` (4 batch queries chống N+1) |
| 17 | `POST` | `/rooms/group` | Tạo phòng nhóm | 13 | Đăng nhập | Đã hoàn thành | Đổi từ `POST /rooms` sang `POST /rooms/group` |
| 18 | `POST` | `/rooms/direct` | Tạo hoặc lấy phòng chat 1:1 | 5 | Đăng nhập | Đã hoàn thành | Kiểm tra chặn 2 chiều (403 BLOCKED) |
| 19 | `GET` | `/rooms/search` | Tìm cuộc trò chuyện theo tên phòng hoặc nội dung | 18 | Đăng nhập | Cần bổ sung |  |
| 20 | `GET` | `/rooms/{roomId}` | Lấy thông tin chi tiết phòng (mở phòng) | 5, 6 | Đăng nhập | Đã hoàn thành | Tự động lấy tên/avatar đối phương nếu là phòng `DIRECT` |
| 21 | `PUT` | `/rooms/{roomId}` | Đổi tên, avatar, theme phòng | — | Admin phòng | Đã có trong spec |  |
| 22 | `DELETE` | `/rooms/{roomId}` | Xóa phòng | 13 | Owner phòng | Đã có trong spec |  |
| 23 | `GET` | `/rooms/{roomId}/members` | Danh sách thành viên và role (`?status=ACTIVE\|PENDING`) | 14, 15 | Đăng nhập | Đã hoàn thành | Hỗ trợ lọc luôn danh sách chờ duyệt qua `?status=PENDING` |
| 24 | `POST` | `/rooms/{roomId}/members` | Thêm thành viên vào phòng | 14 | Đăng nhập | Đã hoàn thành | Hỗ trợ trạng thái `ACTIVE` hoặc `PENDING` (cần duyệt) |
| 25 | `DELETE` | `/rooms/{roomId}/members/{userId}` | Xóa thành viên khỏi phòng | 14 | Admin/Owner phòng | Đã hoàn thành |  |
| 26 | `PUT` | `/rooms/{roomId}/members/{userId}/role` | Đổi quyền thành viên (`ADMIN`, `MEMBER`) | 15 | Owner phòng | Đã hoàn thành |  |
| — | `PUT` | `/rooms/{roomId}/members/{userId}/approve` | Phê duyệt hoặc từ chối thành viên chờ duyệt | — | Admin/Owner phòng | Đã hoàn thành | Gộp duyệt và từ chối qua `ApproveMemberRequest` |
| 27 | `DELETE` | `/rooms/{roomId}/members/leave` | Rời phòng (kèm chuyển quyền Owner nếu là trưởng nhóm) | — | Đăng nhập | Đã hoàn thành | Đặt tại `/rooms/{roomId}/members/leave` |

## Messages

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 28 | `GET` | `/rooms/{roomId}/messages` | Lịch sử tin nhắn, phân trang cursor (`?cursor=...&before=...&limit=20`) | 21 | Đăng nhập | Đã hoàn thành | Kèm danh sách `reactions` nạp theo batch chống N+1 |
| 29 | `POST` | `/rooms/{roomId}/messages` | Gửi tin nhắn (text, image, file, reply) | 7, 8, 9, 10 | Đăng nhập | Đã hoàn thành | Tự động phát realtime qua `/topic/room.{roomId}` |
| 30 | `PUT` | `/rooms/{roomId}/messages/{messageId}` | Chỉnh sửa tin nhắn | 11 | Đăng nhập (Chính chủ) | Đã hoàn thành | Lưu `MessageHistory` + phát realtime qua `/topic/room.{roomId}` |
| 31 | `DELETE` | `/rooms/{roomId}/messages/{messageId}` | Thu hồi tin nhắn (xóa mềm `isRevoked = true`) | 12 | Chính chủ / Admin / Owner | Đã hoàn thành | Ẩn nội dung gốc + phát realtime qua `/topic/room.{roomId}` |
| 32 | `GET` | `/rooms/{roomId}/messages/{messageId}/translate` | Dịch tin nhắn (cache hoặc gọi AI) | 29 | Đăng nhập | Đã có trong spec |  |
| 33 | `GET` | `/rooms/{roomId}/messages/search` | Tìm kiếm tin nhắn trong phòng | 19 | Đăng nhập | Đã có trong spec |  |
| 34 | `GET` | `/rooms/{roomId}/messages/{messageId}/history` | Xem lịch sử chỉnh sửa của tin nhắn | 11 | Đăng nhập | Đã hoàn thành | Dựa trên bảng `message_histories` |
| 35 | `DELETE` | `/rooms/{roomId}/messages` | Xóa lịch sử chat (phía mình) | 21 | Đăng nhập | Cần bổ sung |  |
| 36 | `POST` | `/rooms/{roomId}/messages/{messageId}/pins` | Ghim tin nhắn (`isPinned = true`) | 20 | Đăng nhập | Đã hoàn thành | Phát realtime `MessageResponse` qua `/topic/room.{roomId}` |
| 37 | `DELETE` | `/rooms/{roomId}/messages/{messageId}/pins` | Bỏ ghim tin nhắn (`isPinned = false`) | 20 | Đăng nhập | Đã hoàn thành | Phát realtime `MessageResponse` qua `/topic/room.{roomId}` |
| 38 | `GET` | `/rooms/{roomId}/messages/pins` | Danh sách tin nhắn đã ghim | 20 | Đăng nhập | Đã hoàn thành | Gom về `MessageController` |
| — | `POST` | `/rooms/{roomId}/messages/{messageId}/reactions` | Thả hoặc cập nhật cảm xúc tin nhắn | — | Đăng nhập | Đã hoàn thành | Phát realtime `ReactionResponse` qua `/topic/room.{roomId}.reactions` |
| — | `DELETE` | `/rooms/{roomId}/messages/{messageId}/reactions` | Gỡ cảm xúc đã thả | — | Đăng nhập | Đã hoàn thành | Phát realtime `ReactionResponse` (`reactionType = null`) qua `/topic/room.{roomId}.reactions` |
| — | `GET` | `/rooms/{roomId}/messages/{messageId}/reactions` | Xem danh sách cảm xúc trên tin nhắn | — | Đăng nhập | Đã hoàn thành |  |

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

## WebSocket gửi lên (STOMP Client → Server)

| STT | Hướng | STOMP Destination / Frame | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 63 | Client → Server | `SUBSCRIBE /topic/room.{roomId}` | Đăng ký nhận tin nhắn realtime của phòng (Gửi mới, Sửa, Thu hồi, Ghim, Bỏ ghim) | 5, 6 | Đã kết nối | Đã hoàn thành | Chuẩn STOMP `SUBSCRIBE` |
| 64 | Client → Server | `UNSUBSCRIBE` | Ngừng nhận realtime của phòng, không rời phòng | — | Đã kết nối | Đã hoàn thành | Chuẩn STOMP `UNSUBSCRIBE` |
| 65 | Client → Server | `SEND /app/chat.sendMessage` | Gửi tin nhắn realtime (`ChatMessagePayload`: `roomId`, `content`, `messageType`, `repliedMessageId`) | 5, 6, 7, 8, 9, 10 | Đã kết nối | Đã hoàn thành | Lưu DB qua `MessageService` và phát về `/topic/room.{roomId}` |
| 66 | Client → Server | `SEND /app/chat.typing` (`typing: true`) | Báo đang gõ chữ (`TypingPayload`: `roomId`, `typing: true`) | — | Đã kết nối | Đã hoàn thành | Tối ưu kiểm tra thành viên qua RAM Cache TTL 5 phút |
| 67 | Client → Server | `SEND /app/chat.typing` (`typing: false`) | Báo ngừng gõ chữ (`TypingPayload`: `roomId`, `typing: false`) | — | Đã kết nối | Đã hoàn thành | Gộp chung endpoint `/app/chat.typing` với cờ boolean `typing` |
| 68 | Client → Server | `mark_seen` | Đánh dấu đã nhận (`lastDeliveredMessageId`) và đã đọc (`lastReadMessageId`) | — | Đã kết nối | Đang thực hiện | Lưu trên bảng `room_members` |
| 69 | Client → Server | `poll_vote` | Bình chọn poll qua WebSocket | 28 | Đã kết nối | Đã có trong spec |  |
| 70 | Client → Server | `ping` | Kiểm tra kết nối còn sống (STOMP Heartbeat) | — | Đã kết nối | Đã có trong spec |  |

## WebSocket nhận về (STOMP Server → Client)

| STT | Hướng | STOMP Topic / Queue | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 71 | Server → Client | `/topic/room.{roomId}` | Có tin nhắn mới (`MessageResponse`) | 5, 6 | Đã kết nối | Đã hoàn thành | Phát từ cả WS `/app/chat.sendMessage` và REST `POST /messages` |
| 72 | Server → Client | `/topic/room.{roomId}` | Tin nhắn bị sửa (`MessageResponse` với `isEdited = true`) | 11 | Đã kết nối | Đã hoàn thành | Phát tự động khi gọi REST `PUT /messages/{messageId}` |
| 73 | Server → Client | `/topic/room.{roomId}` | Tin nhắn bị thu hồi (`MessageResponse` với `isRevoked = true`) | 12 | Đã kết nối | Đã hoàn thành | Phát tự động khi gọi REST `DELETE /messages/{messageId}` |
| 74 | Server → Client | `/topic/room.{roomId}` | Tin nhắn vừa được ghim (`MessageResponse` với `isPinned = true`) | 20 | Đã kết nối | Đã hoàn thành | Phát tự động khi gọi REST `POST /messages/{messageId}/pins` |
| 75 | Server → Client | `/topic/room.{roomId}` | Tin nhắn vừa bị bỏ ghim (`MessageResponse` với `isPinned = false`) | 20 | Đã kết nối | Đã hoàn thành | Phát tự động khi gọi REST `DELETE /messages/{messageId}/pins` |
| — | Server → Client | `/topic/room.{roomId}.reactions` | Thả / đổi / gỡ cảm xúc (`ReactionResponse`, `reactionType = null` khi gỡ) | — | Đã kết nối | Đã hoàn thành | Phát tự động khi gọi REST `POST / DELETE .../reactions` |
| 76 | Server → Client | `/topic/room.{roomId}.typing` | Có người đang gõ (`TypingEventResponse` với `typing = true`) | — | Đã kết nối | Đã hoàn thành | Kèm `userId`, `username`, `avatarUrl` |
| 77 | Server → Client | `/topic/room.{roomId}.typing` | Người đó ngừng gõ (`TypingEventResponse` với `typing = false`) | — | Đã kết nối | Đã hoàn thành |  |
| 78 | Server → Client | `message_seen` | Có người đã xem tin | — | Đã kết nối | Đang thực hiện |  |
| 79 | Server → Client | `message_delivered` | Tin đã tới thiết bị người nhận | — | Đã kết nối | Đang thực hiện |  |
| 80 | Server → Client | `poll_updated` | Poll mới, có người vote hoặc poll bị đóng | 27, 28 | Đã kết nối | Đã có trong spec |  |
| 81 | Server → Client | `reminder_created` | Lịch hẹn mới | 25 | Đã kết nối | Đã có trong spec |  |
| 82 | Server → Client | `reminder_updated` | Có người phản hồi RSVP hoặc lịch hẹn đổi trạng thái | 26 | Đã kết nối | Đã có trong spec |  |
| 83 | Server → Client | `ai_suggestion` | AI gợi ý poll hoặc lịch hẹn (chưa lưu DB) | 30, 31 | Đã kết nối | Đã có trong spec |  |
| 84 | Server → Client | `/user/queue/notifications` | Thông báo mới | 24 | Đã kết nối | Đã có trong spec |  |
| 85 | Server → Client | `user_online` | Bạn bè kết nối | 16 | Đã kết nối | Đang thực hiện |  |
| 86 | Server → Client | `user_offline` | Bạn bè ngắt kết nối | 16 | Đã kết nối | Đang thực hiện |  |
| 87 | Server → Client | `room_updated` | Phòng đổi tên, avatar hoặc theme | — | Đã kết nối | Đã có trong spec |  |
| 88 | Server → Client | `member_changed` | Thành viên được thêm, bị xóa hoặc rời phòng | 14 | Đã kết nối | Đã có trong spec |  |
| 89 | Server → Client | `/user/queue/errors` | Báo lỗi (`ApiResponse`) riêng cho user vừa gửi frame STOMP lỗi | — | Đã kết nối | Đã hoàn thành | Xử lý qua `@MessageExceptionHandler` |
| 90 | Server → Client | `pong` | Trả lời ping | — | Đã kết nối | Đã có trong spec |  |
