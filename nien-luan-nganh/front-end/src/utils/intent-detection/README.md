# intent-detection

Module nhận diện **"tin nhắn này có nên hiện nút Tạo poll không"**, chạy hoàn toàn ở FE
bằng regex và chấm điểm. **Không gọi API, không gọi AI.** AI chỉ được gọi khi user bấm nút.

```js
import {
  scorePoll,
  detectPollFromConversation,
  isEligibleForPollButton,
} from '@/utils/intent-detection'

scorePoll('lẩu hay nướng')
// { type: 'POLL', shouldSuggest: true, score: 2, reasons: ['A-or-B'] }
```

---

## 1. Bức tranh tổng thể

Có **2 cách** để quyết định hiện nút, chạy song song:

| Cách | Xét cái gì | Hàm | Ví dụ bắt được |
|---|---|---|---|
| A. Theo **từng tin** | Nội dung 1 tin nhắn | `scorePoll(text)` | "lẩu hay nướng", "vote nhé" |
| B. Theo **cụm hội thoại** | Nhiều tin liên tiếp | `detectPollFromConversation(msgs)` | A hỏi "ăn gì ta", B "lẩu đi", C "nướng nha" |

Ngoài ra `isEligibleForPollButton(msg)` là bộ lọc điều kiện chung (tin còn mới không,
có bị thu hồi không...). Kết quả của A và B chỉ cho biết "nên mời bấm", còn quyết định
cuối cùng vẫn là AI ở backend khi user bấm.

```
text ──► normalize ──► matcher (so khớp keyword, "A hay B") ──► pollDetector (cộng điểm) ──► shouldSuggest
                         ▲
                         └── packs/vi.js, packs/en.js  (dữ liệu từ khóa theo ngôn ngữ)

nhiều tin ──► conversation.js (đếm tin ngắn của nhiều người sau 1 câu hỏi) ──► anchorId
```

---

## 2. Cấu trúc file

```
intent-detection/
├── index.js           # cửa ra vào: những gì bên ngoài được import
├── normalize.js       # chuẩn hóa chuỗi trước khi so khớp
├── matcher.js         # compile keyword thành regex + kiểm tra cấu trúc "A hay B"
├── pollDetector.js    # scorePoll(): chấm điểm 1 tin nhắn
├── conversation.js    # detectPollFromConversation(): bắt cụm hội thoại
├── packs/
│   ├── vi.js          # từ khóa tiếng Việt
│   └── en.js          # từ khóa tiếng Anh
└── detect.spec.js     # test
```

---

## 3. Giải thích từng file

### `index.js`
Chỉ re-export, không có logic. Bên ngoài chỉ nên import từ file này.

| Export | Là gì |
|---|---|
| `scorePoll` | chấm điểm 1 tin (từ `pollDetector.js`) |
| `POLL_THRESHOLD` | ngưỡng điểm để hiện nút (hiện là `2`) |
| `detectPollFromConversation` | bắt cụm hội thoại (từ `conversation.js`) |
| `isEligibleForPollButton(msg, opts)` | bộ lọc điều kiện hiện nút (định nghĩa ngay trong `index.js`) |

**`isEligibleForPollButton(msg, { now, maxAgeMs, usedAsPollSource })`**
Trả `true` khi đồng thời thỏa:
- `msg.type === 'TEXT'` và chưa bị thu hồi (`!msg.revoked`)
- tin còn trong `maxAgeMs` (mặc định **1 giờ** = `3_600_000` ms)
- id của tin **không** nằm trong `usedAsPollSource` (một `Set` các messageId đã được dùng làm nguồn cho poll, lấy từ `PollResource`)

Chưa bao gồm: quyền tạo poll của user trong room. Phần đó bạn kiểm tra riêng ở component.

---

### `normalize.js`
Làm sạch chuỗi để so khớp ổn định, kể cả khi người dùng gõ không dấu hoặc viết tắt.

