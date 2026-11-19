import { describe, expect, it } from "vitest";
import { detectPollFromConversation, scorePoll } from "./index";
const pos = [
  "Trưa nay ăn lẩu hay nướng?",
  "lẩu hay nướng",
  "7h hay 8h",
  "an lau hay nuong",
  "ăn lẩu hay nướng đây",
  "Mọi người thấy sao, vote nhé",
  "Bình chọn đi",
  "ko biết chọn gì, ý kiến thế nào?",
  "Mọi người prefer Monday hay Wednesday?",
  "Should we meet Monday or Wednesday?",
  "pizza or sushi",
  "let's vote",
  "Which one do you like?",
  "mai họp 9h hay 10h?"
];
const neg = [
  "Cái này hay quá",
  "có đi hay không",
  "Phim này hay lắm",
  "rất hay",
  "nhớ bạn quá",
  "ok nha",
  "Hôm nay trời đẹp quá?",
  "Which floor is it?",
  "I'll call you or text you later.",
  "Bạn làm gì đó?",
  "hay ghê",
  "Anh có ăn hay chưa?"
];
describe("scorePoll", () => {
  it.each(pos)("POS: %s", (t) => expect(scorePoll(t).shouldSuggest).toBe(true));
  it.each(neg)("NEG: %s", (t) => expect(scorePoll(t).shouldSuggest).toBe(false));
});
let n = 0;
const m = (sender, text, t) => ({ id: `m${++n}`, senderId: sender, text, createdAt: t * 1e3, type: "TEXT" });
describe("conversation cluster", () => {
  it("câu hỏi + nhiều đáp án ngắn từ nhiều người", () => {
    const r = detectPollFromConversation([
      m("A", "Trưa nay ăn gì ta", 0),
      m("B", "lẩu đi", 10),
      m("C", "nướng nha", 20),
      m("D", "cơm tấm", 30)
    ]);
    expect(r?.anchorId).toBe(`m${n}`);
    expect(r?.coversIds.length).toBe(4);
  });
  it("chỉ 1 người trả lời -> không", () => {
    expect(detectPollFromConversation([
      m("A", "ăn gì ta?", 0),
      m("B", "lẩu đi", 10),
      m("B", "nướng nha", 20)
    ])).toBeNull();
  });
  it("cùng đáp án -> không", () => {
    expect(detectPollFromConversation([
      m("A", "ăn gì ta?", 0),
      m("B", "lẩu", 10),
      m("C", "lẩu", 20)
    ])).toBeNull();
  });
  it("tin dài chen vào phá cụm", () => {
    expect(detectPollFromConversation([
      m("A", "ăn gì ta?", 0),
      m("B", "lẩu đi", 10),
      m("C", "hôm qua mình mới ăn lẩu xong nên hôm nay thôi nha", 20),
      m("D", "nướng", 30)
    ])).toBeNull();
  });
  it("quá 5 phút -> không", () => {
    expect(detectPollFromConversation([
      m("A", "ăn gì ta?", 0),
      m("B", "lẩu đi", 400),
      m("C", "nướng nha", 410)
    ])).toBeNull();
  });
  it("chỉ haha/ok -> không", () => {
    expect(detectPollFromConversation([
      m("A", "ăn gì ta?", 0),
      m("B", "haha", 10),
      m("C", "ok", 20)
    ])).toBeNull();
  });
});
