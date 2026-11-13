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
| Users | REST | 9 |
| Friendship | REST | 7 |
| Rooms | REST | 17 |
| Messages | REST | 11 |
| Uploads | REST | 1 |
| Polls | REST | 6 |
| Reminders | REST | 6 |
| Notifications | REST | 3 |
| AI | REST | 2 |
| Admin | REST | 10 |
| WebSocket gửi lên | WebSocket | 9 |
| WebSocket nhận về | WebSocket | 20 |
| **Tổng** | | **105** |

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
| 5 | `GET` | `/users/me` | Lấy thông tin bản thân | 4 | Đăng nhập | Đã hoàn thành | Kèm trường `isOnline` |
| 6 | `PUT` | `/users/me` | Cập nhật hồ sơ, avatar, ngôn ngữ ưa thích | 4 | Đăng nhập | Đã hoàn thành |  |
| — | `PUT` | `/user/me/email` | Đổi email | — | Đăng nhập | Đã hoàn thành | Hỗ trợ cả `/users/me/email` |
| — | `PUT` | `/user/me/phone` | Đổi SĐT | — | Đăng nhập | Đã hoàn thành | Hỗ trợ cả `/users/me/phone` |
| — | `PUT` | `/user/me/password` | Đổi mật khẩu | — | Đăng nhập | Đã hoàn thành | Hỗ trợ cả `/users/me/password` |
| 7 | `GET` | `/users/search` | Tìm người dùng theo email, SĐT hoặc username (`?q=...`) | 17 | Đăng nhập | Đã hoàn thành | Kèm trường `isOnline` của từng kết quả |
| 8 | `GET` | `/users/{userId}` | Xem profile người dùng | — | Đăng nhập | Đã hoàn thành | Kèm trường `isOnline` |
| — | `GET` | `/users/{userId}/presence` | Lấy trạng thái hiện diện (Online / lastSeenAt) | 16 | Đăng nhập | Đã hoàn thành | Trả `UserPresenceResponse` |
| — | `POST` | `/users/presence/batch` | Lấy trạng thái hiện diện hàng loạt cho nhiều user | 16 | Đăng nhập | Đã hoàn thành | Nhận `BatchPresenceRequest` (tối đa 100 ID, tự khử trùng). Khuyên dùng cho Sidebar: Polling 30–60s kết hợp Window Focus & Tab Visibility Change |

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
| — | `PUT` | `/rooms/{roomId}/members/me/read` | Đánh dấu tin nhắn đã đọc (Read / Seen) | — | Đăng nhập | Đã hoàn thành | Nhận `MarkReadRequest` (`messageId`), broadcast realtime tới `/topic/room.{roomId}.read` và `.receipts` |
| — | `PUT` | `/rooms/{roomId}/members/me/delivered` | Đánh dấu tin nhắn đã nhận (Delivered) | — | Đăng nhập | Đã hoàn thành | Nhận `MarkReadRequest` (`messageId`), broadcast realtime tới `/topic/room.{roomId}.delivered` và `.receipts` |
| — | `GET` | `/rooms/{roomId}/members/invitable` | Lấy danh sách bạn bè có thể mời vào phòng | — | Đăng nhập | Đã hoàn thành | Loại trừ những người đã ở trong phòng |