| Hàm | Việc làm |
|---|---|
| `baseNormalize(s)` | Unicode NFC, chữ thường, gộp khoảng trắng, đổi `？` `؟` thành `?`, đổi nháy cong `’` thành `'` |
| `stripDiacritics(s)` | Bỏ dấu ("lẩu" thành "lau"), xử lý riêng `đ` thành `d`. **Chỉ dùng cho chữ Latin** |
| `normalizeFor(text, latin)` | Hàm chính. `latin = true`: `baseNormalize` + bỏ dấu + chat-speak. `latin = false` (CJK): chỉ `baseNormalize` |

**Bảng chat-speak** (hằng `CHAT_SPEAK`, chạy sau khi đã bỏ dấu):
`ko / k / hk / hong / khg → khong`, `j → gi`, `dc → duoc`, `ntn → nhu the nao`.

> Vì sao không bỏ dấu cho tiếng Nhật/Trung/Hàn? Bỏ dấu sẽ làm mất dakuten (が thành か) và đổi nghĩa.
>
> Vì sao không đổi `;` thành `?` ? Tiếng Anh dùng `;` rất nhiều, đổi sẽ nhận nhầm thành câu hỏi.

**Muốn đổi:** thêm chat-speak mới thì sửa `CHAT_SPEAK`. Nhớ viết **không dấu** (cả key lẫn value).

---

### `matcher.js`
Biến danh sách từ khóa thành regex và kiểm tra cấu trúc "A hay B".

**`compile(pack)`** → trả về "pack đã biên dịch" gồm:
`strong`, `weak`, `cues` (3 regex), `orG` (regex từ nối), `notAfter`, `notBefore`.
Được gọi **một lần** khi load module, nên không tốn công mỗi lần render.
- Nhóm Latin: bọc regex bằng lookaround Unicode `(?<![\p{L}\p{N}])...(?![\p{L}\p{N}])` để khớp **nguyên từ** (không dùng `\b` vì hỏng với chữ có dấu).
- Nhóm CJK: khớp trực tiếp (không có khoảng trắng giữa từ).
- Keyword dài được thử trước (vd "hay là" thử trước "hay").
- Keyword trong pack cũng được chuẩn hóa bằng `normalizeFor`, nên bạn có thể viết **có dấu** trong pack, hệ thống tự bỏ dấu.

**`hit(re, text)`**: trả `true` nếu regex khớp.

**`units(s, latin)`**: đếm độ dài. Latin đếm **số từ**, CJK đếm **số ký tự**.

**`hasAorB(text, cp, hasQ)`**: kiểm tra có cấu trúc "A <hay> B" **hợp lệ** không. Điều kiện:
1. Cả tin ngắn: tối đa **12 từ** (CJK: 30 ký tự).
2. Vế trái tối đa **6 từ**, vế phải tối đa **5 từ** (CJK: 14 / 12 ký tự).
3. **Không có dấu `?`** thì tin phải ngắn hơn `orBareMaxUnits` của pack (vi: 8, en: 4).
4. Từ đầu của vế phải **không** thuộc `orNotAfter` (loại "hay không", "hay quá", "or not"...).
5. Từ cuối của vế trái **không** thuộc `orNotBefore` (loại "rất hay", "thật hay"...).

Các con số ở mục 1 và 2 nằm trong biến `lim` của hàm. Muốn nới/siết thì sửa ở đó.

---

### `pollDetector.js`
**`scorePoll(text)`** là hàm chấm điểm chính.

Chạy **tất cả pack** (không detect ngôn ngữ), nên tin pha trộn như "Mọi người prefer Monday hay Wednesday?" vẫn bắt được.

| Tín hiệu | Điểm | Mã trong `reasons` |
|---|---|---|
| Có keyword **mạnh** (vote, bình chọn...) | +3 | `strong` |
| Có cấu trúc "A hay B" hợp lệ | +2 | `A-or-B` |
| Có dấu hỏi `? ？ ؟` | +1 | `question-mark` |
| Có keyword **yếu** (chọn, nên...) | +1 | `weak` |

Trả về: `{ type: 'POLL', shouldSuggest, score, reasons }`
`shouldSuggest = score >= POLL_THRESHOLD`.

Các export khác: `PACKS` (mảng pack đang bật), `COMPILED` (bản đã biên dịch), `POLL_THRESHOLD`.

