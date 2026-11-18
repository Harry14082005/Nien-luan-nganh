function baseNormalize(s) {
  return s.normalize("NFC").toLowerCase().replace(/[？؟]/gu, "?").replace(/[’‘]/gu, "'").replace(/\s+/gu, " ").trim();
}
function stripDiacritics(s) {
  return s.normalize("NFD").replace(/\p{M}/gu, "").replace(/đ/gu, "d").normalize("NFC");
}
const CHAT_SPEAK = {
  ko: "khong",
  k: "khong",
  hk: "khong",
  hong: "khong",
  khg: "khong",
  j: "gi",
  dc: "duoc",
  ntn: "nhu the nao"
};
const CHAT_RE = new RegExp(
  `(?<![\\p{L}\\p{N}])(${Object.keys(CHAT_SPEAK).join("|")})(?![\\p{L}\\p{N}])`,
  "gu"
);
function normalizeFor(text, latin) {
  const b = baseNormalize(text);
  if (!latin) return b;
  return stripDiacritics(b).replace(CHAT_RE, (m) => CHAT_SPEAK[m]);
}
export {
  baseNormalize,
  normalizeFor,
  stripDiacritics
};
