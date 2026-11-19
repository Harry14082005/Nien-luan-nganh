import { scorePoll, POLL_THRESHOLD } from "./pollDetector.js";
import { detectPollFromConversation } from "./conversation.js";
function isEligibleForPollButton(m, o = {}) {
  const { now = Date.now(), maxAgeMs = 36e5, usedAsPollSource } = o;
  return m.type === "TEXT" && !m.revoked && now - m.createdAt <= maxAgeMs && !usedAsPollSource?.has(m.id);
}
export {
  POLL_THRESHOLD,
  detectPollFromConversation,
  isEligibleForPollButton,
  scorePoll
};
