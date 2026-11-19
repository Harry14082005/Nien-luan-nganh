import { http } from './http'

/**
 * API: GỢI Ý NỘI DUNG TỪ AI (POLL & REMINDER)
 */

/**
 * Gọi AI gợi ý nội dung (POLL hoặc REMINDER) từ tin nhắn anchor và ngữ cảnh hội thoại
 * @param {string} roomId 
 * @param {'POLL' | 'REMINDER'} type 
 * @param {string} anchorMessageId 
 */
export const getAiSuggestions = async (roomId, type, anchorMessageId) => {
  const res = await http.post(`/rooms/${roomId}/ai/suggestions`, {
    type,
    anchorMessageId,
  })
  return res.data?.data ?? res.data?.result ?? res.data
}

