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

/**
 * Lấy TẤT CẢ cuộc bình chọn của người dùng từ tất cả các phòng chat
 * @param {{ openOnly?: boolean }} options
 */
export const getAllMyPolls = async (options = {}) => {
  // Lấy danh sách các phòng
  const roomsRes = await http.get('/rooms', { params: { pageNo: 0, pageSize: 50 } })
  const roomsData = roomsRes.data?.data ?? roomsRes.data
  const rooms = Array.isArray(roomsData)
    ? roomsData
    : roomsData?.content ?? []

  if (!rooms.length) return []

  // Gọi song song lấy polls từng phòng
  const results = await Promise.allSettled(
    rooms.map((room) =>
      http
        .get(`/room/${room.roomId}/polls`)
        .then((res) => {
          const list = res.data?.data ?? res.data?.result ?? res.data ?? []
          return (Array.isArray(list) ? list : []).map((p) => ({
            ...p,
            roomId: room.roomId,
            roomName: room.roomName,
            roomAvatarURL: room.roomAvatarURL,
          }))
        })
        .catch(() => []),
    ),
  )

  let all = results.flatMap((r) => (r.status === 'fulfilled' ? r.value : []))

  // Lọc bình chọn đang mở nếu cần
  if (options.openOnly) {
    all = all.filter((p) => p.isActive === true)
  }

  // Sắp xếp mới nhất lên đầu
  all.sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))

  return all
}