## Messages

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 28 | `GET` | `/rooms/{roomId}/messages` | Lịch sử tin nhắn, phân trang cursor (`?cursor=...&before=...&limit=20`) | 21 | Đăng nhập | Đã hoàn thành | Kèm danh sách `reactions` nạp theo batch chống N+1 |
| 29 | `POST` | `/rooms/{roomId}/messages` | Gửi tin nhắn (text, image, file, reply) | 7, 8, 9, 10 | Đăng nhập | Đã hoàn thành | Tự động phát realtime qua `/topic/room.{roomId}` |
| 30 | `PUT` | `/rooms/{roomId}/messages/{messageId}` | Chỉnh sửa tin nhắn | 11 | Đăng nhập (Chính chủ) | Đã hoàn thành | Lưu `MessageHistory` + phát realtime qua `/topic/room.{roomId}` |
| 31 | `DELETE` | `/rooms/{roomId}/messages/{messageId}` | Thu hồi tin nhắn (xóa mềm `isRevoked = true`) | 12 | Chính chủ / Admin / Owner | Đã hoàn thành | Ẩn nội dung gốc + phát realtime qua `/topic/room.{roomId}` |
| 32 | `POST` | `/messages/{messageId}/translate` | Dịch tin nhắn (gọi Groq LLaMA AI, tự động lưu cache vào DB translations) | 29 | Đăng nhập | Đã hoàn thành | Nhận diện ngôn ngữ gốc và dịch sang `preferredLanguage` |
| 33 | `GET` | `/rooms/{roomId}/messages/search` | Tìm kiếm tin nhắn trong phòng cho Drawer Search Preview (`?q=...`) | 19 | Đăng nhập | Đã hoàn thành | Hỗ trợ alias `/search/sidebar` |
| 34 | `GET` | `/rooms/{roomId}/messages/{messageId}/history` | Xem lịch sử chỉnh sửa của tin nhắn | 11 | Đăng nhập | Đã hoàn thành | Dựa trên bảng `message_histories` |
| 35 | `DELETE` | `/rooms/{roomId}/messages` | Xóa lịch sử chat (phía mình) | 21 | Đăng nhập | Cần bổ sung |  |
| 36 | `POST` | `/rooms/{roomId}/messages/{messageId}/pins` | Ghim tin nhắn (`isPinned = true`) | 20 | Đăng nhập | Đã hoàn thành | Phát realtime `MessageResponse` qua `/topic/room.{roomId}` |
| 37 | `DELETE` | `/rooms/{roomId}/messages/{messageId}/pins` | Bỏ ghim tin nhắn (`isPinned = false`) | 20 | Đăng nhập | Đã hoàn thành | Phát realtime `MessageResponse` qua `/topic/room.{roomId}` |
| 38 | `GET` | `/rooms/{roomId}/messages/pins` | Danh sách tin nhắn đã ghim | 20 | Đăng nhập | Đã hoàn thành | Gom về `MessageController` |
| — | `GET` | `/rooms/{roomId}/messages/media` | Lấy toàn bộ file phương tiện (Ảnh, Video, Tài liệu) trong phòng chat | — | Đăng nhập | Đã hoàn thành | Dùng cho Sidebar thông tin phòng chat, sắp xếp mới nhất (DESC) |
| — | `POST` | `/rooms/{roomId}/messages/{messageId}/reactions` | Thả hoặc cập nhật cảm xúc tin nhắn | — | Đăng nhập | Đã hoàn thành | Phát realtime `ReactionResponse` qua `/topic/room.{roomId}.reactions` |
| — | `DELETE` | `/rooms/{roomId}/messages/{messageId}/reactions` | Gỡ cảm xúc đã thả | — | Đăng nhập | Đã hoàn thành | Phát realtime `ReactionResponse` (`reactionType = null`) qua `/topic/room.{roomId}.reactions` |
| — | `GET` | `/rooms/{roomId}/messages/{messageId}/reactions` | Xem danh sách cảm xúc trên tin nhắn | — | Đăng nhập | Đã hoàn thành |  |

