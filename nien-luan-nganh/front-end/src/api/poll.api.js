import { http } from './http'

/**
 * API: BÌNH CHỌN (POLL TRONG PHÒNG CHAT)
 */

// Lấy danh sách bình chọn của phòng
export const getRoomPolls = async (roomId) => {
  const res = await http.get(`/room/${roomId}/polls`)
  return res.data?.data ?? res.data?.result ?? res.data ?? []
}

// Lấy chi tiết 1 bình chọn
export const getPollDetail = async (roomId, pollId) => {
  const res = await http.get(`/room/${roomId}/polls/${pollId}`)
  return res.data?.data ?? res.data?.result ?? res.data
}

// Tạo bình chọn mới
export const createPoll = async (roomId, data) => {
  const res = await http.post(`/room/${roomId}/polls`, data)
  return res.data?.data ?? res.data?.result ?? res.data
}

// Thêm lựa chọn mới vào bình chọn đang mở
export const addPollOption = async (roomId, pollId, optionText) => {
  const res = await http.post(`/room/${roomId}/polls/${pollId}/options`, { optionText })
  return res.data?.data ?? res.data?.result ?? res.data
}

// Bỏ phiếu / Thay đổi phiếu bầu (optionIds: Array<UUID>)
export const votePoll = async (roomId, pollId, optionIds) => {
  const res = await http.put(`/room/${roomId}/polls/${pollId}/vote`, { optionIds })
  return res.data?.data ?? res.data?.result ?? res.data
}

// Kết thúc / Khóa bình chọn (chỉ creator hoặc admin)
export const closePoll = async (roomId, pollId) => {
  const res = await http.put(`/room/${roomId}/polls/${pollId}/close`)
  return res.data?.data ?? res.data?.result ?? res.data
}