**Muốn đổi:**
- Hiện nút dễ hơn/khó hơn: sửa `POLL_THRESHOLD` (hạ xuống `1` sẽ hiện rất nhiều, nâng lên `3` thì chỉ còn keyword mạnh hoặc "A hay B" kèm dấu hỏi).
- Đổi trọng số: sửa các dòng `score += ...`.
- **Debug khi nút hiện sai/thiếu:** gọi `scorePoll(text).reasons` để xem tin đó ăn điểm ở đâu.

---

### `conversation.js`
**`detectPollFromConversation(msgs)`** bắt kiểu hội thoại không tin nào có từ khóa:

```
A: trưa nay ăn gì ta     ← câu hỏi
B: lẩu đi                ← đáp án ngắn
C: nướng nha             ← đáp án ngắn, người khác
D: cơm tấm               ← đáp án ngắn, người khác  → nút hiện ở tin này
```

`msgs` là mảng tin của room, **sắp theo thời gian tăng dần** (dạng như mục 4). Gọi mỗi khi có tin mới.

**Điều kiện để trả kết quả** (nếu không thỏa, trả `null`):
1. Có ít nhất 3 tin hợp lệ (TEXT, chưa thu hồi).
2. Chỉ xét các tin trong **5 phút** gần nhất so với tin mới nhất.
3. Đi ngược từ cuối tìm **tin câu hỏi** (có `?` hoặc chứa từ nghi vấn như "gì / nào / đâu", "what / which"). Nếu gặp **tin dài** không phải câu hỏi giữa đường thì cụm bị phá, trả `null`.
4. Tin mới nhất phải **ngắn** (tối đa 5 từ, hoặc 12 ký tự CJK).
5. Sau câu hỏi có ít nhất **2 đáp án**, từ ít nhất **2 người khác** người hỏi, và **ít nhất 2 đáp án khác nhau**.
6. Các tin kiểu "haha", "ok", "kkk", "+1" **không** tính là đáp án (regex `FILLER`).

**Kết quả:** `{ anchorId, coversIds, reasons }`
- `anchorId`: id tin mới nhất trong cụm, nơi gắn nút.
- `coversIds`: id mọi tin trong cụm. Dùng để **ẩn nút regex ở các tin cũ**, chỉ giữ 1 nút ở cuối cụm.

**Lưu ý khi dùng:** hàm chỉ xét đuôi danh sách. Khi có tin dài chen vào sau đó thì kết quả lần gọi sau sẽ là `null` và nút có thể biến mất. Vì vậy phía component nên **lưu `anchorId` vào một `Set`** ngay khi nhận được.

**Hằng số ở đầu file có thể chỉnh:** `WINDOW_MS` (5 phút), `MAX_SHORT_LATIN` (5 từ), `MAX_SHORT_CJK` (12 ký tự), `FILLER` (danh sách tin "không tính").

---

### `packs/vi.js`, `packs/en.js`
Dữ liệu thuần cho từng ngôn ngữ, **không có logic**. Cấu trúc một pack:

```js
export const vi = {
  lang: 'vi',
  latin: true,          // true: chữ Latin (bỏ dấu + khớp nguyên từ); false: CJK
  poll: {
    strong: [...],      // keyword mạnh, +3 điểm
    weak: [...],        // keyword yếu, +1 điểm
    or: [...],          // từ nối "A <or> B": 'hay là', 'hay', 'hoặc'
    orNotAfter: [...],  // nếu NGAY SAU từ nối là các từ này thì loại ("hay không", "hay quá")
    orNotBefore: [...], // nếu NGAY TRƯỚC từ nối là các từ này thì loại ("rất hay")
    orBareMaxUnits: 8,  // không có "?" thì cả tin phải ngắn hơn số từ này
    questionCues: [...] // từ nghi vấn, dùng cho conversation.js (gì, nào, đâu...)
  },
}
```

---

### `detect.spec.js`
Test bằng vitest, chia 2 nhóm:
- `scorePoll`: bảng câu **nên hiện nút** (`pos`) và **không nên** (`neg`).
- `conversation cluster`: các tình huống cụm hội thoại (đủ điều kiện, 1 người trả lời, cùng đáp án, tin dài chen vào, quá 5 phút, chỉ haha/ok).

