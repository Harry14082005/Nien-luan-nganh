import { COMPILED } from "./pollDetector.js";
import { hit, units } from "./matcher.js";
import { normalizeFor } from "./normalize.js";
const WINDOW_MS = 5 * 60 * 1e3;
const MAX_SHORT_LATIN = 5;
const MAX_SHORT_CJK = 12;
const QMARK = /[?？؟]/u;
const FILLER = /^(ok|oke|okie|uh|ừ|ừm|haha+|hihi+|hehe+|kkk+|lol|:\)+|=\)+|\+1)$/u;
const isEligible = (m) => m.type === "TEXT" && !m.revoked;
function isShort(text) {
  const t = text.trim();
  const cjk = /[\u3040-\u30ff\u3400-\u9fff\uac00-\ud7af]/u.test(t);
  return cjk ? units(t, false) <= MAX_SHORT_CJK : units(t, true) <= MAX_SHORT_LATIN;
}
function isQuestion(text) {
  if (QMARK.test(text)) return true;
  return COMPILED.some((cp) => hit(cp.cues, normalizeFor(text, cp.pack.latin)));
}
const key = (text) => normalizeFor(text, true);
function detectPollFromConversation(msgs) {
  const list = msgs.filter(isEligible);
  if (list.length < 3) return null;
  const last = list[list.length - 1];
  const tail = list.filter((m) => last.createdAt - m.createdAt <= WINDOW_MS);
  let qi = -1;
  for (let j = tail.length - 2; j >= 0; j--) {
    if (isQuestion(tail[j].text)) {
      qi = j;
      break;
    }
    if (!isShort(tail[j].text)) return null;
  }
  if (qi < 0 || !isShort(last.text)) return null;
  const q = tail[qi];
  const replies = tail.slice(qi + 1).filter((m) => !FILLER.test(m.text.trim().toLowerCase()));
  const senders = new Set(replies.filter((m) => m.senderId !== q.senderId).map((m) => m.senderId));
  const answers = new Set(replies.map((m) => key(m.text)));
  if (replies.length < 2 || senders.size < 2 || answers.size < 2) return null;
  if (FILLER.test(last.text.trim().toLowerCase())) return null;
  return {
    anchorId: last.id,
    coversIds: tail.slice(qi).map((m) => m.id),
    reasons: ["conversation-cluster"]
  };
}
export {
  detectPollFromConversation
};
