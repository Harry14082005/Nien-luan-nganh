import { compile, hasAorB, hit } from "./matcher.js";
import { normalizeFor } from "./normalize.js";
import { en } from "./packs/en.js";
import { vi } from "./packs/vi.js";
const PACKS = [vi, en];
const COMPILED = PACKS.map(compile);
const POLL_THRESHOLD = 2;
const QMARK = /[?？؟]/u;
function scorePoll(text) {
  const reasons = [];
  let score = 0;
  const hasQ = QMARK.test(text);
  let strong = false, weak = false, aorb = false;
  for (const cp of COMPILED) {
    const t = normalizeFor(text, cp.pack.latin);
    strong ||= hit(cp.strong, t);
    weak ||= hit(cp.weak, t);
    aorb ||= hasAorB(t, cp, hasQ);
  }
  if (strong) {
    score += 3;
    reasons.push("strong");
  }
  if (aorb) {
    score += 2;
    reasons.push("A-or-B");
  }
  if (hasQ) {
    score += 1;
    reasons.push("question-mark");
  }
  if (weak) {
    score += 1;
    reasons.push("weak");
  }
  return { type: "POLL", shouldSuggest: score >= POLL_THRESHOLD, score, reasons };
}
export {
  COMPILED,
  PACKS,
  POLL_THRESHOLD,
  scorePoll
};