Chạy: `npx vitest run`

---

## 4. Dạng message đầu vào

Dùng cho `detectPollFromConversation` và `isEligibleForPollButton`:

```js
{
  id: 'uuid',            // messageId
  senderId: 'uuid',      // người gửi
  text: 'lẩu đi',        // nội dung
  createdAt: 1731800000000, // thời điểm gửi, epoch mili giây
  type: 'TEXT',          // 'TEXT' | 'SYSTEM' | ... (chỉ TEXT được xét)
  revoked: false         // tùy chọn, true nếu đã thu hồi
}
```

Nếu dữ liệu từ backend đặt tên field khác (ví dụ `content`, `sentAt`, `userId`), tạo một hàm map
sang dạng trên trước khi truyền vào, không cần sửa module.

---

## 5. Muốn đổi gì thì sửa ở đâu

| Muốn làm | Sửa ở đâu |
|---|---|
| Thêm / bớt từ khóa tiếng Việt, Anh | `packs/vi.js`, `packs/en.js` (mảng `strong`, `weak`, `or`) |
| Bớt nhận nhầm kiểu "hay quá" | `orNotAfter` / `orNotBefore` trong pack |
| Hiện nút dễ hơn / khó hơn | `POLL_THRESHOLD` trong `pollDetector.js` |
| Đổi điểm từng tín hiệu | các dòng `score += ...` trong `pollDetector.js` |
| "A hay B" cho phép dài hơn | biến `lim` trong `hasAorB` (`matcher.js`) |
| Tin không có `?` cũng được | tăng `orBareMaxUnits` trong pack (nhưng sẽ nhiễu hơn) |
| Thêm chat-speak (ví dụ `z` → `vay`) | `CHAT_SPEAK` trong `normalize.js` |
| Tin còn hiện nút trong bao lâu | `maxAgeMs` trong `isEligibleForPollButton` (mặc định 1 giờ) |
| Cửa sổ thời gian của cụm hội thoại | `WINDOW_MS` trong `conversation.js` |
| Đáp án "ngắn" là bao nhiêu từ | `MAX_SHORT_LATIN` / `MAX_SHORT_CJK` trong `conversation.js` |
| Loại thêm kiểu tin không tính là đáp án | `FILLER` trong `conversation.js` |
| Thêm ngôn ngữ mới | xem mục 6 |

Sau khi sửa, luôn chạy `npx vitest run`. Khi gặp tin bị bắt nhầm/sót ngoài đời thật, **thêm nó vào
`detect.spec.js` trước**, rồi mới chỉnh pack cho đến khi test pass.

---

## 6. Thêm ngôn ngữ mới (ví dụ tiếng Nhật)

1. Tạo `packs/ja.js`:
```js
export const ja = {
  lang: 'ja',
  latin: false,   // CJK: không bỏ dấu, khớp trực tiếp
  poll: {
    strong: ['投票', '多数決', 'どっちがいい'],
    weak: ['選ぶ', '決めよう'],
    or: ['それとも', 'か'],
    orNotAfter: [],
    orNotBefore: [],
    orBareMaxUnits: 15,   // đơn vị là ký tự
    questionCues: ['どこ', '何', 'いつ', 'どれ'],
  },
}
```
2. Trong `pollDetector.js`: `import { ja } from './packs/ja'` và đổi `PACKS = [vi, en, ja]`.
3. Thêm câu positive/negative của tiếng Nhật vào `detect.spec.js`.

> Với CJK, từ nối ngắn như `か` rất dễ khớp nhầm. Hãy để `orBareMaxUnits` nhỏ và ưu tiên cụm dài như `それとも`.

---

## 7. Giới hạn đã biết

- Từ khóa yếu "nên" (sau khi bỏ dấu thành `nen`) có thể trùng với "nền" ("nền tảng"). Hiện chỉ cộng 1 điểm nên ít gây hại.
- Tiếng Anh dùng "or" rất phổ biến nên bị siết chặt hơn (tin không có `?` phải ≤ 4 từ).
- Ngưỡng và danh sách từ là **giá trị khởi đầu**, nên điều chỉnh theo dữ liệu thật.
- Chưa có pack ja / zh / ko / es / fr.