## Uploads & File Storage

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 39 | `POST` | `/users/me/avatar` | Upload cập nhật ảnh đại diện người dùng (multipart) | 4 | Đăng nhập | Đã hoàn thành | Lưu file vào `uploads/avatars` |
| — | `POST` | `/rooms/{roomId}/messages/images` | Gửi tin nhắn hình ảnh đính kèm (multipart) | 8 | Đăng nhập | Đã hoàn thành | Hỗ trợ cả trả lời tin nhắn (`repliedMessageId`) |
| — | `POST` | `/rooms/{roomId}/messages/files` | Gửi tin nhắn tệp tin đính kèm (multipart) | 9 | Đăng nhập | Đã hoàn thành | Hỗ trợ cả trả lời tin nhắn (`repliedMessageId`) |
| — | `GET` | `/files` | Tải và xem file đính kèm (`?storageKey=...`) | — | Đăng nhập | Đã hoàn thành | Trả `Resource` với MediaType và Content-Disposition inline |
| — | `GET` | `/files/{fileName}` | Xem ảnh đại diện người dùng (`/files/{fileName}`) | — | Công khai | Đã hoàn thành | Trả `Resource` từ `uploads/avatars` |

## Polls

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| 40 | `GET` | `/room/{roomId}/polls` | Danh sách cuộc bình chọn trong phòng | 27 | Đăng nhập | Đã hoàn thành | Hỗ trợ cả `/room/{roomId}/poll` |
| 41 | `POST` | `/room/{roomId}/polls` | Tạo cuộc bình chọn mới trong nhóm chat | 27, 31 | Đăng nhập | Đã hoàn thành | Nhận tiêu đề, câu hỏi, danh sách lựa chọn ban đầu |
| 42 | `GET` | `/room/{roomId}/polls/{pollId}` | Xem thông tin chi tiết 1 cuộc bình chọn | 28 | Đăng nhập | Đã hoàn thành | Trả kèm chi tiết số vote và người vote từng phương án |
| — | `POST` | `/room/{roomId}/polls/{pollId}/options` | Thêm phương án lựa chọn mới vào cuộc bình chọn | — | Đăng nhập | Đã hoàn thành |  |
| 43 | `PUT` | `/room/{roomId}/polls/{pollId}/vote` | Bỏ phiếu hoặc đổi phiếu bầu cho lựa chọn | 28 | Đăng nhập | Đã hoàn thành | Broadcast cập nhật realtime qua `/topic/room.{roomId}.poll` |
| 44 | `PUT` | `/room/{roomId}/polls/{pollId}/close` | Đóng / Kết thúc cuộc bình chọn (`isActive = false`) | — | Người tạo hoặc Admin | Đã hoàn thành | Broadcast cập nhật realtime qua WebSocket |

## Reminders

