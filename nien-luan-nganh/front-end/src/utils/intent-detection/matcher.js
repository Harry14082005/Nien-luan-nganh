import { normalizeFor } from "./normalize.js";
const esc = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
function build(words, pack, flags = "u") {
  if (!words.length) return null;
  const alt = [...new Set(words.map((w) => esc(normalizeFor(w, pack.latin)).replace(/ /g, "\\s+")))].sort((a, b) => b.length - a.length).join("|");
  return pack.latin ? new RegExp(`(?<![\\p{L}\\p{N}])(?:${alt})(?![\\p{L}\\p{N}])`, flags) : new RegExp(alt, flags);
}
function compile(pack) {
  const p = pack.poll;
  const n = (xs) => xs.map((x) => normalizeFor(x, pack.latin));
  return {
    pack,
    strong: build(p.strong, pack),
    weak: build(p.weak, pack),
    cues: build(p.questionCues, pack),
    orG: build(p.or, pack, "gu"),
    notAfter: n(p.orNotAfter),
    notBefore: new Set(n(p.orNotBefore))
  };
}
const units = (s, latin) => latin ? s.split(" ").filter(Boolean).length : [...s.replace(/\s/gu, "")].length;
const hit = (re, text) => !!re && re.test(text);
function hasAorB(text, cp, hasQ) {
  if (!cp.orG) return false;
  const { latin, poll } = cp.pack;
  const lim = latin ? { total: 16, left: 10, right: 8 } : { total: 36, left: 18, right: 15 };
  const t = text.replace(/[?!.,]+/gu, " ").replace(/\s+/gu, " ").trim();
  const total = units(t, latin);
  if (total > lim.total) return false;
  if (!hasQ && total > poll.orBareMaxUnits) return false;
  for (const m of t.matchAll(cp.orG)) {
    const left = t.slice(0, m.index).trim();
    const right = t.slice((m.index ?? 0) + m[0].length).trim();
    if (!left || !right) continue;
    if (units(left, latin) > lim.left || units(right, latin) > lim.right) continue;
    const rightHead = latin ? right.split(" ")[0] : right;
    if (cp.notAfter.some((x) => latin ? rightHead === x : right.startsWith(x))) continue;
    const leftTail = latin ? left.split(" ").pop() : left.slice(-1);
    if (cp.notBefore.has(leftTail)) continue;
    return true;
  }
  return false;
}
export {
  compile,
  hasAorB,
  hit,
  units
};