| STT | Method | Endpoint | Mô tả | STT CN | Quyền | Tình trạng | Ghi chú |
|---|---|---|---|---|---|---|---|
| — | `GET` | `/reminders` | Lấy tất cả lời nhắc trong các room mà người dùng tham gia | — | — | — |  |
| 45 | `GET` | `/rooms/{roomId}/reminders` | Danh sách lịch hẹn trong phòng (Tối ưu chống N+1) | 25 | Đăng nhập | Đã hoàn thành | Batching 3 câu SQL, map RAM $O(1)$ |
| 46 | `POST` | `/rooms/{roomId}/reminders` | Tạo lịch hẹn cá nhân hoặc nhóm (thủ công hoặc AI) | 25, 30 | Đăng nhập | Đã hoàn thành | 2-step persist, đồng bộ `Message` và `ReminderResource` |
| 47 | `GET` | `/rooms/{roomId}/reminders/{reminderId}` | Xem chi tiết 1 lịch hẹn | 25 | Đăng nhập | Đã hoàn thành | Trả kèm chi tiết người tham gia và thống kê RSVP |
| 48 | `DELETE`/`PUT` | `/rooms/{roomId}/reminders/{reminderId}` | Hủy lịch hẹn (`CANCELLED`) | 25 | Chính chủ / Admin / Owner | Đã hoàn thành | Chặn hủy lịch đã kết thúc `DONE`, phát realtime |
| 49 | `PUT`/`PATCH` | `/rooms/{roomId}/reminders/{reminderId}/rsvp` | Phản hồi lịch hẹn (`ACCEPTED`, `DECLINED`) | 26 | Đăng nhập | Đã hoàn thành | Bảo vệ lịch cá nhân, phát realtime `/topic/room.{roomId}.reminder` |

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
| 68 | Client → Server | `SEND /app/chat.markRead` | Đánh dấu đã đọc (`MarkMessageReceiptPayload`: `roomId`, `messageId`) | — | Đã kết nối | Đã hoàn thành | Cập nhật Watermark `lastReadMessageId`, broadcast tới `/topic/room.{roomId}.receipts` (`receiptType: READ`) |
| — | Client → Server | `SEND /app/chat.markDelivered` | Đánh dấu đã nhận (`MarkMessageReceiptPayload`: `roomId`, `messageId`) | — | Đã kết nối | Đã hoàn thành | Cập nhật Watermark `lastDeliveredMessageId`, broadcast tới `/topic/room.{roomId}.receipts` (`receiptType: DELIVERED`) |
| 69 | Client → Server | `poll_vote` | Bình chọn poll qua WebSocket | 28 | Đã kết nối | Đã có trong spec |  |
| 70 | Client → Server | `ping` | Kiểm tra kết nối còn sống (STOMP Heartbeat) | — | Đã kết nối | Đã hoàn thành | Cấu hình Heartbeat 15s/15s kèm `ThreadPoolTaskScheduler` |

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
| 78 | Server → Client | `/topic/room.{roomId}.receipts` | Biên nhận tin nhắn thời gian thực: Đã nhận (2 tích xám) hoặc Đã đọc (avatar người xem) | — | Đã kết nối | Đã hoàn thành | Kênh hợp nhất chuẩn hóa, kèm `receiptType: "DELIVERED" \| "READ"`, `userId`, `username`, `avatarUrl`, `messageId`, `timestamp` |
| 80 | Server → Client | `poll_updated` | Poll mới, có người vote hoặc poll bị đóng | 27, 28 | Đã kết nối | Đã có trong spec |  |
| 81 | Server → Client | `reminder_created` | Lịch hẹn mới | 25 | Đã kết nối | Đã có trong spec |  |
| 82 | Server → Client | `reminder_updated` | Có người phản hồi RSVP hoặc lịch hẹn đổi trạng thái | 26 | Đã kết nối | Đã có trong spec |  |
| 83 | Server → Client | `ai_suggestion` | AI gợi ý poll hoặc lịch hẹn (chưa lưu DB) | 30, 31 | Đã kết nối | Đã có trong spec |  |
| 84 | Server → Client | `/user/queue/notifications` | Thông báo mới | 24 | Đã kết nối | Đã có trong spec |  |
| 85 | Server → Client | `/topic/users.presence` & `/topic/users.{userId}.presence` | Trạng thái Online (`UserPresenceResponse` với `isOnline = true`) | 16 | Đã kết nối | Đã hoàn thành | Broadcast khi sessionCount chuyển từ 0 lên 1 (Multi-session) |
| 86 | Server → Client | `/topic/users.presence` & `/topic/users.{userId}.presence` | Trạng thái Offline (`UserPresenceResponse` với `isOnline = false`, `lastSeenAt`) | 16 | Đã kết nối | Đã hoàn thành | Broadcast & lưu DB khi sessionCount về 0 hoặc ngắt do rớt mạng |
| 87 | Server → Client | `room_updated` | Phòng đổi tên, avatar hoặc theme | — | Đã kết nối | Đã có trong spec |  |
| 88 | Server → Client | `member_changed` | Thành viên được thêm, bị xóa hoặc rời phòng | 14 | Đã kết nối | Đã có trong spec |  |
| 89 | Server → Client | `/user/queue/errors` | Báo lỗi (`ApiResponse`) riêng cho user vừa gửi frame STOMP lỗi | — | Đã kết nối | Đã hoàn thành | Xử lý qua `@MessageExceptionHandler` |
| 90 | Server → Client | `pong` | Trả lời ping | — | Đã kết nối | Đã hoàn thành | Tự động xử lý qua STOMP Heartbeat ping-pong định kỳ |
